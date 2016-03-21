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

import it.cnr.jatecs.classification.ClassificationScoreDB;
import it.cnr.jatecs.classification.adaboost.AdaBoostClassifierCustomizer;
import it.cnr.jatecs.classification.adaboost.AdaBoostDataManager;
import it.cnr.jatecs.classification.module.Classifier;
import it.cnr.jatecs.classification.treeboost.TreeBoostClassifier;
import it.cnr.jatecs.classification.treeboost.TreeBoostClassifierCustomizer;
import it.cnr.jatecs.classification.treeboost.TreeBoostDataManager;
import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDB;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveClassificationDBType;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveContentDBType;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveReadWriteHelper;
import it.cnr.jatecs.io.FileSystemStorageManager;
import it.cnr.jatecs.utils.Os;

import java.io.File;
import java.io.IOException;

public class ClassifyTreeBoost {

    public static void main(String[] args) throws IOException {
        if (args.length != 2 && args.length != 3 && args.length != 4) {
            System.err
                    .println("Usage: ClassifyTreeBoost <testIndexDirectory> <classifierDirectory> [<iterationCount>] [-d]\n"
                            + "\tthe -d option dumps all the classification confidences in a file.");
            return;
        }

        String indexFile = args[0];

        File file = new File(indexFile);

        String indexName = file.getName();
        String indexPath = file.getParent();

        String classifierFile = args[1];

        file = new File(classifierFile);

        // Load test index from disk.
        String classifierName = file.getName();
        String classifierPath = file.getParent();
        FileSystemStorageManager storageManager = new FileSystemStorageManager(
                indexPath, false);
        storageManager.open();
        IIndex test = TroveReadWriteHelper.readIndex(storageManager, indexName,
                TroveContentDBType.Full, TroveClassificationDBType.Full);
        storageManager.close();

        // Read treeboost classifier from disk.
        AdaBoostDataManager internalDataManager = new AdaBoostDataManager();
        TreeBoostDataManager dataManager = new TreeBoostDataManager(
                internalDataManager);
        storageManager = new FileSystemStorageManager(classifierPath, false);
        storageManager.open();
        TreeBoostClassifier classifier = (TreeBoostClassifier) dataManager
                .read(storageManager, classifierName);
        storageManager.close();

        // The number of iterations used in learning phase is already stored on disk and does
        // not need to be specified at classification time. It is however possible to change this by specify directly
        // the number of iterations to use while classifying documents.
        int iterCount = -1;
        if (args.length == 3 || args.length == 4) {
            try {
                iterCount = Integer.parseInt(args[2]);
                AdaBoostClassifierCustomizer internalClassifierCustomizer = new AdaBoostClassifierCustomizer();
                internalClassifierCustomizer.groupHypothesis(true);
                internalClassifierCustomizer.setNumIterations(iterCount);
                TreeBoostClassifierCustomizer classifierCustomizer = new TreeBoostClassifierCustomizer(
                        internalClassifierCustomizer);
                classifier.setRuntimeCustomizer(classifierCustomizer);
                System.out.println("Setting custom iteration number to "
                        + iterCount);
            } catch (Exception e) {
            }
        }

        // If requested by the user, we also dump classification confidence scores.
        boolean dumpConfidences = false;
        if (args.length == 4 && args[3].equals("-d"))
            dumpConfidences = true;
        if (args.length == 3 && args[2].equals("-d"))
            dumpConfidences = true;

        // Perform classification over test index.
        Classifier classifierModule = new Classifier(test, classifier,
                dumpConfidences);
        classifierModule.exec();

        // Retrieve the classification results (not including scores, just the predictions).
        IClassificationDB testClassification = classifierModule
                .getClassificationDB();

        // Write classification results on disk.
        String classificationName = indexName + "_" + classifierName;
        if (iterCount > 0)
            classificationName += "_iter-" + iterCount;
        testClassification.setName(classificationName);
        storageManager = new FileSystemStorageManager(indexPath, false);
        storageManager.open();
        TroveReadWriteHelper.writeClassification(storageManager,
                testClassification, classificationName + ".cla", true);
        storageManager.close();

        // If the case, write on disk also scores.
        if (dumpConfidences) {
            ClassificationScoreDB confidences = classifierModule.getConfidences();
            ClassificationScoreDB.write(indexPath + Os.pathSeparator()
                    + classificationName + ".confidences", confidences);
        }
    }
}
