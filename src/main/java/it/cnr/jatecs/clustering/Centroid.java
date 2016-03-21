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

package it.cnr.jatecs.clustering;

import gnu.trove.TDoubleArrayList;
import gnu.trove.TIntArrayList;
import it.cnr.jatecs.clustering.similarity.ISimilarityFunction;
import it.cnr.jatecs.utils.iterators.TIntArrayListIterator;
import it.cnr.jatecs.weighting.interfaces.IWeighting3D;

public class Centroid {

    /**
     * The centroid features vector.
     */
    public double[] features;

    /**
     * Current generic documents assigned to the cluster represented by this centroid
     */
    public TIntArrayList documents;

    /**
     * The distances of each category from currenty centroid.
     */
    public TDoubleArrayList distances;


    public Centroid(int numFeatures) {
        features = new double[numFeatures];
        documents = new TIntArrayList();
        distances = new TDoubleArrayList();
    }

    /**
     * Compute the centroid of the cluster considering the current assigned categories and
     * on the basis of the specified vector distributions.
     *
     * @param dists The vector distributions.
     */
    public static Centroid computeCentroid(TIntArrayList docs, int numFeatures, IWeighting3D dists) {
        if (docs.size() == 0)
            return new Centroid(numFeatures);

        int l = numFeatures;
        Centroid c = new Centroid(l);
        c.documents = docs;
        c.features = new double[l];

        TIntArrayListIterator it = new TIntArrayListIterator(docs);
        while (it.hasNext()) {
            int docID = it.next();
            for (int i = 0; i < c.features.length; i++) {
                c.features[i] += dists.getWeight(docID, i, 0);
            }
        }

        // Compute the average of the features.
        double norm = 0;
        for (int i = 0; i < c.features.length; i++) {
            c.features[i] = c.features[i] / c.documents.size();
            norm += c.features[i];
        }


        // Normalize the the centroid features vector to be features distribution.
        for (int i = 0; i < c.features.length; i++) {
            c.features[i] /= norm;
        }

        return c;
    }

    public void computeDistances(IWeighting3D dists, ISimilarityFunction func) {
        for (int i = 0; i < documents.size(); i++) {
            int docID = documents.get(i);
            double d = func.computeSimilarity(features, dists, docID);
            distances.set(i, d);
        }
    }

    /**
     * Compute the centroid of the cluster considering the current assigned categories and
     * on the basis of the specified vector distributions.
     *
     * @param dists The vector distributions.
     */
    public void computeCentroid(IWeighting3D dists) {
        if (documents.size() == 0)
            return;

        int l = features.length;
        features = new double[l];

        TIntArrayListIterator it = new TIntArrayListIterator(documents);
        while (it.hasNext()) {
            int docID = it.next();
            for (int i = 0; i < features.length; i++) {
                features[i] += dists.getWeight(docID, i, 0);
            }
        }

        // Compute the average of the features.
        double norm = 0;
        for (int i = 0; i < features.length; i++) {
            features[i] = features[i] / documents.size();
            norm += features[i];
        }


        // Normalize the the centroid features vector to be features distribution.
        for (int i = 0; i < features.length; i++) {
            features[i] /= norm;
        }
    }
}
