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

import gnu.trove.TShortArrayList;
import it.cnr.jatecs.classification.OptimalConfiguration;
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.interfaces.IClassifierRuntimeCustomizer;
import it.cnr.jatecs.classification.interfaces.ILearnerRuntimeCustomizer;
import it.cnr.jatecs.classification.validator.KFoldRuntimeCustomizer;
import it.cnr.jatecs.classification.validator.KFoldValidator;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.Pair;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Vector;

public class KnnCommitteeFoldValidator extends KFoldValidator {
    public KnnCommitteeFoldValidator(KnnCommitteeFakeLearner learner,
                                     KnnCommitteeClassifierOptimizer optimizer) {
        super(learner, optimizer);
    }

    protected void prepareCustomizers(KnnCommitteeClassifier cl,
                                      ArrayList<IKnnClassifierCustomizer> originalCustomizers,
                                      short catID, IIndex trainingIndex) {

        for (int i = 0; i < cl._classifiers.size(); i++) {
            IClassifier curCl = cl._classifiers.get(i);
            IKnnClassifierCustomizer copy = originalCustomizers.get(i)
                    .getCustomizerForCat(catID);
            curCl.setRuntimeCustomizer(copy);
        }
    }

    protected void resetOriginalCustomizers(KnnCommitteeClassifier cl,
                                            ArrayList<IKnnClassifierCustomizer> originalCustomizers) {
        for (int i = 0; i < cl._classifiers.size(); i++) {
            IClassifier curCl = cl._classifiers.get(i);
            curCl.setRuntimeCustomizer(originalCustomizers.get(i));
        }
    }

    @Override
    public IClassifier build(IIndex trainingIndex) {
        KFoldRuntimeCustomizer cust = (KFoldRuntimeCustomizer) getRuntimeCustomizer();

        Hashtable<Short, Vector<OptimalConfiguration>> results = new Hashtable<Short, Vector<OptimalConfiguration>>();

        KnnCommitteeFakeLearner learn = (KnnCommitteeFakeLearner) _usedLearner;


        boolean[] sameIndexedOld = new boolean[learn._cl._classifiers.size()];
        for (int i = 0; i < learn._cl._classifiers.size(); i++) {
            // KnnClassifier kc = learn._cl._classifiers.get(i);
            IClassifier kc = learn._cl._classifiers.get(i);
            IKnnClassifierCustomizer kcust = (IKnnClassifierCustomizer) kc
                    .getRuntimeCustomizer();
            if (kcust != null) {
                boolean old = kcust.getSearcher().useSameIndexesData();
                kcust.getSearcher().setUseSameIndexesData(true);
                sameIndexedOld[i] = old;
            }
        }

        ILearnerRuntimeCustomizer c = _usedLearner.getRuntimeCustomizer();
        if (c != null) {
            c = c.cloneObject();
        }

        // Save original customizers.
        ArrayList<IKnnClassifierCustomizer> originalCustomizers = new ArrayList<IKnnClassifierCustomizer>();
        KnnCommitteeFakeLearner fl = (KnnCommitteeFakeLearner) _usedLearner;
        for (int i = 0; i < fl._cl._classifiers.size(); i++) {
            IClassifier curCl = fl._cl._classifiers.get(i);
            originalCustomizers.add((IKnnClassifierCustomizer) curCl
                    .getRuntimeCustomizer().cloneObject());
        }

        IShortIterator cats = trainingIndex.getCategoryDB().getCategories();
        int count = 1;
        while (cats.hasNext()) {
            short catID = cats.next();

            TShortArrayList internalCategories = new TShortArrayList();
            internalCategories.add((short) 0);
            TShortArrayList externalCategories = new TShortArrayList();
            externalCategories.add(catID);

            // Prepare customizers for current category.
            prepareCustomizers(fl._cl, originalCustomizers, catID,
                    trainingIndex);

            JatecsLogger.status().println(
                    ""
                            + count
                            + ". Begin optimization of category "
                            + trainingIndex.getCategoryDB().getCategoryName(
                            catID));
            count++;

            Vector<OptimalConfiguration> v = new Vector<OptimalConfiguration>();

            for (int i = 0; i < cust.getKFoldValidationSteps(); i++) {
                JatecsLogger.status().println(
                        "Doing validation " + (i + 1) + "/"
                                + cust.getKFoldValidationSteps() + "...");

                // Split the index in two parts: a training and a valiudation
                // set.
                Pair<IIndex, IIndex> indexes = splitIndex(i, trainingIndex,
                        catID, cust.getKFoldValidationSteps());
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

        for (int i = 0; i < learn._cl._classifiers.size(); i++) {
            IClassifier kc = learn._cl._classifiers.get(i);
            IKnnClassifierCustomizer kcust = (IKnnClassifierCustomizer) kc
                    .getRuntimeCustomizer();
            kcust.getSearcher().setUseSameIndexesData(sameIndexedOld[i]);
        }

        JatecsLogger.status().println("Start constructing optimal learner");
        // Build optimal learner.
        _usedLearner.setRuntimeCustomizer(c);
        cust.setOptimizedLearnerRuntimeCustomizer(c);
        IClassifier clas = _usedLearner.build(trainingIndex);
        JatecsLogger.status().println("Optimal learner constructed");

        resetOriginalCustomizers(fl._cl, originalCustomizers);

        if (_classifierOptimizationRequired) {
            // Discover best classifier configurations for each category.
            KnnCommitteeClassifierCustomizer runtimeC = (KnnCommitteeClassifierCustomizer) clas
                    .getRuntimeCustomizer();
            sameIndexedOld = new boolean[learn._cl._classifiers.size()];
            for (int i = 0; i < learn._cl._classifiers.size(); i++) {
                IClassifier kc = learn._cl._classifiers.get(i);
                IKnnClassifierCustomizer kcust = (IKnnClassifierCustomizer) kc
                        .getRuntimeCustomizer();
                boolean old = kcust.getSearcher().useSameIndexesData();
                kcust.getSearcher().setUseSameIndexesData(true);
                sameIndexedOld[i] = old;
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
            for (int i = 0; i < learn._cl._classifiers.size(); i++) {
                IClassifier kc = learn._cl._classifiers.get(i);
                IKnnClassifierCustomizer kcust = (IKnnClassifierCustomizer) kc
                        .getRuntimeCustomizer();
                kcust.getSearcher().setUseSameIndexesData(sameIndexedOld[i]);
            }
            clas.setRuntimeCustomizer(runtimeC);
            cust.setOptimizedClassifierRuntimeCustomizer(runtimeC);
        }

        return clas;
    }

}
