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

package it.cnr.jatecs.classification.svm;

import it.cnr.jatecs.classification.interfaces.ILearnerRuntimeCustomizer;
import libsvm.svm_parameter;

import java.util.Hashtable;

public class SvmLearnerCustomizer implements ILearnerRuntimeCustomizer {

    protected Hashtable<Short, Double> _cost;

    protected double _defaultCost;
    protected boolean _optimize;
    private svm_parameter _param;

    public SvmLearnerCustomizer() {
        _optimize = false;
        _cost = new Hashtable<Short, Double>();
        _defaultCost = 1.0;
        _param = new svm_parameter();
        _param.svm_type = svm_parameter.C_SVC;
        _param.kernel_type = svm_parameter.LINEAR;
        _param.degree = 3;
        _param.gamma = 0; // 1/k
        _param.coef0 = 0;
        _param.nu = 0.5;
        _param.cache_size = 100;
        _param.eps = 1e-3;
        _param.p = 0.1;
        _param.shrinking = 1;
        _param.probability = 0;
        _param.nr_weight = 0;
        _param.weight_label = new int[0];
        _param.weight = new double[0];
    }

    public void enableOptimization(boolean optimize) {
        _optimize = optimize;
    }

    public ILearnerRuntimeCustomizer cloneObject() {

        SvmLearnerCustomizer cust = new SvmLearnerCustomizer();
        cust._cost = new Hashtable<Short, Double>(_cost);
        cust._optimize = _optimize;

        return cust;
    }

    /**
     * Get the libsvm parameters structure used to customize the learning process.
     *
     * @return The libsvm parameters structure.
     */
    public svm_parameter getSVMParameter() {
        return _param;
    }

    public void setSVMParameter(svm_parameter param) {
        _param = param;
    }

    public void setSoftMarginCost(short catID, double cost) {
        _cost.put(catID, cost);
    }

    public void setSoftMarginDefaultCost(double cost) {
        _defaultCost = cost;
    }

    public double getSoftMarginCost(short catID) {
        if (_cost.containsKey(catID))
            return _cost.get(catID);
        else
            return _defaultCost;
    }
}
