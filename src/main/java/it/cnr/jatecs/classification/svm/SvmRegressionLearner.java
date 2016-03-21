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

package it.cnr.jatecs.classification.svm;

import gnu.trove.TShortDoubleHashMap;
import it.cnr.jatecs.classification.BaseLearner;
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.interfaces.ILearnerRuntimeCustomizer;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;
import libsvm.*;

import java.util.Vector;

public class SvmRegressionLearner extends BaseLearner {

    public SvmRegressionLearner() {
        _customizer = new SvmRegressionLearnerCustomizer();
    }

    public IClassifier build(IIndex trainingIndex) {

        svm_node[][] x = new svm_node[trainingIndex.getDocumentDB()
                .getDocumentsCount()][];

        IIntIterator docs = trainingIndex.getDocumentDB().getDocuments();
        while (docs.hasNext()) {
            int docID = docs.next();
            x[docID] = new svm_node[trainingIndex.getContentDB()
                    .getDocumentFeaturesCount(docID)];
            IIntIterator feats = trainingIndex.getContentDB()
                    .getDocumentFeatures(docID);
            int j = 0;
            while (feats.hasNext()) {
                int featID = feats.next();
                svm_node node = new svm_node();
                node.index = featID + 1;
                node.value = trainingIndex.getWeightingDB()
                        .getDocumentFeatureWeight(docID, featID);
                x[docID][j++] = node;
            }
        }

        svm_problem problem = new svm_problem();
        problem.l = trainingIndex.getDocumentDB().getDocumentsCount();
        problem.x = x;
        problem.y = new double[problem.l];

        svm_parameter param = ((SvmRegressionLearnerCustomizer) _customizer)
                .getSVMParameter();

        TShortDoubleHashMap map = new TShortDoubleHashMap();

        IShortIterator cats = trainingIndex.getCategoryDB().getCategories();
        while (cats.hasNext()) {
            short cat = cats.next();
            map.put(cat, Double.parseDouble(trainingIndex.getCategoryDB()
                    .getCategoryName(cat)));
        }

        JatecsLogger.status().print("Processing data");

        // Prepare data.
        docs.begin();
        while (docs.hasNext()) {
            int docID = docs.next();
            problem.y[docID] = (short) 0;
            for (int i = 1; i <= trainingIndex.getCategoryDB()
                    .getCategoriesCount(); i++)
                if (trainingIndex.getClassificationDB().hasDocumentCategory(
                        docID, (short) i)) {
                    problem.y[docID] = map.get((short) i);
                    break;
                }
        }

        svm_model model = null;

        // Build model for category.
        model = svm.svm_train(problem, param);

        SvmRegressionClassifier cl = new SvmRegressionClassifier(model);

        JatecsLogger.status().println("done.");

        return cl;
    }

    @Override
    public IClassifier mergeClassifiers(Vector<IClassifier> classifiers) {

        SvmRegressionClassifier c = (SvmRegressionClassifier) classifiers
                .get(0);
        SvmRegressionClassifier cl = new SvmRegressionClassifier(c.model());
        return cl;
    }

    @Override
    public void setRuntimeCustomizer(
            Vector<ILearnerRuntimeCustomizer> customizers) {
    }

    @Override
    public ILearnerRuntimeCustomizer getRuntimeCustomizer(short catID) {
        return null;
    }

}
