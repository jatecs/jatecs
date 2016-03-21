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
import it.cnr.jatecs.classification.knn.KnnClassifier;
import it.cnr.jatecs.classification.knn.KnnClassifierCustomizer;
import it.cnr.jatecs.classification.module.Classifier;
import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDB;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveClassificationDBType;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveContentDBType;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveReadWriteHelper;
import it.cnr.jatecs.io.FileSystemStorageManager;
import it.cnr.jatecs.utils.Os;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.io.File;
import java.io.IOException;

public class ClassifyKnn {

    public static void main(String[] args) throws IOException {
        if (args.length != 3 && args.length != 4) {
            System.err
                    .println("Usage: ClassifyKnn <k-value> <trainIndexDirectory> <testIndexDirectory> [-d]\n"
                            + "\tthe -d option dumps all the classification confidences in a file.");
            return;
        }

        int k = Integer.parseInt(args[0]);

        String trainFile = args[1];

        File file = new File(trainFile);

        String trainName = file.getName();
        String trainPath = file.getParent();

        String testFile = args[2];

        file = new File(testFile);

        String testName = file.getName();
        String testPath = file.getParent();

        boolean dumpConfidences = false;
        if (args.length == 4 && args[3].equals("-d"))
            dumpConfidences = true;

        FileSystemStorageManager storageManager = new FileSystemStorageManager(
                trainPath, false);
        storageManager.open();
        IIndex training = TroveReadWriteHelper.readIndex(storageManager,
                trainName, TroveContentDBType.Full,
                TroveClassificationDBType.Full);
        storageManager.close();

        storageManager = new FileSystemStorageManager(testPath, false);
        storageManager.open();
        IIndex test = TroveReadWriteHelper.readIndex(storageManager, testName,
                TroveContentDBType.Full, TroveClassificationDBType.Full);
        storageManager.close();

        KnnClassifier classifier = new KnnClassifier(training);

        // setting the value k
        KnnClassifierCustomizer customizer = new KnnClassifierCustomizer();
        IShortIterator cats = training.getCategoryDB().getCategories();
        while (cats.hasNext()) {
            short cat = cats.next();
            customizer.setK(cat, k);
        }
        classifier.setRuntimeCustomizer(customizer);

        // CLASSIFICATION
        String classificationName = testName + "_KNN-" + k + "_" + trainName;

        Classifier classifierModule = new Classifier(test, classifier,
                dumpConfidences);
        classifierModule.exec();

        IClassificationDB testClassification = classifierModule
                .getClassificationDB();

        storageManager = new FileSystemStorageManager(testPath, false);
        storageManager.open();
        TroveReadWriteHelper.writeClassification(storageManager,
                testClassification, classificationName + ".cla", true);
        storageManager.close();
        if (dumpConfidences) {
            ClassificationScoreDB confidences = classifierModule.getConfidences();
            ClassificationScoreDB.write(testPath + Os.pathSeparator()
                    + classificationName + ".confidences", confidences);
        }
    }
}
