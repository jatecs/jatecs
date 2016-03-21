/*
 * This file is part of JaTeCS.
 *
 * JaTeCS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JaTeCS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JaTeCS.  If not, see <http://www.gnu.org/licenses/>.
 *
 * The software has been mainly developed by (in alphabetical order):
 * - Andrea Esuli (andrea.esuli@isti.cnr.it)
 * - Tiziano Fagni (tiziano.fagni@isti.cnr.it)
 * - Alejandro Moreo Fernández (alejandro.moreo@isti.cnr.it)
 * Other past contributors were:
 * - Giacomo Berardi (giacomo.berardi@isti.cnr.it)
 */

package it.cnr.jatecs.classification.cotraining;

import gnu.trove.*;
import it.cnr.jatecs.classification.ClassificationResult;
import it.cnr.jatecs.classification.ClassifierRange;
import it.cnr.jatecs.classification.ThresholdOptimizerType;
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.knn.IKnnClassifierCustomizer;
import it.cnr.jatecs.classification.module.Classifier;
import it.cnr.jatecs.evaluation.ClassificationComparer;
import it.cnr.jatecs.evaluation.ContingencyTable;
import it.cnr.jatecs.evaluation.ContingencyTableSet;
import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDB;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveMainIndexBuilder;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.Pair;
import it.cnr.jatecs.utils.iterators.FilteredShortIterator;
import it.cnr.jatecs.utils.iterators.TIntArrayListIterator;
import it.cnr.jatecs.utils.iterators.TShortArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.util.ArrayList;

/**
 * Support to cotraining experiments
 *
 * @author Tiziano Fagni
 */
public class KnnCoTrainerValidator {

    private double _minConfidence;
    private double _maxConfidence;
    private double _incConfidence;
    private ThresholdOptimizerType _optType;
    private int _validationSteps;
    public KnnCoTrainerValidator() {
        _minConfidence = 0.4;
        _maxConfidence = 0.7;
        _incConfidence = 0.05;
        _optType = ThresholdOptimizerType.F1;
        _validationSteps = 5;
    }

    public void setThresholds(double minConfidence, double maxConfidence,
                              double step) {
        _minConfidence = minConfidence;
        _maxConfidence = maxConfidence;
        _incConfidence = step;
    }

    public ArrayList<CotrainOutputData> cotrain(IIndex index,
                                                ArrayList<CotrainInputData> classifiers) {
        ArrayList<CotrainOutputData> out = new ArrayList<CotrainOutputData>();

        IShortIterator cats = index.getCategoryDB().getCategories();

        for (int curCl = 0; curCl < classifiers.size(); curCl++) {
            CotrainInputData cid = classifiers.get(curCl);
            CotrainOutputData cod = new CotrainOutputData();
            cod.catsThreshold = new TDoubleArrayList();
            int count = 1;
            cats.begin();
            while (cats.hasNext()) {
                short catID = cats.next();

                TShortArrayList internalCategories = new TShortArrayList();
                internalCategories.add((short) 0);
                TShortArrayList externalCategories = new TShortArrayList();
                externalCategories.add(catID);

                JatecsLogger.status().println(
                        "" + count + ". Begin optimization of category "
                                + index.getCategoryDB().getCategoryName(catID));
                count++;

                double avgThreshold = 0;
                int numComputation = 0;

                for (int i = 0; i < _validationSteps; i++) {
                    JatecsLogger.status().println(
                            "Doing validation " + (i + 1) + "/"
                                    + _validationSteps + "...");

                    // Split the index in two parts: a training and a
                    // valiudation set.
                    Pair<IIndex, IIndex> indexes = splitIndex(i, index, catID,
                            _validationSteps);
                    if (indexes == null)
                        // All possible steps was done.
                        break;

                    // Build learning model for current category and split and
                    // for each classifier.
                    ArrayList<IClassifier> srcClassifiers = new ArrayList<IClassifier>();
                    for (int z = 0; z < classifiers.size(); z++) {
                        CotrainInputData id = classifiers.get(z);
                        if (id == cid)
                            continue;

                        id.learner.setRuntimeCustomizer(id.learnerCustomizer);
                        IClassifier classifier = id.learner.build(indexes
                                .getFirst());
                        if (id.classifierCustomizer instanceof IKnnClassifierCustomizer) {
                            IKnnClassifierCustomizer c = (IKnnClassifierCustomizer) id.classifierCustomizer;

                            classifier.setRuntimeCustomizer(c
                                    .getCustomizerForCat(catID));
                        } else
                            classifier
                                    .setRuntimeCustomizer(id.classifierCustomizer);

                        srcClassifiers.add(classifier);
                    }

                    // Cotrain this configuration.
                    double threshold = cotrainCategory(indexes.getFirst(),
                            indexes.getSecond(), cid, (short) 0, srcClassifiers);

                    avgThreshold += threshold;
                    numComputation++;
                }

                // Compute best threshold for current category.
                avgThreshold /= numComputation;

                cod.catsThreshold.add(avgThreshold);

                System.out.println("Classifier " + (curCl + 1)
                        + " best threshold: " + avgThreshold);
            }

            // Save current results.
            out.add(cod);
        }

        return out;
    }

