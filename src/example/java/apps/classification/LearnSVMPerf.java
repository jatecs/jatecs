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

import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.svmlight.SvmPerfClassifierCustomizer;
import it.cnr.jatecs.classification.svmlight.SvmPerfDataManager;
import it.cnr.jatecs.classification.svmlight.SvmPerfLearner;
import it.cnr.jatecs.classification.svmlight.SvmPerfLearnerCustomizer;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveClassificationDBType;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveContentDBType;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveReadWriteHelper;
import it.cnr.jatecs.io.FileSystemStorageManager;
import it.cnr.jatecs.utils.Os;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.IOException;

public class LearnSVMPerf {

    public static void main(String[] args) throws IOException {
        String cmdLineSyntax = LearnSVMPerf.class.getName()
                + " [OPTIONS] <path to svm_perf> <trainingIndexDirectory>";

        Options options = new Options();

        OptionBuilder.withArgName("c");
        OptionBuilder
                .withDescription("The c value for svm_perf (default 0.01)");
        OptionBuilder.withLongOpt("c");
        OptionBuilder.isRequired(false);
        OptionBuilder.hasArg();
        options.addOption(OptionBuilder.create());

        OptionBuilder.withArgName("t");
        OptionBuilder.withDescription("Path for temporary files");
        OptionBuilder.withLongOpt("t");
        OptionBuilder.isRequired(false);
        OptionBuilder.hasArg();
        options.addOption(OptionBuilder.create());

        OptionBuilder.withArgName("l");
        OptionBuilder
                .withDescription("The loss function to optimize (default 2):\n"
                        + "               0  Zero/one loss: 1 if vector of predictions contains error, 0 otherwise.\n"
                        + "               1  F1: 100 minus the F1-score in percent.\n"
                        + "               2  Errorrate: Percentage of errors in prediction vector.\n"
                        + "               3  Prec/Rec Breakeven: 100 minus PRBEP in percent.\n"
                        + "               4  Prec@p: 100 minus precision at p in percent.\n"
                        + "               5  Rec@p: 100 minus recall at p in percent.\n"
                        + "               10  ROCArea: Percentage of swapped pos/neg pairs (i.e. 100 - ROCArea).");
        OptionBuilder.withLongOpt("l");
        OptionBuilder.isRequired(false);
        OptionBuilder.hasArg();
        options.addOption(OptionBuilder.create());

        OptionBuilder.withArgName("w");
        OptionBuilder
                .withDescription("Choice of structural learning algorithm (default 9):\n"
                        + "               0: n-slack algorithm described in [2]\n"
                        + "               1: n-slack algorithm with shrinking heuristic\n"
                        + "               2: 1-slack algorithm (primal) described in [5]\n"
                        + "               3: 1-slack algorithm (dual) described in [5]\n"
                        + "               4: 1-slack algorithm (dual) with constraint cache [5]\n"
                        + "               9: custom algorithm in svm_struct_learn_custom.c");
        OptionBuilder.withLongOpt("w");
        OptionBuilder.isRequired(false);
        OptionBuilder.hasArg();
        options.addOption(OptionBuilder.create());

        OptionBuilder.withArgName("p");
        OptionBuilder
                .withDescription("The value of p used by the prec@p and rec@p loss functions (default 0)");
        OptionBuilder.withLongOpt("p");
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

        SvmPerfLearnerCustomizer classificationLearnerCustomizer = null;

        GnuParser parser = new GnuParser();
        String[] remainingArgs = null;
        try {
            CommandLine line = parser.parse(options, args);

            remainingArgs = line.getArgs();

            classificationLearnerCustomizer = new SvmPerfLearnerCustomizer(
                    remainingArgs[0]);

            if (line.hasOption("c"))
                classificationLearnerCustomizer.setC(Float.parseFloat(line
                        .getOptionValue("c")));

            if (line.hasOption("w"))
                classificationLearnerCustomizer.setW(Integer.parseInt(line
                        .getOptionValue("w")));

            if (line.hasOption("p"))
                classificationLearnerCustomizer.setP(Integer.parseInt(line
                        .getOptionValue("p")));

            if (line.hasOption("l"))
                classificationLearnerCustomizer.setL(Integer.parseInt(line
                        .getOptionValue("l")));

            if (line.hasOption("v"))
                classificationLearnerCustomizer.printSvmPerfOutput(true);

            if (line.hasOption("s"))
                classificationLearnerCustomizer.setDeleteTrainingFiles(false);

            if (line.hasOption("t"))
                classificationLearnerCustomizer.setTempPath(line
                        .getOptionValue("t"));

        } catch (Exception exp) {
            System.err.println("Parsing failed.  Reason: " + exp.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(cmdLineSyntax, options);
            System.exit(-1);
        }

        if (remainingArgs.length != 2) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(cmdLineSyntax, options);
            System.exit(-1);
        }

        String indexFile = remainingArgs[1];

        File file = new File(indexFile);

        String indexName = file.getName();
        String indexPath = file.getParent();

        // LEARNING
        SvmPerfLearner classificationLearner = new SvmPerfLearner();

        classificationLearner
                .setRuntimeCustomizer(classificationLearnerCustomizer);

        FileSystemStorageManager storageManager = new FileSystemStorageManager(
                indexPath, false);
        storageManager.open();
        IIndex training = TroveReadWriteHelper.readIndex(storageManager,
                indexName, TroveContentDBType.Full,
                TroveClassificationDBType.Full);
        storageManager.close();

        IClassifier classifier = classificationLearner.build(training);

        File executableFile = new File(
                classificationLearnerCustomizer.getSvmPerfLearnPath());
        SvmPerfDataManager dataManager = new SvmPerfDataManager(
                new SvmPerfClassifierCustomizer(executableFile.getParentFile()
                        .getAbsolutePath()
                        + Os.pathSeparator()
                        + "svm_perf_classify"));
        String description = "_SVMPerf_C-"
                + classificationLearnerCustomizer.getC() + "_W-"
                + classificationLearnerCustomizer.getW() + "_L-"
                + classificationLearnerCustomizer.getL();
        if (classificationLearnerCustomizer.getL() == 4
                || classificationLearnerCustomizer.getL() == 5)
            description += "_P-" + classificationLearnerCustomizer.getP();
        if (classificationLearnerCustomizer.getAdditionalParameters().length() > 0)
            description += "_"
                    + classificationLearnerCustomizer.getAdditionalParameters();

        storageManager = new FileSystemStorageManager(indexPath, false);
        storageManager.open();
        dataManager.write(storageManager, indexName + description, classifier);
        storageManager.close();
    }

}
