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

package it.cnr.jatecs.classification.thresholdboost;

import it.cnr.jatecs.classification.ClassifierRange;
import it.cnr.jatecs.classification.adaboost.AdaBoostClassifier;
import it.cnr.jatecs.classification.adaboost.AdaBoostDataManager;
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.interfaces.IClassifierRuntimeCustomizer;
import it.cnr.jatecs.classification.interfaces.IDataManager;
import it.cnr.jatecs.classification.interfaces.ILearnerRuntimeCustomizer;
import it.cnr.jatecs.io.FileSystemStorageManager;
import it.cnr.jatecs.io.IStorageManager;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.Os;

import java.io.*;
import java.util.Iterator;

public class ThresholdBoostDataManager implements IDataManager {

    public IClassifier read(String modelDir) throws IOException {
        AdaBoostDataManager manager = new AdaBoostDataManager();
        FileSystemStorageManager storageManager = new FileSystemStorageManager(
                modelDir, false);
        storageManager.open();
        AdaBoostClassifier c = (AdaBoostClassifier) manager.read(
                storageManager, "internal");
        storageManager.close();

        ThresholdBoostClassifier cl = new ThresholdBoostClassifier(c);
        DataInputStream is = new DataInputStream(new BufferedInputStream(
                new FileInputStream(modelDir + Os.pathSeparator()
                        + "mapping.db")));
        int numCats = is.readInt();
        while (numCats > 0) {
            short catID = is.readShort();
            ClassifierRange r = new ClassifierRange();
            r.border = is.readDouble();
            r.maximum = is.readDouble();
            r.minimum = is.readDouble();

            cl._cust._ranges.put(catID, r);

            JatecsLogger.status().println(
                    "For category " + catID + " the border is " + r.border);

            numCats--;
        }

        is.close();

        return cl;
    }

    public void write(String modelDir, IClassifier c) throws IOException {

        ThresholdBoostClassifier cl = (ThresholdBoostClassifier) c;
        File f = new File(modelDir);
        if (!f.exists())
            f.mkdirs();

        AdaBoostDataManager manager = new AdaBoostDataManager();
        FileSystemStorageManager storageManager = new FileSystemStorageManager(
                modelDir, false);
        storageManager.open();
        manager.write(storageManager, "internal", cl._cl);
        storageManager.close();

        // Write thresholds for all categories.
        DataOutputStream os = new DataOutputStream(new BufferedOutputStream(
                new FileOutputStream(modelDir + Os.pathSeparator()
                        + "mapping.db")));

        os.writeInt(cl._cust._ranges.size());
        Iterator<Short> it = cl._cust._ranges.keySet().iterator();
        while (it.hasNext()) {
            short catID = it.next();
            ClassifierRange r = cl._cust.getClassifierRange(catID);
            os.writeShort(catID);
            os.writeDouble(r.border);
            os.writeDouble(r.maximum);
            os.writeDouble(r.minimum);

            JatecsLogger.status().println(
                    "For category " + catID + " the border is " + r.border);
        }

        os.close();
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

        ThresholdBoostClassifier cl = (ThresholdBoostClassifier) learningData;

        String p = modelName + storageManager.getPathSeparator() + "internal";
        AdaBoostDataManager manager = new AdaBoostDataManager();
        manager.write(storageManager, p, cl._cl);

        // Write thresholds for all categories.
        DataOutputStream os = new DataOutputStream(new BufferedOutputStream(
                storageManager.getOutputStreamForResource(modelName
                        + storageManager.getPathSeparator() + "mapping.db")));

        try {
            try {
                os.writeInt(cl._cust._ranges.size());
                Iterator<Short> it = cl._cust._ranges.keySet().iterator();
                while (it.hasNext()) {
                    short catID = it.next();
                    ClassifierRange r = cl._cust.getClassifierRange(catID);
                    os.writeShort(catID);
                    os.writeDouble(r.border);
                    os.writeDouble(r.maximum);
                    os.writeDouble(r.minimum);

                    JatecsLogger.status().println(
                            "For category " + catID + " the border is "
                                    + r.border);
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

        String p = modelName + storageManager.getPathSeparator() + "internal";
        AdaBoostDataManager manager = new AdaBoostDataManager();
        AdaBoostClassifier c = (AdaBoostClassifier) manager.read(
                storageManager, p);

        ThresholdBoostClassifier cl = new ThresholdBoostClassifier(c);
        DataInputStream is = new DataInputStream(new BufferedInputStream(
                storageManager.getInputStreamForResource(modelName
                        + storageManager.getPathSeparator() + "mapping.db")));
        try {
            try {
                int numCats = is.readInt();
                while (numCats > 0) {
                    short catID = is.readShort();
                    ClassifierRange r = new ClassifierRange();
                    r.border = is.readDouble();
                    r.maximum = is.readDouble();
                    r.minimum = is.readDouble();

                    cl._cust._ranges.put(catID, r);

                    JatecsLogger.status().println(
                            "For category " + catID + " the border is "
                                    + r.border);

                    numCats--;
                }

                return cl;
            } finally {
                is.close();
            }
        } catch (Exception e) {
            throw new RuntimeException("Readining classifier data", e);
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
