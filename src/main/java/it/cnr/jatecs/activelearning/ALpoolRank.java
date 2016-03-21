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

package it.cnr.jatecs.activelearning;

import gnu.trove.TIntArrayList;
import gnu.trove.TIntDoubleHashMap;
import it.cnr.jatecs.classification.ClassificationScoreDB;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.Ranker;

public class ALpoolRank extends PoolRank {

    protected TIntArrayList ranking = null;
    protected TIntDoubleHashMap rankingMap;
    protected int maxDocId = -Integer.MAX_VALUE;
    protected double maxDocScore = Double.NEGATIVE_INFINITY;
    protected int unlabelledSize;

    public ALpoolRank(ClassificationScoreDB confidenceUnlabelled, IIndex trainingSet) {
        super(confidenceUnlabelled, trainingSet);
        unlabelledSize = confidenceUnlabelled.getDocumentCount();
        rankingMap = new TIntDoubleHashMap(
                (int) (unlabelledSize + unlabelledSize * 0.25), (float) 0.75);
    }

    @Override
    public TIntArrayList getFirstMacro(int n) {
        if (n == 1 && maxDocId >= 0) {
            return new TIntArrayList(new int[]{maxDocId});
        } else {
            if (ranking == null) {
                Ranker r = new Ranker();
                ranking = r.get(rankingMap);
            }
            if (1 < n && n < ranking.size()) {
                return ranking.subList(0, n);
            } else {
                return ranking;
            }
        }
    }

    @Override
    public TIntArrayList getFirstMicro(int n) {
        return getFirstMacro(n);
    }

    public TIntDoubleHashMap getRanking() {
        return rankingMap;
    }

    /**
     * After computing each score of the ranking, one can update the first
     * document of the ranking (used when AL returns one document for each
     * retraining)
     *
     * @param docId
     * @param value The score assigned to docId
     */
    protected void updateMax(int docId, double value) {
        if (value > maxDocScore) {
            maxDocScore = value;
            maxDocId = docId;
        }
    }
}
