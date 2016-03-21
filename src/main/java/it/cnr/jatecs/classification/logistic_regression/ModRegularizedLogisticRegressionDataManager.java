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

import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.interfaces.IClassifierRuntimeCustomizer;
import it.cnr.jatecs.classification.interfaces.IDataManager;
import it.cnr.jatecs.classification.interfaces.ILearnerRuntimeCustomizer;
import it.cnr.jatecs.io.IStorageManager;
import it.cnr.jatecs.utils.Os;

import java.io.*;

public class ModRegularizedLogisticRegressionDataManager implements
        IDataManager {

    public void write(String modelDir, IClassifier learningData)
            throws IOException {
        File d = new File(modelDir);
        if (!d.exists())
            d.mkdirs();

        ModRegularizedLogisticRegressionClassifier c = (ModRegularizedLogisticRegressionClassifier) learningData;

        String vc = modelDir + Os.pathSeparator() + "data.db";
        DataOutputStream os = new DataOutputStream(new BufferedOutputStream(
                new FileOutputStream(vc), 4096));
        os.writeInt(c.getCategoryCount());

        os.writeInt(c.weigths[0].length);
        for (short catID = 0; catID < c.weigths.length; catID++) {
            for (int i = 0; i < c.weigths[0].length; i++) {
                os.writeDouble(c.weigths[catID][i]);
            }
        }

        os.close();
    }

    public IClassifier read(String modelDir) throws IOException {
        String vc = modelDir + Os.pathSeparator() + "data.db";
        DataInputStream is = new DataInputStream(new BufferedInputStream(
                new FileInputStream(vc), 4096));

        int numCats = is.readInt();
        int numFeatures = is.readInt();

        ModRegularizedLogisticRegressionClassifier c = new ModRegularizedLogisticRegressionClassifier(
                numCats);
        for (int catID = 0; catID < numCats; catID++) {
            c.weigths[catID] = new double[numFeatures];
            for (int i = 0; i < numFeatures; i++) {
                double w = is.readDouble();
                c.weigths[catID][i] = w;
            }
        }

        is.close();

        return c;
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

        ModRegularizedLogisticRegressionClassifier c = (ModRegularizedLogisticRegressionClassifier) learningData;

        String vc = modelName + storageManager.getPathSeparator() + "data.db";
        DataOutputStream os = new DataOutputStream(new BufferedOutputStream(
                storageManager.getOutputStreamForResource(vc), 4096));
        try {
            try {
                os.writeInt(c.getCategoryCount());

                os.writeInt(c.weigths[0].length);
                for (short catID = 0; catID < c.weigths.length; catID++) {
                    for (int i = 0; i < c.weigths[0].length; i++) {
                        os.writeDouble(c.weigths[catID][i]);
                    }
                }
            } finally {
                os.close();
            }
        } catch (Exception e) {
            throw new RuntimeException("Writing classifier data", e);
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

        String vc = modelName + storageManager.getPathSeparator() + "data.db";
        DataInputStream is = new DataInputStream(new BufferedInputStream(
                storageManager.getInputStreamForResource(vc), 4096));

        try {
            try {
                int numCats = is.readInt();
                int numFeatures = is.readInt();

                ModRegularizedLogisticRegressionClassifier c = new ModRegularizedLogisticRegressionClassifier(
                        numCats);
                for (int catID = 0; catID < numCats; catID++) {
                    c.weigths[catID] = new double[numFeatures];
                    for (int i = 0; i < numFeatures; i++) {
                        double w = is.readDouble();
                        c.weigths[catID][i] = w;
                    }
                }

                return c;
            } finally {
                is.close();
            }
        } catch (Exception e) {
            throw new RuntimeException("Reading classifier data", e);
        }
    }

    @Override
    public void writeLearnerRuntimeConfiguration(
            IStorageManager storageManager, String modelName,
            ILearnerRuntimeCustomizer customizer) {
        // TODO Auto-generated method stub

    }

    @Override
    public ILearnerRuntimeCustomizer readLearnerRuntimeConfiguration(
            IStorageManager storageManager, String modelName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void writeClassifierRuntimeConfiguration(
            IStorageManager storageManager, String modelName,
            IClassifierRuntimeCustomizer customizer) {
        // TODO Auto-generated method stub

    }

    @Override
    public IClassifierRuntimeCustomizer readClassifierRuntimeConfiguration(
            IStorageManager storageManager, String modelName) {
        // TODO Auto-generated method stub
        return null;
    }

}
