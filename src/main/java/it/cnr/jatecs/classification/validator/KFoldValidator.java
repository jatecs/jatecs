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
import gnu.trove.TShortArrayList;
import it.cnr.jatecs.classification.BaseLearner;
import it.cnr.jatecs.classification.OptimalConfiguration;
import it.cnr.jatecs.classification.interfaces.*;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.Pair;
import it.cnr.jatecs.utils.iterators.TIntArrayListIterator;
import it.cnr.jatecs.utils.iterators.TShortArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.util.Hashtable;
import java.util.Vector;

public class KFoldValidator extends BaseLearner {

    protected IClassifierOptimizer _optimizer;

    protected ILearner _usedLearner;

    protected boolean _classifierOptimizationRequired;

    public KFoldValidator(ILearner learner, IClassifierOptimizer optimizer) {
        _usedLearner = learner;
        _optimizer = optimizer;
        _customizer = new KFoldRuntimeCustomizer();
        _classifierOptimizationRequired = true;
    }

    public static Pair<IIndex, IIndex> splitIndex(int step, IIndex index,
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
                        + index.getCategoryDB().getCategoryName(catID)
                        + "...");

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

    public void enableClassifierOptimization(boolean enable) {
        _classifierOptimizationRequired = enable;
    }

    public IClassifier build(IIndex trainingIndex) {
        KFoldRuntimeCustomizer cust = (KFoldRuntimeCustomizer) getRuntimeCustomizer();

        Hashtable<Short, Vector<OptimalConfiguration>> results = new Hashtable<Short, Vector<OptimalConfiguration>>();

        ILearnerRuntimeCustomizer c = _usedLearner.getRuntimeCustomizer();
        if (c != null) {
            c = c.cloneObject();
        }

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

            Vector<OptimalConfiguration> v = new Vector<OptimalConfiguration>();

            for (int i = 0; i < cust._k; i++) {
                JatecsLogger.status().println(
                        "Doing validation " + (i + 1) + "/" + cust._k + "...");

                // Split the index in two parts: a training and a
                // valiudation set.
                Pair<IIndex, IIndex> indexes = splitIndex(i, trainingIndex,
                        catID, cust._k);
                if (indexes == null)
                    // All possible steps was done.
                    break;

                // Optimize classifier.
                OptimalConfiguration res = _optimizer.optimizeFor(_usedLearner,
                        indexes.getFirst(), indexes.getSecond(),
                        internalCategories);

                // Keep track of current result.
                v.add(res);

            }

            // Prepare learner configurations.
            Vector<ILearnerRuntimeCustomizer> customizers = new Vector<ILearnerRuntimeCustomizer>();
            for (int i = 0; i < v.size(); i++) {
                customizers.add(v.get(i).learnerCustomizer);
            }
            _optimizer.assignBestLearnerConfiguration(c, externalCategories,
                    customizers, internalCategories);

            results.put(catID, v);

        }

        JatecsLogger.status().println("Start constructing optimal learner");
        // Build optimal learner.
        _usedLearner.setRuntimeCustomizer(c);
        cust._optimizedLRC = c;
        IClassifier clas = _usedLearner.build(trainingIndex);
        JatecsLogger.status().println("Optimal learner constructed");

        if (_classifierOptimizationRequired) {
            // Discover best classifier configurations for each category.
            IClassifierRuntimeCustomizer runtimeC = clas.getRuntimeCustomizer();

            if (runtimeC instanceof IThresholdClassifier) {
                IThresholdClassifier cu = (IThresholdClassifier) runtimeC;
                cu.reserveMemoryFor(trainingIndex.getCategoryDB()
                        .getCategoriesCount());
            }

            cats.begin();
            while (cats.hasNext()) {
                short catID = cats.next();

                Vector<OptimalConfiguration> r = results.get(catID);
                Vector<IClassifierRuntimeCustomizer> custs = new Vector<IClassifierRuntimeCustomizer>();
                for (int i = 0; i < r.size(); i++) {
                    custs.add(r.get(i).classifierCustomizer);
                }

                TShortArrayList catsExternal = new TShortArrayList();
                catsExternal.add(catID);

                TShortArrayList catsInternal = new TShortArrayList();
                catsInternal.add((short) 0);

                _optimizer.assignBestClassifierConfiguration(runtimeC,
                        catsExternal, custs, catsInternal);
            }

            // Set best parameters for classifier.
            clas.setRuntimeCustomizer(runtimeC);
            cust._optmizedCRC = runtimeC;
        }

        return clas;
    }

    public void setRuntimeCustomizer(ILearnerRuntimeCustomizer customizer) {
        _customizer = (KFoldRuntimeCustomizer) customizer;
    }

    @Override
    public IClassifier mergeClassifiers(Vector<IClassifier> classifiers) {
        return _usedLearner.mergeClassifiers(classifiers);
    }

    @Override
    public void setRuntimeCustomizer(
            Vector<ILearnerRuntimeCustomizer> customizers) {
        KFoldRuntimeCustomizer cust = (KFoldRuntimeCustomizer) _customizer;
        Vector<ILearnerRuntimeCustomizer> c = new Vector<ILearnerRuntimeCustomizer>();
        for (int i = 0; i < customizers.size(); i++) {
            KFoldRuntimeCustomizer cc = (KFoldRuntimeCustomizer) customizers
                    .get(i);
            c.add(cc._optimizedLRC);
        }

        _usedLearner.setRuntimeCustomizer(c);
        cust._optimizedLRC = _usedLearner.getRuntimeCustomizer();
    }

    @Override
    public ILearnerRuntimeCustomizer getRuntimeCustomizer(short catID) {
        return null;
    }

}
