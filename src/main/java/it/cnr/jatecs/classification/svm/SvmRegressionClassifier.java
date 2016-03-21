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

import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TDoubleShortHashMap;
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

public class SvmRegressionClassifier extends BaseClassifier {

    private svm_model _model;
    private TDoubleShortHashMap _map;
    private double[] _values;

    SvmRegressionClassifier(svm_model model) {
        _model = model;
        _map = new TDoubleShortHashMap();
        _values = null;
    }

    public double[] values() {
        return _values;
    }

    private void setValues(TDoubleDoubleHashMap optimizedValues, IIndex testIndex) {
        _values = new double[testIndex.getCategoryDB().getCategoriesCount()];
        IShortIterator cats = testIndex.getCategoryDB().getCategories();
        while (cats.hasNext()) {
            short cat = cats.next();
            double value = Double.parseDouble(testIndex.getCategoryDB()
                    .getCategoryName(cat));
            if (optimizedValues == null) {
                _map.put(value, cat);
                _values[cat] = value;
            } else {
                _map.put(optimizedValues.get(value), cat);
                _values[cat] = optimizedValues.get(value);
            }
        }
    }


    public ClassificationResult classify(IIndex testIndex, int docID) {
        ClassificationResult res = new ClassificationResult();

        SvmRegressionClassifierCustomizer cust = (SvmRegressionClassifierCustomizer) getRuntimeCustomizer();
        if (cust != null)
            setValues(cust.getOptimizedValues(), testIndex);
        else
            setValues(null, testIndex);

        IIntIterator feats = testIndex.getContentDB()
                .getDocumentFeatures(docID);
        svm_node[] doc = new svm_node[testIndex.getContentDB()
                .getDocumentFeaturesCount(docID)];
        int i = 0;
        int featID = 0;
        while (feats.hasNext()) {
            featID = feats.next();
            svm_node node = new svm_node();
            node.index = featID + 1;
            node.value = testIndex.getWeightingDB().getDocumentFeatureWeight(
                    docID, featID);
            doc[i++] = node;
        }

        res.documentID = docID;

        double prediction = svm.svm_predict(_model, doc);
        // Find closest value
        double value = _values[0];
        double minDist = Double.MAX_VALUE;
        for (int k = 0; k < _values.length; ++k) {
            double currentDist = Math.abs(_values[k] - prediction);
            if (currentDist < minDist) {
                minDist = currentDist;
                value = _values[k];
            }
        }
        short catID = _map.get(value);

        boolean found = false;
        IShortIterator catIt = testIndex.getCategoryDB().getCategories();
        while (catIt.hasNext()) {
            short category = catIt.next();
            res.categoryID.add(category);
            if (category == catID) {
                found = true;
                res.score.add(1.0);
            } else
                res.score.add(-1.0);
        }

        assert (found);

        return res;
    }

    public ClassifierRange getClassifierRange(short catID) {
        ClassifierRange cr = new ClassifierRange();
        cr.border = 0;
        cr.maximum = 1;
        cr.minimum = -1;

        return cr;
    }

    @Override
    public int getCategoryCount() {
        return 1;
    }

    @Override
    public IShortIterator getCategories() {
        TShortArrayList l = new TShortArrayList();
        l.add((short) 0);
        return new TShortArrayListIterator(l);
    }

    public double classifyTest(IIndex testIndex, int docID) {
        ClassificationResult res = new ClassificationResult();

        IIntIterator feats = testIndex.getContentDB()
                .getDocumentFeatures(docID);
        svm_node[] doc = new svm_node[testIndex.getContentDB()
                .getDocumentFeaturesCount(docID)];
        int i = 0;
        int featID = 0;
        while (feats.hasNext()) {
            featID = feats.next();
            svm_node node = new svm_node();
            node.index = featID + 1;
            node.value = testIndex.getWeightingDB().getDocumentFeatureWeight(
                    docID, featID);
            doc[i++] = node;
        }

        res.documentID = docID;

        double prediction = svm.svm_predict(_model, doc);
        return prediction;
    }

    public svm_model model() {
        return _model;
    }
}
