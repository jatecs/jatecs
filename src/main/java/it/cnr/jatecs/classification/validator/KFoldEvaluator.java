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

package it.cnr.jatecs.classification.validator;

import gnu.trove.TIntArrayList;
import gnu.trove.TIntHashSet;
import gnu.trove.TShortArrayList;
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.interfaces.IClassifierRuntimeCustomizer;
import it.cnr.jatecs.classification.interfaces.ILearner;
import it.cnr.jatecs.classification.interfaces.ILearnerRuntimeCustomizer;
import it.cnr.jatecs.classification.module.Classifier;
import it.cnr.jatecs.evaluation.ClassificationComparer;
import it.cnr.jatecs.evaluation.ContingencyTable;
import it.cnr.jatecs.evaluation.ContingencyTableSet;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveMainIndexBuilder;
import it.cnr.jatecs.utils.IOperationStatusListener;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.Pair;
import it.cnr.jatecs.utils.iterators.FilteredIntIterator;
import it.cnr.jatecs.utils.iterators.TIntArrayListIterator;
import it.cnr.jatecs.utils.iterators.TShortArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.util.ArrayList;

public class KFoldEvaluator {

    protected ILearner _usedLearner;

    protected ILearnerRuntimeCustomizer _learnerCustomizer;
    protected IClassifierRuntimeCustomizer _classifierCustomizer;
    protected int _percentageToUse;

    /**
     * The k value for K-fold validation.
     */
    protected int _k;


    public KFoldEvaluator(ILearner learner, ILearnerRuntimeCustomizer learnerCustomizer,
                          IClassifierRuntimeCustomizer classifierCustomizer) {
        _usedLearner = learner;
        _learnerCustomizer = learnerCustomizer;
        _classifierCustomizer = classifierCustomizer;
        _k = 5;
        _percentageToUse = 100;
    }

