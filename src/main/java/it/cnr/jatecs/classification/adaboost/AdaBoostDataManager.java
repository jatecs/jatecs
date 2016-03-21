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

import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.interfaces.IClassifierRuntimeCustomizer;
import it.cnr.jatecs.classification.interfaces.IDataManager;
import it.cnr.jatecs.classification.interfaces.ILearnerRuntimeCustomizer;
import it.cnr.jatecs.io.IStorageManager;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.Os;

import java.io.*;

public class AdaBoostDataManager implements IDataManager {

    static final String CATEGORY_MAPPING_ORIGINAL_MODEL = "CategoryMappingOriginalModel.db";

    static final String DISTRIBUTION_MATRIX = "DistributionMatrix.db";

    static final String WEAK_HYPOTHESIS_COUNTER = "whc.db";


    public AdaBoostDataManager() {
    }

    protected void writeCategoriesMappingOriginalModel(
            IStorageManager storageManager, String modelName,
            AdaBoostClassifier learningData) {
        String fname = modelName + storageManager.getPathSeparator()
                + CATEGORY_MAPPING_ORIGINAL_MODEL;
        DataOutputStream os = new DataOutputStream(new BufferedOutputStream(
                storageManager.getOutputStreamForResource(fname)));

        try {
            try {

                os.writeInt(learningData._validCategories);

            } finally {
                // Close the stream.
                os.close();
            }
        } catch (Exception e) {
            throw new RuntimeException("Writing categories mapping", e);
        }
    }

    protected void writeAllWeakHypothesis(String modelDir,
                                          AdaBoostClassifier learningData) throws IOException {
        String fout = modelDir + Os.pathSeparator() + WEAK_HYPOTHESIS_COUNTER;
        DataOutputStream osCounter = new DataOutputStream(new FileOutputStream(
                fout));

        osCounter.writeInt(learningData._hypothesis.length);
        osCounter.close();

        for (short catID = 0; catID < learningData._validCategories; catID++) {
            String fname = modelDir + Os.pathSeparator() + catID + ".db";

            DataOutputStream os = new DataOutputStream(
                    new BufferedOutputStream(new FileOutputStream(fname)));

            for (int i = 0; i < learningData._hypothesis.length; i++) {
                IWeakHypothesis hyp = learningData._hypothesis[i];
                HypothesisData d = hyp.value(catID);

                os.writeInt(d.pivot);
                os.writeDouble(d.c0);
                os.writeDouble(d.c1);
            }

            os.close();
        }
    }

