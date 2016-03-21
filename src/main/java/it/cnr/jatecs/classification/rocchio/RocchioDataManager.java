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

package it.cnr.jatecs.classification.rocchio;

import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntDoubleIterator;
import it.cnr.jatecs.classification.ClassifierRange;
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.interfaces.IClassifierRuntimeCustomizer;
import it.cnr.jatecs.classification.interfaces.IDataManager;
import it.cnr.jatecs.classification.interfaces.ILearnerRuntimeCustomizer;
import it.cnr.jatecs.io.IStorageManager;
import it.cnr.jatecs.utils.Os;

import java.io.*;
import java.util.Hashtable;

public class RocchioDataManager implements IDataManager {

    public void write(String modelDir, IClassifier learningData)
            throws IOException {
        File d = new File(modelDir);
        if (!d.exists())
            d.mkdirs();

        RocchioClassifier c = (RocchioClassifier) learningData;

        String vc = modelDir + Os.pathSeparator() + "validCategories.db";
        DataOutputStream valid_os = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(vc), 4096));
        valid_os.writeInt(c.getCategoryCount());

        // Write categories vectors and ranges.
        for (short catID = 0; catID < c.getCategoryCount(); catID++) {
            valid_os.writeShort(catID);

            String fname = modelDir + Os.pathSeparator() + catID + ".db";

            DataOutputStream os = new DataOutputStream(
                    new BufferedOutputStream(new FileOutputStream(fname), 4096));

            os.writeInt(c.vectors[catID].size());

            TIntDoubleIterator it = c.vectors[catID].iterator();
            while (it.hasNext()) {
                it.advance();

                os.writeInt(it.key());
                os.writeDouble(it.value());
            }

            os.close();

            fname = modelDir + Os.pathSeparator() + catID + "_range.db";
            os = new DataOutputStream(new BufferedOutputStream(
                    new FileOutputStream(fname), 4096));
            RocchioClassifierCustomizer cust = (RocchioClassifierCustomizer) c
                    .getRuntimeCustomizer();
            os.writeDouble(cust.getClassifierRange(catID).border);
            os.writeDouble(cust.getClassifierRange(catID).maximum);
            os.writeDouble(cust.getClassifierRange(catID).minimum);
            os.close();
        }

        valid_os.close();
    }

    public IClassifier read(String modelDir) throws IOException {
        RocchioClassifier c = new RocchioClassifier();

        String vc = modelDir + Os.pathSeparator() + "validCategories.db";
        DataInputStream valid_os = new DataInputStream(new BufferedInputStream(
                new FileInputStream(vc), 4096));

        int numCats = valid_os.readInt();

        RocchioClassifierCustomizer cust = (RocchioClassifierCustomizer) c
                .getRuntimeCustomizer();
        cust._ranges = new Hashtable<Short, ClassifierRange>(numCats);
        c.vectors = new TIntDoubleHashMap[numCats];

        for (short catID = 0; catID < numCats; catID++) {
            String fname = modelDir + Os.pathSeparator() + catID + ".db";
            DataInputStream is = new DataInputStream(new BufferedInputStream(
                    new FileInputStream(fname), 4096));

            c.vectors[catID] = new TIntDoubleHashMap();

            int numFeatures = is.readInt();
            for (int i = 0; i < numFeatures; i++) {
                int featID = is.readInt();
                double w = is.readDouble();

                c.vectors[catID].put(featID, w);
            }

            is.close();

            fname = modelDir + Os.pathSeparator() + catID + "_range.db";
            is = new DataInputStream(new BufferedInputStream(
                    new FileInputStream(fname), 4096));
            ClassifierRange cr = new ClassifierRange();

            cr.border = is.readDouble();
            cr.maximum = is.readDouble();
            cr.minimum = is.readDouble();
            is.close();

            cust._ranges.put(catID, cr);
        }

        valid_os.close();
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

        RocchioClassifier c = (RocchioClassifier) learningData;

        String vc = modelName + storageManager.getPathSeparator()
                + "validCategories.db";
        DataOutputStream valid_os = new DataOutputStream(
                new BufferedOutputStream(
                        storageManager.getOutputStreamForResource(vc), 4096));
        try {
            valid_os.writeInt(c.getCategoryCount());

            // Write categories vectors and ranges.
            for (short catID = 0; catID < c.getCategoryCount(); catID++) {
                valid_os.writeShort(catID);

                String fname = modelName + storageManager.getPathSeparator()
                        + catID + ".db";

                DataOutputStream os = new DataOutputStream(
                        new BufferedOutputStream(storageManager
                                .getOutputStreamForResource(fname), 4096));

                os.writeInt(c.vectors[catID].size());

                TIntDoubleIterator it = c.vectors[catID].iterator();
                while (it.hasNext()) {
                    it.advance();

                    os.writeInt(it.key());
                    os.writeDouble(it.value());
                }

                os.close();

                fname = modelName + storageManager.getPathSeparator() + catID
                        + "_range.db";
                os = new DataOutputStream(new BufferedOutputStream(
                        storageManager.getOutputStreamForResource(fname), 4096));
                RocchioClassifierCustomizer cust = (RocchioClassifierCustomizer) c
                        .getRuntimeCustomizer();
                os.writeDouble(cust.getClassifierRange(catID).border);
                os.writeDouble(cust.getClassifierRange(catID).maximum);
                os.writeDouble(cust.getClassifierRange(catID).minimum);
                os.close();
            }

            valid_os.close();
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

        RocchioClassifier c = new RocchioClassifier();

        try {
            String vc = modelName + storageManager.getPathSeparator()
                    + "validCategories.db";
            DataInputStream valid_os = new DataInputStream(
                    new BufferedInputStream(
                            storageManager.getInputStreamForResource(vc), 4096));

            int numCats = valid_os.readInt();

            RocchioClassifierCustomizer cust = (RocchioClassifierCustomizer) c
                    .getRuntimeCustomizer();
            cust._ranges = new Hashtable<Short, ClassifierRange>(numCats);
            c.vectors = new TIntDoubleHashMap[numCats];

            for (short catID = 0; catID < numCats; catID++) {
                String fname = modelName + storageManager.getPathSeparator()
                        + catID + ".db";
                DataInputStream is = new DataInputStream(
                        new BufferedInputStream(storageManager
                                .getInputStreamForResource(fname), 4096));

                c.vectors[catID] = new TIntDoubleHashMap();

                int numFeatures = is.readInt();
                for (int i = 0; i < numFeatures; i++) {
                    int featID = is.readInt();
                    double w = is.readDouble();

                    c.vectors[catID].put(featID, w);
                }

                is.close();

                fname = modelName + storageManager.getPathSeparator() + catID
                        + "_range.db";
                is = new DataInputStream(new BufferedInputStream(
                        storageManager.getInputStreamForResource(fname), 4096));
                ClassifierRange cr = new ClassifierRange();

                cr.border = is.readDouble();
                cr.maximum = is.readDouble();
                cr.minimum = is.readDouble();
                is.close();

                cust._ranges.put(catID, cr);
            }

            return c;
        } catch (Exception e) {
            throw new RuntimeException("Reading classifier data", e);
        }
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
