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

package it.cnr.jatecs.clustering.kmeans;

import it.cnr.jatecs.clustering.Centroid;
import it.cnr.jatecs.weighting.interfaces.IWeighting3D;

import java.util.Random;

public class RandomClusterizerInitalizer implements IClusterizerInitializer {

    public void initializeClusters(IWeighting3D dists, Centroid[] centroids) {
        Random r = new Random();

        boolean[] assigned = new boolean[dists.getFirstDimensionSize()];

        // Choose k random centroids.
        for (int i = 0; i < centroids.length; i++) {
            centroids[i].documents.clear();
            int next = r.nextInt(dists.getFirstDimensionSize());
            int idx = -1;
            if (assigned[next]) {
                for (int doc = (next + 1) % assigned.length; doc != next; doc = (doc + 1) % assigned.length) {
                    if (!assigned[doc]) {
                        assigned[doc] = true;
                        idx = doc;
                        break;
                    }
                }
            } else {
                idx = next;
                assigned[idx] = true;
            }

            centroids[i].documents.add((short) idx);
        }

        // Update centroid for each cluster.
        for (int i = 0; i < centroids.length; i++)
            centroids[i].computeCentroid(dists);
    }

}
