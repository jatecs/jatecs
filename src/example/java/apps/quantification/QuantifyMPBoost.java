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
import it.cnr.jatecs.classification.adaboost.AdaBoostClassifierCustomizer;
import it.cnr.jatecs.classification.adaboost.AdaBoostDataManager;
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * This app applies a pool of MPBoost-based quantification models to a test set.
 *
 * @author Andrea Esuli
 */
public class QuantifyMPBoost {

    public static void main(String[] args) throws IOException {
        if (args.length != 2 && args.length != 3) {
            System.err
                    .println("Usage: QuantifyMPBoost <testIndexDirectory> <quantifierDirectory> [<iterationCount>]\n");
            return;
        }

        String indexFile = args[0];

        File file = new File(indexFile);

        String indexName = file.getName();
        String indexPath = file.getParent();

        String quantifierFilename = args[1];

        FileSystemStorageManager indexFssm = new FileSystemStorageManager(
                indexPath, false);
        indexFssm.open();

        IIndex test = TroveReadWriteHelper.readIndex(indexFssm, indexName,
                TroveContentDBType.Full, TroveClassificationDBType.Full);

        indexFssm.close();

        FileSystemStorageManager quantifierFssm = new FileSystemStorageManager(
                quantifierFilename, false);
        quantifierFssm.open();

        AdaBoostDataManager classifierDataManager = new AdaBoostDataManager();

        FileSystemStorageManager fssm = new FileSystemStorageManager(
                quantifierFilename, false);
        fssm.open();

        IQuantifier[] quantifiers = QuantificationLearner.read(fssm,
                classifierDataManager, ClassificationMode.PER_DOCUMENT);
        fssm.close();

        quantifierFssm.close();

        int iterCount = -1;
        if (args.length == 3) {
            try {
                iterCount = Integer.parseInt(args[2]);
                AdaBoostClassifierCustomizer cust = new AdaBoostClassifierCustomizer();
                cust.setNumIterations(iterCount);
                CCQuantifier ccQuantifier = (CCQuantifier) quantifiers[0];
                ccQuantifier.getClassifier().setRuntimeCustomizer(cust);
                PAQuantifier paQuantifier = (PAQuantifier) quantifiers[1];
                paQuantifier.getClassifier().setRuntimeCustomizer(cust);
                ScaledQuantifier scaledQuantifier = (ScaledQuantifier) quantifiers[2];
                ccQuantifier = (CCQuantifier) scaledQuantifier
                        .getInternalQuantifier();
                ccQuantifier.setClassifierRuntimeCustomizer(cust);
                scaledQuantifier = (ScaledQuantifier) quantifiers[3];
                ccQuantifier = (CCQuantifier) scaledQuantifier
                        .getInternalQuantifier();
                ccQuantifier.setClassifierRuntimeCustomizer(cust);
                scaledQuantifier = (ScaledQuantifier) quantifiers[4];
                paQuantifier = (PAQuantifier) scaledQuantifier
                        .getInternalQuantifier();
                paQuantifier.setClassifierRuntimeCustomizer(cust);
                System.out.println("Setting custom iteration number to "
                        + iterCount);
            } catch (Exception e) {
            }
        }

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

        if (iterCount > 0)
            quantificationName += "_iter-" + iterCount;

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
