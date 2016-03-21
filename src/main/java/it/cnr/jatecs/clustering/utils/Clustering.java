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

package it.cnr.jatecs.clustering.utils;

import gnu.trove.TIntDoubleHashMap;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;

public class Clustering {

    public static TIntDoubleHashMap computeDocumentCentroid(IIntIterator docs,
                                                            IIndex index) {
        TIntDoubleHashMap centroid = new TIntDoubleHashMap(index.getFeatureDB()
                .getFeaturesCount());

        int numDoc = 0;
        docs.begin();
        while (docs.hasNext()) {
            int docID = docs.next();
            IIntIterator feats = index.getContentDB()
                    .getDocumentFeatures(docID);
            while (feats.hasNext()) {
                int featID = feats.next();

                centroid.put(
                        featID,
                        centroid.get(featID)
                                + index.getWeightingDB()
                                .getDocumentFeatureWeight(docID, featID));
            }

            numDoc++;
        }

        int keys[] = centroid.keys();
        for (int i = 0; i < keys.length; i++) {
            centroid.put(keys[i], centroid.get(keys[i]) / (double) numDoc);
        }

        return centroid;
    }

}
