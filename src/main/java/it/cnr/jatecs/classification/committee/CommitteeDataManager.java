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

package it.cnr.jatecs.classification.committee;

import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.interfaces.IClassifierRuntimeCustomizer;
import it.cnr.jatecs.classification.interfaces.IDataManager;
import it.cnr.jatecs.classification.interfaces.ILearnerRuntimeCustomizer;
import it.cnr.jatecs.io.IStorageManager;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

public class CommitteeDataManager implements IDataManager {

    protected Vector<IDataManager> _managers;

    public CommitteeDataManager() {
        _managers = new Vector<IDataManager>();
    }

    /**
     * Add a data manager for the learner in the committee which has index
     * "index".
     *
     * @param man   The manager to add.
     * @param index The index of the reference learner.
     */
    public void addDataManager(int index, IDataManager man) {
        _managers.insertElementAt(man, index);
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

        CommitteeClassifier c = (CommitteeClassifier) learningData;

        String fname = modelName + storageManager.getPathSeparator()
                + "models.db";
        try {
            DataOutputStream os = new DataOutputStream(
                    new BufferedOutputStream(
                            storageManager.getOutputStreamForResource(fname)));
            try {
                os.writeInt(c._classifiers.size());
            } finally {
                os.close();
            }

            for (int i = 0; i < c._classifiers.size(); i++) {
                IClassifier clas = c._classifiers.get(i);
                _managers.get(i)
                        .write(storageManager,
                                modelName + storageManager.getPathSeparator()
                                        + i, clas);
            }

            // Write mapping.
            for (int i = 0; i < c._mapping.size(); i++) {
                String path = modelName + storageManager.getPathSeparator()
                        + "mapping";
                File fi = new File(modelName
                        + storageManager.getPathSeparator() + "mapping");
                if (!fi.exists())
                    fi.mkdirs();

                HashMap<String, Short> map = c._mapping.get(i);
                os = new DataOutputStream(new BufferedOutputStream(
                        storageManager.getOutputStreamForResource(path
                                + storageManager.getPathSeparator() + i)));
                try {
                    os.writeInt(map.size());
                    Iterator<String> keys = map.keySet().iterator();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        short catID = map.get(key);
                        os.writeUTF(key);
                        os.writeShort(catID);
                    }
                } finally {
                    os.close();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Writing committee classifier data", e);
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

        CommitteeClassifier cl = new CommitteeClassifier();
        DataInputStream is = new DataInputStream(new BufferedInputStream(
                storageManager.getInputStreamForResource(modelName
                        + storageManager.getPathSeparator() + "models.db")));
        int numClas = 0;
        try {
            try {
                numClas = is.readInt();

                for (int i = 0; i < numClas; i++) {
                    IClassifier c = _managers.get(i).read(storageManager,
                            modelName + storageManager.getPathSeparator() + i);
                    cl._classifiers.add(c);
                }
                cl._policy = new MajorityVotePolicy();
            } finally {
                is.close();
            }

            // Read mapping.
            for (int i = 0; i < numClas; i++) {
                String path = modelName + storageManager.getPathSeparator()
                        + "mapping";
                HashMap<String, Short> map = new HashMap<String, Short>();
                is = new DataInputStream(new BufferedInputStream(
                        storageManager.getInputStreamForResource(path
                                + storageManager.getPathSeparator() + i)));
                try {
                    int num = is.readInt();
                    while (num > 0) {
                        String key = is.readUTF();
                        short catID = is.readShort();
                        map.put(key, catID);
                        num--;
                    }
                } finally {
                    is.close();
                }
                cl._mapping.add(map);
            }
        } catch (Exception e) {
            throw new RuntimeException("Reading classifier data", e);
        }

        return cl;
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
