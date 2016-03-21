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

package it.cnr.jatecs.indexing.tsr.ranks;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * Ranking Similarity Measure that measures the extent to which two ranks are
 * similiar with respect to a certain ratio. That is, this function computes the
 * proportion of features in both rankings that are ranked in the first
 * ratio*rankSize elements.
 * <p>
 * For example, given R1=[3,1,4,2] and R2=[1,4,3,2], this measure returns 0.5
 * for the ratio 1/2, and 1 for the ratio 3/4 (that is, considering the first 3
 * features, both rankings are exactly similar.
 *
 * @author Alejandro Moreo Fernandez (using Jatecs)
 */
public class RatioSimilarity extends RankSimilarityMethod {

    private double ratio;

    public RatioSimilarity(Rank rank1, Rank rank2, double ratio) {
        this.rank1 = rank1;
        this.rank2 = rank2;
        this.ratio = ratio;
    }

    public static String getName() {
        return "RATIOSIMILARITY";
    }

    @Override
    public double rankSimilarity() {
        int S = (int) (this.rank1.size() * ratio);
        List<Integer> sub1 = rank1.getRank().subList(0, S);
        List<Integer> sub2 = rank2.getRank().subList(0, S);

        HashSet<Integer> c1 = new HashSet<Integer>(sub1);
        HashSet<Integer> c2 = new HashSet<Integer>(sub2);

        return (intersection(c1, c2)).size() * 1.0 / S;
    }

    private HashSet<Integer> intersection(HashSet<Integer> x, HashSet<Integer> y) {
        HashSet<Integer> inter = new HashSet<Integer>();

        // optimizacion: iterar O(n) sobre el menor, y buscar O(1) sobre el
        // mayor
        HashSet<Integer> smaller = ((x.size() <= y.size()) ? x : y);
        HashSet<Integer> bigger = ((x.size() > y.size()) ? x : y);

        for (Iterator<Integer> it = smaller.iterator(); it.hasNext(); ) {
            Integer el = it.next();
            if (bigger.contains(el)) {
                inter.add(el);
            }
        }
        return inter;

    }
}
