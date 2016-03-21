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

package apps.quantification;

import gnu.trove.TShortDoubleHashMap;
import it.cnr.jatecs.classification.ClassificationMode;
import it.cnr.jatecs.classification.interfaces.IDataManager;
import it.cnr.jatecs.classification.svmlight.SvmLightClassifierCustomizer;
import it.cnr.jatecs.classification.svmlight.SvmLightDataManager;
import it.cnr.jatecs.classification.svmlight.SvmLightLearner;
import it.cnr.jatecs.classification.svmlight.SvmLightLearnerCustomizer;
import it.cnr.jatecs.evaluation.ContingencyTable;
import it.cnr.jatecs.evaluation.ContingencyTableSet;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveClassificationDBType;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveContentDBType;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveReadWriteHelper;
import it.cnr.jatecs.io.FileSystemStorageManager;
import it.cnr.jatecs.quantification.QuantificationLearner;
import it.cnr.jatecs.quantification.interfaces.IQuantifier;
import it.cnr.jatecs.quantification.scalingFunctions.LogisticFunction;
import it.cnr.jatecs.utils.IOperationStatusListener;
import it.cnr.jatecs.utils.Os;
import it.cnr.jatecs.utils.TextualProgressBar;
import org.apache.commons.cli.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * This app learns a pool of quantification models out of a training set using
 * SVM_light as the base learner.
 * <p>
 * The SVM_light learn executable must be locally available.
 *
 * @author Andrea Esuli
 */
public class LearnQuantificationSVMLight {

