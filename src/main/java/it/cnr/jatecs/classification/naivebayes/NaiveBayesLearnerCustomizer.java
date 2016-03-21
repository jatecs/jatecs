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

package it.cnr.jatecs.classification.naivebayes;

import it.cnr.jatecs.classification.interfaces.ILearnerRuntimeCustomizer;

public class NaiveBayesLearnerCustomizer implements ILearnerRuntimeCustomizer {

    boolean _multinomial;
    double _smooth;

    public NaiveBayesLearnerCustomizer() {
        super();
        _multinomial = true;
        _smooth = 1.0;
    }

    public void useMultivariateModel() {
        _multinomial = false;
    }

    public void useMultinomialModel() {
        _multinomial = true;
    }

    public double getSmoothingFactor() {
        return _smooth;
    }

    public void setSmoothingFactor(double d) {
        _smooth = d;
    }

    public ILearnerRuntimeCustomizer cloneObject() {
        NaiveBayesLearnerCustomizer c = new NaiveBayesLearnerCustomizer();
        c._multinomial = _multinomial;
        c._smooth = _smooth;
        return c;
    }

    public void readConfiguration(String confDir) {
        throw new RuntimeException(this.getClass().getName()
                + ": method readConfiguration not implemented!");
    }

    public void writeConfiguration(String confDir) {
        throw new RuntimeException(this.getClass().getName()
                + ": method writeConfiguration not implemented!");
    }

}
