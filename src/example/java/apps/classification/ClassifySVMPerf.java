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

package apps.classification;

import it.cnr.jatecs.classification.ClassificationMode;
import it.cnr.jatecs.classification.ClassificationScoreDB;
import it.cnr.jatecs.classification.module.Classifier;
import it.cnr.jatecs.classification.svmlight.SvmPerfClassifier;
import it.cnr.jatecs.classification.svmlight.SvmPerfClassifierCustomizer;
import it.cnr.jatecs.classification.svmlight.SvmPerfDataManager;
import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDB;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveClassificationDBType;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveContentDBType;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveReadWriteHelper;
import it.cnr.jatecs.io.FileSystemStorageManager;
import it.cnr.jatecs.utils.Os;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.IOException;

public class ClassifySVMPerf {

    public static void main(String[] args) throws IOException {

        boolean dumpConfidences = false;

        String cmdLineSyntax = ClassifySVMPerf.class.getName()
                + " [OPTIONS] <path to svm_perf_classify> <testIndexDirectory> <modelDirectory>";

        Options options = new Options();

        OptionBuilder.withArgName("d");
        OptionBuilder.withDescription("Dump confidences file");
        OptionBuilder.withLongOpt("d");
        OptionBuilder.isRequired(false);
        OptionBuilder.hasArg(false);
        options.addOption(OptionBuilder.create());

        OptionBuilder.withArgName("t");
        OptionBuilder.withDescription("Path for temporary files");
        OptionBuilder.withLongOpt("t");
        OptionBuilder.isRequired(false);
        OptionBuilder.hasArg();
        options.addOption(OptionBuilder.create());

        OptionBuilder.withArgName("v");
        OptionBuilder.withDescription("Verbose output");
        OptionBuilder.withLongOpt("v");
        OptionBuilder.isRequired(false);
        OptionBuilder.hasArg(false);
        options.addOption(OptionBuilder.create());

        OptionBuilder.withArgName("s");
        OptionBuilder
                .withDescription("Don't delete temporary training file in svm_perf format (default: delete)");
        OptionBuilder.withLongOpt("s");
        OptionBuilder.isRequired(false);
        OptionBuilder.hasArg(false);
        options.addOption(OptionBuilder.create());

        SvmPerfClassifierCustomizer customizer = null;

        GnuParser parser = new GnuParser();
        String[] remainingArgs = null;
        try {
            CommandLine line = parser.parse(options, args);

            remainingArgs = line.getArgs();

            customizer = new SvmPerfClassifierCustomizer(remainingArgs[0]);

            if (line.hasOption("d"))
                dumpConfidences = true;

            if (line.hasOption("v"))
                customizer.printSvmPerfOutput(true);

            if (line.hasOption("s")) {
                customizer.setDeleteTestFiles(false);
                customizer.setDeletePredictionsFiles(false);
            }

            if (line.hasOption("t"))
                customizer.setTempPath(line.getOptionValue("t"));

        } catch (Exception exp) {
            System.err.println("Parsing failed.  Reason: " + exp.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(cmdLineSyntax, options);
            System.exit(-1);
        }

        if (remainingArgs.length != 3) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(cmdLineSyntax, options);
            System.exit(-1);
        }

        String testFile = remainingArgs[1];

        File file = new File(testFile);

        String testName = file.getName();
        String testPath = file.getParent();

        String classifierFile = remainingArgs[2];

        file = new File(classifierFile);

        String classifierName = file.getName();
        String classifierPath = file.getParent();
        FileSystemStorageManager storageManager = new FileSystemStorageManager(
                testPath, false);
        storageManager.open();
        IIndex test = TroveReadWriteHelper.readIndex(storageManager, testName,
                TroveContentDBType.Full, TroveClassificationDBType.Full);
        storageManager.close();

        SvmPerfDataManager dataManager = new SvmPerfDataManager(customizer);
        storageManager = new FileSystemStorageManager(classifierPath, false);
        storageManager.open();
        SvmPerfClassifier classifier = (SvmPerfClassifier) dataManager.read(
                storageManager, classifierName);
        storageManager.close();

        classifier.setRuntimeCustomizer(customizer);

        // CLASSIFICATION
        String classificationName = testName + "_" + classifierName;

        Classifier classifierModule = new Classifier(test, classifier,
                dumpConfidences);
        classifierModule.setClassificationMode(ClassificationMode.PER_CATEGORY);
        classifierModule.exec();

        IClassificationDB testClassification = classifierModule
                .getClassificationDB();

        storageManager = new FileSystemStorageManager(testPath, false);
        storageManager.open();
        TroveReadWriteHelper.writeClassification(storageManager,
                testClassification, classificationName + ".cla", true);
        storageManager.close();

        if (dumpConfidences) {
            ClassificationScoreDB confidences = classifierModule.getConfidences();
            ClassificationScoreDB.write(testPath + Os.pathSeparator()
                    + classificationName + ".confidences", confidences);
        }
    }
}
