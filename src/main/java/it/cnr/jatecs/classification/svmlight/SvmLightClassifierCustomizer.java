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

import it.cnr.jatecs.classification.interfaces.IClassifierRuntimeCustomizer;
import it.cnr.jatecs.utils.Os;

public class SvmLightClassifierCustomizer implements
        IClassifierRuntimeCustomizer {

    /**
     * The path to the directory where to save temporary files.
     */
    protected String _tempPath;
    /**
     * If true print the output of svm_light command.
     */
    protected boolean _printOutputSvm;
    /**
     * The path to the svm_light_classify executable.
     */
    String _svm_light_classify_executablePath;
    private boolean _deletePredictions;

    private boolean _deleteTest;

    /**
     * @param svm_light_classify_executablePath The path to the svm_light_classify executable.
     */
    public SvmLightClassifierCustomizer(String svm_light_classify_executablePath) {
        this(svm_light_classify_executablePath, Os.getTemporaryDirectory());
    }

    /**
     * @param svm_light_classify_executablePath The path to the svm_light_classify executable.
     * @param tempPath                          The path to the directory where to save temporary files.
     */
    public SvmLightClassifierCustomizer(
            String svm_light_classify_executablePath, String tempPath) {
        super();
        _svm_light_classify_executablePath = svm_light_classify_executablePath;
        _tempPath = tempPath + Os.pathSeparator() + System.currentTimeMillis();
        _printOutputSvm = false;
        _deletePredictions = true;
        _deleteTest = true;
    }

    public void setDeletePredictionsFiles(boolean deletePredictionFiles) {
        _deletePredictions = deletePredictionFiles;
    }

    public void setDeleteTestFiles(boolean deleteTestFiles) {
        _deleteTest = deleteTestFiles;
    }

    /**
     * returns the path to the svm_light_classify executable.
     */
    public String getSvmLightExecutablePath() {
        return _svm_light_classify_executablePath;
    }

    /**
     * Set the path to the svm_light_classify executable.
     *
     * @param path The directory path.
     */
    public void setSvmLightExecutablePath(String path) {
        _svm_light_classify_executablePath = path;
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

    /**
     * Indicates to print or not the output of svm_light command.
     *
     * @param print True if the svm_light must be printed, false otherwise.
     */
    public void printSvmLightOutput(boolean print) {
        _printOutputSvm = print;
    }

    public boolean isPrintingOutput() {
        return _printOutputSvm;
    }

    public IClassifierRuntimeCustomizer cloneObject() {
        SvmLightClassifierCustomizer customizer = new SvmLightClassifierCustomizer(
                _svm_light_classify_executablePath, _tempPath);
        customizer._printOutputSvm = _printOutputSvm;
        return customizer;
    }

    public boolean isDeletingPredictionFiles() {
        return _deletePredictions;
    }

    public boolean isDeletingTestFiles() {
        return _deleteTest;
    }

}
