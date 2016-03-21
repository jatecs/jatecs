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
import it.cnr.jatecs.indexes.DB.interfaces.IContentDB;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveWeightingDBBuilder;
import it.cnr.jatecs.utils.TextualProgressBar;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;

import java.util.HashMap;
import java.util.Iterator;

public class TfNormalizedIdf implements IWeighting {

    private IContentDB refContentDB;

    private boolean useRealDocumentTermFrequency;

    private double alpha;

    private HashMap<String, String> dictPrefixesExcluded;

    public TfNormalizedIdf(IIndex idfIndex) {
        this(idfIndex.getContentDB());
    }


    public TfNormalizedIdf(IContentDB refContentDB) {
        if (refContentDB == null)
            throw new NullPointerException("The content DB is 'null'");
        this.refContentDB = refContentDB;
        useRealDocumentTermFrequency = false;
        dictPrefixesExcluded = new HashMap<String, String>();
        setAlpha(0.75d);
    }


    public void putFeaturePrefixToExclude(String prefix) {
        if (prefix == null || prefix.length() == 0)
            throw new IllegalArgumentException("The prefix is invalid");

        dictPrefixesExcluded.put(prefix, prefix);
    }

    public void removeFeaturePrefixToExclude(String prefix) {
        if (prefix == null || prefix.length() == 0)
            throw new IllegalArgumentException("The prefix is invalid");

        dictPrefixesExcluded.remove(prefix);
    }


    public boolean isUseRealDocumentTermFrequency() {
        return useRealDocumentTermFrequency;
    }

    public void setUseRealDocumentTermFrequency(
            boolean useRealDocumentTermFrequency) {
        this.useRealDocumentTermFrequency = useRealDocumentTermFrequency;
    }


    private boolean isFeatureToExclude(String featName) {
        Iterator<String> keys = dictPrefixesExcluded.keySet().iterator();
        while (keys.hasNext()) {
            String prefix = keys.next();
            if (featName.startsWith(prefix))
                return true;
        }

        return false;
    }

    public IIndex computeWeights(IIndex index) {
        TextualProgressBar bar = new TextualProgressBar(
                "Compute Tf normalized - Idf weighting");
        int total = refContentDB.getDocumentDB().getDocumentsCount();
        int step = 0;

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
                String featName = index.getFeatureDB().getFeatureName(featID);
                if (isFeatureToExclude(featName))
                    continue;

                int numOccurrences = index.getContentDB()
                        .getDocumentFeatureFrequency(docID, featID);

                double weight = computeTfIdf(numOccurrences, total, refContentDB.getFeatureDocumentsCount(featID));

                weighting.setDocumentFeatureWeight(docID, featID, weight);
                normalization += (weight * weight);
            }

            // Compute the definitive normalization to apply to all
            // features of this document.
            normalization = Math.sqrt(normalization);

            // Normalize feature weights.
            if (normalization > 0) {
                itFeats.begin();
                while (itFeats.hasNext()) {
                    int featID = itFeats.next();
                    String featName = index.getFeatureDB().getFeatureName(featID);
                    if (isFeatureToExclude(featName))
                        continue;

                    double weight = weighting.getWeightingDB()
                            .getDocumentFeatureWeight(docID, featID);

                    weight /= normalization;

                    if (weight < 0 || weight > 1)
                        System.out.println("Non ci siamo");
                    assert (weight >= 0 && weight <= 1);
                    weighting.setDocumentFeatureWeight(docID, featID, weight);
                }
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

        //could be the case that feature frequency is 0... since it is taken from a different index
        if (featureFrequency == 0)
            return 0.0;

        double tmp1 = ((double) numTotalDocuments)
                / ((double) (featureFrequency));
        double tmp2 = 0;
        if (!isUseRealDocumentTermFrequency()) {
            if (numOccurrencesInsideDoc > 0)
                tmp2 = Math.log(((double) numOccurrencesInsideDoc)) + 1;
        } else {
            tmp2 = numOccurrencesInsideDoc;
        }
        tfidf = tmp2 * Math.pow(Math.log(tmp1), alpha);

        return tfidf;
    }

    public double getAlpha() {
        return alpha;
    }

    public void setAlpha(double alpha) {
        this.alpha = alpha;
    }
}
