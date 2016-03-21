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

public class SvmLightLearnerCustomizer implements ILearnerRuntimeCustomizer {

    /**
     * The path to the svm_light_learn executables.
     */
    protected String _svm_light_learnExecutablePath;

    /**
     * The path to the directory where to save temporary files.
     */
    protected String _tempPath;

    /**
     * Additional parameters to be passed to the svm_light_learn executable
     */
    protected String _additionalParameters;

    /**
     * If true print the output of svm_light command.
     */
    protected boolean _printOutput;

    /**
     * The c value, from the svm_light documentation:
     * <p>
     * <pre>
     * 	           -c float    -> C: trade-off between training error
     *                         and margin (default 1)
     * </pre>
     */
    protected float _c;

    /**
     * Whether the training files in svm_light format have to be deleted or not
     * after training
     */
    protected boolean _deleteTrainingFiles;

    protected int _kernelType;

    /**
     * @param svm_lightPath The path to the directory where to find the svm_light
     *                      executables.
     */
    public SvmLightLearnerCustomizer(String svm_lightPath) {
        this(svm_lightPath, Os.getTemporaryDirectory());
    }

    /**
     * @param svm_lightPath The path to svm_light_learn executable.
     * @param tempPath      The path to the directory where to save temporary files.
     */
    public SvmLightLearnerCustomizer(String svm_light_learnExecutablePath,
                                     String tempPath) {
        super();
        _svm_light_learnExecutablePath = svm_light_learnExecutablePath;
        _tempPath = tempPath + Os.pathSeparator() + System.currentTimeMillis();
        _additionalParameters = null;
        _printOutput = false;
        _c = 1f;
        _deleteTrainingFiles = true;
        _kernelType = 0;
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

    public int getKernelType() {
        return _kernelType;
    }

    public void setKernelType(int kernelType) {
        _kernelType = kernelType;
    }

    public float getC() {
        return _c;
    }

    /**
     * Sets the value of the c parameter of svm_light
     *
     * @param c
     */
    public void setC(float c) {
        _c = c;
    }

    /**
     * returns the path to the svm_light_learn executable.
     */
    public String getSvmLightLearnPath() {
        return _svm_light_learnExecutablePath;
    }

    /**
     * Set the path to the svm_light_learn executable.
     *
     * @param path The directory path.
     */
    public void setSvmLightLearnPath(String path) {
        _svm_light_learnExecutablePath = path;
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
     * Sets additional parameters to be passed to the svm_light_learn
     * executable.
     *
     * @param additionalParameters additional parameters, e.g., "-k 1 -v 2", pass an empty string
     *                             or null to have no additional parameters.
     */
    public void setAdditionalParameters(String additionalParameters) {
        _additionalParameters = additionalParameters;
    }

    /**
     * Indicates to print or not the output of svm_light command.
     *
     * @param print True if the svm_light must be printed, false otherwise.
     */
    public void printSvmLightOutput(boolean print) {
        _printOutput = print;
    }

    public boolean isPrintingOutput() {
        return _printOutput;
    }

    public ILearnerRuntimeCustomizer cloneObject() {
        SvmLightLearnerCustomizer customizer = new SvmLightLearnerCustomizer(
                _svm_light_learnExecutablePath, _tempPath);
        customizer._printOutput = _printOutput;
        customizer._c = _c;
        customizer._additionalParameters = _additionalParameters;
        return customizer;
    }
}