    public static Pair<IIndex, IIndex> splitIndex(int step, IIndex index, short catID, int validationSteps) {

        int numPositives = index.getClassificationDB().getCategoryDocumentsCount(catID);

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

        int numTrainingDocuments = (index.getDocumentDB().getDocumentsCount() * numPositivesInTraining) / numPositives;
        int numValidationDocuments = index.getDocumentDB().getDocumentsCount() - numTrainingDocuments;
        if (numPositives == 1) {
            numTrainingDocuments = (index.getDocumentDB().getDocumentsCount() * 70) / 100;
            numValidationDocuments = index.getDocumentDB().getDocumentsCount() - numTrainingDocuments;
        }

        // Select positives for training and validation.
        TIntArrayList tr = new TIntArrayList();
        TIntArrayList va = new TIntArrayList();

        TIntArrayList catsPositives = new TIntArrayList();
        IIntIterator positives = index.getClassificationDB().getCategoryDocuments(catID);
        while (positives.hasNext()) {
            catsPositives.add(positives.next());
        }
        catsPositives.sort();

        int docID = (index.getDocumentDB().getDocumentsCount() / validationSteps) * step;
        int numValidationToReach = numValidationDocuments - numPositivesInValidation;
        while (numValidationToReach > 0) {
            if (catsPositives.contains(docID)) {
                docID = ((docID + 1) % index.getDocumentDB().getDocumentsCount());
                continue;
            }
            va.add(docID);
            docID = ((docID + 1) % index.getDocumentDB().getDocumentsCount());
            numValidationToReach--;
        }

        int numTrainingToReach = numTrainingDocuments - numPositivesInTraining;
        while (numTrainingToReach > 0) {
            if (catsPositives.contains(docID)) {
                docID = ((docID + 1) % index.getDocumentDB().getDocumentsCount());
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


        JatecsLogger.status().print("Computing the training and validation sets for category " + index.getCategoryDB().getCategoryName(catID) + "...");

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

        assert (trIndex.getClassificationDB().getCategoryDocumentsCount((short) 0) == numPositivesInTraining);
        assert (vaIndex.getClassificationDB().getCategoryDocumentsCount((short) 0) == numPositivesInValidation);


        JatecsLogger.status().println("done. The training contains " + tr.size() + " document(s) and the validation contains " + va.size() + " document(s).");

        Pair<IIndex, IIndex> ret = new Pair<IIndex, IIndex>(trIndex, vaIndex);
        return ret;
    }

    public void setKFoldValue(int k) {
        _k = k;
    }

    protected void integrateTestIndex(int step, IIndex idx, IIndex originalIndex, TIntArrayList docs, int numTotalSteps) {
        if (docs.size() == 0)
            return;

        String catName = idx.getCategoryDB().getCategoryName((short) 0);

        int positive = idx.getClassificationDB().getCategoryDocumentsCount((short) 0);
        if (positive <= numTotalSteps)
            numTotalSteps = positive;
        if (numTotalSteps == 0)
            numTotalSteps = 1;

        int numDocs = docs.size();
        int perStep = numDocs / numTotalSteps;
        if (numDocs % numTotalSteps != 0)
            perStep++;

        TroveMainIndexBuilder builder = new TroveMainIndexBuilder(idx);
        int startVal = perStep * step;
        int endVal = (startVal + perStep) <= docs.size() ? startVal + perStep : docs.size();
        for (int i = startVal; i < endVal; i++) {
            int docID = docs.get(i);
            String docName = originalIndex.getDocumentDB().getDocumentName(docID);
            docName += "_" + System.currentTimeMillis();

            // Prepare categories.
            ArrayList<String> catsToInsert = new ArrayList<String>();
            IShortIterator cats = originalIndex.getClassificationDB().getDocumentCategories(docID);
            while (cats.hasNext()) {
                short curCatID = cats.next();
                String curCatName = originalIndex.getCategoryDB().getCategoryName(curCatID);
                if (curCatName.equals(catName))
                    catsToInsert.add(curCatName);
            }

            // Prepare features.
            ArrayList<String> featsToInsert = new ArrayList<String>();
            IIntIterator feats = originalIndex.getContentDB().getDocumentFeatures(docID);
            while (feats.hasNext()) {
                int featID = feats.next();
                featsToInsert.add(originalIndex.getFeatureDB().getFeatureName(featID));
            }

            builder.addDocument(docName, featsToInsert.toArray(new String[0]), catsToInsert.toArray(new String[0]));
        }


        JatecsLogger.status().println("After integration the validation contain " + idx.getDocumentDB().getDocumentsCount() + " document(s)");
    }

    public ContingencyTableSet evaluate(IIndex index, IOperationStatusListener status) {
        // Compute a subview of the index.
        Pair<IIndex, TIntArrayList> pair = getIndexSubview(index, _percentageToUse);
        IIndex trainingIndex = pair.getFirst();

        ContingencyTableSet globalCT = new ContingencyTableSet();
        IShortIterator cats = trainingIndex.getCategoryDB().getCategories();


        int numComputed = 0;
        int toCompute = trainingIndex.getCategoryDB().getCategoriesCount();


        int count = 1;
        cats.begin();
        while (cats.hasNext()) {
            short catID = cats.next();

            TShortArrayList internalCategories = new TShortArrayList();
            internalCategories.add((short) 0);
            TShortArrayList externalCategories = new TShortArrayList();
            externalCategories.add(catID);

            JatecsLogger.status().println("" + count + ". Begin optimization of category " + trainingIndex.getCategoryDB().getCategoryName(catID));
            count++;

            ContingencyTable curCT = new ContingencyTable(trainingIndex.getCategoryDB().getCategoryName(catID));


            for (int i = 0; i < _k; i++) {
                JatecsLogger.status().println("Doing validation " + (i + 1) + "/" + _k + "...");

                // Split the index in two parts: a training and a validation set.
                Pair<IIndex, IIndex> indexes = splitIndex(i, trainingIndex, catID, _k);
                if (indexes == null)
                    // All possible steps was done.
                    break;

                // In the case of computing accuracy over a subset of the original index,
                // we make train over the subset of documents but we test all documents
                // available on the original index.
                integrateTestIndex(i, indexes.getSecond(), index, pair.getSecond(), _k);

                // Build learner with training.
                _usedLearner.setRuntimeCustomizer(_learnerCustomizer);
                IClassifier cl = _usedLearner.build(indexes.getFirst());

                // Set the wanted classifier runtime customizer.
                cl.setRuntimeCustomizer(_classifierCustomizer);

                // Test learner with testing.
                Classifier classifier = new Classifier(indexes.getSecond(), cl);
                classifier.exec();
                ClassificationComparer cc = new ClassificationComparer(classifier.getClassificationDB(), indexes.getSecond().getClassificationDB());
                ContingencyTableSet tableSet = cc.evaluate();
                ContingencyTable ct = tableSet.getCategoryContingencyTable((short) 0);

                curCT.setFN(curCT.fn() + ct.fn());
                curCT.setFP(curCT.fp() + ct.fp());
                curCT.setTN(curCT.tn() + ct.tn());
                curCT.setTP(curCT.tp() + ct.tp());
            }

            globalCT.addContingenyTable(catID, curCT);

            numComputed++;
            double percentage = ((double) (numComputed * 100)) / ((double) (toCompute));
            if (status != null)
                status.operationStatus(percentage);

        }

        if (status != null)
            status.operationStatus(100);


        return globalCT;
    }

    public void setPercentageToUse(int percentage) {
        _percentageToUse = percentage;
    }

    protected Pair<IIndex, TIntArrayList> getIndexSubview(IIndex index, int percentage) {
        if (percentage == 100)
            return new Pair<IIndex, TIntArrayList>(index, new TIntArrayList());

        TIntHashSet map = new TIntHashSet();

        // Select a percentage subset of documents for each category.
        IShortIterator cats = index.getCategoryDB().getCategories();
        while (cats.hasNext()) {
            short catID = cats.next();

            int numToSelect = (index.getClassificationDB().getCategoryDocumentsCount(catID) * percentage) / 100;
            if ((index.getClassificationDB().getCategoryDocumentsCount(catID) * percentage) % 100 != 0)
                numToSelect++;
            IIntIterator docs = index.getClassificationDB().getCategoryDocuments(catID);
            while (numToSelect > 0) {
                int docID = docs.next();
                if (!map.contains(docID))
                    map.add(docID);
                numToSelect--;
            }
        }

        // Select documents with no categories.
        IIntIterator docs = index.getDocumentDB().getDocuments();
        int selected = 0;
        while (docs.hasNext()) {
            int docID = docs.next();
            if (index.getClassificationDB().getDocumentCategoriesCount(docID) == 0) {
                selected++;
            }
        }
        int numToSelect = (selected * percentage) / 100;
        if ((selected * percentage) % 100 != 0)
            numToSelect++;
        docs.begin();
        while (numToSelect > 0) {
            int docID = docs.next();
            if (!map.contains(docID))
                map.add(docID);
            numToSelect--;
        }


        IIndex idx = index.cloneIndex();
        FilteredIntIterator removedDocuments = new FilteredIntIterator(idx.getDocumentDB().getDocuments(), map, true);
        idx.removeDocuments(removedDocuments, false);

        JatecsLogger.status().info("Original document(s) in index: " + index.getDocumentDB().getDocumentsCount() + " Selected " +
                "a subview containing " + idx.getDocumentDB().getDocumentsCount() + " document(s).");

        TIntArrayList ar = new TIntArrayList();
        removedDocuments.begin();
        while (removedDocuments.hasNext()) {
            int docID = removedDocuments.next();
            ar.add(docID);
        }

        Pair<IIndex, TIntArrayList> pair = new Pair<IIndex, TIntArrayList>(idx, ar);
        return pair;
    }


}
