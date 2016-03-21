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

package it.cnr.jatecs.classification.adaboost;

import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;
import it.cnr.jatecs.weighting.interfaces.IWeighting3DManager;

public class AdaBoostRegressionLearner extends AdaBoostLearner {

    public AdaBoostRegressionLearner() {
        super();

        _customizer = new AdaBoostRegressionLearnerCustomizer();
    }

    protected float phi_prime(int catID, int middle, int last) {
        catID++;
        middle++;
        last++;
        float catValue = 0;
        if (catID <= middle)
            catValue = (float) (catID - middle - 1) / (float) middle;
        else
            catValue = (float) (catID - middle) / (last - middle);

        return catValue;
    }

    protected float phi_second(int catID, int middle, int last, float p) {
        float catVal = phi_prime(catID, middle, last);
        if (catVal >= 0) {
            catVal = (float) Math.pow(catVal, p);
        } else
            catVal = -(float) Math.pow(-catVal, p);

        return catVal;
    }

    @Override
    protected void updateDistributionMatrix(IWeighting3DManager matrix,
                                            IWeakHypothesis wh, IIndex index) {
        AdaBoostRegressionLearnerCustomizer customizer = (AdaBoostRegressionLearnerCustomizer) _customizer;

        float normalization = 0;

        int origFirstCat = customizer.getTreeNode().getNegativeCategories()[0];
        int origLastCat = customizer.getTreeNode().getPositiveCategories()[customizer
                .getTreeNode().getPositiveCategories().length - 1];
        int middle = origFirstCat + ((origLastCat - origFirstCat) / 2);

        for (int document = 0; document < matrix.getSecondDimensionSize(); document++) {

            String docName = index.getDocumentDB().getDocumentName(document);
            IShortIterator docCats = customizer
                    .getOriginalIndex()
                    .getClassificationDB()
                    .getDocumentCategories(
                            customizer.getOriginalIndex().getDocumentDB()
                                    .getDocument(docName));
            assert (docCats.hasNext());
            short catID = docCats.next();
            assert (catID >= origFirstCat && catID <= origLastCat);
            float catValue = phi_second(catID, middle, origLastCat,
                    customizer.getP());
            assert (!Float.isNaN(catValue) && !Float.isInfinite(catValue));
            catValue = -catValue;

            // Compute the weak hypothesis value.
            double value = 0;
            HypothesisData v = wh.value((short) 0);
            int pivot = v.pivot;
            if (pivot >= 0) {
                if (index.getContentDB().hasDocumentFeature(document, pivot))
                    value = v.c1;
                else
                    value = v.c0;
            }

            double exponent = 0;
            if (catValue < 0 && value > 0) {// true positive
                exponent = catValue * value * customizer._TPcorrection;
                matrix.setWeight(matrix.getWeight(0, document, 0)
                                * customizer._lossFunction.getLoss(exponent), 0,
                        document, 0);
                assert (!Double.isNaN(matrix.getWeight(0, document, 0)) && !Double
                        .isInfinite(matrix.getWeight(0, document, 0)));
            } else if (catValue > 0 && value < 0) {// true negative
                exponent = catValue * value * customizer._TNcorrection;
                matrix.setWeight(matrix.getWeight(0, document, 0)
                                * customizer._lossFunction.getLoss(exponent), 0,
                        document, 0);
                assert (!Double.isNaN(matrix.getWeight(0, document, 0)) && !Double
                        .isInfinite(matrix.getWeight(0, document, 0)));
            } else if (catValue < 0 && value < 0) {// false negative
                exponent = catValue * value * customizer._FNcorrection;
                matrix.setWeight(matrix.getWeight(0, document, 0)
                                * customizer._lossFunction.getLoss(exponent), 0,
                        document, 0);
                assert (!Double.isNaN(matrix.getWeight(0, document, 0)) && !Double
                        .isInfinite(matrix.getWeight(0, document, 0)));
            } else if (catValue > 0 && value > 0) { // false positive
                exponent = catValue * value * customizer._FPcorrection;
                matrix.setWeight(matrix.getWeight(0, document, 0)
                                * customizer._lossFunction.getLoss(exponent), 0,
                        document, 0);
                assert (!Double.isNaN(matrix.getWeight(0, document, 0)) && !Double
                        .isInfinite(matrix.getWeight(0, document, 0)));
            }

            // Update normalization value.
            if (Double.isNaN(matrix.getWeight(0, document, 0)))
                System.out.println("nan");
            normalization += matrix.getWeight(0, document, 0);

        }

        if (customizer._perCategoryNormalization) {
            for (int catID = 0; catID < matrix.getFirstDimensionSize(); catID++) {
                normalization = 0;
                for (int docID = 0; docID < matrix.getSecondDimensionSize(); docID++) {
                    assert (!Double.isInfinite(matrix
                            .getWeight(catID, docID, 0)));
                    normalization += matrix.getWeight(catID, docID, 0);
                }

                normalization *= matrix.getFirstDimensionSize();

                for (int docID = 0; docID < matrix.getSecondDimensionSize(); docID++) {
                    matrix.setWeight(matrix.getWeight(catID, docID, 0)
                            / normalization, catID, docID, 0);
                    assert (!Double.isNaN(matrix.getWeight(catID, docID, 0)));
                }
            }
        } else {
            // Update the distribution values with normalization factor.
            for (int docID = 0; docID < matrix.getSecondDimensionSize(); docID++) {
                for (int catID = 0; catID < matrix.getFirstDimensionSize(); catID++) {
                    matrix.setWeight(matrix.getWeight(catID, docID, 0)
                            / normalization, catID, docID, 0);
                }
            }
        }
    }
}
