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

import it.cnr.jatecs.classification.BaseLearner;
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.interfaces.ILearnerRuntimeCustomizer;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;
import libsvm.*;

import java.util.Vector;

public class SvmLearner extends BaseLearner {

    public SvmLearner() {
        _customizer = new SvmLearnerCustomizer();
    }

    public IClassifier build(IIndex trainingIndex) {

        svm_problem problem = new svm_problem();
        problem.l = trainingIndex.getDocumentDB().getDocumentsCount();
        problem.x = null;
        problem.y = new double[problem.l];

        //set the input data for global representation
        if (!trainingIndex.getDomainDB().hasLocalRepresentation()) {
            problem.x = setXglobalRepresentation(trainingIndex);
        }

        svm_parameter param = ((SvmLearnerCustomizer) _customizer)
                .getSVMParameter();

        SvmClassifier cl = new SvmClassifier();
        cl._models = new svm_model[trainingIndex.getCategoryDB().getCategoriesCount()];

        IShortIterator cats = trainingIndex.getCategoryDB().getCategories();
        while (cats.hasNext()) {
            short catID = cats.next();

            SvmLearnerCustomizer cust = (SvmLearnerCustomizer) _customizer;
            param.C = cust.getSoftMarginCost(catID);

            //set the input data for global representation
            if (trainingIndex.getDomainDB().hasLocalRepresentation()) {
                problem.x = setXlocalRepresentation(trainingIndex, catID);
            }

            JatecsLogger.status().print("Processing category " + trainingIndex.getCategoryDB().getCategoryName(catID) + "...");

            IIntIterator docs = trainingIndex.getDocumentDB().getDocuments();
            while (docs.hasNext()) {
                int docID = docs.next();
                if (trainingIndex.getClassificationDB().hasDocumentCategory(docID, catID)) {
                    problem.y[docID] = 1;
                } else {
                    problem.y[docID] = -1;
                }
            }

            // Build model for category.
            svm_model model = svm.svm_train(problem, param);

            cl._models[catID] = model;

            JatecsLogger.status().println("done.");
        }

        return cl;
    }

    //generates the problem input assuming global representation
    private svm_node[][] setXglobalRepresentation(IIndex trainingIndex) {
        svm_node[][] x = new svm_node[trainingIndex.getDocumentDB().getDocumentsCount()][];

        IIntIterator docs = trainingIndex.getDocumentDB().getDocuments();
        while (docs.hasNext()) {
            int docID = docs.next();
            x[docID] = new svm_node[trainingIndex.getContentDB().getDocumentFeaturesCount(docID)];
            IIntIterator feats = trainingIndex.getContentDB().getDocumentFeatures(docID);
            int j = 0;
            while (feats.hasNext()) {
                int featID = feats.next();
                svm_node node = new svm_node();
                node.index = featID + 1;
                node.value = trainingIndex.getWeightingDB().getDocumentFeatureWeight(docID, featID);
                x[docID][j++] = node;
            }
        }

        return x;
    }

    //generates the problem input assuming local representation
    private svm_node[][] setXlocalRepresentation(IIndex trainingIndex, short catID) {
        svm_node[][] x = new svm_node[trainingIndex.getDocumentDB().getDocumentsCount()][];

        IIntIterator docs = trainingIndex.getDocumentDB().getDocuments();
        while (docs.hasNext()) {
            int docID = docs.next();
            x[docID] = new svm_node[trainingIndex.getDocumentFeaturesCount(docID, catID)];
            IIntIterator feats = trainingIndex.getDocumentFeatures(docID, catID);
            int j = 0;
            while (feats.hasNext()) {
                int featID = feats.next();
                svm_node node = new svm_node();
                node.index = featID + 1;
                node.value = trainingIndex.getDocumentFeatureWeight(docID, featID, catID);
                x[docID][j++] = node;
            }
        }

        return x;
    }

    @Override
    public IClassifier mergeClassifiers(Vector<IClassifier> classifiers) {
        SvmClassifier cl = new SvmClassifier();

        cl._models = new svm_model[classifiers.size()];

        for (int i = 0; i < classifiers.size(); i++) {
            SvmClassifier c = (SvmClassifier) classifiers.get(i);
            cl._models[i] = c._models[0];
        }

        return cl;
    }

    @Override
    public void setRuntimeCustomizer(
            Vector<ILearnerRuntimeCustomizer> customizers) {
        SvmLearnerCustomizer cust = (SvmLearnerCustomizer) getRuntimeCustomizer();
        cust._cost.clear();
        for (short i = 0; i < customizers.size(); i++) {
            SvmLearnerCustomizer c = (SvmLearnerCustomizer) customizers.get(i);
            cust._cost.put(i, c.getSoftMarginCost((short) 0));
        }
    }

    @Override
    public ILearnerRuntimeCustomizer getRuntimeCustomizer(short catID) {
        return null;
    }
}
