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
import it.cnr.jatecs.classification.adaboost.AdaBoostClassifier;
import it.cnr.jatecs.classification.adaboost.AdaBoostClassifierCustomizer;
import it.cnr.jatecs.classification.adaboost.AdaBoostDataManager;
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

public class ClassifyMPBoost {

    public static void main(String[] args) throws IOException {
        if (args.length != 2 && args.length != 3 && args.length != 4) {
            System.err
                    .println("Usage: ClassifyMPBoost <testIndexDirectory> <classifierDirectory> [<iterationCount>] [-d]\n"
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

        FileSystemStorageManager fssm = new FileSystemStorageManager(indexPath,
                false);
        fssm.open();
        IIndex test = TroveReadWriteHelper.readIndex(fssm, indexName,
                TroveContentDBType.Full, TroveClassificationDBType.Full);
        fssm.close();

        fssm = new FileSystemStorageManager(classifierPath,
                false);
        AdaBoostDataManager dataManager = new AdaBoostDataManager();
        AdaBoostClassifier classifier = (AdaBoostClassifier) dataManager
                .read(fssm, classifierName);
        fssm.close();

        int iterCount = -1;
        if (args.length == 3 || args.length == 4) {
            try {
                iterCount = Integer.parseInt(args[2]);
                AdaBoostClassifierCustomizer cust = new AdaBoostClassifierCustomizer();
                cust.setNumIterations(iterCount);
                classifier.setRuntimeCustomizer(cust);
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


        Classifier classifierModule = new Classifier(test, classifier, dumpConfidences);
        classifierModule.exec();

        IClassificationDB testClassification = classifierModule
                .getClassificationDB();

        String classificationName = indexName + "_" + classifierName;

        if (iterCount > 0)
            classificationName += "_iter-" + iterCount;

        testClassification.setName(classificationName);

        FileSystemStorageManager fssmOut = new FileSystemStorageManager(indexPath,
                true);
        fssmOut.open();

        TroveReadWriteHelper.writeClassification(fssm, testClassification,
                classificationName + ".cla", true);

        fssmOut.close();

        if (dumpConfidences) {
            ClassificationScoreDB confidences = classifierModule.getConfidences();
            ClassificationScoreDB.write(indexPath + Os.pathSeparator() + classificationName + ".confidences", confidences);
        }

    }
}
