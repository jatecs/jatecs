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

package it.cnr.jatecs.classification.weightedboost;

import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.interfaces.IClassifierRuntimeCustomizer;
import it.cnr.jatecs.classification.interfaces.IDataManager;
import it.cnr.jatecs.classification.interfaces.ILearnerRuntimeCustomizer;
import it.cnr.jatecs.indexing.discretization.DiscreteBin;
import it.cnr.jatecs.io.IStorageManager;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.Os;
import it.cnr.jatecs.weighting.mmap.MemoryMappedWeighting3DBuilder;

import java.io.*;
import java.util.Iterator;
import java.util.TreeSet;

public class WeightedBoostDataManager implements IDataManager {

    static final String CATEGORY_MAPPING_ORIGINAL_MODEL = "CategoryMappingOriginalModel.db";

    static final String DISTRIBUTION_MATRIX = "DistributionMatrix.db";

    static final String WEAK_HYPOTHESIS_COUNTER = "whc.db";

    public WeightedBoostDataManager() {
    }

    @Override
    public void write(IStorageManager storageManager, String modelName,
                      IClassifier learningData) {
        if (storageManager == null)
            throw new NullPointerException("The storage manager is 'null'");
        if (modelName == null || modelName.isEmpty())
            throw new IllegalArgumentException(
                    "The model name is 'null' or 'empty");
        if (learningData == null)
            throw new NullPointerException("The classifier model is 'null'");
        if (!storageManager.isOpen())
            throw new IllegalStateException("The storage manager is not open");

        WeightedBoostClassifier ld = (WeightedBoostClassifier) learningData;

        JatecsLogger
                .status()
                .print("Writing to disk mapping between original categories IDs and model categories IDs...");
        // First write the mapping between orinal categories IDs and categories
        // model IDs.
        writeCategoriesMappingOriginalModel(storageManager, modelName, ld);
        JatecsLogger.status().println("done.");

        JatecsLogger.status().print("Writing to disk all weak hypothesis...");
        // Next write all weak hypothesis to disk.
        writeAllWeakHypothesis(storageManager, modelName, ld);
        JatecsLogger.status().println("done.");

        if (ld._distributionMatrixFilename != null) {
            JatecsLogger.status().print(
                    "Writing to disk the computed distribution matrix...");
            writeDistributionMatrix(storageManager, modelName, ld);
            JatecsLogger.status().println("done.");
        }
    }

    protected void writeCategoriesMappingOriginalModel(
            IStorageManager storageManager, String modelName,
            WeightedBoostClassifier learningData) {
        String fname = modelName + storageManager.getPathSeparator()
                + CATEGORY_MAPPING_ORIGINAL_MODEL;
        DataOutputStream os = new DataOutputStream(new BufferedOutputStream(
                storageManager.getOutputStreamForResource(fname)));

        try {
            try {
                os.writeInt(learningData._validCategories);
            } finally {
                os.close();
            }
        } catch (Exception e) {
            throw new RuntimeException("Writing categories mapping", e);
        }
    }

    protected void writeAllWeakHypothesis(IStorageManager storageManager,
                                          String modelName, WeightedBoostClassifier learningData) {
        String fout = WEAK_HYPOTHESIS_COUNTER;
        DataOutputStream osCounter = new DataOutputStream(
                storageManager.getOutputStreamForResource(fout));

        try {
            osCounter.writeInt(learningData._hypothesis.length);
            writeDiscretizationBins(osCounter, learningData.bins);
            osCounter.close();
        } catch (Exception e) {
            throw new RuntimeException("Writing discretization bins", e);
        }

        for (short catID = 0; catID < learningData._validCategories; catID++) {
            String fname = modelName + storageManager.getPathSeparator()
                    + catID + ".db";

            DataOutputStream os = new DataOutputStream(
                    new BufferedOutputStream(
                            storageManager.getOutputStreamForResource(fname)));

            try {
                try {
                    for (int i = 0; i < learningData._hypothesis.length; i++) {
                        IWeightedWeakHypothesis hyp = learningData._hypothesis[i];
                        WeightedHypothesisData d = hyp.value(catID);

                        os.writeInt(d.pivot);
                        os.writeDouble(d.c0);
                        os.writeInt(d.c1ConstantValues.length);
                        for (int j = 0; j < d.c1ConstantValues.length; j++)
                            os.writeDouble(d.c1ConstantValues[j]);
                    }
                } finally {

                    os.close();
                }
            } catch (Exception e) {
                throw new RuntimeException("Writing weak hypothesis", e);
            }
        }
    }

