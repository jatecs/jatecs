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

package it.cnr.jatecs.classification.svmlight;

import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.interfaces.IClassifierRuntimeCustomizer;
import it.cnr.jatecs.classification.interfaces.IDataManager;
import it.cnr.jatecs.classification.interfaces.ILearnerRuntimeCustomizer;
import it.cnr.jatecs.io.IStorageManager;
import it.cnr.jatecs.utils.Os;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class SvmPerfDataManager implements IDataManager {

    protected SvmPerfClassifierCustomizer _svmPerfClassifierCustomizer;

    public SvmPerfDataManager(
            SvmPerfClassifierCustomizer svmPerfClassifierCustomizer) {
        _svmPerfClassifierCustomizer = svmPerfClassifierCustomizer;
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
        SvmPerfClassifier c = (SvmPerfClassifier) learningData;

        // Copy all generated classifier in the specified model directory.
        DataOutputStream os = new DataOutputStream(
                storageManager.getOutputStreamForResource(modelName
                        + storageManager.getPathSeparator() + "mapping.db"));

        try {
            os.writeInt(c._catsMapping.size());

            short[] cats = c._catsMapping.keys();
            for (int i = 0; i < cats.length; i++) {
                short catID = cats[i];
                String relativePath = (String) c._catsMapping.get(catID);
                FileInputStream src = new FileInputStream(
                        c.getClassifierDataDir() + Os.pathSeparator()
                                + relativePath + Os.pathSeparator()
                                + "model.txt");

                OutputStream dest = storageManager
                        .getOutputStreamForResource(modelName
                                + storageManager.getPathSeparator()
                                + relativePath
                                + storageManager.getPathSeparator()
                                + "model.txt");

                try {
                    Os.copy(src, dest);
                } finally {
                    src.close();
                    dest.close();
                }

                os.writeShort(catID);
                os.writeUTF(relativePath);
            }

            os.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public IClassifier read(IStorageManager storageManager, String modelName) {
        try {
            InputStream mappingis = storageManager
                    .getInputStreamForResource(modelName
                            + storageManager.getPathSeparator() + "mapping.db");

            Path tempPath = Files.createTempDirectory("jatecs_"
                    + Long.toString(System.nanoTime()));
            OutputStream mapping = new FileOutputStream(tempPath
                    + Os.pathSeparator() + "mapping.db");
            try {
                Os.copy(mappingis, mapping);
            } finally {
                mappingis.close();
                mapping.close();
            }

            SvmPerfClassifier classifier = new SvmPerfClassifier(
                    tempPath.toString());

            // Copy all generated classifier in the specified model directory.
            DataInputStream is = new DataInputStream(
                    storageManager.getInputStreamForResource(modelName
                            + storageManager.getPathSeparator() + "mapping.db"));
            int length = is.readInt();

            classifier.setRuntimeCustomizer(_svmPerfClassifierCustomizer);

            for (int i = 0; i < length; i++) {
                short catID = is.readShort();
                String relativePath = is.readUTF();

                File f = new File(classifier.getClassifierDataDir()
                        + Os.pathSeparator() + relativePath);
                if (!f.exists()) {
                    f.mkdirs();
                }

                FileOutputStream dest = new FileOutputStream(
                        classifier.getClassifierDataDir() + Os.pathSeparator()
                                + relativePath + Os.pathSeparator()
                                + "model.txt");

                InputStream src = storageManager
                        .getInputStreamForResource(modelName
                                + storageManager.getPathSeparator()
                                + relativePath
                                + storageManager.getPathSeparator()
                                + "model.txt");

                try {
                    Os.copy(src, dest);
                } finally {
                    src.close();
                    dest.close();
                }

                classifier._catsMapping.put(catID, relativePath);
            }

            is.close();

            return classifier;
        } catch (IOException e) {
            throw new RuntimeException(e);
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
        return (SvmPerfClassifierCustomizer) _svmPerfClassifierCustomizer
                .cloneObject();
    }

}
