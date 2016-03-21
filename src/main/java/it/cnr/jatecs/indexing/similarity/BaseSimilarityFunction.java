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

package it.cnr.jatecs.indexing.similarity;

import gnu.trove.TIntDoubleHashMap;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;

public abstract class BaseSimilarityFunction implements ISimilarityFunction {

    public double compute(TIntDoubleHashMap doc1, int doc2, IIndex index) {

        TIntDoubleHashMap d2 = new TIntDoubleHashMap(index.getFeatureDB()
                .getFeaturesCount());
        IIntIterator features = index.getFeatureDB().getFeatures();
        while (features.hasNext()) {
            int featID = features.next();
            d2.put(featID,
                    index.getWeightingDB().getDocumentFeatureWeight(doc2,
                            featID));
        }

        features.begin();
        return compute(doc1, d2, features);
    }

    public double compute(int doc1, int doc2, IIndex index) {

        TIntDoubleHashMap ar1 = new TIntDoubleHashMap(index.getFeatureDB()
                .getFeaturesCount());
        TIntDoubleHashMap ar2 = new TIntDoubleHashMap(index.getFeatureDB()
                .getFeaturesCount());

        IIntIterator features = index.getFeatureDB().getFeatures();
        while (features.hasNext()) {
            int featID = features.next();

            ar1.put(featID,
                    index.getWeightingDB().getDocumentFeatureWeight(doc1,
                            featID));
            ar2.put(featID,
                    index.getWeightingDB().getDocumentFeatureWeight(doc2,
                            featID));
        }

        features.begin();
        return compute(ar1, ar2, features);
    }

    public double compute(int doc1, IIndex idx1, int doc2, IIndex idx2) {

        TIntDoubleHashMap ar1 = new TIntDoubleHashMap(idx1.getFeatureDB()
                .getFeaturesCount());
        TIntDoubleHashMap ar2 = new TIntDoubleHashMap(idx1.getFeatureDB()
                .getFeaturesCount());

        IIntIterator features = idx1.getFeatureDB().getFeatures();
        while (features.hasNext()) {
            int featID = features.next();

            ar1.put(featID,
                    idx1.getWeightingDB()
                            .getDocumentFeatureWeight(doc1, featID));
            ar2.put(featID,
                    idx2.getWeightingDB()
                            .getDocumentFeatureWeight(doc2, featID));
        }

        features.begin();
        return compute(ar1, ar2, features);
    }

}
