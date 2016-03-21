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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.cnr.jatecs.utils;

import gnu.trove.TIntArrayList;
import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntDoubleIterator;
import gnu.trove.TIntDoubleProcedure;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Create a rank of the integer keys of a hashmap, according to the double
 * values.
 *
 * @author giacomo
 */
public class Ranker {

    static public int getMax(TIntDoubleHashMap table) {
        int maxKey = -Integer.MIN_VALUE;
        double maxValue = Double.NEGATIVE_INFINITY;
        TIntDoubleIterator it = table.iterator();
        while (it.hasNext()) {
            it.advance();
            if (it.value() > maxValue) {
                maxValue = it.value();
                maxKey = it.key();
            }
        }
        return maxKey;
    }

    public TIntArrayList get(TIntDoubleHashMap table) {
        final ArrayList<ComparablePair> list = new ArrayList<ComparablePair>(
                table.size());
        class Procedure implements TIntDoubleProcedure {
            @Override
            public boolean execute(int a, double b) {
                list.add(new ComparablePair(a, b));
                return true;
            }
        }
        table.forEachEntry(new Procedure());
        Collections.sort(list);
        TIntArrayList result = new TIntArrayList(list.size());
        for (int i = 0; i < list.size(); i++) {
            result.add(list.get(i).getFirst());
        }
        return result;
    }

    class ComparablePair extends Pair<Integer, Double> implements
            Comparable<ComparablePair> {
        public ComparablePair(int first, double second) {
            super(first, second);
        }

        @Override
        public int compareTo(ComparablePair o) {
            if (this.getSecond() > o.getSecond()) {
                return -1;
            } else if (this.getSecond() == o.getSecond()) {
                return 0;
            } else {
                return 1;
            }
        }
    }
}
