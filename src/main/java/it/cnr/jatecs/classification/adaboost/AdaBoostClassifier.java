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

package it.cnr.jatecs.classification.adaboost;

import gnu.trove.*;
import it.cnr.jatecs.classification.BaseClassifier;
import it.cnr.jatecs.classification.ClassificationResult;
import it.cnr.jatecs.classification.ClassifierRange;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.iterators.TShortArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

/**
 * This class implements the skeleton of a boosting classifier, which uses the
 * weak hypothesis learned during the learning phase.
 *
 * @author Andrea Esuli
 */
public class AdaBoostClassifier extends BaseClassifier {

    /**
     * The set of weak hypothesis computed by the boosting algorithm.
     */
    public IWeakHypothesis[] _hypothesis;
    /**
     * The number of valid categories learned by the classifier, ranging from 0
     * to _validCategories-1
     */
    protected int _validCategories;
    /**
     * The maximum number of iterations stored in the learning model.
     */
    protected int _maxNumIterations;
    /**
     * The file containing the generated distribution matrix.
     */
    String _distributionMatrixFilename;
    private Vector<TIntObjectHashMap<HypothesisData>> _grouped;
    private boolean _hypothesisGrouped;

    public AdaBoostClassifier() {
        _maxNumIterations = 0;
        _distributionMatrixFilename = null;
        _customizer = new AdaBoostClassifierCustomizer();
        _hypothesisGrouped = false;
    }

    protected void mergeHypothesis(IIndex index) {
        _grouped = new Vector<TIntObjectHashMap<HypothesisData>>();
        for (short catID = 0; catID < _validCategories; catID++) {
            _grouped.add(catID, new TIntObjectHashMap<HypothesisData>());
            int numHypoythesis = Math
                    .min(_hypothesis.length,
                            ((AdaBoostClassifierCustomizer) _customizer)._numIterations);
            for (int i = 0; i < numHypoythesis; ++i) {
                IWeakHypothesis h = _hypothesis[i];
                HypothesisData hd = h.value(catID);

                HypothesisData d = (HypothesisData) _grouped.get(catID).get(
                        hd.pivot);
                if (d == null) {
                    d = new HypothesisData();
                    d.c0 = hd.c0;
                    d.c1 = hd.c1;
                    d.pivot = hd.pivot;
                    _grouped.get(catID).put(d.pivot, d);
                } else {
                    d.c0 += hd.c0;
                    d.c1 += hd.c1;
                }

            }

        }
    }

    public ClassificationResult computeVariance(IIndex testIndex, int docID) {
        if (_customizer == null) {
            _customizer = new AdaBoostClassifierCustomizer();
        }

        boolean groupHypothesis = ((AdaBoostClassifierCustomizer) _customizer)._groupHypothesis;
        if (groupHypothesis && !_hypothesisGrouped) {
            mergeHypothesis(testIndex);
            _hypothesisGrouped = true;
        }

        ClassificationResult res = new ClassificationResult();
        res.documentID = docID;

        if (!groupHypothesis) {
            for (short catID = 0; catID < _validCategories; catID++) {
                double w = 0;
                TDoubleArrayList values = new TDoubleArrayList();
                int numIterations = Math
                        .min(_hypothesis.length,
                                ((AdaBoostClassifierCustomizer) _customizer)._numIterations);
                for (int i = 0; i < numIterations; i++) {
                    HypothesisData hd = _hypothesis[i].value(catID);
                    int pivot = hd.pivot;
                    if (pivot >= 0) {
                        if (testIndex.getContentDB().hasDocumentFeature(docID,
                                pivot)) {
                            values.add(hd.c1);
                            w += hd.c1;
                        } else {
                            values.add(hd.c0);
                            w += hd.c0;
                        }
                    }
                }
                double average = w / values.size();
                w = 0;
                for (int i = 0; i < values.size(); ++i) {
                    w += Math.pow(values.getQuick(i) - average, 2.0);
                }
                res.categoryID.add(catID);
                res.score.add(w);
            }
        } else {
            for (short catID = 0; catID < _validCategories; catID++) {
                double w = 0;
                TDoubleArrayList values = new TDoubleArrayList();
                TIntObjectHashMap<HypothesisData> groups = _grouped.get(catID);
                TIntObjectIterator<HypothesisData> it = groups.iterator();
                while (it.hasNext()) {
                    it.advance();
                    HypothesisData hd = (HypothesisData) it.value();
                    int pivot = hd.pivot;
                    if (pivot >= 0) {
                        if (testIndex.getContentDB().hasDocumentFeature(docID,
                                pivot)) {
                            values.add(hd.c1);
                            w += hd.c1;
                        } else {
                            values.add(hd.c0);
                            w += hd.c0;
                        }
                    }
                }
                double average = w / values.size();
                w = 0;
                for (int i = 0; i < values.size(); ++i) {
                    w += Math.pow(values.getQuick(i) - average, 2.0);
                }
                res.categoryID.add(catID);
                res.score.add(w);
            }
        }

        return res;
    }

