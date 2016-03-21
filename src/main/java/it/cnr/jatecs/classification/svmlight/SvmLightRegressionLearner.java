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

import it.cnr.jatecs.classification.BaseLearner;
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.interfaces.ILearnerRuntimeCustomizer;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.Os;
import it.cnr.jatecs.utils.StreamRedirect;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Vector;

public class SvmLightRegressionLearner extends BaseLearner {
    /**
     * The output directory where svm_light data will be written.
     */
    private String _outputDir;

    public SvmLightRegressionLearner() {
        _customizer = new SvmLightRegressionLearnerCustomizer();
        _outputDir = Os.getTemporaryDirectory() + Os.pathSeparator()
                + "svm_light" + Os.pathSeparator() + System.currentTimeMillis();
    }

    static void generateTestData(String outFile, IIndex index)
            throws IOException {
        File dir = new File(outFile).getParentFile();
        if (!dir.exists())
            dir.mkdirs();

        FileOutputStream os = new FileOutputStream(outFile);
        OutputStreamWriter out = new OutputStreamWriter(os);

        IIntIterator it = index.getDocumentDB().getDocuments();
        while (it.hasNext()) {
            int docID = it.next();
            StringBuilder b = new StringBuilder();
            b.append("0");

            IIntIterator itFeats = index.getContentDB().getDocumentFeatures(
                    docID);
            while (itFeats.hasNext()) {
                int featID = itFeats.next();
                double score = index.getWeightingDB().getDocumentFeatureWeight(
                        docID, featID);
                if (score == 0)
                    continue;

                b.append(" " + (featID + 1) + ":" + score);
            }

            b.append(" #" + Os.newline());

            out.write(b.toString());
        }

        out.close();
    }

    static void generateClassificationData(String outFile, IIndex index)
            throws IOException {
        File dir = new File(outFile).getParentFile();
        if (!dir.exists())
            dir.mkdirs();

        FileOutputStream os = new FileOutputStream(outFile);
        OutputStreamWriter out = new OutputStreamWriter(os);

        IIntIterator it = index.getDocumentDB().getDocuments();
        while (it.hasNext()) {
            int docID = it.next();
            StringBuilder b = new StringBuilder();
            b.append(index.getClassificationDB().getDocumentCategories(docID)
                    .next());

            IIntIterator itFeats = index.getContentDB().getDocumentFeatures(
                    docID);
            while (itFeats.hasNext()) {
                int featID = itFeats.next();
                double score = index.getWeightingDB().getDocumentFeatureWeight(
                        docID, featID);
                if (score == 0)
                    continue;

                b.append(" " + (featID + 1) + ":" + score);
            }

            b.append(" #" + Os.newline());

            out.write(b.toString());
        }

        out.close();
    }

    static void generateClassificationData(String outFile, IIndex index,
                                           int docID) throws IOException {
        File dir = new File(outFile).getParentFile();
        if (!dir.exists())
            dir.mkdirs();

        FileOutputStream os = new FileOutputStream(outFile);
        OutputStreamWriter out = new OutputStreamWriter(os);

        StringBuilder b = new StringBuilder();
        b.append("0");

        IIntIterator itFeats = index.getContentDB().getDocumentFeatures(docID);
        while (itFeats.hasNext()) {
            int featID = itFeats.next();
            double score = index.getWeightingDB().getDocumentFeatureWeight(
                    docID, featID);
            if (score == 0)
                continue;

            b.append(" " + (featID + 1) + ":" + score);
        }

        b.append(" #" + Os.newline());

        out.write(b.toString());

        out.close();
    }

    public IClassifier build(IIndex trainingIndex) {
        JatecsLogger.status().println(
                "Start processing "
                        + trainingIndex.getCategoryDB().getCategoriesCount()
                        + " categories.");

        _outputDir = Os.getTemporaryDirectory() + Os.pathSeparator()
                + "svm_light" + Os.pathSeparator() + System.currentTimeMillis();

        SvmLightRegressionClassifier c = new SvmLightRegressionClassifier(
                ((SvmLightRegressionLearnerCustomizer) _customizer)._svm_lightPath);
        c._inputTrainingDir = _outputDir;
        c._outputTestDir = Os.getTemporaryDirectory() + "svm_light"
                + Os.pathSeparator() + System.currentTimeMillis();

        ProcessBuilder builder = new ProcessBuilder();

        String training = _outputDir + Os.pathSeparator() + "training.txt";
        String model = _outputDir + Os.pathSeparator() + "model.txt";

        // Generate training data.
        try {
            generateClassificationData(training, trainingIndex);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Call SVMLight learner
        String[] comm = {
                ((SvmLightRegressionLearnerCustomizer) _customizer)._svm_lightPath
                        + Os.pathSeparator() + "svm_learn", "-z", "r",
                training, model};

        builder.command(comm);
        Process learnP;
        try {
            learnP = builder.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (((SvmLightRegressionLearnerCustomizer) _customizer)._printOutputSvm) {
            // any error message?
            StreamRedirect errorRedirect = new StreamRedirect(
                    learnP.getErrorStream(), "svm_light ERR");

            // any output?
            StreamRedirect outputRedirect = new StreamRedirect(
                    learnP.getInputStream(), "svm_light OUT");

            // kick them off
            errorRedirect.start();
            outputRedirect.start();
        }

        // any error???
        int exitVal;
        try {
            exitVal = learnP.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (((SvmLightRegressionLearnerCustomizer) _customizer)._printOutputSvm)
            JatecsLogger.status().println("svm_light exit value: " + exitVal);

        c._modelDir = _outputDir;

        return c;
    }

    @Override
    public IClassifier mergeClassifiers(Vector<IClassifier> classifiers) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ILearnerRuntimeCustomizer getRuntimeCustomizer(short catID) {
        return null;
    }

    @Override
    public void setRuntimeCustomizer(
            Vector<ILearnerRuntimeCustomizer> customizers) {
    }
}
