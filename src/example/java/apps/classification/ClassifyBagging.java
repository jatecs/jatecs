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
import it.cnr.jatecs.classification.bagging.BaggingClassifier;
import it.cnr.jatecs.classification.bagging.BaggingClassifierCustomizer;
import it.cnr.jatecs.classification.bagging.BaggingDataManager;
import it.cnr.jatecs.classification.module.Classifier;
import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDB;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveClassificationDBType;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveContentDBType;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveReadWriteHelper;
import it.cnr.jatecs.io.FileSystemStorageManager;
import it.cnr.jatecs.utils.Os;

import java.io.File;
import java.io.IOException;

public class ClassifyBagging {

    public static void main(String[] args) throws IOException {
        if (args.length != 2 && args.length != 3 && args.length != 4) {
            System.err
                    .println("Usage: ClassifyBagging <testIndexDirectory> <classifierDirectory> [<iterationCount>] [-d]\n"
                            + "\tthe -d option dumps all the classification confidences in a file.");
            return;
        }

        String indexFile = args[0];

        File file = new File(indexFile);

        String indexName = file.getName();
        String indexPath = file.getParent();

        String classifierFile = args[1];

        file = new File(classifierFile);

        String classifierName = file.getName();
        String classifierPath = file.getParent();

        FileSystemStorageManager storageManager = new FileSystemStorageManager(
                indexPath, false);
        storageManager.open();
        IIndex test = TroveReadWriteHelper.readIndex(storageManager, indexName,
                TroveContentDBType.Full, TroveClassificationDBType.Full);
        storageManager.close();

        storageManager = new FileSystemStorageManager(classifierPath, false);
        storageManager.open();

        AdaBoostDataManager internalDataManager = new AdaBoostDataManager();
        BaggingDataManager dataManager = new BaggingDataManager(
                internalDataManager);
        BaggingClassifier classifier = (BaggingClassifier) dataManager.read(
                storageManager, classifierName);

        storageManager.close();

        int iterCount = -1;
        if (args.length == 3 || args.length == 4) {
            try {
                iterCount = Integer.parseInt(args[2]);
                AdaBoostClassifierCustomizer internalClassifierCustomizer = new AdaBoostClassifierCustomizer();
                internalClassifierCustomizer.groupHypothesis(true);
                internalClassifierCustomizer.setNumIterations(iterCount);
                BaggingClassifierCustomizer classifierCustomizer = new BaggingClassifierCustomizer(
                        internalClassifierCustomizer);
                classifier.setRuntimeCustomizer(classifierCustomizer);
                System.out.println("Setting custom iteration number to "
                        + iterCount);
            } catch (Exception e) {
            }
        }

        boolean dumpConfidences = false;
        if (args.length == 4 && args[3].equals("-d"))
            dumpConfidences = true;
        if (args.length == 3 && args[2].equals("-d"))
            dumpConfidences = true;

        Classifier classifierModule = new Classifier(test, classifier,
                dumpConfidences);
        classifierModule.exec();

        IClassificationDB testClassification = classifierModule
                .getClassificationDB();

        String classificationName = indexName + "_" + classifierName;

        if (iterCount > 0)
            classificationName += "_iter-" + iterCount;

        testClassification.setName(classificationName);

        storageManager = new FileSystemStorageManager(indexPath, false);
        storageManager.open();
        TroveReadWriteHelper.writeClassification(storageManager,
                testClassification, classificationName + ".cla", true);
        storageManager.close();

        if (dumpConfidences) {
            ClassificationScoreDB confidences = classifierModule.getConfidences();
            ClassificationScoreDB.write(indexPath + Os.pathSeparator()
                    + classificationName + ".confidences", confidences);
        }
    }
}
