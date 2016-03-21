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

package it.cnr.jatecs.classification.treeboost;

import gnu.trove.TShortObjectHashMap;
import gnu.trove.TShortObjectIterator;
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.interfaces.IClassifierRuntimeCustomizer;
import it.cnr.jatecs.classification.interfaces.IDataManager;
import it.cnr.jatecs.classification.interfaces.ILearnerRuntimeCustomizer;
import it.cnr.jatecs.io.FileSystemStorageManager;
import it.cnr.jatecs.io.IStorageManager;
import it.cnr.jatecs.utils.DoubleMappingShortObject;
import it.cnr.jatecs.utils.Os;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;

public class SingleLabelTreeBoostDataManager implements IDataManager {

    protected static String CATEGORY_MAPPING_ORIGINAL_MODEL = "mapping.db";
    protected static String STORED_LEVELS = "levels.db";

    protected IDataManager _manager;

    public SingleLabelTreeBoostDataManager(IDataManager manager) {
        assert (manager != null);
        _manager = manager;
    }

    public void write(String modelDir, IClassifier learningData)
            throws IOException {
        // First write classifiers.
        writeClassifiers(modelDir,
                (SingleLabelTreeBoostClassifier) learningData);

        // Write mapping data between orginal data and model transformed
        // data.
        writeDataMapping(modelDir,
                (SingleLabelTreeBoostClassifier) learningData);
    }

    protected void writeClassifiers(String modelDir,
                                    SingleLabelTreeBoostClassifier c) throws IOException {
        File fname = new File(modelDir);
        fname.mkdirs();

        String f = modelDir + Os.pathSeparator() + STORED_LEVELS;
        DataOutputStream os = new DataOutputStream(new BufferedOutputStream(
                new FileOutputStream(f)));
        os.writeInt(c._map.size());

        TShortObjectIterator<IClassifier> it = c._map.iterator();
        while (it.hasNext()) {
            it.advance();
            short catID = it.key();
            IClassifier classifier = (IClassifier) c._map.get(catID);
            FileSystemStorageManager storageManager = new FileSystemStorageManager(
                    modelDir, false);
            storageManager.open();
            _manager.write(storageManager, "" + catID, classifier);
            storageManager.close();

            os.writeShort(catID);
        }

        os.close();
    }

    protected void writeClassifiers(IStorageManager storageManager,
                                    String modelName, SingleLabelTreeBoostClassifier c) {

        String f = modelName + storageManager.getPathSeparator()
                + STORED_LEVELS;
        DataOutputStream os = new DataOutputStream(new BufferedOutputStream(
                storageManager.getOutputStreamForResource(f)));

        try {
            os.writeInt(c._map.size());

            TShortObjectIterator<IClassifier> it = c._map.iterator();
            while (it.hasNext()) {
                it.advance();
                short catID = it.key();
                String path = modelName + Os.pathSeparator() + catID;
                IClassifier classifier = (IClassifier) c._map.get(catID);
                _manager.write(storageManager, path, classifier);

                os.writeShort(catID);
            }

        } catch (Exception e) {
            throw new RuntimeException("Writing trebbot classifier data", e);
        } finally {
            try {
                os.close();
            } catch (Exception e2) {
                throw new RuntimeException("Closing output stream", e2);
            }
        }
    }

    protected void writeDataMapping(String modelDir,
                                    SingleLabelTreeBoostClassifier c) throws IOException {
        String fname = modelDir + Os.pathSeparator()
                + CATEGORY_MAPPING_ORIGINAL_MODEL;
        DataOutputStream os = new DataOutputStream(new BufferedOutputStream(
                new FileOutputStream(fname)));

        // Write the size of map.
        os.writeInt(c._mapCatLevel.size());

        TShortObjectIterator<TreeBoostClassifierAddress> it = c._mapCatLevel
                .iterator1();
        while (it.hasNext()) {
            it.advance();
            short realCatID = it.key();
            TreeBoostClassifierAddress addr = (TreeBoostClassifierAddress) it
                    .value();

            os.writeShort(realCatID);
            os.writeShort(addr.level);
            os.writeShort(addr.categoryID);
        }

        os.close();
    }

    protected void writeDataMapping(IStorageManager storageManager,
                                    String modelName, SingleLabelTreeBoostClassifier c) {
        String fname = modelName + storageManager.getPathSeparator()
                + CATEGORY_MAPPING_ORIGINAL_MODEL;
        DataOutputStream os = new DataOutputStream(new BufferedOutputStream(
                storageManager.getOutputStreamForResource(fname)));

        try {
            // Write the size of map.
            os.writeInt(c._mapCatLevel.size());

            TShortObjectIterator<TreeBoostClassifierAddress> it = c._mapCatLevel
                    .iterator1();
            while (it.hasNext()) {
                it.advance();
                short realCatID = it.key();
                TreeBoostClassifierAddress addr = (TreeBoostClassifierAddress) it
                        .value();

                os.writeShort(realCatID);
                os.writeShort(addr.level);
                os.writeShort(addr.categoryID);
            }

            os.close();
        } catch (Exception e) {
            throw new RuntimeException("Writing data mapping", e);
        } finally {
            try {
                os.close();
            } catch (Exception e2) {
                throw new RuntimeException("Closing output stream", e2);
            }
        }
    }

