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

import it.cnr.jatecs.classification.adaboost.*;
import it.cnr.jatecs.classification.bagging.BaggingDataManager;
import it.cnr.jatecs.classification.bagging.BaggingLearner;
import it.cnr.jatecs.classification.bagging.BaggingLearnerCustomizer;
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.mpboost.MPWeakLearnerMultiThread;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveClassificationDBType;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveContentDBType;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveReadWriteHelper;
import it.cnr.jatecs.io.FileSystemStorageManager;

import java.io.File;
import java.io.IOException;

public class LearnBagging {

    public static void main(String[] args) throws IOException {
        if (args.length != 5) {
            System.err
                    .println("Usage: LearnBagging <seed> <bagCount> <threadCount> <iterationCount> <trainingIndexDirectory>");
            return;
        }

        int seed = Integer.parseInt(args[0]);
        int bagCount = Integer.parseInt(args[1]);
        int threadCount = Integer.parseInt(args[2]);
        int iterations = Integer.parseInt(args[3]);
        String indexFile = args[4];

        File file = new File(indexFile);

        String indexName = file.getName();
        String indexPath = file.getParent();

        // LEARNING
        AdaBoostLearner internalLearner = new AdaBoostLearner();
        AdaBoostLearnerCustomizer internalCustomizer = new AdaBoostLearnerCustomizer();
        internalCustomizer.setNumIterations(iterations);
        internalCustomizer.setWeakLearner(new MPWeakLearnerMultiThread(
                threadCount));
        internalCustomizer.setPerCategoryNormalization(true);
        internalCustomizer.setLossFunction(new ExponentialLoss());
        internalCustomizer.keepDistributionMatrix(false);
        internalCustomizer
                .setInitialDistributionType(InitialDistributionMatrixType.UNIFORM);
        internalLearner.setRuntimeCustomizer(internalCustomizer);

        BaggingLearner learner = new BaggingLearner(seed, internalLearner);
        BaggingLearnerCustomizer customizer = new BaggingLearnerCustomizer(
                bagCount, internalCustomizer);
        learner.setRuntimeCustomizer(customizer);

        FileSystemStorageManager storageManager = new FileSystemStorageManager(
                indexPath, false);
        storageManager.open();
        IIndex training = TroveReadWriteHelper.readIndex(storageManager,
                indexName, TroveContentDBType.Full,
                TroveClassificationDBType.Full);

        IClassifier classifier = learner.build(training);

        AdaBoostDataManager internalDataManager = new AdaBoostDataManager();
        BaggingDataManager dataManager = new BaggingDataManager(
                internalDataManager);
        dataManager.write(storageManager, indexName + "-Bagging-" + seed + "-"
                + bagCount + "-" + iterations, classifier);

        storageManager.close();

    }

}
