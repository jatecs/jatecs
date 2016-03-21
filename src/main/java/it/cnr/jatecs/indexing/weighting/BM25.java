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

/**
 * Implementation of BM25 using description in
 * http://citeseerx.ist.psu.edu/viewdoc/summary?doi=10.1.1.97.7340 .
 */
public class BM25 implements IWeighting {

    /**
     * The natural logarithm of 2, used to change the base of logarithms.
     */
    public static final double LOG_2_OF_E = Math.log(2.0D);
    /**
     * The reciprocal of CONSTANT, computed for efficiency.
     */
    public static final double REC_LOG_2_OF_E = 1.0D / LOG_2_OF_E;
    private double k1;
    private double b;
    private boolean useOnlyTFPart;
    private IContentDB refContentDB;

    public BM25(IIndex refIndex) {
        this(refIndex.getContentDB());
    }

    public BM25(IContentDB refContentDB) {
        this.refContentDB = refContentDB;
        k1 = 1.2d;
        b = 0.5d;
        useOnlyTFPart = false;
    }

    public boolean isUseOnlyTFPart() {
        return useOnlyTFPart;
    }

    public void setUseOnlyTFPart(boolean useOnlyTFPart) {
        this.useOnlyTFPart = useOnlyTFPart;
    }

    public double getK1() {
        return k1;
    }

    public void setK1(double k1) {
        this.k1 = k1;
    }

	/*
     * Code coming from Terrier software. See http://ir.dcs.gla.ac.uk/terrier/
	 * for details about software and the license applied.
	 */

    public double getB() {
        return b;
    }

    public void setB(double b) {
        this.b = b;
    }

    public IIndex computeWeights(IIndex index) {
        TextualProgressBar bar = new TextualProgressBar(
                "Compute BM25 weighting");
        bar.setEnabled(false);
        int total = index.getDocumentDB().getDocumentsCount();
        int step = 0;

        double numberOfDocuments = refContentDB.getDocumentDB()
                .getDocumentsCount();
        double avgDocLength = computeDocumentAverageLength(refContentDB);

        TroveWeightingDBBuilder weighting = new TroveWeightingDBBuilder(
                index.getContentDB());
        IIntIterator it = index.getDocumentDB().getDocuments();
        while (it.hasNext()) {
            int docID = it.next();
            IIntIterator itFeats = index.getContentDB().getDocumentFeatures(
                    docID);
            int docLength = index.getContentDB().getDocumentLength(docID);
            double normalization = 0;
            while (itFeats.hasNext()) {
                int featID = itFeats.next();

                double tf = index.getContentDB().getDocumentFeatureFrequency(
                        docID, featID);
                double documentFrequency = refContentDB
                        .getFeatureDocumentsCount(featID);

                double weight = score(tf, numberOfDocuments, documentFrequency,
                        docLength, avgDocLength);

                assert (weight >= 0);
                assert (!Double.isNaN(weight));

                weighting.setDocumentFeatureWeight(docID, featID, weight);
                normalization += (weight * weight);
            }

            // Compute the definitive normalization to apply to all //
            // features of this document.
            normalization = Math.sqrt(normalization);

            // Normalize feature weights.
            itFeats.begin();
            while (itFeats.hasNext()) {
                int featID = itFeats.next();

                double weight = weighting.getWeightingDB()
                        .getDocumentFeatureWeight(docID, featID);
                assert (!Double.isNaN(weight));
                assert (normalization != 0);
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

    protected double computeDocumentAverageLength(final IContentDB contentDB) {
        double avg = 0;

        double step = 0;

        IIntIterator it = contentDB.getDocumentDB().getDocuments();
        while (it.hasNext()) {
            int docID = it.next();
            int docLength = contentDB.getDocumentLength(docID);

            if (step == 0) {
                avg += docLength;
                step++;
            } else {
                double x = (docLength - avg) / (step + 1);
                avg += x;
                step++;
            }
        }

        return avg;
    }

    protected final double score(double tf, double numberOfDocuments,
                                 double documentFrequency, int docLength, double avgDocLength) {

        if (documentFrequency == 0)
            return 0;

        double rsj = Math.log(numberOfDocuments / documentFrequency);
        double K = k1 * ((1 - b) + (b * docLength / avgDocLength));
        double f = ((k1 + 1) * tf) / (K + tf);
        if (isUseOnlyTFPart())
            return f;
        else {
            double weight = f * rsj;
            return weight;
        }
    }

}
