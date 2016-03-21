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

package it.cnr.jatecs.classification.adaboost;

import gnu.trove.TIntArrayList;
import gnu.trove.TShortArrayList;
import it.cnr.jatecs.classification.BaseLearner;
import it.cnr.jatecs.classification.ClassificationResult;
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.interfaces.IClassifierRuntimeCustomizer;
import it.cnr.jatecs.classification.interfaces.ILearner;
import it.cnr.jatecs.classification.interfaces.ILearnerRuntimeCustomizer;
import it.cnr.jatecs.classification.treeboost.TreeBoostClassifier;
import it.cnr.jatecs.classification.treeboost.TreeBoostClassifierCustomizer;
import it.cnr.jatecs.evaluation.ContingencyTable;
import it.cnr.jatecs.evaluation.ContingencyTableSet;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.Pair;
import it.cnr.jatecs.utils.iterators.TIntArrayListIterator;
import it.cnr.jatecs.utils.iterators.TShortArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.util.HashMap;
import java.util.Vector;

/**
 * This adaboost learner includes an optimization step which set the best number
 * of iteration as determined in a k-fold experiment on the training data.
 *
 * @author Tiziano Fagni
 */
public class KFoldBoostingValidator extends BaseLearner {

    protected ILearner _usedLearner;

    public KFoldBoostingValidator(ILearner learner) {
        _usedLearner = learner;
        _customizer = new KFoldBoostingValidatorRuntimeCustomizer();
    }

    public static Pair<IIndex, IIndex> splitIndex(int step, IIndex index,
                                                  short catID, int validationSteps) {

        int numPositives = index.getClassificationDB()
                .getCategoryDocumentsCount(catID);

        int numPositivesInValidation = 0;
        int numPositivesInTraining = 0;

        if (numPositives == 1 && step > 1)
            return null;

        if (numPositives < validationSteps) {
            if ((step + 1) > numPositives && numPositives > 1)
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
            numPositivesInTraining = 1;
            numPositivesInValidation = 0;
            numTrainingDocuments = (index.getDocumentDB().getDocumentsCount() * 50) / 100;
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

    public IClassifier build(IIndex trainingIndex) {
        KFoldBoostingValidatorRuntimeCustomizer cust = (KFoldBoostingValidatorRuntimeCustomizer) _customizer;

        HashMap<Short, HashMap<Integer, ContingencyTable>> globalTables = new HashMap<Short, HashMap<Integer, ContingencyTable>>();
        IShortIterator cats = trainingIndex.getCategoryDB().getCategories();
        int count = 1;
        while (cats.hasNext()) {
            short catID = cats.next();

            TShortArrayList internalCategories = new TShortArrayList();
            internalCategories.add((short) 0);
            TShortArrayList externalCategories = new TShortArrayList();
            externalCategories.add(catID);

            JatecsLogger.status().println(
                    ""
                            + count
                            + ". Begin optimization of category "
                            + trainingIndex.getCategoryDB().getCategoryName(
                            catID));
            count++;

            globalTables.put(catID, new HashMap<Integer, ContingencyTable>());
            HashMap<Integer, ContingencyTable> tables = globalTables.get(catID);

            for (int i = 0; i < cust._k; i++) {
                JatecsLogger.status().println(
                        "Doing validation " + (i + 1) + "/" + cust._k + "...");

                // Split the index in two parts: a training and a valiudation
                // set.
                Pair<IIndex, IIndex> indexes = splitIndex(i, trainingIndex,
                        catID, cust._k);
                if (indexes == null)
                    // All possible steps was done.
                    break;

                IClassifier cl = _usedLearner.build(indexes.getFirst());

                IIndex testIdx = indexes.getSecond();

                for (int it = cust._startIteration; it <= cust._stopIteration; it += cust._step) {
                    AdaBoostClassifierCustomizer c = new AdaBoostClassifierCustomizer();
                    c._numIterations = it;

                    IClassifierRuntimeCustomizer custToUse = null;
                    if (cl instanceof TreeBoostClassifier)
                        custToUse = new TreeBoostClassifierCustomizer(c);
                    else
                        custToUse = c;
                    cl.setRuntimeCustomizer(custToUse);
                    ClassificationResult[] res = cl
                            .classify(testIdx, (short) 0);

                    for (int doc = 0; doc < res.length; doc++) {
                        ClassificationResult r = res[doc];
                        boolean positive = testIdx.getClassificationDB()
                                .hasDocumentCategory(r.documentID, (short) 0);
                        double score = r.score.get(0);
                        double border = cl.getClassifierRange((short) 0).border;

                        ContingencyTable ct = tables.get(it);
                        if (ct == null) {
                            ct = new ContingencyTable();
                            tables.put(it, ct);
                        }

                        if (score >= border && positive)
                            ct.setTP(ct.tp() + 1);
                        else if (score >= border && !positive)
                            ct.setFP(ct.fp() + 1);
                        else if (score < border && positive)
                            ct.setFN(ct.fn() + 1);
                        else
                            ct.setTN(ct.tn() + 1);
                    }
                }

            }

        }

        // Build optimal learner.
        IClassifier cl = _usedLearner.build(trainingIndex);

        // For each configuration tested evaluate its performance.
        double best = -Double.MAX_VALUE;
        int bestNumIterations = 0;
        for (int it = cust._startIteration; it <= cust._stopIteration; it += cust._step) {
            ContingencyTableSet set = new ContingencyTableSet();
            cats.begin();
            while (cats.hasNext()) {
                short catID = cats.next();
                HashMap<Integer, ContingencyTable> tab = globalTables
                        .get(catID);
                ContingencyTable ct = tab.get(it);
                set.addContingenyTable(catID, ct);
            }

            double effectiveness = set.getGlobalContingencyTable().f1();
            if (effectiveness > best) {
                best = effectiveness;
                bestNumIterations = it;
            }
        }

        JatecsLogger.status().println(
                "Best number of iterations: " + bestNumIterations
                        + " Effectiveness: " + best);
        AdaBoostClassifierCustomizer cu = new AdaBoostClassifierCustomizer();
        cu._numIterations = bestNumIterations;
        IClassifierRuntimeCustomizer custToUse = null;
        if (cl instanceof TreeBoostClassifier)
            custToUse = new TreeBoostClassifierCustomizer(cu);
        else
            custToUse = cu;

        cl.setRuntimeCustomizer(custToUse);

        return cl;
    }

    @Override
    public IClassifier mergeClassifiers(Vector<IClassifier> classifiers) {
        return _usedLearner.mergeClassifiers(classifiers);
    }

    @Override
    public ILearnerRuntimeCustomizer getRuntimeCustomizer(short catID) {
        return null;
    }

    @Override
    public void setRuntimeCustomizer(
            Vector<ILearnerRuntimeCustomizer> customizers) {
    }

}
