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
import it.cnr.jatecs.classification.svmlight.SvmLightClassifierCustomizer;
import it.cnr.jatecs.classification.svmlight.SvmLightDataManager;
import it.cnr.jatecs.evaluation.ContingencyTable;
import it.cnr.jatecs.evaluation.ContingencyTableSet;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveClassificationDBType;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveContentDBType;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveReadWriteHelper;
import it.cnr.jatecs.io.FileSystemStorageManager;
import it.cnr.jatecs.quantification.*;
import it.cnr.jatecs.quantification.interfaces.IQuantifier;
import it.cnr.jatecs.utils.Os;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;
import org.apache.commons.cli.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * This app applies a pool of SVM_light-based quantification models to a test set.
 * <p>
 * The SVM_light classify executable must be locally available.
 *
 * @author Andrea Esuli
 */
public class QuantifySVMLight {

    public static void main(String[] args) throws IOException {
        String cmdLineSyntax = QuantifySVMLight.class.getName()
                + " [OPTIONS] <path to svm_light_classify> <testIndexDirectory> <quantificationModelDirectory>";

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
                .withDescription("Don't delete temporary files in svm_light format (default: delete)");
        OptionBuilder.withLongOpt("s");
        OptionBuilder.isRequired(false);
        OptionBuilder.hasArg(false);
        options.addOption(OptionBuilder.create());

        SvmLightClassifierCustomizer customizer = null;

        GnuParser parser = new GnuParser();
        String[] remainingArgs = null;
        try {
            CommandLine line = parser.parse(options, args);

            remainingArgs = line.getArgs();

            customizer = new SvmLightClassifierCustomizer(remainingArgs[0]);

            if (line.hasOption("v"))
                customizer.printSvmLightOutput(true);

            if (line.hasOption("s")) {
                System.out.println("Keeping temporary files.");
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

        String indexFile = remainingArgs[1];

        File file = new File(indexFile);

        String indexName = file.getName();
        String indexPath = file.getParent();

        String quantifierFilename = remainingArgs[2];

        FileSystemStorageManager indexFssm = new FileSystemStorageManager(
                indexPath, false);
        indexFssm.open();

        IIndex test = TroveReadWriteHelper.readIndex(indexFssm, indexName,
                TroveContentDBType.Full, TroveClassificationDBType.Full);

        indexFssm.close();

        FileSystemStorageManager quantifierFssm = new FileSystemStorageManager(
                quantifierFilename, false);
        quantifierFssm.open();

        SvmLightDataManager classifierDataManager = new SvmLightDataManager(
                customizer);

        FileSystemStorageManager fssm = new FileSystemStorageManager(
                quantifierFilename, false);
        fssm.open();

        IQuantifier[] quantifiers = QuantificationLearner.read(fssm,
                classifierDataManager, ClassificationMode.PER_CATEGORY);
        fssm.close();

        quantifierFssm.close();

        Quantification ccQuantification = quantifiers[0].quantify(test);
        Quantification paQuantification = quantifiers[1].quantify(test);
        Quantification accQuantification = quantifiers[2].quantify(test);
        Quantification maxQuantification = quantifiers[3].quantify(test);
        Quantification sccQuantification = quantifiers[4].quantify(test);
        Quantification spaQuantification = quantifiers[5].quantify(test);
        Quantification trueQuantification = new Quantification("True",
                test.getClassificationDB());

        File quantifierFile = new File(quantifierFilename);

        String quantificationName = quantifierFile.getParent()
                + Os.pathSeparator() + indexName + "_"
                + quantifierFile.getName() + ".txt";

        BufferedWriter writer = new BufferedWriter(new FileWriter(
                quantificationName));
        IShortIterator iterator = test.getCategoryDB().getCategories();
        while (iterator.hasNext()) {
            short category = iterator.next();
            String prefix = quantifierFile.getName() + "\t" + indexName + "\t"
                    + test.getCategoryDB().getCategoryName(category) + "\t"
                    + category + "\t"
                    + trueQuantification.getQuantification(category) + "\t";

            writer.write(prefix + ccQuantification.getName() + "\t"
                    + ccQuantification.getQuantification(category) + "\n");
            writer.write(prefix + paQuantification.getName() + "\t"
                    + paQuantification.getQuantification(category) + "\n");
            writer.write(prefix + accQuantification.getName() + "\t"
                    + accQuantification.getQuantification(category) + "\n");
            writer.write(prefix + maxQuantification.getName() + "\t"
                    + maxQuantification.getQuantification(category) + "\n");
            writer.write(prefix + sccQuantification.getName() + "\t"
                    + sccQuantification.getQuantification(category) + "\n");
            writer.write(prefix + spaQuantification.getName() + "\t"
                    + spaQuantification.getQuantification(category) + "\n");
        }
        writer.close();

        BufferedWriter bfs = new BufferedWriter(new FileWriter(
                quantifierFile.getParent() + Os.pathSeparator() + indexName
                        + "_" + quantifierFile.getName() + "_rates.txt"));
        TShortDoubleHashMap simpleTPRs = ((CCQuantifier) quantifiers[0])
                .getSimpleTPRs();
        TShortDoubleHashMap simpleFPRs = ((CCQuantifier) quantifiers[0])
                .getSimpleFPRs();
        TShortDoubleHashMap maxTPRs = ((CCQuantifier) ((ScaledQuantifier) quantifiers[3])
                .getInternalQuantifier()).getSimpleTPRs();
        TShortDoubleHashMap maxFPRs = ((CCQuantifier) ((ScaledQuantifier) quantifiers[3])
                .getInternalQuantifier()).getSimpleFPRs();
        TShortDoubleHashMap scaledTPRs = ((PAQuantifier) quantifiers[1])
                .getScaledTPRs();
        TShortDoubleHashMap scaledFPRs = ((PAQuantifier) quantifiers[1])
                .getScaledFPRs();

        ContingencyTableSet simpleContingencyTableSet = ((CCQuantifier) quantifiers[0])
                .getContingencyTableSet();
        ContingencyTableSet maxContingencyTableSet = ((CCQuantifier) ((ScaledQuantifier) quantifiers[3])
                .getInternalQuantifier()).getContingencyTableSet();

        short[] cats = simpleTPRs.keys();
        for (int i = 0; i < cats.length; ++i) {
            short cat = cats[i];
            String catName = test.getCategoryDB().getCategoryName(cat);
            ContingencyTable simpleContingencyTable = simpleContingencyTableSet
                    .getCategoryContingencyTable(cat);
            ContingencyTable maxContingencyTable = maxContingencyTableSet
                    .getCategoryContingencyTable(cat);
            double simpleTPR = simpleTPRs.get(cat);
            double simpleFPR = simpleFPRs.get(cat);
            double maxTPR = maxTPRs.get(cat);
            double maxFPR = maxFPRs.get(cat);
            double scaledTPR = scaledTPRs.get(cat);
            double scaledFPR = scaledFPRs.get(cat);
            String line = indexName + "_" + quantifierFile.getName()
                    + "\ttest\tsimple\t" + catName + "\t" + cat + "\t"
                    + simpleContingencyTable.tp() + "\t"
                    + simpleContingencyTable.fp() + "\t"
                    + simpleContingencyTable.fn() + "\t"
                    + simpleContingencyTable.tn() + "\t" + simpleTPR + "\t"
                    + simpleFPR + "\n";
            bfs.write(line);
            line = indexName + "_" + quantifierFile.getName() + "\ttest\tmax\t"
                    + catName + "\t" + cat + "\t" + maxContingencyTable.tp()
                    + "\t" + maxContingencyTable.fp() + "\t"
                    + maxContingencyTable.fn() + "\t"
                    + maxContingencyTable.tn() + "\t" + maxTPR + "\t" + maxFPR
                    + "\n";
            bfs.write(line);
            line = indexName + "_" + quantifierFile.getName()
                    + "\ttest\tscaled\t" + catName + "\t" + cat + "\t"
                    + simpleContingencyTable.tp() + "\t"
                    + simpleContingencyTable.fp() + "\t"
                    + simpleContingencyTable.fn() + "\t"
                    + simpleContingencyTable.tn() + "\t" + scaledTPR + "\t"
                    + scaledFPR + "\n";
            bfs.write(line);
        }
        bfs.close();
    }
}
