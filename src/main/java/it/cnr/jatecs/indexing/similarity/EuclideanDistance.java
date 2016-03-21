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

public class EuclideanDistance extends BaseSimilarityFunction {

    public EuclideanDistance() {
    }


    public double compute(TIntDoubleHashMap doc1, TIntDoubleHashMap doc2, IIntIterator features) {

        double l = 0;

        features.begin();
        while (features.hasNext()) {
            int featID = features.next();
            l += Math.pow(doc1.get(featID) - doc2.get(featID), 2);
        }

        l = Math.sqrt(l);

        return l;
    }

    public int compareSimilarity(double score1, double score2) {
        if (score1 < score2)
            return 1;
        else if (score1 == score2)
            return 0;
        else
            return -1;
    }

    @Override
    public double compute(int doc1, int doc2, IIndex index) {
        IIntIterator feats1 = index.getContentDB().getDocumentFeatures(doc1);
        IIntIterator feats2 = index.getContentDB().getDocumentFeatures(doc2);

        int feat1 = -1;
        int feat2 = -1;
        if (feats1.hasNext())
            feat1 = feats1.next();
        if (feats2.hasNext())
            feat2 = feats2.next();

        double score = 0;
        while (feat1 != Integer.MAX_VALUE || feat2 != Integer.MAX_VALUE) {

            if (feat1 < feat2) {
                score += Math.pow(index.getWeightingDB().getDocumentFeatureWeight(doc1, feat1), 2);
                if (feats1.hasNext())
                    feat1 = feats1.next();
                else
                    feat1 = Integer.MAX_VALUE;
            } else if (feat2 < feat1) {
                score += Math.pow(index.getWeightingDB().getDocumentFeatureWeight(doc2, feat2), 2);

                if (feats2.hasNext())
                    feat2 = feats2.next();
                else
                    feat2 = Integer.MAX_VALUE;
            } else {
                score += Math.pow(index.getWeightingDB().getDocumentFeatureWeight(doc1, feat1) - index.getWeightingDB().getDocumentFeatureWeight(doc2, feat2), 2);

                if (feats1.hasNext())
                    feat1 = feats1.next();
                else
                    feat1 = Integer.MAX_VALUE;

                if (feats2.hasNext())
                    feat2 = feats2.next();
                else
                    feat2 = Integer.MAX_VALUE;
            }
        }

        score = Math.sqrt(score);

        return score;
    }


    @Override
    public double compute(int doc1, IIndex idx1, int doc2, IIndex idx2) {
        IIntIterator feats1 = idx1.getContentDB().getDocumentFeatures(doc1);
        IIntIterator feats2 = idx2.getContentDB().getDocumentFeatures(doc2);

        int feat1 = -1;
        int feat2 = -1;
        if (feats1.hasNext())
            feat1 = feats1.next();
        if (feats2.hasNext())
            feat2 = feats2.next();

        double score = 0;
        while (feat1 != Integer.MAX_VALUE || feat2 != Integer.MAX_VALUE) {

            if (feat1 < feat2) {
                score += Math.pow(idx1.getWeightingDB().getDocumentFeatureWeight(doc1, feat1), 2);
                if (feats1.hasNext())
                    feat1 = feats1.next();
                else
                    feat1 = Integer.MAX_VALUE;
            } else if (feat2 < feat1) {
                score += Math.pow(idx2.getWeightingDB().getDocumentFeatureWeight(doc2, feat2), 2);

                if (feats2.hasNext())
                    feat2 = feats2.next();
                else
                    feat2 = Integer.MAX_VALUE;
            } else {
                score += Math.pow(idx1.getWeightingDB().getDocumentFeatureWeight(doc1, feat1) - idx2.getWeightingDB().getDocumentFeatureWeight(doc2, feat2), 2);

                if (feats1.hasNext())
                    feat1 = feats1.next();
                else
                    feat1 = Integer.MAX_VALUE;

                if (feats2.hasNext())
                    feat2 = feats2.next();
                else
                    feat2 = Integer.MAX_VALUE;
            }
        }

        score = Math.sqrt(score);

        return score;
    }


}
