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

public class Random extends StaticRank {

    public Random(int trainSize, ClassificationScoreDB classification, TIntHashSet categoriesFilter) {
        super(trainSize, classification, categoriesFilter);
    }

    public TIntDoubleHashMap getTable() {
        TIntDoubleHashMap rank = new TIntDoubleHashMap(testSize);
        for (int i = 0; i < testSize; i++) {
            rank.put(i, Math.random());
        }
        return rank;
    }

    @Override
    public TIntDoubleHashMap getMacroTable() {
        return getTable();
    }

    @Override
    public TIntDoubleHashMap getMicroTable() {
        return getTable();
    }
}