    private void writeDiscretizationBins(DataOutputStream os,
                                         TreeSet<DiscreteBin>[] bins) throws IOException {
        os.writeInt(bins.length);
        for (int j = 0; j < bins.length; j++) {
            TreeSet<DiscreteBin> featureBins = bins[j];
            os.writeInt(featureBins.size());
            Iterator<DiscreteBin> it = featureBins.iterator();
            while (it.hasNext()) {
                DiscreteBin db = it.next();
                os.writeDouble(db.getStartValue());
                os.writeDouble(db.getEndValue());
            }
        }
    }

    protected void writeDistributionMatrix(IStorageManager storageManager,
                                           String modelName, WeightedBoostClassifier learningData) {
        String fname = modelName + storageManager.getPathSeparator()
                + DISTRIBUTION_MATRIX;

        InputStream is = null;
        OutputStream os = null;
        try {
            try {
                is = new BufferedInputStream(new FileInputStream(
                        learningData._distributionMatrixFilename));
                os = new BufferedOutputStream(
                        storageManager.getOutputStreamForResource(fname));
                Os.copy(is, os);
            } finally {
                if (is != null)
                    is.close();
                if (os != null)
                    os.close();
            }
        } catch (Exception e) {
            throw new RuntimeException("Writing distribution matrix", e);
        }
    }

    @Override
    public IClassifier read(IStorageManager storageManager, String modelName) {
        if (storageManager == null)
            throw new NullPointerException("The storage manager is 'null'");
        if (modelName == null || modelName.isEmpty())
            throw new IllegalArgumentException(
                    "The model name is 'null' or empty");
        if (!storageManager.isOpen())
            throw new IllegalStateException("The storage manager is not open");

        JatecsLogger.status().print(
                "Reading from disk how many categories are stored...");
        // First read the mapping between original categories ID and categories
        // model ID.
        int validCategories = readCategoriesMappingOriginalModel(
                storageManager, modelName);
        JatecsLogger.status().println("done.");

        WeightedBoostClassifier c = new WeightedBoostClassifier();

        c._validCategories = validCategories;

        JatecsLogger.status().print(
                "Reading from disk all stored weak hypothesis...");
        // Init weak hypothesis.
        initWeakHypothesis(c, storageManager, modelName, validCategories);
        JatecsLogger.status().println("done.");

        // File dist = new
        // File(modelDir+Os.pathSeparator()+DISTRIBUTION_MATRIX);
        // if (dist.exists())
        // {
        // JatecsLogger.status().print("Reading from disk the stored
        // distribution matrix...");
        // CompactWeighting3DBuilder matrix = readDistributionMatrix(dist);
        // JatecsLogger.status().println("done.");
        // c._distributionMatrix = matrix;
        // }
        // else
        // c._distributionMatrix = null;

        // Set the maximum and current number of iterations to apply when
        // classify documents.
        c._maxNumIterations = c._hypothesis.length;
        WeightedBoostClassifierCustomizer abcust = (WeightedBoostClassifierCustomizer) c
                .getRuntimeCustomizer();
        abcust._numIterations = c._hypothesis.length;

        JatecsLogger.status().println(
                "The model has stored " + c._maxNumIterations
                        + " iteration(s). Use it as default.");

        return c;
    }

    protected MemoryMappedWeighting3DBuilder readDistributionMatrix(File fname)
            throws IOException {
        MemoryMappedWeighting3DBuilder builder = new MemoryMappedWeighting3DBuilder();

        String path = fname.getParent();
        String name = fname.getName();

        builder.open(path, name, false);

        return builder;
    }

    private int getMaxNumberOfBins(TreeSet<DiscreteBin>[] bins) {
        int maxNumberOfBins = 0;
        for (int i = 0; i < bins.length; i++) {
            if (bins[i].size() > maxNumberOfBins)
                maxNumberOfBins = bins[i].size();
        }

        return maxNumberOfBins;
    }

