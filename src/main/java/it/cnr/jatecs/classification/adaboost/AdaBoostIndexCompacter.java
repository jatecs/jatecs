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
import gnu.trove.TIntIntHashMap;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.Pair;
import it.cnr.jatecs.utils.iterators.FilteredIntIterator;
import it.cnr.jatecs.utils.iterators.TIntArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;

/**
 * This class compacts the passed "test" index based on the pivots found in weak
 * hypothesis of the specified AdaBoost classifier. The training index can be
 * used to known the real name of the pivots contained in the weak hypothesis.
 * The input parameter are read-only and are not modified by the function.
 *
 * @author Andrea Esuli
 */
public class AdaBoostIndexCompacter {

    public AdaBoostIndexCompacter() {

    }

    /**
     * Compact the passed "test" index based on the pivots found in weak
     * hypothesis of the specified AdaBoost classifier. The training index can
     * be used to known the real name of the pivots contained in the weak
     * hypothesis. The input parameter are read-only and are not touched by the
     * function.
     *
     * @param wh       The weak hypothesis.
     * @param training The training index.
     * @param test     The test index.
     * @return A pair containing the compacted test index and the AdaBoost
     * classifier with synchronized features id with the compacted test
     * index.
     */
    public Pair<IIndex, AdaBoostClassifier> compactIndex(
            AdaBoostClassifier classifier, IIndex training, IIndex test) {
        // Clone the test index and compact it.
        IIndex ctest = test.cloneIndex();

        TIntArrayList validFeats = new TIntArrayList();
        TIntIntHashMap map = new TIntIntHashMap(1000);

        // Get all valid features.
        for (int i = 0; i < classifier._hypothesis.length; i++) {
            IWeakHypothesis wh = classifier._hypothesis[i];
            for (short catID = 0; catID < wh.getClassifiersCount(); catID++) {
                HypothesisData data = wh.value(catID);
                if (!map.containsKey(data.pivot)) {
                    validFeats.add(data.pivot);
                    map.put(data.pivot, data.pivot);
                }
            }
        }

        validFeats.sort();
        // Remove invalid features from compact test index.
        IIntIterator filt = new FilteredIntIterator(test.getFeatureDB()
                .getFeatures(), new TIntArrayListIterator(validFeats), true);
        ctest.removeFeatures(filt);

        // Create a new classifier with a synchronized representation with the
        // compact index.
        AdaBoostClassifier ncl = new AdaBoostClassifier();
        ncl.setRuntimeCustomizer(classifier.getRuntimeCustomizer());
        ncl._distributionMatrixFilename = classifier._distributionMatrixFilename;
        ncl._maxNumIterations = classifier._maxNumIterations;
        ncl._validCategories = classifier._validCategories;
        // ncl.distributionMatrix = classifier.distributionMatrix;
        ncl._hypothesis = new IWeakHypothesis[classifier._hypothesis.length];
        for (int i = 0; i < classifier._hypothesis.length; i++) {
            IWeakHypothesis wh = classifier._hypothesis[i];
            InMemoryWeakHypothesis nwh = new InMemoryWeakHypothesis(
                    wh.getClassifiersCount());
            for (short catID = 0; catID < wh.getClassifiersCount(); catID++) {
                HypothesisData data = wh.value(catID);
                HypothesisData ndata = new HypothesisData();
                ndata.c0 = data.c0;
                ndata.c1 = data.c1;
                if (data.pivot >= 0) {
                    String name = training.getFeatureDB().getFeatureName(
                            data.pivot);
                    assert (name != null);
                    int npivot = ctest.getFeatureDB().getFeature(name);
                    assert (npivot != -1);
                    ndata.pivot = npivot;
                } else
                    ndata.pivot = data.pivot;
                nwh.setValue(catID, ndata);
            }
            ncl._hypothesis[i] = nwh;
        }

        Pair<IIndex, AdaBoostClassifier> ret = new Pair<IIndex, AdaBoostClassifier>(
                ctest, ncl);
        return ret;
    }

}
