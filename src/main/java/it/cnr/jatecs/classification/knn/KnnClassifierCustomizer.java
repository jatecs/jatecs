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
import gnu.trove.TShortIntHashMap;
import it.cnr.jatecs.classification.ClassifierRange;
import it.cnr.jatecs.classification.interfaces.IClassifierRuntimeCustomizer;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;

import java.util.Hashtable;
import java.util.Iterator;

public class KnnClassifierCustomizer implements IClassifierRuntimeCustomizer,
        IKnnClassifierCustomizer {

    protected IKnnSearcher _searcher;
    protected Hashtable<Short, ClassifierRange> _ranges;
    protected TShortDoubleHashMap _efficacy;
    protected KnnType _type;
    protected IIndex _training;
    TShortIntHashMap _kValues;

    public KnnClassifierCustomizer() {
        _searcher = new TextualKnnSearcher();
        _ranges = new Hashtable<Short, ClassifierRange>();
        _type = KnnType.GALAVOTTI;
        _efficacy = new TShortDoubleHashMap();
        _kValues = new TShortIntHashMap();
    }

    public int getK(short catID) {
        if (!_kValues.containsKey(catID))
            return 30;
        else
            return _kValues.get(catID);
    }

    public int getMaxKValue() {
        int max = -Integer.MAX_VALUE;
        short[] keys = _kValues.keys();
        for (int i = 0; i < keys.length; i++) {
            short catID = keys[i];
            int k = _kValues.get(catID);
            if (k > max) {
                max = k;
            }
        }

        if (30 > max)
            max = 30;

        return max;
    }

    public IKnnClassifierCustomizer getCustomizerForCat(short catID) {
        KnnClassifierCustomizer cust = new KnnClassifierCustomizer();
        cust._searcher = _searcher;
        cust._ranges = new Hashtable<Short, ClassifierRange>();
        ClassifierRange cr = getClassifierRange(catID);

        ClassifierRange toInsert = new ClassifierRange();
        toInsert.border = cr.border;
        toInsert.maximum = cr.maximum;
        toInsert.minimum = cr.minimum;
        cust._ranges.put((short) 0, toInsert);

        cust._type = _type;

        double efficacy = getEfficacy(catID);
        cust.setEfficacy((short) 0, efficacy);

        int k = getK(catID);
        cust.setK((short) 0, k);

        return cust;
    }

    public KnnType getKnnType() {
        return _type;
    }

    public void setKnnType(KnnType t) {
        _type = t;
    }

    public void setK(short catID, int k) {
        _kValues.put(catID, k);
    }

    public IClassifierRuntimeCustomizer cloneObject() {
        KnnClassifierCustomizer cust = new KnnClassifierCustomizer();
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

        cust._type = _type;
        short[] keys = _efficacy.keys();
        for (int i = 0; i < keys.length; i++)
            cust._efficacy.put(keys[i], _efficacy.get(keys[i]));

        keys = _kValues.keys();
        for (int i = 0; i < keys.length; i++)
            cust._kValues.put(keys[i], _kValues.get(keys[i]));

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
            if (_type == KnnType.CLASSIC) {
                int k = getK(catID);
                cr = new ClassifierRange();
                cr.border = k / 2;
                cr.minimum = 0;
                cr.maximum = k;

                return cr;
            } else {
                cr = new ClassifierRange();
                cr.border = 0;
                cr.minimum = -1;
                cr.maximum = 1;

                return cr;
            }
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

    public IKnnSearcher getSearcher() {
        return _searcher;
    }

    public void setSearcher(IKnnSearcher searcher) {
        _searcher = searcher;
    }

    public static enum KnnType {
        /**
         * A la Verdone style. It doesn't keep track of contribution of negative
         * examples.
         */
        CLASSIC,

        /**
         * Galavotti style. It keeps track of contribution of negative examples.
         */
        GALAVOTTI
    }

}
