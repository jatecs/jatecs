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

package it.cnr.jatecs.classification.thresholdboost;

import it.cnr.jatecs.classification.BaseClassifier;
import it.cnr.jatecs.classification.ClassificationResult;
import it.cnr.jatecs.classification.ClassifierRange;
import it.cnr.jatecs.classification.adaboost.AdaBoostClassifier;
import it.cnr.jatecs.classification.adaboost.AdaBoostClassifierCustomizer;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.util.Iterator;

public class ThresholdBoostClassifier extends BaseClassifier {

    protected AdaBoostClassifier _cl;

    protected ThresholdBoostClassifierCustomizer _cust;

    public ThresholdBoostClassifier(AdaBoostClassifier cl) {
        _cl = cl;
        _cust = new ThresholdBoostClassifierCustomizer(cl);

        _customizer = _cust;
    }

    public ClassificationResult classify(IIndex testIndex, int docID) {
        ClassificationResult res = _cl.classify(testIndex, docID);
        return res;
    }

    @Override
    public int getCategoryCount() {
        return _cust.getClassifiersCount();
    }

    @Override
    public IShortIterator getCategories() {
        return _cl.getCategories();
    }

    public ClassifierRange getClassifierRange(short catID) {
        return _cust.getClassifierRange(catID);
    }

    public void setAdaBoostClassifierCustomizer(AdaBoostClassifierCustomizer c) {
        _cl.setRuntimeCustomizer(c);
    }

    public void assignSameThresholdForAllCategories(double v) {
        Iterator<Short> it = _cust._ranges.keySet().iterator();
        while (it.hasNext()) {
            short catID = it.next();
            ClassifierRange r = _cust._ranges.get(catID);
            r.border = v;
            _cust._ranges.put(catID, r);
        }
    }

    public void assignThreshold(int cat, double v) {
        short catID = (short) cat;
        ClassifierRange r = _cust._ranges.get(catID);
        r.border = v;
        _cust._ranges.put(catID, r);
    }

}
