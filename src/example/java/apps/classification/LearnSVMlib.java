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
import it.cnr.jatecs.classification.svm.SvmDataManager;
import it.cnr.jatecs.classification.svm.SvmLearner;
import it.cnr.jatecs.classification.svm.SvmLearnerCustomizer;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveClassificationDBType;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveContentDBType;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveReadWriteHelper;
import it.cnr.jatecs.io.FileSystemStorageManager;
import libsvm.svm_parameter;

import java.io.File;
import java.io.IOException;

public class LearnSVMlib {

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err
                    .println("Usage: LearnSVMlib <trainingIndexPath>");
            return;
        }

        String indexFile = args[0];

        File file = new File(indexFile);

        String indexName = file.getName();
        String indexPath = file.getParent();

        // Build the SVM learner.
        SvmLearner learner = new SvmLearner();

        // Create a specific customizer for the learner.
        SvmLearnerCustomizer customizer = new SvmLearnerCustomizer();
        // Specify a liner kernel where C is controlling controlling
        // the tradeoff between a wide margin and classifier error.
        customizer.getSVMParameter().svm_type = svm_parameter.C_SVC;
        customizer.getSVMParameter().kernel_type = svm_parameter.LINEAR;
        // Set a default value of C for all categories...
        customizer.setSoftMarginDefaultCost(1.0);
        // or set a specific C value for each category, e.g. C = 2.0 per category with ID 0.
        //customizer.setSoftMarginCost(0, 2.0);

        // Set the customizer into the learner.
        learner.setRuntimeCustomizer(customizer);

        // Load training index data into RAM memory.
        FileSystemStorageManager fssmIn = new FileSystemStorageManager(indexPath,
                false);
        fssmIn.open();

        IIndex training = TroveReadWriteHelper.readIndex(fssmIn, indexName,
                TroveContentDBType.Full, TroveClassificationDBType.Full);

        fssmIn.close();

        // Build the SVM classifier.
        IClassifier classifier = learner.build(training);

        // Save the generated SVM classifier on disk.
        FileSystemStorageManager fssmOut = new FileSystemStorageManager(indexPath,
                true);
        fssmOut.open();
        SvmDataManager dataManager = new SvmDataManager();
        dataManager.write(fssmOut, indexName
                + "_SVMlib", classifier);
        fssmOut.close();

    }

}
