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

package it.cnr.jatecs.classification.svm;

import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.interfaces.IClassifierRuntimeCustomizer;
import it.cnr.jatecs.classification.interfaces.IDataManager;
import it.cnr.jatecs.classification.interfaces.ILearnerRuntimeCustomizer;
import it.cnr.jatecs.io.IStorageManager;
import it.cnr.jatecs.utils.Os;
import libsvm.svm;

import java.io.*;
import java.util.Iterator;

public class SvmRegressionDataManager implements IDataManager {

    public IClassifier read(String modelDir) throws IOException {

        String vc = modelDir + Os.pathSeparator() + "validCategories.db";
        DataInputStream valid_os = new DataInputStream(new BufferedInputStream(
                new FileInputStream(vc), 4096));
        valid_os.close();

        String fname = modelDir + Os.pathSeparator() + "0.db";
        SvmRegressionClassifier cl = new SvmRegressionClassifier(
                svm.svm_load_model(fname));
        return cl;
    }

    public void write(String modelDir, IClassifier learningData)
            throws IOException {
        SvmRegressionClassifier cl = (SvmRegressionClassifier) learningData;
        File f = new File(modelDir);
        f.mkdirs();
        String vc = modelDir + Os.pathSeparator() + "validCategories.db";
        DataOutputStream valid_os = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(vc), 4096));
        valid_os.writeInt(cl.getCategoryCount());

        // Write categories vectors and ranges.
        for (short catID = 0; catID < cl.getCategoryCount(); catID++) {
            valid_os.writeShort(catID);

            String filename = modelDir + Os.pathSeparator() + catID + ".db";

            svm.svm_save_model(filename, cl.model());
        }

        valid_os.close();
    }

    public ILearnerRuntimeCustomizer readLearnerRuntimeConfiguration(
            String confDir) throws IOException {
        DataInputStream is = new DataInputStream(
                new BufferedInputStream(new FileInputStream(confDir
                        + Os.pathSeparator() + "config.db")));
        SvmLearnerCustomizer c = new SvmLearnerCustomizer();
        c._optimize = is.readBoolean();
        int numCats = is.readInt();
        while (numCats > 0) {
            short catID = is.readShort();
            double val = is.readDouble();

            c._cost.put(catID, val);
            numCats--;
        }

        is.close();

        return c;
    }

    public void writeLearnerRuntimeConfiguration(String confDir,
                                                 ILearnerRuntimeCustomizer cust) throws IOException {
        File f = new File(confDir);
        if (!f.exists())
            f.mkdirs();

        SvmLearnerCustomizer customizer = (SvmLearnerCustomizer) cust;

        DataOutputStream os = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(confDir
                        + Os.pathSeparator() + "config.db")));
        os.writeBoolean(customizer._optimize);
        os.writeInt(customizer._cost.size());
        Iterator<Short> it = customizer._cost.keySet().iterator();
        while (it.hasNext()) {
            short catID = it.next();
            double v = customizer._cost.get(catID);

            os.writeShort(catID);
            os.writeDouble(v);
        }

        os.close();
    }

    public IClassifierRuntimeCustomizer readClassifierRuntimeConfiguration(
            String confDir) throws IOException {
        return null;
    }

    public void writeClassifierRuntimeConfiguration(String confDir,
                                                    IClassifierRuntimeCustomizer customizer) throws IOException {
        throw new RuntimeException("Not implemented");
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
        if (!(learningData instanceof SvmRegressionClassifier))
            throw new RuntimeException("The classifier instance must be of "
                    + SvmRegressionClassifier.class.getName());
        if (!storageManager.isOpen())
            throw new IllegalStateException("The storage manager is not open");

        try {
            SvmRegressionClassifier cl = (SvmRegressionClassifier) learningData;

            String filename = Os.getTemporaryDirectory() + "/"
                    + System.currentTimeMillis() + "/tmp.db";
            File f = new File(filename);
            f.getParentFile().mkdirs();
            svm.svm_save_model(filename, cl.model());

            InputStream is = new BufferedInputStream(new FileInputStream(f));
            String fname = modelName + storageManager.getPathSeparator()
                    + "0.db";
            OutputStream os = new BufferedOutputStream(
                    storageManager.getOutputStreamForResource(fname));
            try {
                Os.copy(is, os);
            } finally {
                is.close();
                os.close();
            }
        } catch (Exception e) {
            throw new RuntimeException("Writing classifier model", e);
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

        try {
            String sname = modelName + storageManager.getPathSeparator()
                    + "0.db";
            InputStream is = new BufferedInputStream(
                    storageManager.getInputStreamForResource(sname));
            String fname = Os.getTemporaryDirectory() + "/"
                    + System.currentTimeMillis() + "/tmp.db";
            File f = new File(fname);
            f.getParentFile().mkdirs();
            OutputStream os = new BufferedOutputStream(new FileOutputStream(f));
            try {
                Os.copy(is, os);
            } finally {
                is.close();
                os.close();
            }
            SvmRegressionClassifier cl = new SvmRegressionClassifier(
                    svm.svm_load_model(fname));
            return cl;
        } catch (Exception e) {
            throw new RuntimeException("Reading classifier model", e);
        }

    }

    @Override
    public void writeLearnerRuntimeConfiguration(
            IStorageManager storageManager, String modelName,
            ILearnerRuntimeCustomizer cust) {
        if (storageManager == null)
            throw new NullPointerException("The storage manager is 'null'");
        if (modelName == null || modelName.isEmpty())
            throw new IllegalArgumentException("The model name is invalid");
        if (cust == null)
            throw new NullPointerException("The customizer is 'null'");
        if (!(cust instanceof SvmLearnerCustomizer))
            throw new IllegalArgumentException("The customizer is not of type "
                    + SvmLearnerCustomizer.class.getName());
        if (!storageManager.isOpen())
            throw new IllegalStateException("The storage manager is not open");

        SvmLearnerCustomizer customizer = (SvmLearnerCustomizer) cust;

        String fname = modelName + Os.pathSeparator() + "config.db";
        DataOutputStream os = new DataOutputStream(new BufferedOutputStream(
                storageManager.getOutputStreamForResource(fname)));
        try {
            try {
                os.writeBoolean(customizer._optimize);
                os.writeInt(customizer._cost.size());
                Iterator<Short> it = customizer._cost.keySet().iterator();
                while (it.hasNext()) {
                    short catID = it.next();
                    double v = customizer._cost.get(catID);

                    os.writeShort(catID);
                    os.writeDouble(v);
                }
            } finally {

                os.close();
            }
        } catch (Exception e) {
            throw new RuntimeException("Writing learner runtime configuration");
        }
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

        String fname = modelName + storageManager.getPathSeparator()
                + "config.db";
        DataInputStream is = new DataInputStream(new BufferedInputStream(
                storageManager.getInputStreamForResource(fname)));
        try {
            try {
                SvmLearnerCustomizer c = new SvmLearnerCustomizer();
                c._optimize = is.readBoolean();
                int numCats = is.readInt();
                while (numCats > 0) {
                    short catID = is.readShort();
                    double val = is.readDouble();

                    c._cost.put(catID, val);
                    numCats--;
                }
                return c;
            } finally {
                is.close();
            }
        } catch (Exception e) {
            throw new RuntimeException(
                    "Reading learning runtime configuration", e);
        }
    }

    @Override
    public void writeClassifierRuntimeConfiguration(
            IStorageManager storageManager, String modelName,
            IClassifierRuntimeCustomizer customizer) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public IClassifierRuntimeCustomizer readClassifierRuntimeConfiguration(
            IStorageManager storageManager, String modelName) {
        throw new RuntimeException("Not implemented");
    }

}
