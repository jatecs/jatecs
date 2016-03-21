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

package it.cnr.jatecs.classification.svmor;

import it.cnr.jatecs.classification.interfaces.ILearnerRuntimeCustomizer;

public class SvmORLearnerCustomizer implements ILearnerRuntimeCustomizer {

    /**
     * The path (only directory) where to find the svm_light executables.
     */
    protected String _libsvm_orPath;

    protected Double _cost;

    protected Double _gamma;

    /**
     * If true print teh output of svm_light command.
     */
    protected boolean _printOutputSvm;

    public SvmORLearnerCustomizer() {
        super();
        _libsvm_orPath = "svm_bin";
        _printOutputSvm = true;
        _cost = null;
        _gamma = null;
    }

    /**
     * Set the path (only directory) where to find the svm_light executables.
     *
     * @param path The directory path.
     */
    public void setPath(String path) {
        _libsvm_orPath = path;
    }

    public Double getCost() {
        return _cost;
    }

    public void setCost(double cost) {
        _cost = cost;
    }

    public Double getGamma() {
        return _gamma;
    }

    public void setGamma(double gamma) {
        _gamma = gamma;
    }

    public ILearnerRuntimeCustomizer cloneObject() {
        SvmORLearnerCustomizer customizer = new SvmORLearnerCustomizer();
        customizer._libsvm_orPath = _libsvm_orPath;
        return customizer;
    }

    /**
     * Indicate to print or not the output of svm_light command.
     *
     * @param print True if the svm_light must be printed, false otherwise.
     */
    public void printSvmLightOutput(boolean print) {
        _printOutputSvm = print;
    }
}
