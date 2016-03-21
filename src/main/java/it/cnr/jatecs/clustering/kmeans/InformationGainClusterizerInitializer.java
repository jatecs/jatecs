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
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexing.tsr.InformationGain;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;
import it.cnr.jatecs.weighting.interfaces.IWeighting3D;

import java.util.Iterator;
import java.util.TreeSet;

public class InformationGainClusterizerInitializer implements
        IClusterizerInitializer {

    private IIndex _index;


    public InformationGainClusterizerInitializer(IIndex index) {
        _index = index;
    }

    public void initializeClusters(IWeighting3D dists, Centroid[] centroids) {
        InformationGain func = new InformationGain();
        TreeSet<FeatureItem> ordered = new TreeSet<FeatureItem>();
        IIntIterator features = _index.getFeatureDB().getFeatures();
        IShortIterator categories = _index.getCategoryDB().getCategories();
        while (features.hasNext()) {
            int featureID = features.next();
            double max = -Double.MAX_VALUE;
            categories.begin();
            while (categories.hasNext()) {
                short catID = categories.next();
                double gain = func.compute(catID, featureID, _index);
                if (gain > max)
                    max = gain;
            }

            FeatureItem ci = new FeatureItem();
            ci.featureID = featureID;
            ci.gain = max;
            assert (!Double.isNaN(max));
            ordered.add(ci);
            if (ordered.size() > centroids.length)
                ordered.remove(ordered.first());
        }

        Iterator<FeatureItem> items = ordered.iterator();

        // Choose k random centroids.
        for (int i = 0; i < centroids.length; i++) {
            centroids[i].documents.clear();
            FeatureItem ci = items.next();
            centroids[i].documents.add(ci.featureID);
        }

        // Update centroid for each cluster.
        for (int i = 0; i < centroids.length; i++)
            centroids[i].computeCentroid(dists);
    }

    static class FeatureItem implements Comparable<FeatureItem> {
        public int featureID;
        public double gain;

        public int compareTo(FeatureItem o) {
            if (gain < o.gain)
                return 1;
            else if (gain > o.gain)
                return -1;
            else {
                if (featureID < o.featureID)
                    return 1;
                else if (featureID > o.featureID)
                    return -1;
                else
                    return 0;
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof FeatureItem))
                return false;

            return compareTo((FeatureItem) obj) == 0;
        }

        @Override
        public int hashCode() {
            return new Integer(featureID).hashCode();
        }


    }

}
