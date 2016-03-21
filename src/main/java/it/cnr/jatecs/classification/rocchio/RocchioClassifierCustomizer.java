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

package it.cnr.jatecs.classification.rocchio;

import it.cnr.jatecs.classification.ClassifierRange;
import it.cnr.jatecs.classification.interfaces.IClassifierRuntimeCustomizer;
import it.cnr.jatecs.classification.interfaces.IThresholdClassifier;

import java.util.Hashtable;
import java.util.Iterator;

public class RocchioClassifierCustomizer implements
        IClassifierRuntimeCustomizer, IThresholdClassifier {

    Hashtable<Short, ClassifierRange> _ranges;
    RocchioClassifier _classifier;

    public RocchioClassifierCustomizer(RocchioClassifier c) {
        _classifier = c;
        _ranges = new Hashtable<Short, ClassifierRange>(200);
    }

    public void setClassifierRange(short catID, ClassifierRange range) {
        assert (range != null);
        _ranges.put(catID, range);
    }

    public IClassifierRuntimeCustomizer cloneObject() {
        RocchioClassifierCustomizer c = new RocchioClassifierCustomizer(
                _classifier);

        Iterator<Short> it = _ranges.keySet().iterator();
        while (it.hasNext()) {
            short i = it.next();
            ClassifierRange cr = new ClassifierRange();
            cr.border = _ranges.get(i).border;
            cr.minimum = _ranges.get(i).minimum;
            cr.maximum = _ranges.get(i).maximum;
            c._ranges.put((short) i, cr);
        }

        return c;
    }

    public ClassifierRange getClassifierRange(short catID) {
        return _classifier.getClassifierRange(catID);
    }

    public int getClassifiersCount() {
        return _classifier.getCategoryCount();
    }

    public void reserveMemoryFor(int numProfiles) {
        _ranges = new Hashtable<Short, ClassifierRange>(numProfiles);
        for (int i = 0; i < numProfiles; i++) {
            ClassifierRange cr = new ClassifierRange();
            cr.border = 0;
            cr.minimum = 0;
            cr.maximum = 1;
            _ranges.put((short) i, cr);
        }
    }
}
