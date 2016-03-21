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

package it.cnr.jatecs.clustering.similarity;

import it.cnr.jatecs.weighting.interfaces.IWeighting3D;

public class KLSimilarity implements ISimilarityFunction {

    public int compareSimilarity(double score1, double score2) {
        if (score1 > score2)
            return -1;
        else if (score1 < score2)
            return 1;
        else
            return 0;
    }

    public double computeSimilarity(double[] v1, double[] v2) {
        double score = 0;
        for (int i = 0; i < v1.length; i++) {
            if (v1[i] != 0 && v2[i] == 0)
                score += v1[i];
            else if (v1[i] != 0)
                score += v1[i] * Math.log(v1[i] / v2[i]);
        }

        double score2 = 0;
        for (int i = 0; i < v2.length; i++) {
            if (v1[i] == 0 && v2[i] != 0)
                score2 += v1[i];
            else if (v2[i] != 0)
                score2 += v2[i]
                        * Math.log(v2[i] / v1[i]);
        }

        score = (score + score2) / 2;

//		Double v = 1 / (1 + score);
//		assert (!Double.isNaN(v));
//		return v;
        return score;
    }

    public double computeSimilarity(double[] v1, IWeighting3D v2, int index) {
        // Mek the measure symmetric
        double score = 0;
        for (int i = 0; i < v1.length; i++) {
            if (v1[i] != 0 && v2.getWeight(index, i, 0) == 0)
                score += v1[i];
            else if (v1[i] != 0)
                score += v1[i] * Math.log(v1[i] / v2.getWeight(index, i, 0));
        }

        double score2 = 0;
        for (int i = 0; i < v2.getSecondDimensionSize(); i++) {
            if (v1[i] == 0 && v2.getWeight(index, i, 0) != 0)
                score2 += v1[i];
            else if (v2.getWeight(index, i, 0) != 0)
                score2 += v2.getWeight(index, i, 0)
                        * Math.log(v2.getWeight(index, i, 0) / v1[i]);
        }

        score = (score + score2) / 2;

        Double v = 1 / (1 + score);
        assert (!Double.isNaN(v));
        return v;
    }

}
