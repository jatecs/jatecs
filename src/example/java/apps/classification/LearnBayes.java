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

import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.naivebayes.NaiveBayesDataManager;
import it.cnr.jatecs.classification.naivebayes.NaiveBayesLearner;
import it.cnr.jatecs.classification.naivebayes.NaiveBayesLearnerCustomizer;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveClassificationDBType;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveContentDBType;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveReadWriteHelper;
import it.cnr.jatecs.io.FileSystemStorageManager;
import it.cnr.jatecs.utils.Os;

import java.io.File;
import java.io.IOException;

public class LearnBayes {

    public static void main(String[] args) throws IOException {
        if (args.length != 1 && args.length != 2) {
            System.err
                    .println("Usage: LearnBayes <trainingIndexDirectory> [<smoothing factor, default 1.0>]");
            return;
        }

        String indexFile = args[0];

        File file = new File(indexFile);

        String indexName = file.getName();
        String indexPath = file.getParent();

        double smooth = 1.0;
        if (args.length == 2)
            smooth = Double.parseDouble(args[1]);

        // LEARNING
        NaiveBayesLearner learner = new NaiveBayesLearner();
        NaiveBayesLearnerCustomizer customizer = new NaiveBayesLearnerCustomizer();
        customizer.setSmoothingFactor(smooth);
        customizer.useMultinomialModel();
        learner.setRuntimeCustomizer(customizer);

        FileSystemStorageManager storageManager = new FileSystemStorageManager(
                indexPath, false);
        storageManager.open();
        IIndex training = TroveReadWriteHelper.readIndex(storageManager,
                indexName, TroveContentDBType.IL, TroveClassificationDBType.IL);
        storageManager.close();
        IClassifier classifier = learner.build(training);

        NaiveBayesDataManager dataManager = new NaiveBayesDataManager();
        dataManager.write(indexPath + Os.pathSeparator() + indexName + "-NB-"
                + Os.generateDoubleString(smooth, 2), classifier);

    }

}
