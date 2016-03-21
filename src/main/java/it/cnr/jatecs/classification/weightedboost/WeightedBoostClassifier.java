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

package it.cnr.jatecs.classification.weightedboost;

import gnu.trove.*;
import it.cnr.jatecs.classification.BaseClassifier;
import it.cnr.jatecs.classification.ClassificationResult;
import it.cnr.jatecs.classification.ClassifierRange;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexing.discretization.DiscreteBin;
import it.cnr.jatecs.utils.iterators.TShortArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

public class WeightedBoostClassifier extends BaseClassifier {

    /**
     * The set of weak hypothesis computed by the boosting algorithm.
     */
    public IWeightedWeakHypothesis[] _hypothesis;
    /**
     * The number of valid categories learned by the classifier, ranging from 0
     * to _validCategories-1
     */
    protected int _validCategories;
    /**
     * The maximum number of iterations stored in the learning model.
     */
    protected int _maxNumIterations;
    TreeSet<DiscreteBin>[] bins;
    /**
     * The file containing the generated distribution matrix.
     */
    String _distributionMatrixFilename;
    private Vector<TIntObjectHashMap<WeightedHypothesisData>> _grouped;
    private boolean _hypothesisGrouped;

    public WeightedBoostClassifier() {
        _maxNumIterations = 0;
        _distributionMatrixFilename = null;
        _customizer = new WeightedBoostClassifierCustomizer();
        _hypothesisGrouped = false;
    }

    protected void mergeHypothesis(IIndex index) {
        _grouped = new Vector<TIntObjectHashMap<WeightedHypothesisData>>();
        for (short catID = 0; catID < _validCategories; catID++) {
            _grouped.add(catID, new TIntObjectHashMap<WeightedHypothesisData>());
            int numHypoythesis = Math
                    .min(
                            _hypothesis.length,
                            ((WeightedBoostClassifierCustomizer) _customizer)._numIterations);
            for (int i = 0; i < numHypoythesis; ++i) {
                IWeightedWeakHypothesis h = _hypothesis[i];
                WeightedHypothesisData hd = h.value(catID);

                WeightedHypothesisData d = (WeightedHypothesisData) _grouped.get(catID).get(
                        hd.pivot);
                if (d == null) {
                    d = new WeightedHypothesisData(hd.c1ConstantValues.length);
                    d.c0 = hd.c0;
                    d.c1ConstantValues = new double[hd.c1ConstantValues.length];
                    for (int k = 0; k < d.c1ConstantValues.length; k++) {
                        d.c1ConstantValues[k] = hd.c1ConstantValues[k];
                    }
                    d.pivot = hd.pivot;
                    _grouped.get(catID).put(d.pivot, d);
                } else {
                    d.c0 += hd.c0;
                    for (int k = 0; k < d.c1ConstantValues.length; k++) {
                        d.c1ConstantValues[k] += hd.c1ConstantValues[k];
                    }
                }

            }

        }
    }

    public ClassificationResult computeVariance(IIndex testIndex, int docID) {
        boolean groupHypothesis = ((WeightedBoostClassifierCustomizer) _customizer)._groupHypothesis;
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
                        .min(
                                _hypothesis.length,
                                ((WeightedBoostClassifierCustomizer) _customizer)._numIterations);
                for (int i = 0; i < numIterations; i++) {
                    WeightedHypothesisData hd = _hypothesis[i].value(catID);
                    int pivot = hd.pivot;
                    if (pivot >= 0) {
                        TreeSet<DiscreteBin> b = bins[pivot];
                        if (testIndex.getContentDB().hasDocumentFeature(docID,
                                pivot)) {
                            double we = testIndex.getWeightingDB().getDocumentFeatureWeight(docID, pivot);
                            int valIndex = computeBinIndex(we, b);
                            values.add(hd.c1ConstantValues[valIndex]);
                            w += hd.c1ConstantValues[valIndex];
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
                TIntObjectHashMap<WeightedHypothesisData> groups = _grouped.get(catID);
                TIntObjectIterator<WeightedHypothesisData> it = groups.iterator();
                while (it.hasNext()) {
                    it.advance();
                    WeightedHypothesisData hd = (WeightedHypothesisData) it.value();
                    int pivot = hd.pivot;
                    if (pivot >= 0) {
                        TreeSet<DiscreteBin> b = bins[pivot];
                        if (testIndex.getContentDB().hasDocumentFeature(docID,
                                pivot)) {
                            double we = testIndex.getWeightingDB().getDocumentFeatureWeight(docID, pivot);
                            int valIndex = computeBinIndex(we, b);
                            values.add(hd.c1ConstantValues[valIndex]);
                            w += hd.c1ConstantValues[valIndex];
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
        boolean groupHypothesis = ((WeightedBoostClassifierCustomizer) _customizer)._groupHypothesis;
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
                        .min(
                                _hypothesis.length,
                                ((WeightedBoostClassifierCustomizer) _customizer)._numIterations);
                for (int i = 0; i < numIterations; i++) {
                    WeightedHypothesisData hd = _hypothesis[i].value(catID);
                    int pivot = hd.pivot;
                    if (pivot >= 0) {
                        TreeSet<DiscreteBin> b = bins[pivot];
                        if (testIndex.getContentDB().hasDocumentFeature(docID,
                                pivot)) {
                            double we = testIndex.getWeightingDB().getDocumentFeatureWeight(docID, pivot);
                            int valIndex = computeBinIndex(we, b);
                            w += hd.c1ConstantValues[valIndex];
                        } else
                            w += hd.c0;
                    }
                }
                res.categoryID.add(catID);
                res.score.add(w);
            }
        } else {
            for (short catID = 0; catID < _validCategories; catID++) {
                double w = 0;
                TIntObjectHashMap<WeightedHypothesisData> values = _grouped.get(catID);
                TIntObjectIterator<WeightedHypothesisData> it = values.iterator();
                while (it.hasNext()) {
                    it.advance();
                    WeightedHypothesisData hd = (WeightedHypothesisData) it.value();
                    int pivot = hd.pivot;
                    if (pivot >= 0) {
                        TreeSet<DiscreteBin> b = bins[pivot];
                        if (testIndex.getContentDB().hasDocumentFeature(docID,
                                pivot)) {
                            double we = testIndex.getWeightingDB().getDocumentFeatureWeight(docID, pivot);
                            int valIndex = computeBinIndex(we, b);
                            w += hd.c1ConstantValues[valIndex];
                        } else
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
        IWeightedWeakHypothesis hyp = _hypothesis[0];
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
                map.put(_hypothesis[i].value(catID).pivot, _hypothesis[i]
                        .value(catID).pivot);
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


    private int computeBinIndex(double we, TreeSet<DiscreteBin> bins) {
        Iterator<DiscreteBin> it = bins.iterator();
        if (bins.size() == 0)
            throw new IllegalArgumentException("The set of bins is empty");
        if (we < bins.first().getStartValue())
            return 0;
        int idx = 0;
        while (it.hasNext()) {
            DiscreteBin db = it.next();
            if (we <= db.getEndValue())
                return idx;

            idx++;
        }

        return idx - 1;
    }
}
