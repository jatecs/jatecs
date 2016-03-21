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

package it.cnr.jatecs.classification.bagging;

import gnu.trove.TDoubleArrayList;
import it.cnr.jatecs.classification.BaseClassifier;
import it.cnr.jatecs.classification.ClassificationResult;
import it.cnr.jatecs.classification.ClassifierRange;
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.interfaces.IClassifierRuntimeCustomizer;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

public class BaggingClassifier extends BaseClassifier {

    protected IClassifier[] _classifiers;

    public BaggingClassifier(int bagCount) {
        _classifiers = new IClassifier[bagCount];
    }

    public IClassifier[] internalClassifiers() {
        return _classifiers;
    }

    @Override
    public BaggingClassifierCustomizer getRuntimeCustomizer() {
        return (BaggingClassifierCustomizer) _customizer;
    }

    public void setRuntimeCustomizer(IClassifierRuntimeCustomizer customizer) {
        super.setRuntimeCustomizer(customizer);

        if (getRuntimeCustomizer() != null) {
            for (int i = 0; i < _classifiers.length; ++i)
                _classifiers[i].setRuntimeCustomizer(getRuntimeCustomizer()
                        .getInternalCustomizer());
        }
    }

    public ClassificationResult classify(IIndex testIndex, int docID) {
        ClassificationResult bagres = new ClassificationResult();
        bagres.documentID = docID;

        for (int i = 0; i < _classifiers.length; ++i) {
            ClassificationResult res = _classifiers[i].classify(testIndex,
                    docID);
            if (bagres.categoryID.size() == 0) {
                for (int j = 0; j < res.categoryID.size(); ++j) {
                    bagres.categoryID.add(res.categoryID.getQuick(j));
                    bagres.score.add(0);
                }
            }
            for (int j = 0; j < res.score.size(); ++j) {
                bagres.score.setQuick(j,
                        bagres.score.getQuick(j) + res.score.getQuick(j));
            }
        }

        for (int j = 0; j < bagres.score.size(); ++j) {
            bagres.score.setQuick(j, bagres.score.getQuick(j)
                    / _classifiers.length);
        }
        return bagres;
    }

    public ClassifierRange getClassifierRange(short catID) {
        if (_classifiers != null && _classifiers.length > 0)
            return _classifiers[0].getClassifierRange(catID);

        return new ClassifierRange();
    }

    @Override
    public int getCategoryCount() {
        return _classifiers.length;
    }

    @Override
    public IShortIterator getCategories() {
        return _classifiers[0].getCategories();
    }

    @Override
    public void destroy() {
        for (int i = 0; i < _classifiers.length; ++i)
            _classifiers[i].destroy();
    }

    public ClassificationResult computeVariance(IIndex index, int doc) {
        ClassificationResult bagres = new ClassificationResult();
        bagres.documentID = doc;

        double[] avg = null;
        TDoubleArrayList[] values = null;
        for (int i = 0; i < _classifiers.length; ++i) {
            ClassificationResult res = _classifiers[i].classify(index, doc);
            if (bagres.categoryID.size() == 0) {
                avg = new double[res.categoryID.size()];
                values = new TDoubleArrayList[res.categoryID.size()];
                for (int j = 0; j < res.categoryID.size(); ++j) {
                    bagres.categoryID.add(res.categoryID.getQuick(j));
                    bagres.score.add(0);
                    avg[j] = 0;
                    values[j] = new TDoubleArrayList();
                }
            }
            for (int j = 0; j < res.score.size(); ++j) {
                double value = res.score.getQuick(j);
                values[j].add(value);
                avg[j] += value;
            }
        }

        for (int j = 0; j < bagres.score.size(); ++j) {
            avg[j] /= _classifiers.length;
        }

        for (int j = 0; j < bagres.score.size(); ++j) {
            for (int i = 0; i < values[j].size(); ++i) {
                bagres.score.setQuick(j, bagres.score.getQuick(j) + Math.pow(avg[j] - values[j].getQuick(i), 2.0));
            }
        }
        return bagres;
    }
}
