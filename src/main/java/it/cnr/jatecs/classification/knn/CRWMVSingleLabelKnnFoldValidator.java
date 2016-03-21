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

package it.cnr.jatecs.classification.knn;

import gnu.trove.TIntArrayList;
import gnu.trove.TIntIntHashMap;
import it.cnr.jatecs.classification.BaseLearner;
import it.cnr.jatecs.classification.ClassificationResult;
import it.cnr.jatecs.classification.OptimalConfiguration;
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.interfaces.ILearnerRuntimeCustomizer;
import it.cnr.jatecs.classification.validator.KFoldRuntimeCustomizer;
import it.cnr.jatecs.evaluation.ClassificationComparer;
import it.cnr.jatecs.evaluation.ConfusionMatrix;
import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDBBuilder;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveClassificationDBBuilder;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.Pair;
import it.cnr.jatecs.utils.iterators.TIntArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.util.Vector;

public class CRWMVSingleLabelKnnFoldValidator extends BaseLearner {

//	private static class SplitOperation {
//		IIndex training;
//		IIndex test;
//		float[][] similarityMatrix;
//	}

    protected CRWMVSingleLabelKnnCommitteeClassifier _classifier;
    protected float[][] _globalSimMatrix;

    public CRWMVSingleLabelKnnFoldValidator(
            CRWMVSingleLabelKnnCommitteeClassifier classifier) {
        _classifier = classifier;
        _globalSimMatrix = null;
        _customizer = new KFoldRuntimeCustomizer();
    }

    protected static int findMinimumNumberOfPositives(IIndex index) {
        int minimum = Integer.MAX_VALUE;
        IShortIterator cats = index.getCategoryDB().getCategories();
        while (cats.hasNext()) {
            short catID = cats.next();
            int docs = index.getClassificationDB().getCategoryDocumentsCount(
                    catID);
            if (docs < minimum) {
                minimum = docs;
            }
        }

        return minimum;
    }

    public static Pair<IIndex, IIndex> splitIndex(int step, IIndex index,
                                                  int numValidationSteps) {
        int numPositives = index.getDocumentDB().getDocumentsCount();

        int numSteps = Math.min(numPositives, numValidationSteps);
        if (step >= numSteps)
            return null;

        TIntArrayList tr = new TIntArrayList();
        TIntArrayList va = new TIntArrayList();

        int numPositivesInValidation = numPositives / numSteps;
        int numPositivesInTraining = numPositives - numPositivesInValidation;
        int startTrainingID = (numPositives / numSteps) * step;
        int endTrainingID = (startTrainingID + numPositivesInTraining - 1);
        TIntIntHashMap map = new TIntIntHashMap();
        for (int i = startTrainingID; i <= endTrainingID; i++) {
            int v = i % numPositives;
            map.put(v, v);
        }

        int curDoc = 0;
        IIntIterator docs = index.getDocumentDB().getDocuments();
        while (docs.hasNext()) {
            int docID = docs.next();
            if (map.containsKey(curDoc)) {
                tr.add(docID);
            } else {
                va.add(docID);
            }
            curDoc++;
        }

        tr.sort();
        va.sort();

        IIndex trIndex = index.cloneIndex();
        trIndex.removeDocuments(new TIntArrayListIterator(va), false);

        IIndex vaIndex = index.cloneIndex();
        vaIndex.removeDocuments(new TIntArrayListIterator(tr), false);

        JatecsLogger.status().println(
                "done. The training contains " + tr.size()
                        + " document(s) and the validation contains "
                        + va.size() + " document(s).");

        Pair<IIndex, IIndex> ret = new Pair<IIndex, IIndex>(trIndex, vaIndex);
        return ret;

    }

    public IClassifier build(IIndex trainingIndex) {
        return buildNotTextual(trainingIndex);
    }