    protected double cotrainCategory(IIndex trainingAllCats,
                                     IIndex testAllCats, CotrainInputData cid, short catID,
                                     ArrayList<IClassifier> srcCl) {
        double bestEffectiveness = -Double.MAX_VALUE;
        double bestConfidence = _minConfidence;

        // Generate base training index.
        IIndex training = generateIndex(trainingAllCats, catID);
        IIndex test = generateIndex(testAllCats, catID);

        for (double confidence = _minConfidence; confidence <= _maxConfidence; confidence += _incConfidence) {
            // Select the most confident documents.
            TIntIntHashMap map = selectConfidentDocuments(testAllCats, srcCl,
                    catID, confidence);

            System.out.println("Selected " + map.size()
                    + " docs to add to current training data.");
            if (map.size() == 0)
                continue;

            // Generate enriched training.
            IIndex curTraining = createEnrichedTraining(map, testAllCats,
                    training, catID);

            // Generate current classifier.
            cid.learner.setRuntimeCustomizer(cid.learnerCustomizer);
            IClassifier curCl = cid.learner.build(curTraining);
            curCl.setRuntimeCustomizer(cid.classifierCustomizer);

            // Evaluate it in test documents.
            double effectiveness = evaluateClassifier(curCl, test);

            if (effectiveness > bestEffectiveness) {
                bestEffectiveness = effectiveness;
                bestConfidence = confidence;
            }
        }

        return bestConfidence;
    }

    protected double evaluateClassifier(IClassifier curCl, IIndex test) {
        Classifier classifier = new Classifier(test, curCl);
        classifier.exec();
        IClassificationDB predictedDB = classifier.getClassificationDB();
        ClassificationComparer cc = new ClassificationComparer(predictedDB,
                test.getClassificationDB());
        ContingencyTableSet tableSet = cc.evaluate();
        ContingencyTable ct = tableSet.getGlobalContingencyTable();
        if (_optType == ThresholdOptimizerType.ACCURACY)
            return ct.accuracy();
        else if (_optType == ThresholdOptimizerType.F1)
            return ct.f1();
        else if (_optType == ThresholdOptimizerType.PRECISION)
            return ct.precision();
        else if (_optType == ThresholdOptimizerType.RECALL)
            return ct.recall();
        else
            throw new RuntimeException("Invalid threhold optimization type: "
                    + _optType);
    }