    public IClassifier read(String modelDir) throws IOException {
        SingleLabelTreeBoostClassifier c = new SingleLabelTreeBoostClassifier();

        // Read the stored classifiers.
        readClassifiers(modelDir, c);

        // Read original data mapping.
        readDataMapping(modelDir, c);

        return c;
    }

    protected void readClassifiers(String modelDir,
                                   SingleLabelTreeBoostClassifier c) throws IOException {
        String f = modelDir + Os.pathSeparator() + STORED_LEVELS;
        DataInputStream os = new DataInputStream(new BufferedInputStream(
                new FileInputStream(f)));

        int numCats = os.readInt();

        c._map = new TShortObjectHashMap<IClassifier>(numCats);

        for (int i = 0; i < numCats; i++) {
            short catID = os.readShort();

            FileSystemStorageManager storageManager = new FileSystemStorageManager(
                    modelDir, false);
            storageManager.open();
            IClassifier cl = _manager.read(storageManager, "" + catID);
            storageManager.close();

            c._map.put(catID, cl);
        }

        os.close();
    }

    protected void readClassifiers(IStorageManager storageManager,
                                   String modelDir, SingleLabelTreeBoostClassifier c) {
        String f = modelDir + storageManager.getPathSeparator() + STORED_LEVELS;
        DataInputStream os = new DataInputStream(new BufferedInputStream(
                storageManager.getInputStreamForResource(f)));

        try {
            int numCats = os.readInt();

            c._map = new TShortObjectHashMap<IClassifier>(numCats);

            for (int i = 0; i < numCats; i++) {
                short catID = os.readShort();

                String path = modelDir + Os.pathSeparator() + catID;
                IClassifier cl = _manager.read(storageManager, path);

                c._map.put(catID, cl);
            }
        } catch (Exception e) {
            throw new RuntimeException("Reading classifiers", e);
        } finally {
            try {
                os.close();
            } catch (Exception e2) {
                throw new RuntimeException("Closing output stream", e2);
            }
        }
    }

    protected void readDataMapping(String modelDir,
                                   SingleLabelTreeBoostClassifier c) throws IOException {
        String f = modelDir + Os.pathSeparator()
                + CATEGORY_MAPPING_ORIGINAL_MODEL;
        DataInputStream os = new DataInputStream(new BufferedInputStream(
                new FileInputStream(f)));

        int s = os.readInt();

        c._mapCatLevel = new DoubleMappingShortObject<TreeBoostClassifierAddress>(
                s);

        for (int i = 0; i < s; i++) {
            short realCatID = os.readShort();
            short level = os.readShort();
            short catID = os.readShort();

            TreeBoostClassifierAddress addr = new TreeBoostClassifierAddress();
            addr.level = level;
            addr.categoryID = catID;

            c._mapCatLevel.put(realCatID, addr);
        }

        os.close();
    }

    protected void readDataMapping(IStorageManager storageManager,
                                   String modelDir, SingleLabelTreeBoostClassifier c) {
        String f = modelDir + storageManager.getPathSeparator()
                + CATEGORY_MAPPING_ORIGINAL_MODEL;
        DataInputStream os = new DataInputStream(new BufferedInputStream(
                storageManager.getInputStreamForResource(f)));

        try {
            int s = os.readInt();

            c._mapCatLevel = new DoubleMappingShortObject<TreeBoostClassifierAddress>(
                    s);

            for (int i = 0; i < s; i++) {
                short realCatID = os.readShort();
                short level = os.readShort();
                short catID = os.readShort();

                TreeBoostClassifierAddress addr = new TreeBoostClassifierAddress();
                addr.level = level;
                addr.categoryID = catID;

                c._mapCatLevel.put(realCatID, addr);
            }
        } catch (Exception e) {
            throw new RuntimeException("Reading data mapping", e);
        } finally {
            try {
                os.close();
            } catch (Exception e2) {
                throw new RuntimeException("Closing output stream", e2);
            }
        }
    }

