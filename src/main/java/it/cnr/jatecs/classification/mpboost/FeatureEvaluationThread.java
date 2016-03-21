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

package it.cnr.jatecs.classification.mpboost;

import it.cnr.jatecs.classification.adaboost.HypothesisData;
import it.cnr.jatecs.classification.adaboost.InMemoryWeakHypothesis;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;
import it.cnr.jatecs.weighting.interfaces.IWeighting3DManager;

public class FeatureEvaluationThread implements Runnable {

    private double[] weight_b1;
    private double[] weight_bminus_1;
    private int threadCount;
    private IIndex index;
    private IWeighting3DManager matrix;
    private InMemoryWeakHypothesis wh;
    private double epsilon;
    private double[] minimum_Z_s;
    private MPWeakLearnerMultiThread parent;
    private int id;

    public FeatureEvaluationThread(int id, int threadCount,
                                   MPWeakLearnerMultiThread parent, double[] weight_b1,
                                   double[] weight_bminus_1, IIndex index, IWeighting3DManager matrix,
                                   double epsilon) {
        this.id = id;
        this.weight_b1 = weight_b1;
        this.weight_bminus_1 = weight_bminus_1;
        this.threadCount = threadCount;
        this.index = index;
        this.matrix = matrix;
        this.epsilon = epsilon;
        this.parent = parent;
        minimum_Z_s = new double[index.getCategoryDB().getCategoriesCount()];
    }

    public InMemoryWeakHypothesis GetBestHypothesis() {
        return wh;
    }

    public double[] GetBestHypothesisScores() {
        return minimum_Z_s;
    }

    @Override
    public void run() {
        try {
            int catsSize = index.getCategoryDB().getCategoriesCount();

            double[] weight_b1_x0 = new double[catsSize];
            double[] weight_b1_x1 = new double[catsSize];

            double[] weight_bminus_1_x0 = new double[catsSize];
            double[] weight_bminus_1_x1 = new double[catsSize];

            double[] bestC0 = new double[catsSize];
            double[] bestC1 = new double[catsSize];
            int[] pivot = new int[catsSize];

            // Initialize structures.
            for (int pos = 0; pos < catsSize; pos++) {
                weight_b1_x0[pos] = 0;
                weight_b1_x1[pos] = 0;

                weight_bminus_1_x0[pos] = 0;
                weight_bminus_1_x1[pos] = 0;

                bestC0[pos] = 0;
                bestC1[pos] = 0;
                pivot[pos] = -1;
                minimum_Z_s[pos] = Double.MAX_VALUE;
            }
            IIntIterator featsID = index.getFeatureDB().getFeatures();

            while (featsID.hasNext()) {
                int realFeatID = featsID.next();
                if (realFeatID % threadCount != id)
                    continue;

                for (int pos = 0; pos < catsSize; pos++) {
                    weight_b1_x0[pos] = 0;
                    weight_b1_x1[pos] = 0;

                    weight_bminus_1_x0[pos] = 0;
                    weight_bminus_1_x1[pos] = 0;
                }

                IIntIterator itFeatDocs = index.getContentDB()
                        .getFeatureDocuments(realFeatID);

                while (itFeatDocs.hasNext()) {
                    int docID = itFeatDocs.next();
                    IShortIterator itDocCats = index.getClassificationDB()
                            .getDocumentCategories(docID);
                    short nextCatID = 0;
                    while (itDocCats.hasNext()) {
                        short currentCatID = itDocCats.next();

                        for (short i = nextCatID; i < currentCatID; i++) {
                            double distValue = matrix.getWeight(i, docID, 0);
                            assert (distValue >= 0);
                            weight_bminus_1_x1[i] += distValue;
                        }

                        double distValue = matrix.getWeight(currentCatID,
                                docID, 0);
                        assert (distValue >= 0);
                        weight_b1_x1[currentCatID] += distValue;

                        nextCatID = (short) (currentCatID + 1);
                    }
                    for (short i = nextCatID; i < catsSize; ++i) {
                        double distValue = matrix.getWeight(i, docID, 0);
                        assert (distValue >= 0);
                        weight_bminus_1_x1[i] += distValue;
                    }
                }

                for (int catID = 0; catID < catsSize; catID++) {
                    double v = weight_b1[catID] - weight_b1_x1[catID];
                    if (v < 0)
                        v = 0;

                    weight_b1_x0[catID] = v;

                    v = weight_bminus_1[catID] - weight_bminus_1_x1[catID];
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
                    assert (weight_b1_x1[catID] >= 0);
                    assert (weight_bminus_1_x1[catID] >= 0);

                    double Z_s = 0;
                    double first = Math.sqrt(weight_b1_x0[catID]
                            * weight_bminus_1_x0[catID]);
                    double second = Math.sqrt(weight_b1_x1[catID]
                            * weight_bminus_1_x1[catID]);
                    Z_s = (first + second);
                    Z_s = 2 * Z_s;

                    if (Z_s < minimum_Z_s[catID]) {

                        pivot[catID] = realFeatID;

                        double c0 = Math.log((weight_b1_x0[catID] + epsilon)
                                / (weight_bminus_1_x0[catID] + epsilon)) / 2.0;
                        double c1 = Math.log((weight_b1_x1[catID] + epsilon)
                                / (weight_bminus_1_x1[catID] + epsilon)) / 2.0;

                        bestC0[catID] = c0;
                        bestC1[catID] = c1;

                        minimum_Z_s[catID] = Z_s;
                    }

                }
            }

            wh = new InMemoryWeakHypothesis(catsSize);
            for (short i = 0; i < catsSize; i++) {
                HypothesisData hd = new HypothesisData();
                hd.c0 = bestC0[i];
                hd.c1 = bestC1[i];
                hd.pivot = pivot[i];

                wh.setValue(i, hd);
            }
        } finally {
            parent.signalThreadEnd(id);
        }
    }
}
