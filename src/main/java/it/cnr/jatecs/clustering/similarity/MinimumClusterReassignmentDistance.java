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

import it.cnr.jatecs.clustering.Centroid;
import it.cnr.jatecs.weighting.interfaces.IWeighting3D;

public class MinimumClusterReassignmentDistance implements IReassignmentDistance {

    private ISimilarityFunction _similarity;

    public MinimumClusterReassignmentDistance(ISimilarityFunction func) {
        _similarity = func;
    }


    public int compareDistance(double score1, double score2) {
        return _similarity.compareSimilarity(score1, score2);
    }

    public double computeDistance(Centroid[] centroids,
                                  int whichCluster, int docID, IWeighting3D dists) {
        double min = _similarity.compareSimilarity(0, Double.MAX_VALUE) > 0 ? 0 : Double.MAX_VALUE;

        for (int i = 0; i < centroids.length; i++) {
            if (i == whichCluster)
                continue;
            double score = _similarity.computeSimilarity(centroids[i].features, dists, docID);
            if (_similarity.compareSimilarity(score, min) < 0) {
                min = score;
            }
        }

        return min;
    }

}
