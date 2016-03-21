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
import it.cnr.jatecs.classification.module.Classifier;
import it.cnr.jatecs.classification.svm.SvmClassifier;
import it.cnr.jatecs.classification.svm.SvmDataManager;
import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDB;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveClassificationDBType;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveContentDBType;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveReadWriteHelper;
import it.cnr.jatecs.io.FileSystemStorageManager;
import it.cnr.jatecs.utils.Os;

import java.io.File;
import java.io.IOException;

public class ClassifySVMlib {

    public static void main(String[] args) throws IOException {
        if (args.length != 2 && args.length != 3) {
            System.err
                    .println("Usage: ClassifySVMlib <testIndexDirectory> <classifierDirectory> [-d]\n"
                            + "\tthe -d option dumps all the classification confidences in a file.");
            return;
        }

        String testFile = args[0];

        File file = new File(testFile);

        String testName = file.getName();
        String testPath = file.getParent();

        String classifierFile = args[1];

        file = new File(classifierFile);

        String classifierName = file.getName();
        String classifierPath = file.getParent();

        boolean dumpConfidences = false;
        if (args.length == 3 && args[2].equals("-d"))
            dumpConfidences = true;

        // Load test index.
        FileSystemStorageManager fssm = new FileSystemStorageManager(testPath,
                false);
        fssm.open();
        IIndex test = TroveReadWriteHelper.readIndex(fssm, testName,
                TroveContentDBType.Full, TroveClassificationDBType.Full);
        fssm.close();

        // Load SVM classifier.
        fssm = new FileSystemStorageManager(classifierPath,
                false);
        fssm.open();
        SvmDataManager dataManager = new SvmDataManager();
        SvmClassifier classifier = (SvmClassifier) dataManager
                .read(fssm, classifierName);
        fssm.close();

        // Perform classification through the utility class Classifier.
        Classifier classifierModule = new Classifier(test, classifier, dumpConfidences);
        classifierModule.exec();

        // Get last performed classification results.
        IClassificationDB testClassification = classifierModule
                .getClassificationDB();


        // Save classification results on disk.
        FileSystemStorageManager fssmOut = new FileSystemStorageManager(testPath,
                true);
        fssmOut.open();

        String classificationName = testName + "_" + classifierName;
        TroveReadWriteHelper.writeClassification(fssmOut, testClassification,
                classificationName + ".cla", true);

        fssmOut.close();

        // Save also classification confidences.
        if (dumpConfidences) {
            ClassificationScoreDB confidences = classifierModule.getConfidences();
            ClassificationScoreDB.write(testPath + Os.pathSeparator() + classificationName + ".confidences", confidences);
        }
    }
}
