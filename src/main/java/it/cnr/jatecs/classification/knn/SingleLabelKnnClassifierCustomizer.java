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

import gnu.trove.TShortDoubleHashMap;
import it.cnr.jatecs.classification.ClassifierRange;
import it.cnr.jatecs.classification.interfaces.IClassifierRuntimeCustomizer;

import java.util.Hashtable;
import java.util.Iterator;

public class SingleLabelKnnClassifierCustomizer implements
        IClassifierRuntimeCustomizer, IKnnClassifierCustomizer {

    protected IKnnSearcher _searcher;

    protected int _k;
    protected Hashtable<Short, ClassifierRange> _ranges;

    protected TShortDoubleHashMap _efficacy;
    protected double _defaultMargin;

    public SingleLabelKnnClassifierCustomizer() {
        _searcher = new TextualKnnSearcher();
        _ranges = new Hashtable<Short, ClassifierRange>();
        _efficacy = new TShortDoubleHashMap();
        _k = 30;
        _defaultMargin = Double.MIN_VALUE;
    }

    public int getK() {
        return _k;
    }

    public void setK(int k) {
        _k = k;
    }

    public IClassifierRuntimeCustomizer cloneObject() {
        SingleLabelKnnClassifierCustomizer cust = new SingleLabelKnnClassifierCustomizer();
        cust._searcher = _searcher;
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

        short[] keys = _efficacy.keys();
        for (int i = 0; i < keys.length; i++)
            cust._efficacy.put(keys[i], _efficacy.get(keys[i]));

        cust._k = _k;

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
            cr.border = _defaultMargin;
            cr.minimum = -_k;
            cr.maximum = _k;

            return cr;
        }
    }

    public void setEfficacy(short catID, double efficacy) {
        _efficacy.put(catID, efficacy);
    }

    public double getEfficacy(short catID) {
        if (_efficacy.containsKey(catID))
            return _efficacy.get(catID);
        else
            return 1;
    }

    public IKnnClassifierCustomizer getCustomizerForCat(short catID) {
        SingleLabelKnnClassifierCustomizer cust = new SingleLabelKnnClassifierCustomizer();
        cust._searcher = _searcher;
        cust._ranges = new Hashtable<Short, ClassifierRange>();
        ClassifierRange cr = getClassifierRange(catID);

        ClassifierRange toInsert = new ClassifierRange();
        toInsert.border = cr.border;
        toInsert.maximum = cr.maximum;
        toInsert.minimum = cr.minimum;
        cust._ranges.put((short) 0, toInsert);

        double efficacy = getEfficacy(catID);
        cust.setEfficacy((short) 0, efficacy);

        return cust;
    }

    public IKnnSearcher getSearcher() {
        return _searcher;
    }

    public void setSearcher(IKnnSearcher searcher) {
        _searcher = searcher;
    }
}
