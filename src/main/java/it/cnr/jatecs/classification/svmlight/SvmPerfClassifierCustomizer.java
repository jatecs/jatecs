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

public class SvmPerfClassifierCustomizer implements
        IClassifierRuntimeCustomizer {

    /**
     * The path to the directory where to save temporary files.
     */
    protected String _tempPath;
    /**
     * If true print the output of svm_perf command.
     */
    protected boolean _printOutputSvm;
    /**
     * The path to the svm_perf_classify executable.
     */
    String _svm_perf_classify_executablePath;
    private boolean _deletePredictions;

    private boolean _deleteTest;

    /**
     * @param svm_perf_classify_executablePath The path to the svm_perf_classify executable.
     */
    public SvmPerfClassifierCustomizer(String svm_perf_classify_executablePath) {
        this(svm_perf_classify_executablePath, Os.getTemporaryDirectory());
    }

    /**
     * @param svm_perf_classify_executablePath The path to the svm_perf_classify executable.
     * @param tempPath                         The path to the directory where to save temporary files.
     */
    public SvmPerfClassifierCustomizer(String svm_perf_classify_executablePath,
                                       String tempPath) {
        super();
        _svm_perf_classify_executablePath = svm_perf_classify_executablePath;
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
     * returns the path to the svm_perf_classify executable.
     */
    public String getSvmPerfExecutablePath() {
        return _svm_perf_classify_executablePath;
    }

    /**
     * Set the path to the svm_perf_classify executable.
     *
     * @param path The directory path.
     */
    public void setSvmPerfExecutablePath(String path) {
        _svm_perf_classify_executablePath = path;
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
     * Indicates to print or not the output of svm_perf command.
     *
     * @param print True if the svm_perf must be printed, false otherwise.
     */
    public void printSvmPerfOutput(boolean print) {
        _printOutputSvm = print;
    }

    public boolean isPrintingOutput() {
        return _printOutputSvm;
    }

    public IClassifierRuntimeCustomizer cloneObject() {
        SvmPerfClassifierCustomizer customizer = new SvmPerfClassifierCustomizer(
                _svm_perf_classify_executablePath, _tempPath);
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