    protected void initWeakHypothesis(WeightedBoostClassifier c,
                                      IStorageManager storageManager, String modelName, int validCats) {
        String fname = modelName + storageManager.getPathSeparator()
                + WEAK_HYPOTHESIS_COUNTER;
        DataInputStream isCounter = new DataInputStream(
                storageManager.getInputStreamForResource(fname));

        try {
            int maxNumberBins = 0;
            try {
                int numWeakHypothesis = isCounter.readInt();

                TreeSet<DiscreteBin>[] bins = readDiscretizationBins(isCounter);
                c.bins = bins;

                maxNumberBins = getMaxNumberOfBins(bins);
                c._hypothesis = new IWeightedWeakHypothesis[numWeakHypothesis];

            } finally {
                isCounter.close();
            }

            for (short i = 0; i < validCats; i++) {
                short modelCatID = i;
                fname = modelCatID + ".db";

                DataInputStream is = new DataInputStream(
                        new BufferedInputStream(storageManager
                                .getInputStreamForResource(fname)));

                try {
                    int index = 0;
                    while (true) {
                        IWeightedWeakHypothesis wh = c._hypothesis[index];
                        if (wh == null) {
                            wh = new InMemoryWeightedWeakHypothesis(validCats);
                            c._hypothesis[index] = wh;
                        }
                        WeightedHypothesisData hd = new WeightedHypothesisData(
                                maxNumberBins);

                        int pivot = is.readInt();
                        double c0 = is.readDouble();
                        hd.c0 = c0;
                        int numC1 = is.readInt();
                        for (int j = 0; j < numC1; j++) {
                            double c1 = is.readDouble();
                            hd.c1ConstantValues[j] = c1;
                        }

                        hd.pivot = pivot;

                        wh.setValue(modelCatID, hd);
                        index++;
                    }
                } catch (Exception e) {
                } finally {
                    is.close();
                }

            }
        } catch (Exception e) {
            throw new RuntimeException("Initializing weak hypothesis", e);
        }
    }

    private TreeSet<DiscreteBin>[] readDiscretizationBins(DataInputStream is)
            throws IOException {
        int numFeatures = is.readInt();
        @SuppressWarnings("unchecked")
        TreeSet<DiscreteBin>[] bins = new TreeSet[numFeatures];
        for (int i = 0; i < bins.length; i++) {
            TreeSet<DiscreteBin> b = new TreeSet<DiscreteBin>();
            bins[i] = b;
            int numBins = is.readInt();
            for (int j = 0; j < numBins; j++) {
                double startInterval = is.readDouble();
                double endInterval = is.readDouble();
                DiscreteBin db = new DiscreteBin(startInterval, endInterval);
                b.add(db);
            }
        }

        return bins;
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

    public IClassifierRuntimeCustomizer readClassifierRuntimeConfiguration(
            String confDir) throws IOException {
        return null;
    }

    public ILearnerRuntimeCustomizer readLearnerRuntimeConfiguration(
            String confDir) throws IOException {
        return null;
    }

    public void writeClassifierRuntimeConfiguration(String confDir,
                                                    IClassifierRuntimeCustomizer customizer) throws IOException {

    }

    public void writeLearnerRuntimeConfiguration(String confDir,
                                                 ILearnerRuntimeCustomizer customizer) throws IOException {

    }

    @Override
    public void writeLearnerRuntimeConfiguration(
            IStorageManager storageManager, String modelName,
            ILearnerRuntimeCustomizer customizer) {

    }

    @Override
    public ILearnerRuntimeCustomizer readLearnerRuntimeConfiguration(
            IStorageManager storageManager, String modelName) {
        return null;
    }

    @Override
    public void writeClassifierRuntimeConfiguration(
            IStorageManager storageManager, String modelName,
            IClassifierRuntimeCustomizer customizer) {

    }

    @Override
    public IClassifierRuntimeCustomizer readClassifierRuntimeConfiguration(
            IStorageManager storageManager, String modelName) {
        return null;
    }

}
