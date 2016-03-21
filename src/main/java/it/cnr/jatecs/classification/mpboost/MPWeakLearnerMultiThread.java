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

import it.cnr.jatecs.classification.adaboost.IWeakHypothesis;
import it.cnr.jatecs.classification.adaboost.IWeakLearner;
import it.cnr.jatecs.classification.adaboost.InMemoryWeakHypothesis;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.weighting.interfaces.IWeighting3DManager;

public class MPWeakLearnerMultiThread implements IWeakLearner {

    int threadCount;
    private int completedThreads;

    public MPWeakLearnerMultiThread() {
        threadCount = 2;
    }

    public MPWeakLearnerMultiThread(int threadCount) {
        if (threadCount < 1)
            throw new IllegalArgumentException(
                    "The specified number of threads must be greater equals to 1");
        this.threadCount = threadCount;
    }

    public synchronized IWeakHypothesis getNewWeakHypothesis(
            IWeighting3DManager matrix, IIndex index) {
        int catsSize = matrix.getFirstDimensionSize();
        int docsSize = matrix.getSecondDimensionSize();

        double _epsilon = 1.0 / (double) (catsSize * docsSize);

        double[] weight_b1 = new double[catsSize];
        double[] weight_bminus_1 = new double[catsSize];

        for (int pos = 0; pos < catsSize; pos++) {
            weight_b1[pos] = 0;
            weight_bminus_1[pos] = 0;
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

        completedThreads = 0;
        FeatureEvaluationThread[] threads = new FeatureEvaluationThread[threadCount];
        for (int t = 0; t < threadCount; ++t) {
            threads[t] = new FeatureEvaluationThread(t, threadCount, this,
                    weight_b1, weight_bminus_1, index, matrix, _epsilon);
            Thread th = new Thread(threads[t]);
            th.start();
        }

        try {
            this.wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        InMemoryWeakHypothesis bestWh = threads[0].GetBestHypothesis();
        double[] bestScores = threads[0].GetBestHypothesisScores();
        for (int i = 1; i < threadCount; ++i) {
            InMemoryWeakHypothesis otherHyp = threads[i].GetBestHypothesis();
            double[] otherScores = threads[i].GetBestHypothesisScores();
            for (int j = 0; j < catsSize; ++j) {
                if (otherScores[j] < bestScores[j]) {
                    bestWh.setValue((short) j, otherHyp.value((short) j));
                    bestScores[j] = otherScores[j];
                }
            }
        }
        return bestWh;

    }

    protected synchronized void signalThreadEnd(int id) {
        ++completedThreads;
        if (completedThreads == threadCount)
            this.notify();
    }

}
