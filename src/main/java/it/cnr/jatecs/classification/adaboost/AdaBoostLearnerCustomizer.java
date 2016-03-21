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
import it.cnr.jatecs.utils.IOperationStatusListener;

public class AdaBoostLearnerCustomizer implements ILearnerRuntimeCustomizer {

    /**
     * The type of initial distribution to load in the distribution matrix.
     */
    protected InitialDistributionMatrixType _initDistType;

    /**
     * The name of the file containing the initial distribution matrix to load before start the
     * boosting algorithm.
     */
    protected String _matrixFileLoad;

    /**
     * The name of the file used to save the distribution matrix obtained after have applied
     * the boosting algorithms.
     */
    protected boolean _keepMatrix;

    /**
     * The balance factor for positive example to be used
     * when building a CategoryOrientedDistributionMatrix
     */
    protected float _factorPositive;

    /**
     * Indicate if the normalization must be done by considering a single category or all categories.
     */
    protected boolean _perCategoryNormalization;


    /**
     * The number of iterations to compute in boosting algorithm.
     */
    protected int _maxNumIterations;


    /**
     * The weak learner to use.
     */
    protected IWeakLearner _wl;

    /**
     * The correction factor to be used for True Positive examples
     * when updating the distribution matrix
     */
    protected float _TPcorrection;

    /**
     * The correction factor to be used for True Negative examples
     * when updating the distribution matrix
     */
    protected float _TNcorrection;

    /**
     * The correction factor to be used for False Positive examples
     * when updating the distribution matrix
     */
    protected float _FPcorrection;

    /**
     * The correction factor to be used for False Negative examples
     * when updating the distribution matrix
     */
    protected float _FNcorrection;

    /**
     * The loss function to be used when updating the distribution matrix
     */
    protected ILossFunction _lossFunction;


    /**
     * The status listener.
     */
    protected IOperationStatusListener _status;


    public AdaBoostLearnerCustomizer() {
        super();
        _factorPositive = 1;
        _perCategoryNormalization = false;
        _maxNumIterations = 200;
        _wl = new MHWeakLearner();
        _initDistType = InitialDistributionMatrixType.UNIFORM;
        _keepMatrix = true;
        _lossFunction = new ExponentialLoss();
        _TPcorrection = 1;
        _FPcorrection = 1;
        _TNcorrection = 1;
        _FNcorrection = 1;
        _status = null;
    }

    public IWeakLearner getWeakLearner() {
        return _wl;
    }

    public void setWeakLearner(IWeakLearner l) {
        if (l != null)
            _wl = l;
    }

    public void setLossFunction(ILossFunction lossFunction) {
        if (lossFunction != null)
            _lossFunction = lossFunction;
    }

    public int getNumIterations() {
        return _maxNumIterations;
    }

    /**
     * Set the number of iterations to compute in boosting algorithm.
     *
     * @param maxNumIterations The number of iterations to compute in boosting algorithm.
     *                         model.
     */
    public void setNumIterations(int numIterations) {
        _maxNumIterations = numIterations;
    }

    /**
     * Set the initial distribution to use for initialization of the matrix.
     *
     * @param t The initial distribution to use.
     */
    public void setInitialDistributionType(InitialDistributionMatrixType t) {
        _initDistType = t;
    }

    /**
     * Set the flag to perform per category normalization
     *
     * @param enable true to enable per category normalization
     */
    public void setPerCategoryNormalization(boolean enable) {
        _perCategoryNormalization = enable;
    }

    public ILearnerRuntimeCustomizer cloneObject() {
        AdaBoostLearnerCustomizer cust = new AdaBoostLearnerCustomizer();
        cust._factorPositive = _factorPositive;
        cust._FNcorrection = _FNcorrection;
        cust._FPcorrection = _FPcorrection;
        cust._initDistType = _initDistType;
        cust._lossFunction = _lossFunction;
        cust._matrixFileLoad = _matrixFileLoad;
        cust._maxNumIterations = _maxNumIterations;
        cust._perCategoryNormalization = _perCategoryNormalization;
        cust._keepMatrix = _keepMatrix;
        cust._TNcorrection = _TNcorrection;
        cust._TPcorrection = _TPcorrection;
        cust._wl = _wl;
        return cust;
    }

    public void keepDistributionMatrix(boolean keepMatrix) {
        _keepMatrix = keepMatrix;
    }

    public IOperationStatusListener getStatusListener() {
        return _status;
    }

    public void setStatusListener(IOperationStatusListener status) {
        _status = status;
    }

}