    protected void writeAllWeakHypothesis(IStorageManager storageManager,
                                          String modelName, AdaBoostClassifier learningData) {
        String fout = modelName + storageManager.getPathSeparator()
                + WEAK_HYPOTHESIS_COUNTER;
        DataOutputStream osCounter = new DataOutputStream(
                storageManager.getOutputStreamForResource(fout));
        try {
            try {
                osCounter.writeInt(learningData._hypothesis.length);
            } finally {
                osCounter.close();
            }
        } catch (Exception e) {
            throw new RuntimeException("Writing hypothesis length", e);
        }

        try {
            for (short catID = 0; catID < learningData._validCategories; catID++) {
                String fname = modelName + storageManager.getPathSeparator()
                        + catID + ".db";

                DataOutputStream os = new DataOutputStream(
                        new BufferedOutputStream(
                                storageManager
                                        .getOutputStreamForResource(fname)));
                try {
                    for (int i = 0; i < learningData._hypothesis.length; i++) {
                        IWeakHypothesis hyp = learningData._hypothesis[i];
                        HypothesisData d = hyp.value(catID);

                        os.writeInt(d.pivot);
                        os.writeDouble(d.c0);
                        os.writeDouble(d.c1);
                    }
                } finally {
                    os.close();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Writing hypothesis data", e);
        }
    }

    protected void writeDistributionMatrix(IStorageManager storageManager,
                                           String modelName, AdaBoostClassifier learningData) {
        try {
            String fname = modelName + storageManager.getPathSeparator()
                    + DISTRIBUTION_MATRIX;

            File src = new File(learningData._distributionMatrixFilename);

            InputStream is = new FileInputStream(src);
            OutputStream os = storageManager.getOutputStreamForResource(fname);
            try {
                byte[] oBuffer = new byte[1024 * 4];
                int n = 0;
                while ((n = is.read(oBuffer)) != -1)
                    os.write(oBuffer, 0, n);
            } finally {
                is.close();
                os.close();
            }
        } catch (Exception e) {
            throw new RuntimeException("Writing distribution matrix", e);
        }
    }

    protected void initWeakHypothesis(IStorageManager storageManager,
                                      AdaBoostClassifier c, String modelName, int validCats) {
        try {
            String fname = modelName + storageManager.getPathSeparator()
                    + WEAK_HYPOTHESIS_COUNTER;
            DataInputStream isCounter = new DataInputStream(
                    storageManager.getInputStreamForResource(fname));

            int numWeakHypothesis = 0;
            try {
                numWeakHypothesis = isCounter.readInt();
            } finally {
                isCounter.close();
            }

            c._hypothesis = new IWeakHypothesis[numWeakHypothesis];

            for (short i = 0; i < validCats; i++) {
                short modelCatID = i;
                fname = modelName + storageManager.getPathSeparator()
                        + modelCatID + ".db";

                DataInputStream is = new DataInputStream(
                        new BufferedInputStream(storageManager
                                .getInputStreamForResource(fname)));

                try {
                    int index = 0;
                    for (int j = 0; j < numWeakHypothesis; j++) {
                        int pivot = is.readInt();
                        double c0 = is.readDouble();
                        double c1 = is.readDouble();

                        IWeakHypothesis wh = c._hypothesis[index];
                        if (wh == null) {
                            wh = new InMemoryWeakHypothesis(validCats);
                            c._hypothesis[index] = wh;
                        }

                        HypothesisData hd = new HypothesisData();
                        hd.c0 = c0;
                        hd.c1 = c1;
                        hd.pivot = pivot;

                        wh.setValue(modelCatID, hd);
                        index++;
                    }
                } finally {
                    is.close();
                }

            }
        } catch (Exception e) {
            throw new RuntimeException("Reading weak hypothesis", e);
        }
    }

    protected int readCategoriesMappingOriginalModel(
            IStorageManager storageManager, String modelName) {
        String fname = modelName + storageManager.getPathSeparator()
                + CATEGORY_MAPPING_ORIGINAL_MODEL;
        DataInputStream is = new DataInputStream(new BufferedInputStream(
                storageManager.getInputStreamForResource(fname)));
        try {
            try {
                int numCats = is.readInt();
                return numCats;
            } finally {

                is.close();
            }
        } catch (Exception e) {
            throw new RuntimeException("Reading categories mapping", e);
        }
    }

    @Override
    public void write(IStorageManager storageManager, String modelName,
                      IClassifier learningData) {
        if (storageManager == null)
            throw new NullPointerException("The storage manager is 'null'");
        if (modelName == null || modelName.isEmpty())
            throw new IllegalArgumentException("The model name is invalid");
        if (learningData == null)
            throw new NullPointerException("The classifier is 'null'");
        if (!storageManager.isOpen())
            throw new IllegalStateException("The storage manager is not open");

        AdaBoostClassifier ld = (AdaBoostClassifier) learningData;

        JatecsLogger
                .status()
                .fine("Writing to disk mapping between original categories IDs and model categories IDs...");
        // First write the mapping between orinal categories IDs and categories
        // model IDs.
        writeCategoriesMappingOriginalModel(storageManager, modelName, ld);
        JatecsLogger.status().fine("done.\n");

        JatecsLogger.status().fine("Writing to disk all weak hypothesis...");
        // Next write all weak hypothesis to disk.
        writeAllWeakHypothesis(storageManager, modelName, ld);
        JatecsLogger.status().fine("done.\n");

        if (ld._distributionMatrixFilename != null) {
            JatecsLogger.status().fine(
                    "Writing to disk the computed distribution matrix...");
            writeDistributionMatrix(storageManager, modelName, ld);
            JatecsLogger.status().fine("done.\n");
        }
    }

    @Override
    public IClassifier read(IStorageManager storageManager, String modelName) {

        if (storageManager == null)
            throw new NullPointerException("The storage manager is 'null'");
        if (modelName == null || modelName.isEmpty())
            throw new IllegalArgumentException("The model name is invalid");
        if (!storageManager.isOpen())
            throw new IllegalStateException("The storage manager is not open");

        JatecsLogger.status().fine(
                "Reading from disk how many categories are stored...");
        // First read the mapping between original categories ID and categories
        // model ID.
        int validCategories = readCategoriesMappingOriginalModel(
                storageManager, modelName);
        JatecsLogger.status().fine("done.\n");

        AdaBoostClassifier c = new AdaBoostClassifier();
        JatecsLogger.status().fine("Using search implementation\n");

        c._validCategories = validCategories;

        JatecsLogger.status().fine(
                "Reading from disk all stored weak hypothesis...");
        // Init weak hypothesis.
        initWeakHypothesis(storageManager, c, modelName, validCategories);
        JatecsLogger.status().fine("done.\n");

        // Set the maximum and current number of iterations to apply when
        // classify documents.
        c._maxNumIterations = c._hypothesis.length;
        AdaBoostClassifierCustomizer abcust = (AdaBoostClassifierCustomizer) c
                .getRuntimeCustomizer();
        abcust._numIterations = c._hypothesis.length;

        JatecsLogger.status().println(
                "The model has stored " + c._maxNumIterations
                        + " iteration(s). Use it as default.");

        return c;
    }

    @Override
    public void writeLearnerRuntimeConfiguration(
            IStorageManager storageManager, String modelName,
            ILearnerRuntimeCustomizer customizer) {
        if (storageManager == null)
            throw new NullPointerException("The storage manager is 'null'");
        if (modelName == null || modelName.isEmpty())
            throw new IllegalArgumentException("The model name is invalid");
        if (customizer == null)
            throw new NullPointerException();
        if (!storageManager.isOpen())
            throw new IllegalStateException("The storage manager is not open");
    }

    @Override
    public ILearnerRuntimeCustomizer readLearnerRuntimeConfiguration(
            IStorageManager storageManager, String modelName) {
        if (storageManager == null)
            throw new NullPointerException("The storage manager is 'null'");
        if (modelName == null || modelName.isEmpty())
            throw new IllegalArgumentException("The model name is invalid");
        if (!storageManager.isOpen())
            throw new IllegalStateException("The storage manager is not open");

        return null;
    }

    @Override
    public void writeClassifierRuntimeConfiguration(
            IStorageManager storageManager, String modelName,
            IClassifierRuntimeCustomizer customizer) {
        if (storageManager == null)
            throw new NullPointerException("The storage manager is 'null'");
        if (modelName == null || modelName.isEmpty())
            throw new IllegalArgumentException("The model name is invalid");
        if (customizer == null)
            throw new NullPointerException();
        if (!storageManager.isOpen())
            throw new IllegalStateException("The storage manager is not open");
    }

    @Override
    public IClassifierRuntimeCustomizer readClassifierRuntimeConfiguration(
            IStorageManager storageManager, String modelName) {
        if (storageManager == null)
            throw new NullPointerException("The storage manager is 'null'");
        if (modelName == null || modelName.isEmpty())
            throw new IllegalArgumentException("The model name is invalid");
        if (!storageManager.isOpen())
            throw new IllegalStateException("The storage manager is not open");

        return null;
    }

}
