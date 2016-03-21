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

import it.cnr.jatecs.classification.interfaces.ILearnerRuntimeCustomizer;

class RocchioLearnerCustomizer implements ILearnerRuntimeCustomizer {
    /**
     * The "beta" parameter in Rocchio formula.
     */
    double _beta;


    /**
     * The gamma parameter in Rocchio formula.
     */
    double _gamma;


    int _numberNearPositives;


    IDistanceFunction _func;


    public RocchioLearnerCustomizer() {
        _beta = 4.0;
        _gamma = 1.0;
        _numberNearPositives = 20;
        _func = new DotProduct();
    }


    public void setBeta(double beta) {
        _beta = beta;
    }


    public void setGamma(double gamma) {
        _gamma = gamma;
    }


    public void setNumberNearPositives(int num) {
        _numberNearPositives = num;
    }


    public ILearnerRuntimeCustomizer cloneObject() {
        RocchioLearnerCustomizer cust = new RocchioLearnerCustomizer();

        cust._beta = _beta;
        cust._func = _func;
        cust._gamma = _gamma;
        cust._numberNearPositives = _numberNearPositives;

        return cust;
    }


    public void readConfiguration(String confDir) {
        throw new RuntimeException(this.getClass().getName() + ": method readConfiguration not implemented!");
    }

    public void writeConfiguration(String confDir) {
        throw new RuntimeException(this.getClass().getName() + ": method writeConfiguration not implemented!");
    }
}
