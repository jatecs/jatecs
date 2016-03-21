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
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import javax.management.RuntimeErrorException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Vector;

public class SvmPerfLearner extends BaseLearner {

    public SvmPerfLearner() {
        _customizer = null;
    }

    static void generateClassificationData(String outFile, IIndex index,
                                           short catID) throws IOException {
        File dir = new File(outFile).getParentFile();
        if (!dir.exists())
            dir.mkdirs();

        FileOutputStream os = new FileOutputStream(outFile);
        OutputStreamWriter out = new OutputStreamWriter(os);

        IIntIterator it = index.getDocumentDB().getDocuments();
        while (it.hasNext()) {
            int docID = it.next();
            StringBuilder b = new StringBuilder();
            if (index.getClassificationDB().hasDocumentCategory(docID, catID))
                b.append("+1");
            else
                b.append("-1");

            IIntIterator itFeats = index.getDocumentFeatures(docID, catID);
            while (itFeats.hasNext()) {
                int featID = itFeats.next();
                double score = index.getDocumentFeatureWeight(docID, featID,
                        catID);
                if (score == 0)
                    continue;

                b.append(" " + (featID + 1) + ":" + score);
            }

            b.append(" #" + Os.newline());

            out.write(b.toString());
        }

        out.close();
        os.close();
    }

    static void generateClassificationData(String outFile, IIndex index,
                                           int docID, short catID) throws IOException {
        File dir = new File(outFile).getParentFile();
        if (!dir.exists())
            dir.mkdirs();

        FileOutputStream os = new FileOutputStream(outFile);
        OutputStreamWriter out = new OutputStreamWriter(os);

        StringBuilder b = new StringBuilder();
        b.append("0");

        IIntIterator itFeats = index.getDocumentFeatures(docID, catID);
        while (itFeats.hasNext()) {
            int featID = itFeats.next();
            double score = index.getDocumentFeatureWeight(docID, featID, catID);
            if (score == 0)
                continue;

            b.append(" " + (featID + 1) + ":" + score);
        }

        b.append(" #" + Os.newline());

        out.write(b.toString());

        out.close();
        os.close();
    }

    public IClassifier build(IIndex trainingIndex) {
        if (_customizer == null)
            throw new RuntimeErrorException(new Error(
                    "Learner customizer not set."),
                    "Learner customizer not set.");

        JatecsLogger.status().println(
                "Start processing "
                        + trainingIndex.getCategoryDB().getCategoriesCount()
                        + " categories.");

        SvmPerfLearnerCustomizer customizer = (SvmPerfLearnerCustomizer) _customizer;

        SvmPerfClassifier classifier = new SvmPerfClassifier(
                customizer.getTempPath() + Os.pathSeparator()
                        + System.currentTimeMillis() + "-"
                        + (int) (Math.random() * 1000));

        ProcessBuilder builder = new ProcessBuilder();

        IShortIterator it = trainingIndex.getCategoryDB().getCategories();
        while (it.hasNext()) {
            short cat = it.next();

            // Compute path to category.
            String relativePath = "";
            relativePath += cat;
            IShortIterator parents = trainingIndex.getCategoryDB()
                    .getParentCategories(cat);
            while (parents.hasNext()) {
                short parent = parents.next();
                relativePath = parent + Os.pathSeparator() + relativePath;
            }

            String path = classifier.getClassifierDataDir()
                    + Os.pathSeparator() + relativePath;

            String training = path + Os.pathSeparator() + "training.txt";
            String model = path + Os.pathSeparator() + "model.txt";

            JatecsLogger.status().print(
                    "Generating training data for category "
                            + trainingIndex.getCategoryDB().getCategoryName(
                            cat) + "...");
            // Generate training data.
            try {
                generateClassificationData(training, trainingIndex, cat);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            JatecsLogger.status().println("done.");

            JatecsLogger.status().println(
                    "Processing category "
                            + trainingIndex.getCategoryDB().getCategoryName(
                            cat) + "...");

            // Call svm_perf learner
            ArrayList<String> cmdList = new ArrayList<String>();

            cmdList.add(customizer.getSvmPerfLearnPath());

            cmdList.add("-c");
            cmdList.add("" + customizer.getC());

            cmdList.add("-l");
            int l = customizer.getL();
            cmdList.add("" + l);
            if (l == 4 || l == 5) {
                cmdList.add("-p");
                cmdList.add("" + customizer.getP());
            }

            cmdList.add("-w");
            cmdList.add("" + customizer.getW());

            String[] split = customizer.getAdditionalParameters().split("\\s+");
            if (split != null) {
                for (int i = 0; i < split.length; i++) {
                    if (split[i].length() > 0) {
                        cmdList.add(split[i]);
                    }
                }
            }

            cmdList.add(training);
            cmdList.add(model);

            String[] cmd = cmdList.toArray(new String[0]);

            JatecsLogger.status().print("Executing: ");
            for (String string : cmdList) {
                JatecsLogger.status().print(string);
                JatecsLogger.status().print(" ");
            }
            JatecsLogger.status().println("");

            builder.command(cmd);
            Process learnP;
            try {
                learnP = builder.start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            if (customizer.isPrintingOutput()) {
                // any error message?
                StreamRedirect errorRedirect = new StreamRedirect(
                        learnP.getErrorStream(), "svm_perf ERR");

                // any output?
                StreamRedirect outputRedirect = new StreamRedirect(
                        learnP.getInputStream(), "svm_perf OUT");

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

            if (customizer.isPrintingOutput())
                JatecsLogger.status()
                        .println("svm_perf exit value: " + exitVal);

            classifier._catsMapping.put(cat, relativePath);

            // Remove training file data.
            if (customizer.isDeletingTrainingFiles()) {
                File f = new File(training);
                f.delete();
            }

            JatecsLogger.status().println("done.");
        }

        return classifier;
    }

    @Override
    public ILearnerRuntimeCustomizer getRuntimeCustomizer(short catID) {
        return null;
    }

    @Override
    public void setRuntimeCustomizer(
            Vector<ILearnerRuntimeCustomizer> customizers) {
    }

    @Override
    public IClassifier mergeClassifiers(Vector<IClassifier> classifiers) {
        throw new UnsupportedOperationException();
    }
}
