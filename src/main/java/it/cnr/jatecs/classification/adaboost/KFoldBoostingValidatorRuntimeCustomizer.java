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

package it.cnr.jatecs.classification.adaboost;

import it.cnr.jatecs.classification.interfaces.ILearnerRuntimeCustomizer;

public class KFoldBoostingValidatorRuntimeCustomizer implements
        ILearnerRuntimeCustomizer {

    protected int _k;

    protected int _startIteration, _stopIteration;

    protected int _step;

    public KFoldBoostingValidatorRuntimeCustomizer() {
        _k = 5;
        _startIteration = 50;
        _stopIteration = 400;
        _step = 10;
    }

    public ILearnerRuntimeCustomizer cloneObject() {
        KFoldBoostingValidatorRuntimeCustomizer c = new KFoldBoostingValidatorRuntimeCustomizer();
        c._k = _k;
        c._startIteration = _startIteration;
        c._step = _step;
        c._stopIteration = _stopIteration;

        return c;
    }

    public void setK(int k) {
        _k = k;
    }

    public void setBehaviour(int startIteration, int stopIteration, int step) {
        _startIteration = startIteration;
        _stopIteration = stopIteration;
        _step = step;
    }
}
