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

package it.cnr.jatecs.classification.regression;

import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.interfaces.IClassifierRuntimeCustomizer;
import it.cnr.jatecs.classification.interfaces.IDataManager;
import it.cnr.jatecs.classification.interfaces.ILearnerRuntimeCustomizer;
import it.cnr.jatecs.io.FileSystemStorageManager;
import it.cnr.jatecs.io.IStorageManager;
import it.cnr.jatecs.utils.Os;

import java.io.*;

public class TreeRegressDataManager implements IDataManager {

    private static final String TREE_REGRESS_DATA = "tree_regress.dat";
    private static final String TREE_REGRESS_CHILDREN = "tree_regress_children.dat";

    protected IDataManager _manager;

    public TreeRegressDataManager(IDataManager manager) {
        _manager = manager;
    }

    public IClassifier read(String modelDir) throws IOException {
        assert (modelDir != null);
        TreeNode root = readClassifiers(modelDir, "0");
        TreeRegressClassifier cl = new TreeRegressClassifier(root);
        return cl;
    }

    protected TreeNode readClassifiers(String inputDir, String id)
            throws IOException {
        String in = inputDir + Os.pathSeparator() + id + Os.pathSeparator()
                + TREE_REGRESS_CHILDREN;
        DataInputStream is = new DataInputStream(new BufferedInputStream(
                new FileInputStream(in)));
        int neg = is.readInt();
        int pos = is.readInt();
        is.close();

        TreeNode negNode = null;
        TreeNode posNode = null;
        if (neg != 0)
            negNode = readClassifiers(inputDir, id + "_-1");
        if (pos != 0)
            posNode = readClassifiers(inputDir, id + "_+1");

        in = inputDir + Os.pathSeparator() + id + Os.pathSeparator()
                + TREE_REGRESS_DATA;
        is = new DataInputStream(new BufferedInputStream(
                new FileInputStream(in)));
        // Read positive categories
        int len = is.readInt();
        short[] positives = new short[len];
        for (int i = 0; i < len; i++) {
            positives[i] = is.readShort();
        }

        // Read negative categories.
        len = is.readInt();
        short[] negatives = new short[len];
        for (int i = 0; i < negatives.length; i++) {
            negatives[i] = is.readShort();
        }

        is.close();

        // Read classifier.
        FileSystemStorageManager storage = new FileSystemStorageManager(
                inputDir, false);
        storage.open();
        IClassifier cl = _manager.read(storage, id);
        storage.close();

        TreeNode node = new TreeNode(positives, negatives, posNode, negNode);
        node.setClassifier(cl);
        return node;
    }

    protected TreeNode readClassifiers(IStorageManager storageManager,
                                       String inputDir, String id) throws IOException {
        String in = inputDir + storageManager.getPathSeparator() + id
                + storageManager.getPathSeparator() + TREE_REGRESS_CHILDREN;
        DataInputStream is = new DataInputStream(new BufferedInputStream(
                storageManager.getInputStreamForResource(in)));
        int neg = is.readInt();
        int pos = is.readInt();
        is.close();

        TreeNode negNode = null;
        TreeNode posNode = null;
        if (neg != 0)
            negNode = readClassifiers(storageManager, inputDir, id + "_-1");
        if (pos != 0)
            posNode = readClassifiers(storageManager, inputDir, id + "_+1");

        in = inputDir + storageManager.getPathSeparator() + id
                + storageManager.getPathSeparator() + TREE_REGRESS_DATA;
        is = new DataInputStream(new BufferedInputStream(
                storageManager.getInputStreamForResource(in)));
        // Read positive categories
        int len = is.readInt();
        short[] positives = new short[len];
        for (int i = 0; i < len; i++) {
            positives[i] = is.readShort();
        }

        // Read negative categories.
        len = is.readInt();
        short[] negatives = new short[len];
        for (int i = 0; i < negatives.length; i++) {
            negatives[i] = is.readShort();
        }

        is.close();

        // Read classifier.
        String input = inputDir + storageManager.getPathSeparator() + id;
        IClassifier cl = _manager.read(storageManager, input);

        TreeNode node = new TreeNode(positives, negatives, posNode, negNode);
        node.setClassifier(cl);
        return node;
    }