    public ClassificationResult classify(IIndex testIndex, int docID) {
        if (_customizer == null) {
            _customizer = new AdaBoostClassifierCustomizer();
        }
        boolean groupHypothesis = ((AdaBoostClassifierCustomizer) _customizer)._groupHypothesis;
        if (groupHypothesis && !_hypothesisGrouped) {
            mergeHypothesis(testIndex);
            _hypothesisGrouped = true;
        }

        ClassificationResult res = new ClassificationResult();
        res.documentID = docID;

        if (!groupHypothesis) {
            for (short catID = 0; catID < _validCategories; catID++) {
                double w = 0;
                int numIterations = Math
                        .min(_hypothesis.length,
                                ((AdaBoostClassifierCustomizer) _customizer)._numIterations);
                for (int i = 0; i < numIterations; i++) {
                    HypothesisData hd = _hypothesis[i].value(catID);
                    int pivot = hd.pivot;
                    if (pivot >= 0) {
                        if (testIndex.getContentDB().hasDocumentFeature(docID,
                                pivot))
                            w += hd.c1;
                        else
                            w += hd.c0;
                    }
                }
                res.categoryID.add(catID);
                res.score.add(w);
            }
        } else {
            for (short catID = 0; catID < _validCategories; catID++) {
                double w = 0;
                TIntObjectHashMap<HypothesisData> values = _grouped.get(catID);
                TIntObjectIterator<HypothesisData> it = values.iterator();
                while (it.hasNext()) {
                    it.advance();
                    HypothesisData hd = (HypothesisData) it.value();
                    int pivot = hd.pivot;
                    if (pivot >= 0) {
                        if (testIndex.getContentDB().hasDocumentFeature(docID,
                                pivot))
                            w += hd.c1;
                        else
                            w += hd.c0;
                    }
                }
                res.categoryID.add(catID);
                res.score.add(w);
            }
        }

        return res;
    }

    public ClassifierRange getClassifierRange(short catID) {
        ClassifierRange range = new ClassifierRange();

        range.border = 0;
        range.maximum = Double.MAX_VALUE;
        range.minimum = -Double.MAX_VALUE;

        return range;
    }

    @Override
    public int getCategoryCount() {
        IWeakHypothesis hyp = _hypothesis[0];
        return hyp.getClassifiersCount();
    }

    @Override
    public IShortIterator getCategories() {
        TShortArrayList l = new TShortArrayList();
        for (short i = 0; i < _validCategories; i++)
            l.add(i);

        return new TShortArrayListIterator(l);
    }

    public TIntArrayList getDistinctPivots(short catID) {
        Hashtable<Integer, Integer> map = new Hashtable<Integer, Integer>();
        for (int i = 0; i < _hypothesis.length; i++) {
            if (!map.containsKey(_hypothesis[i].value(catID).pivot))
                map.put(_hypothesis[i].value(catID).pivot,
                        _hypothesis[i].value(catID).pivot);
        }

        TIntArrayList pivots = new TIntArrayList();
        Iterator<Integer> keys = map.keySet().iterator();
        while (keys.hasNext()) {
            int pivot = keys.next();
            if (pivot >= 0)
                pivots.add(pivot);
        }

        return pivots;
    }
}