    protected IIndex createEnrichedTraining(TIntIntHashMap map,
                                            IIndex testAllCats, IIndex training, short catID) {
        IIndex idx = training.cloneIndex();
        TroveMainIndexBuilder builder = new TroveMainIndexBuilder(idx);

        String catName = testAllCats.getCategoryDB().getCategoryName(catID);

        int[] keys = map.keys();
        for (int i = 0; i < keys.length; i++) {
            int docID = keys[i];
            ArrayList<String> features = new ArrayList<String>(100);
            String docName = testAllCats.getDocumentDB().getDocumentName(docID);

            IIntIterator feats = testAllCats.getContentDB()
                    .getDocumentFeatures(docID);
            while (feats.hasNext()) {
                int featID = feats.next();
                String featName = testAllCats.getFeatureDB().getFeatureName(
                        featID);
                int count = testAllCats.getContentDB()
                        .getDocumentFeatureFrequency(docID, featID);
                for (int j = 0; j < count; j++)
                    features.add(featName);
            }

            String[] categories = new String[0];
            if (map.get(docID) == 1) {
                categories = new String[1];
                categories[0] = catName;
            }

            builder.addDocument(docName, features.toArray(new String[0]),
                    categories);
        }

        return idx;
    }

    protected TIntIntHashMap selectConfidentDocuments(IIndex test,
                                                      ArrayList<IClassifier> srcCl, short catID, double confidence) {
        TIntObjectHashMap<DocAgreement> ret = new TIntObjectHashMap<DocAgreement>();

        for (int i = 0; i < srcCl.size(); i++) {
            IClassifier cl = srcCl.get(i);
            ClassifierRange crange = cl.getClassifierRange(catID);
            ClassificationResult[] results = cl.classify(test, catID);
            for (int j = 0; j < results.length; j++) {
                ClassificationResult cr = results[j];
                double val = cr.score.get(0) - crange.border;
                int sign = +1;
                if (val >= 0) {
                    double interval = crange.maximum - crange.border;
                    double curValue = val;
                    if (interval != 0)
                        val = curValue / interval;
                    else
                        val = 1;
                    sign = 1;
                } else {
                    double interval = crange.minimum - crange.border;
                    double curValue = val;
                    if (interval != 0)
                        val = -curValue / interval;
                    else
                        val = -1;

                    val = -val;
                    sign = -1;
                }

                if (val >= confidence) {
                    if (!ret.containsKey(j))
                        ret.put(j, new DocAgreement());
                    DocAgreement da = (DocAgreement) ret.get(j);
                    if (sign == 1)
                        da.positives++;
                    else
                        da.negatives++;
                }
            }
        }

        TIntIntHashMap toReturn = new TIntIntHashMap();
        int[] keys = ret.keys();
        for (int i = 0; i < keys.length; i++) {
            int docID = keys[i];
            DocAgreement da = (DocAgreement) ret.get(docID);
            if (da.negatives == 0 || da.positives == 0) {
                int positive = da.negatives == 0 ? 1 : 0;
                toReturn.put(docID, positive);
            }
        }

        return toReturn;
    }

