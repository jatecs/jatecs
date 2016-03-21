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

public class CosineSimilarityFunction extends BaseSimilarityFunction {

    protected IIntIterator _docs1Feats;
    protected IIndex _idx1;
    protected int _doc1;

    protected IIndex _idx2;
    protected IIntIterator _docs2Feats;
    protected int _doc2;

    public CosineSimilarityFunction() {
    }

    public double compute(TIntDoubleHashMap doc1, TIntDoubleHashMap doc2, IIntIterator features) {

        double numerator = 0;
        double denominator1 = 0;
        double denominator2 = 0;

        features.begin();
        while (features.hasNext()) {
            int featID = features.next();

            double doc1s = doc1.get(featID);
            double doc2s = doc2.get(featID);

            numerator += (doc1s * doc2s);
            denominator1 += (doc1s * doc1s);
            denominator2 += (doc2s * doc2s);
        }

        double denominator = Math.sqrt(denominator1) * Math.sqrt(denominator2);

        double similarity = numerator / denominator;
        if (similarity > 1)
            similarity = 1;

        return similarity;
    }

    public int compareSimilarity(double score1, double score2) {
        if (score1 > score2)
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
        else
            return 0;

        if (feats2.hasNext())
            feat2 = feats2.next();
        else
            return 0;

        double numerator = 0;
        double denominator1 = 0;
        double denominator2 = 0;
        while (feat1 != Integer.MAX_VALUE || feat2 != Integer.MAX_VALUE) {

            if (feat1 < feat2) {
                double doc1s = index.getWeightingDB().getDocumentFeatureWeight(doc1, feat1);
                denominator1 += (doc1s * doc1s);
                if (feats1.hasNext())
                    feat1 = feats1.next();
                else
                    feat1 = Integer.MAX_VALUE;
            } else if (feat2 < feat1) {
                double doc2s = index.getWeightingDB().getDocumentFeatureWeight(doc2, feat2);
                denominator2 += (doc2s * doc2s);

                if (feats2.hasNext())
                    feat2 = feats2.next();
                else
                    feat2 = Integer.MAX_VALUE;
            } else {
                double doc1s = index.getWeightingDB().getDocumentFeatureWeight(doc1, feat1);
                double doc2s = index.getWeightingDB().getDocumentFeatureWeight(doc2, feat2);
                numerator += (doc1s * doc2s);
                denominator1 += (doc1s * doc1s);
                denominator2 += (doc2s * doc2s);

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

        double denominator = Math.sqrt(denominator1) * Math.sqrt(denominator2);

        double similarity = numerator / denominator;
        if (similarity > 1)
            similarity = 1;

        return similarity;
    }


    @Override
    public double compute(int doc1, IIndex idx1, int doc2, IIndex idx2) {
        if (_idx1 != idx1 || doc1 != _doc1) {
            _docs1Feats = idx1.getContentDB().getDocumentFeatures(doc1);
            _idx1 = idx1;
            _doc1 = doc1;
        }

        IIntIterator feats1 = _docs1Feats;
        feats1.begin();

        if (_idx2 != idx2 || doc2 != _doc2) {
            _docs2Feats = idx2.getContentDB().getDocumentFeatures(doc2);
            _idx2 = idx2;
            _doc2 = doc2;
        }
        IIntIterator feats2 = _docs2Feats;
        feats2.begin();

        int feat1 = -1;
        int feat2 = -1;
        if (feats1.hasNext())
            feat1 = feats1.next();
        else
            return 0;

        if (feats2.hasNext())
            feat2 = feats2.next();
        else
            return 0;

        double numerator = 0;
        double denominator1 = 0;
        double denominator2 = 0;
        while (feat1 != Integer.MAX_VALUE || feat2 != Integer.MAX_VALUE) {

            if (feat1 < feat2) {
                double doc1s = idx1.getWeightingDB().getDocumentFeatureWeight(doc1, feat1);
                denominator1 += (doc1s * doc1s);
                if (feats1.hasNext())
                    feat1 = feats1.next();
                else
                    feat1 = Integer.MAX_VALUE;
            } else if (feat2 < feat1) {
                double doc2s = idx2.getWeightingDB().getDocumentFeatureWeight(doc2, feat2);
                denominator2 += (doc2s * doc2s);

                if (feats2.hasNext())
                    feat2 = feats2.next();
                else
                    feat2 = Integer.MAX_VALUE;
            } else {
                double doc1s = idx1.getWeightingDB().getDocumentFeatureWeight(doc1, feat1);
                double doc2s = idx2.getWeightingDB().getDocumentFeatureWeight(doc2, feat2);
                numerator += (doc1s * doc2s);
                denominator1 += (doc1s * doc1s);
                denominator2 += (doc2s * doc2s);

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

        double denominator = Math.sqrt(denominator1) * Math.sqrt(denominator2);
        assert (denominator != 0);

        double similarity = numerator / denominator;
        // Correct round problems.
        if (similarity > 1)
            similarity = 1;

        return similarity;
    }

}
