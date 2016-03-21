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
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveClassificationDBType;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveContentDBType;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveReadWriteHelper;
import it.cnr.jatecs.io.FileSystemStorageManager;

import java.io.File;
import java.io.IOException;

public class LearnMPBoostMultiThread {

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.err
                    .println("Usage: LearnMPBoostMultiThread <threadCount> <iterationCount> <trainingIndexDirectory>");
            return;
        }

        int threadCount = Integer.parseInt(args[0]);
        int iterations = Integer.parseInt(args[1]);
        String indexFile = args[2];

        File file = new File(indexFile);

        String indexName = file.getName();
        String indexPath = file.getParent();

        AdaBoostLearner learner = new AdaBoostLearner();
        AdaBoostLearnerCustomizer customizer = new AdaBoostLearnerCustomizer();
        customizer.setNumIterations(iterations);
        customizer.setWeakLearner(new MPWeakLearnerMultiThread(threadCount));
        customizer.setPerCategoryNormalization(true);
        customizer.setLossFunction(new ExponentialLoss());
        customizer
                .setInitialDistributionType(InitialDistributionMatrixType.UNIFORM);
        customizer.keepDistributionMatrix(true);
        learner.setRuntimeCustomizer(customizer);

        FileSystemStorageManager fssmIn = new FileSystemStorageManager(indexPath,
                false);
        fssmIn.open();

        IIndex training = TroveReadWriteHelper.readIndex(fssmIn, indexName,
                TroveContentDBType.Full, TroveClassificationDBType.Full);

        fssmIn.close();

        IClassifier classifier = learner.build(training);

        AdaBoostDataManager dataManager = new AdaBoostDataManager();

        FileSystemStorageManager fssmOut = new FileSystemStorageManager(indexPath,
                true);
        fssmOut.open();

        dataManager.write(fssmOut, indexName + "_MPBoost-" + iterations, classifier);

        fssmOut.close();

    }

}