    protected OptimalConfiguration optimizeFor(
            CRWMVSingleLabelKnnCommitteeClassifier classifier, IIndex training,
            IIndex validation, int minimumK, int maximumK, int stepK) {


        SingleLabelKnnCommitteeClassifierCustomizer cust = (SingleLabelKnnCommitteeClassifierCustomizer) classifier
                .getRuntimeCustomizer().cloneObject();

        JatecsLogger.status().println("Optiming parameters...");

        int bestK = -1;
        double bestEffectiveness = -Double.MAX_VALUE;

        for (int currentK = minimumK; currentK <= maximumK; currentK += stepK) {

            SingleLabelKnnCommitteeClassifierCustomizer cu = (SingleLabelKnnCommitteeClassifierCustomizer) cust
                    .cloneObject();

            cu.setNumSimilar(currentK);

            classifier.setRuntimeCustomizer(cu);

            ClassificationResult[] results = new ClassificationResult[validation
                    .getDocumentDB().getDocumentsCount()];
            IIntIterator docs = validation.getDocumentDB().getDocuments();
            while (docs.hasNext()) {
                int docID = docs.next();

                // Classify validation documents using current
                // configuration.
                ClassificationResult res = classifier.classify(validation,
                        docID);
                results[docID] = res;
            }

            IClassificationDBBuilder builder = new TroveClassificationDBBuilder(
                    validation.getDocumentDB(), validation
                    .getCategoryDB());

            for (int j = 0; j < results.length; j++) {
                ClassificationResult res = results[j];

                builder.setDocumentCategory(res.documentID, res.categoryID
                        .get(0));
            }

            ClassificationComparer cc = new ClassificationComparer(builder
                    .getClassificationDB(), validation
                    .getClassificationDB());
            ConfusionMatrix cm = cc.evaluateSingleLabel();
            if (cm.getAccuracy() > bestEffectiveness) {
                bestEffectiveness = cm.getAccuracy();
                bestK = currentK;
            }

        }

        System.out.println("The best KNN found condifguration is k="
                + bestK);

        cust.setNumSimilar(bestK);
        // cust.setEfficacy(catID, bestEffectiveness);

        OptimalConfiguration conf = new OptimalConfiguration();
        conf.learnerCustomizer = null;
        conf.classifierCustomizer = cust;

        return conf;
    }

    protected IClassifier buildNotTextual(IIndex trainingIndex) {
        KFoldRuntimeCustomizer cust = (KFoldRuntimeCustomizer) getRuntimeCustomizer();

        SingleLabelKnnCommitteeClassifierCustomizer customizer = (SingleLabelKnnCommitteeClassifierCustomizer) _classifier
                .getRuntimeCustomizer();
        boolean sameIndexesOld = customizer._searcher.useSameIndexesData();
        customizer._searcher.setUseSameIndexesData(true);

        Vector<OptimalConfiguration> v = new Vector<OptimalConfiguration>();

        for (int i = 0; i < cust.getKFoldValidationSteps(); i++) {
            JatecsLogger.status().println(
                    "Doing validation " + (i + 1) + "/"
                            + cust.getKFoldValidationSteps() + "...");

            // Split the index in two parts: a training and a valiudation
            // set.
            Pair<IIndex, IIndex> indexes = splitIndex(i, trainingIndex,
                    cust.getKFoldValidationSteps());
            if (indexes == null)
                // All possible steps was done.
                break;

            // Optimize classifier.
            OptimalConfiguration res = optimizeFor(_classifier, indexes
                    .getFirst(), indexes.getSecond(), 1, 33, 2);

            // Keep track of current result.
            v.add(res);

        }

        customizer._searcher.setUseSameIndexesData(sameIndexesOld);

        // Choose optimal K.
        int kAvg = 0;
        for (int i = 0; i < v.size(); i++) {
            OptimalConfiguration conf = v.get(i);
            SingleLabelKnnCommitteeClassifierCustomizer cu = (SingleLabelKnnCommitteeClassifierCustomizer) conf.classifierCustomizer;
            kAvg += cu.getNumSimilar();
        }

        kAvg /= v.size();

        System.out.println("Selected optimal value for k = " + kAvg);

        customizer.setNumSimilar(kAvg);
        customizer._searcher.setUseSameIndexesData(sameIndexesOld);

        return _classifier;

    }

    @Override
    public ILearnerRuntimeCustomizer getRuntimeCustomizer(short catID) {
        return null;
    }

    @Override
    public IClassifier mergeClassifiers(Vector<IClassifier> classifiers) {
        return null;
    }

    @Override
    public void setRuntimeCustomizer(
            Vector<ILearnerRuntimeCustomizer> customizers) {

    }

}