    protected Pair<IIndex, IIndex> splitIndex(int step, IIndex index,
                                              short catID, int validationSteps) {

        int numPositives = index.getClassificationDB()
                .getCategoryDocumentsCount(catID);

        int numPositivesInValidation = 0;
        int numPositivesInTraining = 0;

        if (numPositives < validationSteps) {
            if ((step + 1) > numPositives)
                return null;
            else {
                numPositivesInValidation = 1;
                numPositivesInTraining = numPositives - 1;

                if (numPositivesInTraining == 0) {
                    numPositivesInTraining = 1;
                    numPositivesInValidation = 0;
                }
            }
        } else {
            numPositivesInValidation = numPositives / validationSteps;
            numPositivesInTraining = numPositives - numPositivesInValidation;
        }

        int numTrainingDocuments = (index.getDocumentDB().getDocumentsCount() * numPositivesInTraining)
                / numPositives;
        int numValidationDocuments = index.getDocumentDB().getDocumentsCount()
                - numTrainingDocuments;
        if (numPositives == 1) {
            numTrainingDocuments = (index.getDocumentDB().getDocumentsCount() * 70) / 100;
            numValidationDocuments = index.getDocumentDB().getDocumentsCount()
                    - numTrainingDocuments;
        }

        // Select positives for training and validation.
        TIntArrayList tr = new TIntArrayList();
        TIntArrayList va = new TIntArrayList();

        TIntArrayList catsPositives = new TIntArrayList();
        IIntIterator positives = index.getClassificationDB()
                .getCategoryDocuments(catID);
        while (positives.hasNext()) {
            catsPositives.add(positives.next());
        }
        catsPositives.sort();

        int docID = (index.getDocumentDB().getDocumentsCount() / validationSteps)
                * step;
        int numValidationToReach = numValidationDocuments
                - numPositivesInValidation;
        while (numValidationToReach > 0) {
            if (catsPositives.contains(docID)) {
                docID = ((docID + 1) % index.getDocumentDB()
                        .getDocumentsCount());
                continue;
            }
            va.add(docID);
            docID = ((docID + 1) % index.getDocumentDB().getDocumentsCount());
            numValidationToReach--;
        }

        int numTrainingToReach = numTrainingDocuments - numPositivesInTraining;
        while (numTrainingToReach > 0) {
            if (catsPositives.contains(docID)) {
                docID = ((docID + 1) % index.getDocumentDB()
                        .getDocumentsCount());
                continue;
            }
            tr.add(docID);
            docID = ((docID + 1) % index.getDocumentDB().getDocumentsCount());
            numTrainingToReach--;
        }

        // Next add all positives

        positives.begin();
        int count = 0;
        int startV, stopV;
        startV = numPositivesInValidation * step;
        stopV = (numPositivesInValidation * (step + 1)) - 1;
        while (positives.hasNext()) {
            int doc = positives.next();
            if (count >= startV && count <= stopV)
                va.add(doc);
            else
                tr.add(doc);

            count++;
        }
        tr.sort();
        va.sort();

        // DEBUG
        for (int i = 0; i < tr.size(); i++) {
            int d = tr.get(i);
            assert (!va.contains(d));
        }
        for (int i = 0; i < va.size(); i++) {
            int d = va.get(i);
            assert (!tr.contains(d));
        }

        JatecsLogger.status().print(
                "Computing the training and validation sets for category "
                        + index.getCategoryDB().getCategoryName(catID) + "...");

        TShortArrayList catsToRemove = new TShortArrayList();
        IShortIterator itCats = index.getCategoryDB().getCategories();
        while (itCats.hasNext()) {
            short cat = itCats.next();
            if (cat == catID)
                continue;

            catsToRemove.add(cat);
        }

        IIndex trIndex = index.cloneIndex();
        trIndex.removeCategories(new TShortArrayListIterator(catsToRemove));
        trIndex.removeDocuments(new TIntArrayListIterator(va), false);

        IIndex vaIndex = index.cloneIndex();
        vaIndex.removeCategories(new TShortArrayListIterator(catsToRemove));
        vaIndex.removeDocuments(new TIntArrayListIterator(tr), false);

        assert (trIndex.getClassificationDB().getCategoryDocumentsCount(
                (short) 0) == numPositivesInTraining);
        assert (vaIndex.getClassificationDB().getCategoryDocumentsCount(
                (short) 0) == numPositivesInValidation);

        JatecsLogger.status().println(
                "done. The training contains " + tr.size()
                        + " document(s) and the validation contains "
                        + va.size() + " document(s).");

        Pair<IIndex, IIndex> ret = new Pair<IIndex, IIndex>(trIndex, vaIndex);
        return ret;
    }

    protected IIndex generateIndex(IIndex index, short catID) {
        IIndex idx = index.cloneIndex();

        IShortIterator allCats = idx.getCategoryDB().getCategories();
        TShortArrayList validCats = new TShortArrayList();
        validCats.add(catID);
        FilteredShortIterator toRemove = new FilteredShortIterator(allCats,
                new TShortArrayListIterator(validCats), true);
        idx.removeCategories(toRemove);
        return idx;
    }

    private class DocAgreement {
        public int positives = 0;
        public int negatives = 0;
    }

}
