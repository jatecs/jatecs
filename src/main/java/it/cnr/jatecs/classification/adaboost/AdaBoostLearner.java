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

import it.cnr.jatecs.classification.BaseLearner;
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.interfaces.ILearnerRuntimeCustomizer;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.Os;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;
import it.cnr.jatecs.weighting.interfaces.IWeighting3DManager;
import it.cnr.jatecs.weighting.mmap.MemoryMappedWeighting3DBuilder;

import java.io.File;
import java.io.IOException;
import java.rmi.server.UID;
import java.util.Vector;

/**
 * This class implements the skeleton of a boosting learner, following the
 * AdaBoost.MH algorithm. It can be customized by specifying the weak learner to
 * use.
 */
public class AdaBoostLearner extends BaseLearner {

    public AdaBoostLearner() {
        _customizer = new AdaBoostLearnerCustomizer();
    }

    public IClassifier build(IIndex trainingIndex) {

        AdaBoostLearnerCustomizer customizer = (AdaBoostLearnerCustomizer) _customizer;

        // Declare the distribution matrix used in boosting algorithm.
        MemoryMappedWeighting3DBuilder matrix = new MemoryMappedWeighting3DBuilder(
                trainingIndex.getCategoryDB().getCategoriesCount(),
                trainingIndex.getDocumentDB().getDocumentsCount(), 1);
        String path = Os.getTemporaryDirectory() + Os.pathSeparator()
                + "matrixes" + Os.pathSeparator();
        File dir = new File(path);
        if (dir.exists()) {
            if (!dir.isDirectory()) {
                dir.delete();
                if (!dir.mkdirs())
                    throw new RuntimeException(
                            "Unable to create the directory " + dir.toString());
            }
        } else if (!dir.mkdirs()) {
            throw new RuntimeException("Unable to create the directory "
                    + dir.toString());
        }
        String fname = "matrix_" + new UID().toString().replace(':', '_')
                + ".db";

        try {
            matrix.open(path, fname, true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Get the initial distribution matrix.
        JatecsLogger.status().print("Getting initial distribution matrix...");
        obtainInitialDistributionMatrix(matrix, trainingIndex);
        JatecsLogger.status().println("done.");

        AdaBoostClassifier classifier = new AdaBoostClassifier();
        classifier._hypothesis = new IWeakHypothesis[customizer._maxNumIterations];
        classifier._maxNumIterations = customizer._maxNumIterations;
        AdaBoostClassifierCustomizer abcust = (AdaBoostClassifierCustomizer) classifier
                .getRuntimeCustomizer();
        abcust._numIterations = customizer._maxNumIterations;
        classifier._validCategories = trainingIndex.getCategoryDB()
                .getCategoriesCount();
        classifier._distributionMatrixFilename = path + fname;

        int numComputed = 0;
        int toCompute = customizer._maxNumIterations;

        JatecsLogger.status().println("Computing the iterations...");

        // For each iteration, generate a weak hypothesis.
        for (int i = 0; i < customizer._maxNumIterations; i++) {
            // Get the i-th weak hypothesis.
            IWeakHypothesis wh = null;
            wh = customizer._wl.getNewWeakHypothesis(matrix, trainingIndex);
            classifier._hypothesis[i] = wh;

            // Compute the distribution matrix to use in step (i+1).
            updateDistributionMatrix(matrix, wh, trainingIndex);

            if (((i + 1) % 5) == 0)
                JatecsLogger.status().print("" + (i + 1));
            else
                JatecsLogger.status().print(".");

            if (((i + 1) % 50) == 0)
                JatecsLogger.status().println("");

            numComputed++;
            double percentage = ((double) (numComputed * 100))
                    / ((double) (toCompute));
            if (customizer.getStatusListener() != null)
                customizer.getStatusListener().operationStatus(percentage);
        }

        JatecsLogger.status().println("done.");

        // If necessary, save the distribution matrix.
        try {
            matrix.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (!customizer._keepMatrix) {
            File f = new File(path + fname);
            Os.delete(f);
        }

        if (customizer.getStatusListener() != null)
            customizer.getStatusListener().operationStatus(100);

        return classifier;
    }

    protected void obtainInitialDistributionMatrix(IWeighting3DManager matrix,
                                                   IIndex index) {
        if (((AdaBoostLearnerCustomizer) _customizer)._matrixFileLoad != null) {
            obtainInitialDistributionMatrixFromStoredMatrix(matrix);
        } else {
            if (((AdaBoostLearnerCustomizer) _customizer)._initDistType == InitialDistributionMatrixType.UNIFORM)
                obtainUniformDistributionMatrix(matrix, index);
            else if (((AdaBoostLearnerCustomizer) _customizer)._initDistType == InitialDistributionMatrixType.CATEGORY_ORIENTED)
                obtainCategoryOrientedDistributionMatrix(matrix, index);
            else if (((AdaBoostLearnerCustomizer) _customizer)._initDistType == InitialDistributionMatrixType.MAXIMIZE_F1)
                obtainMaximizeF1DistributionMatrix(matrix, index);
            else
                throw new RuntimeException(
                        "Type "
                                + ((AdaBoostLearnerCustomizer) _customizer)._initDistType
                                .toString() + " not supported");
        }
    }

    protected void obtainInitialDistributionMatrixFromStoredMatrix(
            IWeighting3DManager matrix) {
        JatecsLogger
                .execution()
                .warning(
                        "Called empty method AdaBoostLearner.obtainInitialDistributionMatrixFromStoredMatrix\n");
    }

    protected void obtainUniformDistributionMatrix(IWeighting3DManager matrix,
                                                   IIndex index) {
        float uniformValue = 1 / ((float) (index.getDocumentDB()
                .getDocumentsCount() * index.getCategoryDB()
                .getCategoriesCount()));

        for (int i = 0; i < matrix.getSecondDimensionSize(); i++) {
            for (int j = 0; j < matrix.getFirstDimensionSize(); j++) {
                matrix.setWeight(uniformValue, j, i, 0);
            }
        }
    }

    protected void obtainCategoryOrientedDistributionMatrix(
            IWeighting3DManager matrix, IIndex index) {
        int numDocuments = index.getDocumentDB().getDocumentsCount();

        float normalization = 0;
        float factorPositive = ((AdaBoostLearnerCustomizer) _customizer)._factorPositive;

        IShortIterator catIt = index.getCategoryDB().getCategories();
        while (catIt.hasNext()) {
            short category = catIt.next();
            int catFrequency = index.getClassificationDB()
                    .getCategoryDocumentsCount(category);

            float catProbability = ((float) catFrequency)
                    / ((float) numDocuments);
            float uniformProbability = catProbability / ((float) numDocuments);
            float totalProbPositive = ((float) catFrequency) * factorPositive
                    * uniformProbability;
            float probPositive = totalProbPositive / (float) catFrequency;
            float probNegative = (catProbability - totalProbPositive)
                    / ((float) (numDocuments - catFrequency));

            for (int j = 0; j < matrix.getSecondDimensionSize(); j++) {

                if (index.getClassificationDB()
                        .hasDocumentCategory(j, category)) {
                    matrix.setWeight(probPositive, category, j, 0);
                    normalization += probPositive;
                } else {
                    matrix.setWeight(probNegative, category, j, 0);
                    normalization += probNegative;
                }
            }
        }

        // Normalized matrix.
        for (int i = 0; i < matrix.getFirstDimensionSize(); i++)
            for (int j = 0; j < matrix.getSecondDimensionSize(); j++)
                matrix.setWeight(matrix.getWeight(i, j, 0) / normalization, i,
                        j, 0);
    }

    protected void obtainMaximizeF1DistributionMatrix(
            IWeighting3DManager matrix, IIndex index) {
        int numDocuments = index.getDocumentDB().getDocumentsCount();
        int numCategories = index.getCategoryDB().getCategoriesCount();

        double Z0 = 0.0;

        for (short category = 0; category < numCategories; category++) {
            int catFrequency = index.getClassificationDB()
                    .getCategoryDocumentsCount(category);

            int numDocPositive = catFrequency;

            float probPositive = numDocuments / ((float) numDocPositive);
            float probNegative = 1;

            for (int document = 0; document < matrix.getSecondDimensionSize(); document++) {

                if (index.getClassificationDB().hasDocumentCategory(document,
                        category)) {
                    matrix.setWeight(probPositive, category, document, 0);
                    Z0 += probPositive;
                } else {
                    matrix.setWeight(probNegative, category, document, 0);
                    Z0 += probNegative;
                }
            }
        }

        if (((AdaBoostLearnerCustomizer) _customizer)._perCategoryNormalization) {
            for (int catID = 0; catID < numCategories; catID++) {
                Z0 = 0;
                for (int docID = 0; docID < numDocuments; docID++)
                    Z0 += matrix.getWeight(catID, docID, 0);

                Z0 *= numCategories;

                for (int docID = 0; docID < numDocuments; docID++)
                    matrix.setWeight(matrix.getWeight(catID, docID, 0) / Z0,
                            catID, docID, 0);
            }
        } else {
            // normalization
            for (int i = 0; i < numCategories; i++)
                for (int j = 0; j < numDocuments; j++)
                    matrix.setWeight(matrix.getWeight(i, j, 0) / Z0, i, j, 0);
        }
    }

    protected void updateDistributionMatrix(IWeighting3DManager matrix,
                                            IWeakHypothesis wh, IIndex index) {
        AdaBoostLearnerCustomizer customizer = (AdaBoostLearnerCustomizer) _customizer;

        float normalization = 0;

        for (int document = 0; document < matrix.getSecondDimensionSize(); document++) {
            for (short category = 0; category < matrix.getFirstDimensionSize(); category++) {
                float catValue = 1;
                if (index.getClassificationDB().hasDocumentCategory(document,
                        category)) {
                    catValue = -1;
                }

                // Compute the weak hypothesis value.
                double value = 0;
                HypothesisData v = wh.value(category);
                int pivot = v.pivot;
                if (pivot >= 0) {
                    if (index.getContentDB()
                            .hasDocumentFeature(document, pivot))
                        value = v.c1;
                    else
                        value = v.c0;
                }

                double exponent = 0;
                if (catValue < 0 && value > 0) {// true positive
                    exponent = catValue * value * customizer._TPcorrection;
                    matrix.setWeight(matrix.getWeight(category, document, 0)
                                    * customizer._lossFunction.getLoss(exponent),
                            category, document, 0);
                } else if (catValue > 0 && value < 0) {// true negative
                    exponent = catValue * value * customizer._TNcorrection;
                    matrix.setWeight(matrix.getWeight(category, document, 0)
                                    * customizer._lossFunction.getLoss(exponent),
                            category, document, 0);
                } else if (catValue < 0 && value < 0) {// false negative
                    exponent = catValue * value * customizer._FNcorrection;
                    matrix.setWeight(matrix.getWeight(category, document, 0)
                                    * customizer._lossFunction.getLoss(exponent),
                            category, document, 0);
                } else if (catValue > 0 && value > 0) { // false positive
                    exponent = catValue * value * customizer._FPcorrection;
                    matrix.setWeight(matrix.getWeight(category, document, 0)
                                    * customizer._lossFunction.getLoss(exponent),
                            category, document, 0);
                }

                // Update normalization value.
                normalization += matrix.getWeight(category, document, 0);
            }
        }

        if (customizer._perCategoryNormalization) {
            for (int catID = 0; catID < matrix.getFirstDimensionSize(); catID++) {
                normalization = 0;
                for (int docID = 0; docID < matrix.getSecondDimensionSize(); docID++)
                    normalization += matrix.getWeight(catID, docID, 0);

                normalization *= matrix.getFirstDimensionSize();

                for (int docID = 0; docID < matrix.getSecondDimensionSize(); docID++)
                    matrix.setWeight(matrix.getWeight(catID, docID, 0)
                            / normalization, catID, docID, 0);
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

    @Override
    public IClassifier mergeClassifiers(Vector<IClassifier> classifiers) {
        int maxIterations = -1;
        for (int i = 0; i < classifiers.size(); i++) {
            IClassifier c = classifiers.get(i);
            if (c.getCategoryCount() != 1)
                throw new RuntimeException(
                        "The number of categories data stored in classifier ("
                                + c.getCategoryCount() + ") is greater than 1.");

            AdaBoostClassifier cl = (AdaBoostClassifier) c;

            if (maxIterations == -1)
                maxIterations = cl._maxNumIterations;

            if (cl._maxNumIterations != maxIterations)
                throw new RuntimeException(
                        "At least one classifier differs in the number of stored iterations.");
        }

        AdaBoostClassifier cl = new AdaBoostClassifier();

        cl._hypothesis = new IWeakHypothesis[maxIterations];
        cl._maxNumIterations = maxIterations;
        AdaBoostClassifierCustomizer abcust = (AdaBoostClassifierCustomizer) cl
                .getRuntimeCustomizer();
        abcust._numIterations = maxIterations;
        cl._validCategories = classifiers.size();
        cl._distributionMatrixFilename = null;
        // cl.distributionMatrix = null;

        for (int i = 0; i < maxIterations; i++) {
            IWeakHypothesis hyp = new InMemoryWeakHypothesis(classifiers.size());

            for (short j = 0; j < classifiers.size(); j++) {
                AdaBoostClassifier c = (AdaBoostClassifier) classifiers.get(j);
                HypothesisData d = c._hypothesis[i].value((short) 0);
                hyp.setValue(j, d);
            }

            // Save hypothesis.
            cl._hypothesis[i] = hyp;
        }

        return cl;
    }

    @Override
    public ILearnerRuntimeCustomizer getRuntimeCustomizer(short catID) {
        return null;
    }

    @Override
    public void setRuntimeCustomizer(
            Vector<ILearnerRuntimeCustomizer> customizers) {

    }
}
