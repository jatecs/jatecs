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

public interface ISimilarityFunction {
    /**
     * Compute the similarity between "v1" vector and "v2" vector. The vectors must have the same
     * dimension.
     *
     * @param v1 The first vector.
     * @param v2 The second vector.
     * @return A score similarity between the two specified vectors.
     */
    public double computeSimilarity(double[] v1, double[] v2);


    /**
     * Compare the similarity between score1 and score2 parameters and
     * decide which measure is more similar respective to a certain similarity function.
     *
     * @param score1 The first measure.
     * @param score2 The second measure.
     * @return If score1 is more similar than score2 than 1 will be returned, if score1 is equals to score2 0 will
     * be returneed, if score1 is less similar than score2 -1 will be returned.
     */
    public int compareSimilarity(double score1, double score2);


    public double computeSimilarity(double[] v1, IWeighting3D dists, int catID);
}
