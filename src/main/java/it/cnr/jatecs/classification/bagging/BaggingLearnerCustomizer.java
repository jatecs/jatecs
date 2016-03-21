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

import it.cnr.jatecs.classification.interfaces.ILearnerRuntimeCustomizer;

public class BaggingLearnerCustomizer implements ILearnerRuntimeCustomizer {

    protected ILearnerRuntimeCustomizer _internalCustomizer;
    int _bagCount;

    public BaggingLearnerCustomizer(int bagCount,
                                    ILearnerRuntimeCustomizer internalCustomizer) {
        _bagCount = bagCount;
        _internalCustomizer = internalCustomizer;
    }

    public BaggingLearnerCustomizer() {
        _bagCount = 10;
        _internalCustomizer = null;
    }

    public ILearnerRuntimeCustomizer getInternalCustomizer() {
        return _internalCustomizer;
    }

    public void setInternalCustomizer(ILearnerRuntimeCustomizer customizer) {
        _internalCustomizer = customizer;
    }

    public ILearnerRuntimeCustomizer cloneObject() {
        BaggingLearnerCustomizer newc = new BaggingLearnerCustomizer();
        ILearnerRuntimeCustomizer cInternal = this._internalCustomizer
                .cloneObject();
        newc._internalCustomizer = cInternal;

        return newc;
    }

    public int getBagCount() {
        return _bagCount;
    }

    public void setBagCount(int bagCount) {
        _bagCount = bagCount;
    }

}
