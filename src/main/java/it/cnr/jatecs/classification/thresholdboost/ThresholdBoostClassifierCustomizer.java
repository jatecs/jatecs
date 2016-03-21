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

import it.cnr.jatecs.classification.ClassifierRange;
import it.cnr.jatecs.classification.adaboost.AdaBoostClassifier;
import it.cnr.jatecs.classification.interfaces.IClassifierRuntimeCustomizer;
import it.cnr.jatecs.classification.interfaces.IThresholdClassifier;

import java.util.Hashtable;
import java.util.Iterator;

public class ThresholdBoostClassifierCustomizer implements
        IClassifierRuntimeCustomizer, IThresholdClassifier {

    protected Hashtable<Short, ClassifierRange> _ranges;


    public ThresholdBoostClassifierCustomizer(AdaBoostClassifier cl) {
        _ranges = new Hashtable<Short, ClassifierRange>();
        for (int i = 0; i < cl.getCategoryCount(); i++) {
            ClassifierRange r = cl.getClassifierRange((short) i);
            ClassifierRange ra = new ClassifierRange();
            ra.border = r.border;
            ra.maximum = r.maximum;
            ra.minimum = r.minimum;
            _ranges.put((short) i, ra);
        }
    }


    ThresholdBoostClassifierCustomizer() {
        _ranges = new Hashtable<Short, ClassifierRange>();
    }


    public IClassifierRuntimeCustomizer cloneObject() {
        ThresholdBoostClassifierCustomizer c = new ThresholdBoostClassifierCustomizer();
        Iterator<Short> it = _ranges.keySet().iterator();
        while (it.hasNext()) {
            short catID = it.next();
            ClassifierRange r = new ClassifierRange();
            r.border = _ranges.get(catID).border;
            r.maximum = _ranges.get(catID).maximum;
            r.minimum = _ranges.get(catID).minimum;

            c._ranges.put(catID, r);
        }

        return c;
    }


    public ClassifierRange getClassifierRange(short catID) {
        return _ranges.get(catID);
    }

    public void reserveMemoryFor(int numProfiles) {
        _ranges = new Hashtable<Short, ClassifierRange>(numProfiles);
        for (int i = 0; i < numProfiles; i++) {
            ClassifierRange cr = new ClassifierRange();
            cr.border = 0;
            cr.minimum = Double.MAX_VALUE;
            cr.maximum = -Double.MAX_VALUE;
            _ranges.put((short) i, cr);
        }
    }

    public void setClassifierRange(short catID, ClassifierRange range) {
        _ranges.put(catID, range);
    }


    public int getClassifiersCount() {
        return _ranges.size();
    }


}
