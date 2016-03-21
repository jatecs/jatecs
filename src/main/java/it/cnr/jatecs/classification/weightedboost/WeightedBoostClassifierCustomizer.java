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

import it.cnr.jatecs.classification.interfaces.IClassifierRuntimeCustomizer;

public class WeightedBoostClassifierCustomizer implements IClassifierRuntimeCustomizer {

    protected int _numIterations;

    protected boolean _groupHypothesis;


    public WeightedBoostClassifierCustomizer() {
        this(Integer.MAX_VALUE);
    }

    public WeightedBoostClassifierCustomizer(int numIterations) {
        _numIterations = numIterations;
        _groupHypothesis = true;
    }

    public int getNumIterations() {
        return _numIterations;
    }

    public void setNumIterations(int numIterations) {
        _numIterations = numIterations;
    }

    public void groupHypothesis(boolean groupHypothesis) {
        _groupHypothesis = groupHypothesis;
    }

    public IClassifierRuntimeCustomizer cloneObject() {
        WeightedBoostClassifierCustomizer cust = new WeightedBoostClassifierCustomizer();
        cust._groupHypothesis = _groupHypothesis;
        cust._numIterations = _numIterations;

        return cust;
    }

}
