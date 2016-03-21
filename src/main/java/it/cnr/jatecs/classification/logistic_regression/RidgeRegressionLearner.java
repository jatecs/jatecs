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

package it.cnr.jatecs.classification.logistic_regression;

import it.cnr.jatecs.classification.BaseLearner;
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.interfaces.ILearnerRuntimeCustomizer;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.util.Vector;

public class RidgeRegressionLearner extends BaseLearner {

    public RidgeRegressionLearner() {
        RidgeRegressionLearnerCustomizer cust = new RidgeRegressionLearnerCustomizer();
        setRuntimeCustomizer(cust);
    }

    private double F(double x, double y) {
        double num = Math.exp(Math.abs(y));
        double den = 2 + Math.exp(x) + Math.exp(-x);
        return Math.min(0.25, num / den);
    }

    protected double compute_delta_vj(IIntIterator docs, int j, IIndex index,
                                      double lambda, double[] weight, double[] delta, double[] r,
                                      short catID, double[] y, double[] weightsDoc) {
        int numDocs = index.getDocumentDB().getDocumentsCount();
        double numerator = 0;
        double denominator = 0;
        docs.begin();
        while (docs.hasNext()) {
            int i = docs.next();
            numerator += -1 * (1 / Math.exp(r[i] + 1)) * weightsDoc[i] * y[i];
            denominator += F(r[i], delta[j] * weightsDoc[i]) * weightsDoc[i]
                    * weightsDoc[i];
        }

        numerator += 2 * lambda * numDocs * weight[j];
        denominator += 2 * lambda * numDocs;
        double ret = -(numerator / denominator);
        return ret;
    }

    protected double compute_delta_wj(double delta_vj, double delta_j) {
        return Math.min(Math.max(delta_vj, -delta_j), delta_j);
    }

    @Override
    public IClassifier build(IIndex trainingIndex) {
        RidgeRegressionLearnerCustomizer cust = (RidgeRegressionLearnerCustomizer) getRuntimeCustomizer();
        RidgeRegressionClassifier cl = new RidgeRegressionClassifier(
                trainingIndex.getCategoryDB().getCategoriesCount());
        IShortIterator cats = trainingIndex.getCategoryDB().getCategories();
        while (cats.hasNext()) {
            short catID = cats.next();

            if (catID + 1 % 50 == 0)
                JatecsLogger.status().print(".\n");
            else
                JatecsLogger.status().print(".");

            int numDocs = trainingIndex.getDocumentDB().getDocumentsCount();
            int numFeats = trainingIndex.getFeatureDB().getFeaturesCount();
            double[] delta = new double[numFeats];
            double[] r = new double[numDocs];
            double[] w = new double[numFeats];
            double[] y = new double[numDocs];
            double[] weightsDoc = new double[numDocs];

            // Initialize vectors.
            for (int j = 0; j < delta.length; j++) {
                delta[j] = 1;
                w[j] = 0;
            }

            for (int i = 0; i < numDocs; i++) {
                y[i] = trainingIndex.getClassificationDB().hasDocumentCategory(
                        i, catID) ? +1 : -1;
                weightsDoc[i] = 0;
            }

            for (int k = 0; k < cust.K; k++) {
                for (int j = 0; j < numFeats; j++) {
                    IIntIterator docs = trainingIndex.getContentDB()
                            .getFeatureDocuments(j);
                    while (docs.hasNext()) {
                        int docID = docs.next();
                        weightsDoc[docID] = trainingIndex.getWeightingDB()
                                .getDocumentFeatureWeight(docID, j);
                    }

                    double delta_vj = compute_delta_vj(docs, j, trainingIndex,
                            cust.lambda, w, delta, r, catID, y, weightsDoc);
                    double delta_wj = compute_delta_wj(delta_vj, delta[j]);

                    docs.begin();
                    while (docs.hasNext()) {
                        int i = docs.next();
                        r[i] += delta_wj * weightsDoc[i] * y[i];
                    }

                    w[j] += delta_wj;
                    delta[j] = (2 * Math.abs(delta_wj)) + cust.epsilon;
                }
            }

            cl.weigths[catID] = w;
        }

        JatecsLogger.status().println("done.");

        return cl;
    }

    @Override
    public ILearnerRuntimeCustomizer getRuntimeCustomizer(short catID) {
        return null;
    }

    @Override
    public IClassifier mergeClassifiers(Vector<IClassifier> classifiers) {
        return null;
    }

    @Override
    public void setRuntimeCustomizer(
            Vector<ILearnerRuntimeCustomizer> customizers) {
    }

}