    public static void main(String[] args) throws IOException {
        String cmdLineSyntax = LearnQuantificationSVMLight.class.getName()
                + " [OPTIONS] <path to svm_light_learn> <path to svm_light_classify> <trainingIndexDirectory> <outputDirectory>";

        Options options = new Options();

        OptionBuilder.withArgName("f");
        OptionBuilder.withDescription("Number of folds");
        OptionBuilder.withLongOpt("f");
        OptionBuilder.isRequired(true);
        OptionBuilder.hasArg();
        options.addOption(OptionBuilder.create());

        OptionBuilder.withArgName("c");
        OptionBuilder.withDescription("The c value for svm_light (default 1)");
        OptionBuilder.withLongOpt("c");
        OptionBuilder.isRequired(false);
        OptionBuilder.hasArg();
        options.addOption(OptionBuilder.create());

        OptionBuilder.withArgName("k");
        OptionBuilder
                .withDescription("Kernel type (default 0: linear, 1: polynomial, 2: RBF, 3: sigmoid)");
        OptionBuilder.withLongOpt("k");
        OptionBuilder.isRequired(false);
        OptionBuilder.hasArg();
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
                .withDescription("Don't delete temporary training file in svm_light format (default: delete)");
        OptionBuilder.withLongOpt("s");
        OptionBuilder.isRequired(false);
        OptionBuilder.hasArg(false);
        options.addOption(OptionBuilder.create());

        SvmLightLearnerCustomizer classificationLearnerCustomizer = null;
        SvmLightClassifierCustomizer classificationCustomizer = null;

        int folds = -1;

        GnuParser parser = new GnuParser();
        String[] remainingArgs = null;
        try {
            CommandLine line = parser.parse(options, args);

            remainingArgs = line.getArgs();

            classificationLearnerCustomizer = new SvmLightLearnerCustomizer(
                    remainingArgs[0]);
            classificationCustomizer = new SvmLightClassifierCustomizer(
                    remainingArgs[1]);

            folds = Integer.parseInt(line.getOptionValue("f"));

            if (line.hasOption("c"))
                classificationLearnerCustomizer.setC(Float.parseFloat(line
                        .getOptionValue("c")));

            if (line.hasOption("k")) {
                System.out.println("Kernel type: " + line.getOptionValue("k"));
                classificationLearnerCustomizer.setKernelType(Integer
                        .parseInt(line.getOptionValue("k")));
            }

            if (line.hasOption("v"))
                classificationLearnerCustomizer.printSvmLightOutput(true);

            if (line.hasOption("s"))
                classificationLearnerCustomizer.setDeleteTrainingFiles(false);

            if (line.hasOption("t")) {
                classificationLearnerCustomizer.setTempPath(line
                        .getOptionValue("t"));
                classificationCustomizer.setTempPath(line.getOptionValue("t"));
            }

        } catch (Exception exp) {
            System.err.println("Parsing failed.  Reason: " + exp.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(cmdLineSyntax, options);
            System.exit(-1);
        }

        assert (classificationLearnerCustomizer != null);

        if (remainingArgs.length != 4) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(cmdLineSyntax, options);
            System.exit(-1);
        }

        String indexFile = remainingArgs[2];

        File file = new File(indexFile);

        String indexName = file.getName();
        String indexPath = file.getParent();

        String outputPath = remainingArgs[3];

        SvmLightLearner classificationLearner = new SvmLightLearner();

        classificationLearner
                .setRuntimeCustomizer(classificationLearnerCustomizer);

        FileSystemStorageManager fssm = new FileSystemStorageManager(indexPath,
                false);
        fssm.open();

        IIndex training = TroveReadWriteHelper.readIndex(fssm, indexName,
                TroveContentDBType.Full, TroveClassificationDBType.Full);

        final TextualProgressBar progressBar = new TextualProgressBar(
                "Learning the quantifiers");

        IOperationStatusListener status = new IOperationStatusListener() {

            @Override
            public void operationStatus(double percentage) {
                progressBar.signal((int) percentage);
            }
        };

        QuantificationLearner quantificationLearner = new QuantificationLearner(
                folds, classificationLearner, classificationLearnerCustomizer,
                classificationCustomizer, ClassificationMode.PER_CATEGORY,
                new LogisticFunction(), status);

        IQuantifier[] quantifiers = quantificationLearner.learn(training);

        File executableFile = new File(
                classificationLearnerCustomizer.getSvmLightLearnPath());
        IDataManager classifierDataManager = new SvmLightDataManager(
                new SvmLightClassifierCustomizer(executableFile.getParentFile()
                        .getAbsolutePath()
                        + Os.pathSeparator()
                        + "svm_light_classify"));
        String description = "_SVMLight_C-"
                + classificationLearnerCustomizer.getC() + "_K-"
                + classificationLearnerCustomizer.getKernelType();
        if (classificationLearnerCustomizer.getAdditionalParameters().length() > 0)
            description += "_"
                    + classificationLearnerCustomizer.getAdditionalParameters();
        String quantifierPrefix = indexName + "_Quantifier-" + folds
                + description;

        FileSystemStorageManager fssmo = new FileSystemStorageManager(
                outputPath + File.separatorChar + quantifierPrefix, true);
        fssmo.open();
        QuantificationLearner.write(quantifiers, fssmo, classifierDataManager);
        fssmo.close();

        BufferedWriter bfs = new BufferedWriter(new FileWriter(outputPath
                + File.separatorChar + quantifierPrefix + "_rates.txt"));
        TShortDoubleHashMap simpleTPRs = quantificationLearner.getSimpleTPRs();
        TShortDoubleHashMap simpleFPRs = quantificationLearner.getSimpleFPRs();
        TShortDoubleHashMap scaledTPRs = quantificationLearner.getScaledTPRs();
        TShortDoubleHashMap scaledFPRs = quantificationLearner.getScaledFPRs();

        ContingencyTableSet contingencyTableSet = quantificationLearner
                .getContingencyTableSet();

        short[] cats = simpleTPRs.keys();
        for (int i = 0; i < cats.length; ++i) {
            short cat = cats[i];
            String catName = training.getCategoryDB().getCategoryName(cat);
            ContingencyTable contingencyTable = contingencyTableSet
                    .getCategoryContingencyTable(cat);
            double simpleTPR = simpleTPRs.get(cat);
            double simpleFPR = simpleFPRs.get(cat);
            double scaledTPR = scaledTPRs.get(cat);
            double scaledFPR = scaledFPRs.get(cat);
            String line = quantifierPrefix + "\ttrain\tsimple\t" + catName
                    + "\t" + cat + "\t" + contingencyTable.tp() + "\t"
                    + contingencyTable.fp() + "\t" + contingencyTable.fn()
                    + "\t" + contingencyTable.tn() + "\t" + simpleTPR + "\t"
                    + simpleFPR + "\n";
            bfs.write(line);
            line = quantifierPrefix + "\ttrain\tscaled\t" + catName + "\t"
                    + cat + "\t" + contingencyTable.tp() + "\t"
                    + contingencyTable.fp() + "\t" + contingencyTable.fn()
                    + "\t" + contingencyTable.tn() + "\t" + scaledTPR + "\t"
                    + scaledFPR + "\n";
            bfs.write(line);
        }
        bfs.close();
    }
}
