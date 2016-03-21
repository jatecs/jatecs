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
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;
import it.cnr.jatecs.weighting.interfaces.IWeighting3D;

import java.util.Iterator;
import java.util.TreeSet;

public class PopularCategoriesClusterizerInitializer implements
        IClusterizerInitializer {

    private IIndex _index;


    public PopularCategoriesClusterizerInitializer(IIndex index) {
        _index = index;
    }

    public void initializeClusters(IWeighting3D dists, Centroid[] centroids) {
        TreeSet<CategoryItem> ordered = new TreeSet<CategoryItem>();
        IShortIterator categories = _index.getCategoryDB().getCategories();
        while (categories.hasNext()) {
            short catID = categories.next();
            int positives = _index.getClassificationDB().getCategoryDocumentsCount(catID);
            CategoryItem ci = new CategoryItem();
            ci.catID = catID;
            ci.positives = positives;
            ordered.add(ci);
        }

        Iterator<CategoryItem> items = ordered.iterator();

        // Choose k random centroids.
        for (int i = 0; i < centroids.length; i++) {
            centroids[i].documents.clear();
            CategoryItem ci = items.next();
            centroids[i].documents.add(ci.catID);
        }

        // Update centroid for each cluster.
        for (int i = 0; i < centroids.length; i++)
            centroids[i].computeCentroid(dists);
    }

    static class CategoryItem implements Comparable<CategoryItem> {
        public short catID;
        public int positives;

        public int compareTo(CategoryItem o) {
            if (positives < o.positives)
                return 11;
            else if (positives > o.positives)
                return -1;
            else {
                if (catID < o.catID)
                    return 1;
                else if (catID > o.catID)
                    return -1;
                else
                    return 0;
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof CategoryItem))
                return false;

            return compareTo((CategoryItem) obj) == 0;
        }

        @Override
        public int hashCode() {
            return new Short(catID).hashCode();
        }


    }

}
