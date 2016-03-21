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

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Implements the Ylmaz Tau AP simmetric Rank Similarity Measure This measure is
 * a weighted conjoint ranking similarity measure based on the Kendall's tau
 * unweight conjoint ranking similarity
 *
 * @author Alejandro Moreo Fernandez (using Jatecs)
 */
public class YilmazTauAPasim extends RankSimilarityMethod {

    public YilmazTauAPasim(Rank rank1, Rank rank2) {
        this.rank1 = rank1;
        this.rank2 = rank2;
    }

    // number of agreement pairs (j,i) being j above i in rank 1 with respect to
    // rank 2
    // rank 2 es un mapa <feature-rank> para acelerar la comprobación
    private static int C(int i, ArrayList<Integer> rank_1,
                         HashMap<Integer, Integer> rank_2) {
        int agree = 0;
        int a = rank_1.get(i).intValue();
        for (int k = i - 1; k >= 0; k--) {
            int b = rank_1.get(k).intValue();
            if (precedes(b, a, rank_2)) {
                agree++;
            }
        }
        return agree;
    }

    // returns true if a precedes b in list
    // optimizado por mapa <feat-rank>
    private static boolean precedes(int a, int b, HashMap<Integer, Integer> list) {
        int pos_a = list.get(a);
        int pos_b = list.get(b);
        return pos_a < pos_b;
    }

    public static String getName() {
        return "TAUAPA";
    }

    // weighted conjoint ranking similarity, Yilman et al 2008, based on
    // Kendall's tau unweight conjoint ranking similarity
    @Override
    public double rankSimilarity() {
        double tau = 0;
        int N = rank1.size();

        // in the paper appears i=2, and (i-1) in the denominator. This is
        // because in java we rank from 0 instead of from 1
        for (int i = 1; i < N; i++) {
            tau += ((double) C(i, rank1.getRank(), rank2.getHashRank())) / (i);
        }

        return 2.0 * (tau / (N - 1)) - 1;
    }
}