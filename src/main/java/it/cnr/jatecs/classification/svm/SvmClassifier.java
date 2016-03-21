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

import gnu.trove.TShortArrayList;
import it.cnr.jatecs.classification.BaseClassifier;
import it.cnr.jatecs.classification.ClassificationResult;
import it.cnr.jatecs.classification.ClassifierRange;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.iterators.TShortArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;
import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;

public class SvmClassifier extends BaseClassifier {

    svm_model[] _models;

    SvmClassifier() {
        _models = null;
    }


    public ClassificationResult classify(IIndex testIndex, int docID) {
        ClassificationResult res = new ClassificationResult();

        IIntIterator feats = testIndex.getContentDB().getDocumentFeatures(docID);
        svm_node[] doc = new svm_node[testIndex.getContentDB().getDocumentFeaturesCount(docID)];
        int i = 0;
        int featID = 0;
        while (feats.hasNext()) {
            featID = feats.next();
            svm_node node = new svm_node();
            node.index = featID + 1;
            node.value = testIndex.getWeightingDB().getDocumentFeatureWeight(docID, featID);
            doc[i++] = node;
        }

        res.documentID = docID;

        for (short catID = 0; catID < getCategoryCount(); catID++) {
            svm_model model = _models[catID];
            double[] values = new double[1];
            double prediction = svm.svm_predict_values(model, doc, values);
            res.categoryID.add(catID);
            // If the classifier is completely un-confident (i.e. it has no positive examples for this category in the training set)
            // the confidence value is set to the minimum negative value (negative confidence = negative decision)
            if (values[0] == 0) {
                prediction = -1;
                values[0] = -Double.MIN_VALUE;
            }
            res.score.add(prediction * Math.abs(values[0]));
        }

        return res;
    }

    public ClassifierRange getClassifierRange(short catID) {
        ClassifierRange cr = new ClassifierRange();
        cr.border = 0;
        cr.maximum = Double.MAX_VALUE;
        cr.minimum = -Double.MAX_VALUE;

        return cr;
    }

    @Override
    public int getCategoryCount() {
        return _models.length;
    }

    @Override
    public IShortIterator getCategories() {
        TShortArrayList l = new TShortArrayList();
        for (short j = 0; j < _models.length; j++)
            l.add(j);

        return new TShortArrayListIterator(l);
    }

}
