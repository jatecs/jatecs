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

public class KnnCommitteeClassifierCustomizer implements
        IClassifierRuntimeCustomizer {

    protected Hashtable<Short, ClassifierRange> _ranges;


    protected IKnnCommitteeScorer _scorer;

    private int _k;


    public KnnCommitteeClassifierCustomizer() {
        _ranges = new Hashtable<Short, ClassifierRange>();
        _scorer = new EachScoreKnnCommitteeScorer();
        _k = 40;
    }


    public IClassifierRuntimeCustomizer cloneObject() {
        KnnCommitteeClassifierCustomizer cust = new KnnCommitteeClassifierCustomizer();

        cust._scorer = _scorer;
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


    public void setClassifierRange(short catID, double minimum, double maximum, double border) {
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
            cr.border = _k / 2;
            cr.minimum = -Double.MAX_VALUE;
            cr.maximum = Double.MAX_VALUE;

            return cr;
        }
    }


    public IKnnCommitteeScorer getCommitteeScorer() {
        return _scorer;
    }


    public void setCommitteeScorer(IKnnCommitteeScorer scorer) {
        _scorer = scorer;
    }


    public KnnCommitteeClassifierCustomizer getCustomizerForCat(short catID) throws Exception {
        KnnCommitteeClassifierCustomizer cust = new KnnCommitteeClassifierCustomizer();
        cust._scorer = _scorer;
        cust._k = _k;
        ClassifierRange cr = getClassifierRange(catID);
        cust._ranges.put((short) 0, cr);

        return cust;
    }
}
