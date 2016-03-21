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
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.mpboost.MPWeakLearnerMultiThread;
import it.cnr.jatecs.classification.treeboost.TreeBoostDataManager;
import it.cnr.jatecs.classification.treeboost.TreeBoostLearner;
import it.cnr.jatecs.classification.treeboost.TreeBoostLearnerCustomizer;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveClassificationDBType;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveContentDBType;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveReadWriteHelper;
import it.cnr.jatecs.io.FileSystemStorageManager;

import java.io.File;
import java.io.IOException;

public class LearnTreeBoost {

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.err
                    .println("Usage: LearnTreeBoost <threadCount> <iterationCount> <trainingIndexDirectory>");
            return;
        }

        int threadCount = Integer.parseInt(args[0]);
        int iterations = Integer.parseInt(args[1]);
        String indexFile = args[2];

        File file = new File(indexFile);


        // Load training index.
        String indexName = file.getName();
        String indexPath = file.getParent();
        FileSystemStorageManager storageManager = new FileSystemStorageManager(
                indexPath, false);
        storageManager.open();
        IIndex training = TroveReadWriteHelper.readIndex(storageManager,
                indexName, TroveContentDBType.Full,
                TroveClassificationDBType.Full);
        storageManager.close();

        // Create internal learner to use in learning process at each level of hierarchy.
        AdaBoostLearner internalLearner = new AdaBoostLearner();

        // Customize internal learner.
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

        // Create TreeBoost learner used to exploit the hierarchy in learning process and
        // specify to use as internal learner the AdaBoostLearner previously declared.
        TreeBoostLearner learner = new TreeBoostLearner(internalLearner);
        TreeBoostLearnerCustomizer customizer = new TreeBoostLearnerCustomizer(
                internalCustomizer);
        learner.setRuntimeCustomizer(customizer);


        // Build hierarchical classifier.
        IClassifier classifier = learner.build(training);

        // Save classifier to disk.
        AdaBoostDataManager internalDataManager = new AdaBoostDataManager();
        TreeBoostDataManager dataManager = new TreeBoostDataManager(
                internalDataManager);
        storageManager = new FileSystemStorageManager(indexPath, false);
        storageManager.open();
        dataManager.write(storageManager, indexName + "-TreeBoost-"
                + iterations, classifier);
        storageManager.close();
    }

}
