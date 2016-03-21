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

package it.cnr.jatecs.classification.svmlight;

import it.cnr.jatecs.classification.interfaces.ILearnerRuntimeCustomizer;
import it.cnr.jatecs.utils.Os;

public class SvmPerfLearnerCustomizer implements ILearnerRuntimeCustomizer {

    /**
     * The path to the svm_perf_learn executables.
     */
    protected String _svm_perf_learnExecutablePath;

    /**
     * The path to the directory where to save temporary files.
     */
    protected String _tempPath;

    /**
     * Additional parameters to be passed to the svm_perf_learn executable
     */
    protected String _additionalParameters;

    /**
     * If true print the output of svm_perf command.
     */
    protected boolean _printOutput;

    /**
     * the w value, from the svm_perf documentation:
     * <p>
     * <pre>
     * 	           -w [0,..,9] -> choice of structural learning algorithm (default 9):
     * 	                    0: n-slack algorithm described in [2]
     * 	                    1: n-slack algorithm with shrinking heuristic
     * 	                    2: 1-slack algorithm (primal) described in [5]
     * 	                    3: 1-slack algorithm (dual) described in [5]
     * 	                    4: 1-slack algorithm (dual) with constraint cache [5]
     * 	                    9: custom algorithm in svm_struct_learn_custom.c
     * </pre>
     */
    protected int _w;

    /**
     * The c value, from the svm_perf documentation:
     * <p>
     * <pre>
     * 	           -c float    -> C: trade-off between training error
     *                         and margin (default 0.01)
     * </pre>
     */
    protected float _c;

    /**
     * The l value, which identifies the function to be optimized, from the
     * svm_perf documentation:
     * <p>
     * <pre>
     * The following loss functions can be selected with the -l option:
     *      0  Zero/one loss: 1 if vector of predictions contains error, 0 otherwise.
     *      1  F1: 100 minus the F1-score in percent.
     *      2  Errorrate: Percentage of errors in prediction vector (default).
     *      3  Prec/Rec Breakeven: 100 minus PRBEP in percent.
     *      4  Prec@k: 100 minus precision at k in percent.
     *      5  Rec@k: 100 minus recall at k in percent.
     *     10  ROCArea: Percentage of swapped pos/neg pairs (i.e. 100 - ROCArea).
     * </pre>
     */
    protected int _l;

    /**
     * The value of p, used by the prec@p and rec@p functions.
     */
    protected int _p;

    /**
     * Whether the training files in svm_perf format have to be deleted or not after training
     */
    protected boolean _deleteTrainingFiles;

    /**
     * @param svm_perfPath The path to the directory where to find the svm_perf
     *                     executables.
     */
    public SvmPerfLearnerCustomizer(String svm_perfPath) {
        this(svm_perfPath, Os.getTemporaryDirectory());
    }

    /**
     * @param svm_perfPath The path to svm_perf_learn executable.
     * @param tempPath     The path to the directory where to save temporary files.
     */
    public SvmPerfLearnerCustomizer(String svm_perf_learnExecutablePath,
                                    String tempPath) {
        super();
        _svm_perf_learnExecutablePath = svm_perf_learnExecutablePath;
        _tempPath = tempPath + Os.pathSeparator() + System.currentTimeMillis();
        _additionalParameters = null;
        _printOutput = false;
        _w = 9;
        _c = 0.01f;
        _l = 2;
        _p = 0;
        _deleteTrainingFiles = true;
    }

    /**
     * Sets the deleteTrainingFile flag
     */

    public void setDeleteTrainingFiles(boolean delete) {
        _deleteTrainingFiles = delete;
    }

    public boolean isDeletingTrainingFiles() {
        return _deleteTrainingFiles;
    }

    public int getL() {
        return _l;
    }

    public void setL(int l) {
        _l = l;
    }

    public int getP() {
        return _p;
    }

    public void setP(int p) {
        _p = p;
    }

    public int getW() {
        return _w;
    }

    /**
     * Sets the value of the c parameter of svm_perf
     *
     * @param c
     */
    public void setW(int w) {
        _w = w;
    }

    public float getC() {
        return _c;
    }

    /**
     * Sets the value of the c parameter of svm_perf
     *
     * @param c
     */
    public void setC(float c) {
        _c = c;
    }

    /**
     * returns the path to the svm_perf_learn executable.
     */
    public String getSvmPerfLearnPath() {
        return _svm_perf_learnExecutablePath;
    }

    /**
     * Set the path to the svm_perf_learn executable.
     *
     * @param path The directory path.
     */
    public void setSvmPerfLearnPath(String path) {
        _svm_perf_learnExecutablePath = path;
    }

    /**
     * returns the path to the directory where to save temporary files.
     */
    public String getTempPath() {
        return _tempPath;
    }

    /**
     * Set the path to the directory where to save temporary files.
     *
     * @param tempPath The path to the temporary files directory
     */
    public void setTempPath(String tempPath) {
        _tempPath = tempPath;
    }

    public String getAdditionalParameters() {
        if (_additionalParameters == null)
            return "";
        return _additionalParameters;
    }

    /**
     * Sets additional parameters to be passed to the svm_perf_learn executable.
     *
     * @param additionalParameters additional parameters, e.g., "-k 1 -v 2", pass an empty string
     *                             or null to have no additional parameters.
     */
    public void setAdditionalParameters(String additionalParameters) {
        _additionalParameters = additionalParameters;
    }

    /**
     * Indicates to print or not the output of svm_perf command.
     *
     * @param print True if the svm_perf must be printed, false otherwise.
     */
    public void printSvmPerfOutput(boolean print) {
        _printOutput = print;
    }

    public boolean isPrintingOutput() {
        return _printOutput;
    }

    public ILearnerRuntimeCustomizer cloneObject() {
        SvmPerfLearnerCustomizer customizer = new SvmPerfLearnerCustomizer(
                _svm_perf_learnExecutablePath, _tempPath);
        customizer._printOutput = _printOutput;
        customizer._c = _c;
        customizer._l = _l;
        customizer._w = _w;
        customizer._p = _p;
        customizer._additionalParameters = _additionalParameters;
        return customizer;
    }

}
