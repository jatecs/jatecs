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

package it.cnr.jatecs.indexing.weighting;

import it.cnr.jatecs.indexes.DB.generic.GenericIndex;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveWeightingDBBuilder;
import it.cnr.jatecs.utils.TextualProgressBar;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;

public class TfIdf implements IWeighting {

    private int _numDocs;
    private int[] _numOccurrences;

    public TfIdf(int numDocs, int[] numOccurrences) {
        _numDocs = numDocs;
        _numOccurrences = numOccurrences;
    }

    public IIndex computeWeights(IIndex index) {
        TextualProgressBar bar = new TextualProgressBar(
                "Compute TfIdf weighting");
        int total = _numDocs;
        int step = 0;

        assert (_numOccurrences.length == index.getFeatureDB()
                .getFeaturesCount());

        TroveWeightingDBBuilder weighting = new TroveWeightingDBBuilder(
                index.getContentDB());
        IIntIterator it = index.getDocumentDB().getDocuments();
        while (it.hasNext()) {
            int docID = it.next();
            IIntIterator itFeats = index.getContentDB().getDocumentFeatures(
                    docID);
            double normalization = 0;
            while (itFeats.hasNext()) {
                int featID = itFeats.next();
                if (_numOccurrences[featID] != 0) {
                    double weight = computeTfIdf(index.getContentDB()
                                    .getDocumentFeatureFrequency(docID, featID), total,
                            _numOccurrences[featID]);

                    weighting.setDocumentFeatureWeight(docID, featID, weight);
                    normalization += (weight * weight);
                } else
                    weighting.setDocumentFeatureWeight(docID, featID, 0);
            }

            // Compute the definitive normalization to apply to all
            // features of this document.
            normalization = Math.sqrt(normalization);

            // Normalize feature weights.
            itFeats.begin();
            while (itFeats.hasNext()) {
                int featID = itFeats.next();

                double weight = weighting.getWeightingDB()
                        .getDocumentFeatureWeight(docID, featID);
                weighting.setDocumentFeatureWeight(docID, featID, weight
                        / normalization);
            }

            step++;
            bar.signal((step * 100) / total);
        }

        bar.signal(100);

        GenericIndex i = new GenericIndex("Weighted index",
                index.getFeatureDB(), index.getDocumentDB(),
                index.getCategoryDB(), index.getDomainDB(),
                index.getContentDB(), weighting.getWeightingDB(),
                index.getClassificationDB());

        return i;

    }

    protected double computeTfIdf(int numOccurrencesInsideDoc,
                                  int numTotalDocuments, int featureFrequency) {
        double tfidf = 0;

        double tmp1 = ((double) numTotalDocuments)
                / ((double) (featureFrequency));
        double tmp2 = Math.log(((double) numOccurrencesInsideDoc)) + 1;
        tfidf = tmp2 * Math.log(tmp1);

        return tfidf;
    }
}
