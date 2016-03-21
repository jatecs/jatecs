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

package it.cnr.jatecs.classification.knn;

import it.cnr.jatecs.classification.ClassifierRange;
import it.cnr.jatecs.classification.interfaces.IClassifierRuntimeCustomizer;

import java.util.Hashtable;
import java.util.Iterator;

public class SingleLabelKnnCommitteeClassifierCustomizer implements
        IClassifierRuntimeCustomizer {

    protected Hashtable<Short, ClassifierRange> _ranges;

    protected IKnnSearcher _searcher;

    private int _k;

    private int _numSimilar;

    public SingleLabelKnnCommitteeClassifierCustomizer() {
        _ranges = new Hashtable<Short, ClassifierRange>();
        _k = 40;
        _searcher = new TextualKnnSearcher();
        _numSimilar = 10;
    }

    public int getNumSimilar() {
        return _numSimilar;
    }

    public void setNumSimilar(int numSimilar) {
        _numSimilar = numSimilar;
    }

    public IKnnSearcher getKnnSearcher() {
        return _searcher;
    }

    public void setKnnSearcher(IKnnSearcher searcher) {
        _searcher = searcher;
    }

    public IClassifierRuntimeCustomizer cloneObject() {
        SingleLabelKnnCommitteeClassifierCustomizer cust = new SingleLabelKnnCommitteeClassifierCustomizer();

        cust._ranges = new Hashtable<Short, ClassifierRange>();
        Iterator<Short> it = _ranges.keySet().iterator();
        while (it.hasNext()) {
            short catID = it.next();
            ClassifierRange cra = _ranges.get(catID);
            ClassifierRange cr = new ClassifierRange();
            cr.border = cra.border;
            cr.maximum = cra.maximum;
            cr.minimum = cra.minimum;
            cust._ranges.put(catID, cra);
        }

        return cust;
    }

    public void setClassifierRange(short catID, double minimum, double maximum,
                                   double border) {
        ClassifierRange r = new ClassifierRange();
        r.border = border;
        r.maximum = maximum;
        r.minimum = minimum;

        _ranges.put(catID, r);
    }

    public void setClassifierRange(short catID, ClassifierRange cr) {
        _ranges.put(catID, cr);
    }

    public ClassifierRange getClassifierRange(short catID) {

        ClassifierRange cr = _ranges.get(catID);
        if (cr != null)
            return cr;
        else {
            cr = new ClassifierRange();
            cr.border = Double.MIN_VALUE;
            cr.minimum = 0;
            cr.maximum = _k;

            return cr;
        }
    }

    public SingleLabelKnnCommitteeClassifierCustomizer getCustomizerFor(
            short catID) {
        SingleLabelKnnCommitteeClassifierCustomizer cust = new SingleLabelKnnCommitteeClassifierCustomizer();
        cust._k = _k;
        ClassifierRange cr = getClassifierRange(catID);
        cust._ranges.put((short) 0, cr);

        return cust;
    }
}
