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

package it.cnr.jatecs.satc.rank;

import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntHashSet;
import it.cnr.jatecs.classification.ClassificationScoreDB;

public class ConfidenceBasedOracle extends ConfidenceBased {

    public ConfidenceBasedOracle(int trainSize, ClassificationScoreDB classification,
                                 TIntHashSet categoriesFilter) {
        super(trainSize, classification, categoriesFilter, new double[]{1.0});
    }

    @Override
    public double probability(double x, int catId) {
        return Math.signum(x);
    }

    @Override
    public TIntDoubleHashMap getTable() {
        TIntDoubleHashMap rank = super.getTable();
        for (int docId = 0; docId < testSize; docId++) {
            if (rank.get(docId) != 0.0) {
                rank.adjustValue(docId, Math.random());
            }
        }
        return rank;
    }
}
