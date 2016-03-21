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

public class SvmRegressionLearnerCustomizer implements
        ILearnerRuntimeCustomizer {

    private svm_parameter _param;

    public SvmRegressionLearnerCustomizer() {
        _param = new svm_parameter();
        _param.svm_type = svm_parameter.NU_SVR;
        _param.kernel_type = svm_parameter.LINEAR;
        _param.degree = 3;
        _param.gamma = 0; // 1/k
        _param.coef0 = 0;
        _param.C = 1.0;
        _param.nu = 0.5;
        _param.cache_size = 100;
        _param.eps = 0.1;
        _param.p = 0.1;
        _param.shrinking = 1;
        _param.probability = 0;
        _param.nr_weight = 0;
        _param.weight_label = new int[0];
        _param.weight = new double[0];
    }

    public ILearnerRuntimeCustomizer cloneObject() {

        SvmRegressionLearnerCustomizer cust = new SvmRegressionLearnerCustomizer();

        cust._param = (svm_parameter) _param.clone();

        return cust;
    }

    public double getGamma() {
        return _param.gamma;
    }

    public void setGamma(double gamma) {
        _param.gamma = gamma;
    }

    public double getC() {
        return _param.C;
    }

    public void setC(double C) {
        _param.C = C;
    }

    public svm_parameter getSVMParameter() {
        return _param;
    }

    public void setSVMParameter(svm_parameter param) {
        _param = param;
    }

    public void setKernelType(int type) {
        _param.kernel_type = type;
    }
}