    public void write(String modelDir, IClassifier learningData)
            throws IOException {
        TreeRegressClassifier cl = (TreeRegressClassifier) learningData;
        assert (cl != null);
        File fname = new File(modelDir);
        fname.mkdirs();

        String clOut = modelDir + Os.pathSeparator() + "classifiers";
        writeClassifiers(cl._root, "0", clOut);

    }

    protected void writeClassifiers(TreeNode node, String id, String outputDir)
            throws IOException {

        String f = outputDir + Os.pathSeparator() + id + Os.pathSeparator()
                + TREE_REGRESS_DATA;
        DataOutputStream os = new DataOutputStream(new BufferedOutputStream(
                new FileOutputStream(f)));

        // Write positive categories.
        os.writeInt(node.getPositiveCategories().length);
        for (int i = 0; i < node.getPositiveCategories().length; i++) {
            os.writeShort(node.getPositiveCategories()[i]);
        }

        // Write negative categories.
        os.writeInt(node.getNegativeCategories().length);
        for (int i = 0; i < node.getNegativeCategories().length; i++) {
            os.writeShort(node.getNegativeCategories()[i]);
        }
        os.close();

        // Write positive and negative children nodes.
        f = outputDir + Os.pathSeparator() + id + Os.pathSeparator()
                + TREE_REGRESS_CHILDREN;
        os = new DataOutputStream(new BufferedOutputStream(
                new FileOutputStream(f)));
        int neg = 0;
        if (node.getNegativeChild() != null)
            neg = -1;
        int pos = 0;
        if (node.getPositiveChild() != null)
            pos = 1;
        os.writeInt(neg);
        os.writeInt(pos);
        os.close();

        // Write classifier.
        FileSystemStorageManager storage = new FileSystemStorageManager(
                outputDir, false);
        storage.open();
        _manager.write(storage, id, node.getClassifier());
        storage.close();

        if (neg != 0)
            writeClassifiers(node.getNegativeChild(), id + "_-1", outputDir);

        if (pos != 0)
            writeClassifiers(node.getPositiveChild(), id + "_+1", outputDir);
    }

    protected void writeClassifiers(IStorageManager storageManager,
                                    TreeNode node, String id, String outputDir) throws IOException {
        String out = outputDir + storageManager.getPathSeparator() + id;
        File outF = new File(out);
        outF.mkdirs();

        String f = outputDir + storageManager.getPathSeparator() + id
                + storageManager.getPathSeparator() + TREE_REGRESS_DATA;
        DataOutputStream os = new DataOutputStream(new BufferedOutputStream(
                storageManager.getOutputStreamForResource(f)));

        // Write positive categories.
        os.writeInt(node.getPositiveCategories().length);
        for (int i = 0; i < node.getPositiveCategories().length; i++) {
            os.writeShort(node.getPositiveCategories()[i]);
        }

        // Write negative categories.
        os.writeInt(node.getNegativeCategories().length);
        for (int i = 0; i < node.getNegativeCategories().length; i++) {
            os.writeShort(node.getNegativeCategories()[i]);
        }
        os.close();

        // Write positive and negative children nodes.
        f = outputDir + storageManager.getPathSeparator() + id
                + storageManager.getPathSeparator() + TREE_REGRESS_CHILDREN;
        os = new DataOutputStream(new BufferedOutputStream(
                storageManager.getOutputStreamForResource(f)));
        int neg = 0;
        if (node.getNegativeChild() != null)
            neg = -1;
        int pos = 0;
        if (node.getPositiveChild() != null)
            pos = 1;
        os.writeInt(neg);
        os.writeInt(pos);
        os.close();

        // Write classifier.
        _manager.write(storageManager, out, node.getClassifier());

        if (neg != 0)
            writeClassifiers(node.getNegativeChild(), id + "_-1", outputDir);

        if (pos != 0)
            writeClassifiers(node.getPositiveChild(), id + "_+1", outputDir);
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

        try {
            TreeRegressClassifier cl = (TreeRegressClassifier) learningData;
            assert (cl != null);

            String clOut = modelName + storageManager.getPathSeparator()
                    + "classifiers";
            writeClassifiers(storageManager, cl._root, "0", clOut);
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

        try {
            TreeNode root = readClassifiers(storageManager, modelName, "0");
            TreeRegressClassifier cl = new TreeRegressClassifier(root);
            return cl;
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