    public ILearnerRuntimeCustomizer readLearnerRuntimeConfiguration(
            String confDir) throws IOException {
        TreeBoostLearnerCustomizer c = new TreeBoostLearnerCustomizer();
        DataInputStream is = new DataInputStream(new BufferedInputStream(
                new FileInputStream(confDir + Os.pathSeparator() + "cats.db")));
        int numCats = is.readInt();
        while (numCats > 0) {
            short catID = Short.parseShort(is.readUTF());
            int numInternals = is.readInt();
            while (numInternals > 0) {
                short intCatID = Short.parseShort(is.readUTF());
                FileSystemStorageManager storageManager = new FileSystemStorageManager(
                        confDir, false);
                storageManager.open();
                ILearnerRuntimeCustomizer cust = _manager
                        .readLearnerRuntimeConfiguration(storageManager, catID
                                + Os.pathSeparator() + intCatID);
                storageManager.close();
                c.setInternalCustomizer(catID, intCatID, cust);
                numInternals--;
            }
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

        TreeBoostLearnerCustomizer customizer = (TreeBoostLearnerCustomizer) cust;

        DataOutputStream os = new DataOutputStream(new BufferedOutputStream(
                new FileOutputStream(confDir + Os.pathSeparator() + "cats.db")));
        os.writeInt(customizer._internalCustomizer.size());

        Iterator<Short> it = customizer._internalCustomizer.keySet().iterator();
        while (it.hasNext()) {
            short catID = it.next();
            HashMap<Short, ILearnerRuntimeCustomizer> c = customizer._internalCustomizer
                    .get(catID);
            os.writeUTF("" + catID);
            os.writeInt(c.size());
            Iterator<Short> it2 = c.keySet().iterator();
            while (it2.hasNext()) {
                short intCatID = it2.next();
                ILearnerRuntimeCustomizer cu = c.get(catID);
                FileSystemStorageManager storageManager = new FileSystemStorageManager(
                        confDir, false);
                storageManager.open();
                _manager.writeLearnerRuntimeConfiguration(storageManager, catID
                        + Os.pathSeparator() + intCatID, cu);
                storageManager.close();
                os.writeUTF("" + intCatID);
            }
        }

        os.close();
    }

    public IClassifierRuntimeCustomizer readClassifierRuntimeConfiguration(
            String confDir) throws IOException {
        return null;
    }

    public void writeClassifierRuntimeConfiguration(String confDir,
                                                    IClassifierRuntimeCustomizer customizer) throws IOException {

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

        // First write classifiers.
        writeClassifiers(storageManager, modelName,
                (SingleLabelTreeBoostClassifier) learningData);

        // Write mapping data between orginal data and model transformed
        // data.
        writeDataMapping(storageManager, modelName,
                (SingleLabelTreeBoostClassifier) learningData);
    }

    @Override
    public IClassifier read(IStorageManager storageManager, String modelName) {
        if (storageManager == null)
            throw new NullPointerException("The storage manager is 'null'");
        if (modelName == null || modelName.isEmpty())
            throw new IllegalArgumentException("The model name is invalid");
        if (!storageManager.isOpen())
            throw new IllegalStateException("The storage manager is not open");

        SingleLabelTreeBoostClassifier c = new SingleLabelTreeBoostClassifier();

        // Read the stored classifiers.
        readClassifiers(storageManager, modelName, c);

        // Read original data mapping.
        readDataMapping(storageManager, modelName, c);

        return c;
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
            throw new NullPointerException("THe customizer is 'null'");
        if (!storageManager.isOpen())
            throw new IllegalStateException("The storage manager is not open");

        TreeBoostLearnerCustomizer customizer = (TreeBoostLearnerCustomizer) cust;

        DataOutputStream os = new DataOutputStream(new BufferedOutputStream(
                storageManager.getOutputStreamForResource(modelName
                        + storageManager.getPathSeparator() + "cats.db")));
        try {
            os.writeInt(customizer._internalCustomizer.size());

            Iterator<Short> it = customizer._internalCustomizer.keySet()
                    .iterator();
            while (it.hasNext()) {
                short catID = it.next();
                HashMap<Short, ILearnerRuntimeCustomizer> c = customizer._internalCustomizer
                        .get(catID);
                os.writeUTF("" + catID);
                os.writeInt(c.size());
                Iterator<Short> it2 = c.keySet().iterator();
                while (it2.hasNext()) {
                    short intCatID = it2.next();
                    ILearnerRuntimeCustomizer cu = c.get(catID);
                    _manager.writeLearnerRuntimeConfiguration(
                            storageManager,
                            modelName + Os.pathSeparator() + catID
                                    + Os.pathSeparator() + intCatID, cu);
                    os.writeUTF("" + intCatID);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Writing learner runtime configuration",
                    e);
        } finally {
            try {
                os.close();
            } catch (Exception e2) {
                throw new RuntimeException("Closing output stream", e2);
            }
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

        TreeBoostLearnerCustomizer c = new TreeBoostLearnerCustomizer();
        DataInputStream is = new DataInputStream(new BufferedInputStream(
                storageManager.getInputStreamForResource(modelName
                        + Os.pathSeparator() + "cats.db")));
        try {
            int numCats = is.readInt();
            while (numCats > 0) {
                short catID = Short.parseShort(is.readUTF());
                int numInternals = is.readInt();
                while (numInternals > 0) {
                    short intCatID = Short.parseShort(is.readUTF());
                    ILearnerRuntimeCustomizer cust = _manager
                            .readLearnerRuntimeConfiguration(
                                    storageManager,
                                    modelName + Os.pathSeparator() + catID
                                            + Os.pathSeparator() + intCatID);
                    c.setInternalCustomizer(catID, intCatID, cust);
                    numInternals--;
                }
                numCats--;
            }

            return c;

        } catch (Exception e) {
            throw new RuntimeException("Reading learner runtime configuration",
                    e);
        } finally {
            try {
                is.close();
            } catch (Exception e2) {
                throw new RuntimeException("Closing input stream", e2);
            }
        }

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
