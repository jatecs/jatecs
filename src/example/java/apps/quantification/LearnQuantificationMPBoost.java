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
import it.cnr.jatecs.classification.adaboost.*;
import it.cnr.jatecs.classification.interfaces.IDataManager;
import it.cnr.jatecs.classification.mpboost.MPWeakLearnerMultiThread;
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
import it.cnr.jatecs.utils.TextualProgressBar;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * This app learns a pool of quantification models out of a training set using
 * MPBoost as the base learner.
 *
 * @author Andrea Esuli
 */
public class LearnQuantificationMPBoost {

    public static void main(String[] args) throws IOException {
        if (args.length != 5) {
            System.err
                    .println("Usage: LearnQuantificationMPBoost <folds> <threadCount> <iterationCount> <trainingIndexDirectory> <outputDirectory>");
            return;
        }

        int folds = Integer.parseInt(args[0]);
        int threadCount = Integer.parseInt(args[1]);
        int iterations = Integer.parseInt(args[2]);
        String indexFile = args[3];
        String outputPath = args[4];

        File file = new File(indexFile);

        String indexName = file.getName();
        String indexPath = file.getParent();

        AdaBoostLearner classificationLearner = new AdaBoostLearner();
        AdaBoostLearnerCustomizer classificationLearnerCustomizer = new AdaBoostLearnerCustomizer();
        classificationLearnerCustomizer.setNumIterations(iterations);
        classificationLearnerCustomizer
                .setWeakLearner(new MPWeakLearnerMultiThread(threadCount));
        classificationLearnerCustomizer.setPerCategoryNormalization(true);
        classificationLearnerCustomizer.setLossFunction(new ExponentialLoss());
        classificationLearnerCustomizer
                .setInitialDistributionType(InitialDistributionMatrixType.UNIFORM);
        classificationLearnerCustomizer.keepDistributionMatrix(true);
        classificationLearner
                .setRuntimeCustomizer(classificationLearnerCustomizer);

        FileSystemStorageManager fssm = new FileSystemStorageManager(indexPath,
                false);
        fssm.open();

        IIndex training = TroveReadWriteHelper.readIndex(fssm, indexName,
                TroveContentDBType.Full, TroveClassificationDBType.Full);

        fssm.close();

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
                null, ClassificationMode.PER_DOCUMENT, new LogisticFunction(),
                status);

        String quantifierPrefix = indexName + "_Quantifier-" + folds
                + "_MPBoost-" + iterations;

        IQuantifier[] quantifiers = quantificationLearner.learn(training);

        IDataManager classifierDataManager = new AdaBoostDataManager();

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
