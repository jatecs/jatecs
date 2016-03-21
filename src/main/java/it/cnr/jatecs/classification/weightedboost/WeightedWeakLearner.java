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

package it.cnr.jatecs.classification.weightedboost;

import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexing.discretization.DiscreteBin;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;
import it.cnr.jatecs.weighting.interfaces.IWeighting3DManager;

import java.util.Iterator;
import java.util.TreeSet;

public class WeightedWeakLearner implements IWeightedWeakLearner {


    public IWeightedWeakHypothesis getNewWeakHypothesis(
            IWeighting3DManager matrix, IIndex index, WeakLearnerParams params)

    {
        if (matrix == null)
            throw new NullPointerException("The specified matrix is 'null'");
        if (index == null)
            throw new NullPointerException("The specified index is 'null'");
        if (params == null)
            throw new NullPointerException("The set of params is 'null'");

        int numPositiveRanges = params.getMaxNumberOfBins();

        int catsSize = matrix.getFirstDimensionSize();
        int docsSize = matrix.getSecondDimensionSize();

        double _epsilon = 1.0 / (double) (catsSize * docsSize);

        // Examples positive for a given category.
        double[] weight_b1 = new double[catsSize];
        double[] weight_b1_x0 = new double[catsSize];
        double[][] weight_b1_x1 = new double[catsSize][numPositiveRanges];

        // Examples negative for a given category
        double[] weight_bminus_1 = new double[catsSize];
        double[] weight_bminus_1_x0 = new double[catsSize];
        double[][] weight_bminus_1_x1 = new double[catsSize][numPositiveRanges];

        // Best c0 and c1 values.
        double[] bestC0 = new double[catsSize];
        double[][] bestC1 = new double[catsSize][numPositiveRanges];
        int[] pivot = new int[catsSize];

        double[] minimum_Z_s = new double[catsSize];

        // Initialize structures.
        for (int pos = 0; pos < catsSize; pos++) {
            weight_b1[pos] = 0;
            weight_b1_x0[pos] = 0;
            for (int j = 0; j < weight_b1_x1[pos].length; j++) {
                weight_b1_x1[pos][j] = 0;
                weight_bminus_1_x1[pos][j] = 0;
                bestC1[pos][j] = 0;
            }

            weight_bminus_1[pos] = 0;
            weight_bminus_1_x0[pos] = 0;
            bestC0[pos] = 0;
            pivot[pos] = -1;
            minimum_Z_s[pos] = Double.MAX_VALUE;
        }

        // Compute positive weight for categories.
        for (short catID = 0; catID < catsSize; catID++) {
            IIntIterator it = index.getClassificationDB().getCategoryDocuments(
                    catID);
            while (it.hasNext()) {
                int docID = it.next();
                double distValue = matrix.getWeight(catID, docID, 0);
                assert (distValue >= 0);
                weight_b1[catID] += distValue;
            }

        }

        // Compute global weight for categories.
        for (int catID = 0; catID < catsSize; catID++) {
            double global = 0;

            // Iterate over all distribution matrix.
            for (int docID = 0; docID < docsSize; docID++) {
                double distValue = matrix.getWeight(catID, docID, 0);
                assert (distValue >= 0);
                global += distValue;
            }

            weight_bminus_1[catID] = global - weight_b1[catID];
            assert (weight_bminus_1[catID] >= 0);
        }

        // Iterate over all features.
        IIntIterator featsID = index.getFeatureDB().getFeatures();
        while (featsID.hasNext()) {
            int realFeatID = featsID.next();

            TreeSet<DiscreteBin> bins = params.getBins()[realFeatID];

            // Initialize structures for each feature iteration.
            for (int pos = 0; pos < catsSize; pos++) {
                weight_b1_x0[pos] = 0;

                for (int j = 0; j < weight_b1_x1[pos].length; j++) {
                    weight_b1_x1[pos][j] = 0;
                    weight_bminus_1_x1[pos][j] = 0;
                }

                weight_bminus_1_x0[pos] = 0;
            }

            IIntIterator itFeatDocs = index.getContentDB().getFeatureDocuments(
                    realFeatID);

            while (itFeatDocs.hasNext()) {
                int docID = itFeatDocs.next();
                double we = index.getWeightingDB().getDocumentFeatureWeight(docID, realFeatID);
                if (!(we >= 0 && we <= 1))
                    assert (we >= 0 && we <= 1);
                int valIndex = computeBinIndex(we, bins);
                if (!(valIndex >= 0 && valIndex < numPositiveRanges))
                    assert (valIndex >= 0 && valIndex < numPositiveRanges);
                IShortIterator itDocCats = index.getClassificationDB()
                        .getDocumentCategories(docID);
                short nextCatID = 0;
                while (itDocCats.hasNext()) {
                    short currentCatID = itDocCats.next();

                    for (short i = nextCatID; i < currentCatID; i++) {
                        double distValue = matrix.getWeight(i, docID, 0);
                        assert (distValue >= 0);

                        // Feature compare on document and category not.
                        weight_bminus_1_x1[i][valIndex] += distValue;
                    }

                    double distValue = matrix.getWeight(currentCatID, docID, 0);
                    assert (distValue >= 0);
                    // Feature and category compare together.
                    weight_b1_x1[currentCatID][valIndex] += distValue;

                    nextCatID = (short) (currentCatID + 1);
                }
                for (short i = nextCatID; i < catsSize; ++i) {
                    double distValue = matrix.getWeight(i, docID, 0);
                    assert (distValue >= 0);
                    // Feature compare on document and category not.
                    weight_bminus_1_x1[i][valIndex] += distValue;
                }
            }

            // Compute the remaining values.
            for (int catID = 0; catID < catsSize; catID++) {
                // assert(weight_b1[catID] >= weight_b1_x1[catID]);
                double x1_b1 = 0;
                double x1_bminus1 = 0;
                for (int j = 0; j < weight_b1_x1[catID].length; j++) {
                    x1_b1 += weight_b1_x1[catID][j];
                    x1_bminus1 += weight_bminus_1_x1[catID][j];
                }
                double v = weight_b1[catID] - x1_b1;
                if (v < 0)
                    v = 0;
                weight_b1_x0[catID] = v;

                v = weight_bminus_1[catID] - x1_bminus1;
                // Adjust round errors.
                if (v < 0)
                    v = 0;
                assert (v >= 0);
                weight_bminus_1_x0[catID] = v;
            }

            // Compute actual Z_s.
            for (int catID = 0; catID < catsSize; catID++) {
                assert (weight_b1_x0[catID] >= 0);
                assert (weight_bminus_1_x0[catID] >= 0);
                //assert (weight_b1_x1[catID] >= 0);
                //assert (weight_bminus_1_x1[catID] >= 0);

                double Z_s = 0;
                double first = Math.sqrt(weight_b1_x0[catID]
                        * weight_bminus_1_x0[catID]);

                double second = 0;
                for (int j = 0; j < weight_b1_x1[catID].length; j++) {
                    second += Math.sqrt(weight_b1_x1[catID][j]
                            * weight_bminus_1_x1[catID][j]);
                }

                Z_s = (first + second);
                Z_s = 2 * Z_s;

                if (Z_s < minimum_Z_s[catID]) {

                    pivot[catID] = realFeatID;

                    double c0 = Math.log((weight_b1_x0[catID] + _epsilon)
                            / (weight_bminus_1_x0[catID] + _epsilon)) / 2.0;
                    bestC0[catID] = c0;

                    for (int j = 0; j < weight_b1_x1[catID].length; j++) {
                        bestC1[catID][j] = Math.log((weight_b1_x1[catID][j] + _epsilon)
                                / (weight_bminus_1_x1[catID][j] + _epsilon)) / 2.0;
                    }


                    minimum_Z_s[catID] = Z_s;
                }

            }

        }

        InMemoryWeightedWeakHypothesis wh = new InMemoryWeightedWeakHypothesis(
                catsSize);
        for (short i = 0; i < catsSize; i++) {
            WeightedHypothesisData hd = new WeightedHypothesisData(numPositiveRanges);
            hd.c0 = bestC0[i];

            for (int j = 0; j < numPositiveRanges; j++) {
                hd.c1ConstantValues[j] = bestC1[i][j];
            }
            hd.pivot = pivot[i];

            wh.setValue(i, hd);
        }

        return wh;

    }

    private int computeBinIndex(double we, TreeSet<DiscreteBin> bins) {
        Iterator<DiscreteBin> it = bins.iterator();
        int idx = 0;
        if (bins.size() == 0)
            throw new IllegalArgumentException("The set of bins is empty");
        if (we < bins.first().getStartValue())
            return 0;


        while (it.hasNext()) {
            DiscreteBin db = it.next();
            if (we >= db.getStartValue() && we <= db.getEndValue())
                return idx;

            idx++;
        }

        return idx - 1;
    }

}
