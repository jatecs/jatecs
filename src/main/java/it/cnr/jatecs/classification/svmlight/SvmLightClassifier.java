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

import gnu.trove.TShortArrayList;
import gnu.trove.TShortObjectHashMap;
import it.cnr.jatecs.classification.BaseClassifier;
import it.cnr.jatecs.classification.ClassificationResult;
import it.cnr.jatecs.classification.ClassifierRange;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.Os;
import it.cnr.jatecs.utils.StreamRedirect;
import it.cnr.jatecs.utils.iterators.TShortArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import javax.management.RuntimeErrorException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class SvmLightClassifier extends BaseClassifier {

    TShortObjectHashMap<String> _catsMapping;

    private String _classifierDataDir;

    SvmLightClassifier(String classifierDataDir) {
        _classifierDataDir = classifierDataDir;
        _catsMapping = new TShortObjectHashMap<String>();
        _customizer = null;
    }

    public String getClassifierDataDir() {
        return _classifierDataDir;
    }

    public ClassificationResult classify(IIndex testIndex, int docID) {
        if (_customizer == null)
            throw new RuntimeErrorException(new Error(
                    "Classifier customizer not set."),
                    "Classifier customizer not set.");
        ClassificationResult res = new ClassificationResult(testIndex
                .getCategoryDB().getCategoriesCount());
        res.documentID = docID;

        ProcessBuilder builder = new ProcessBuilder();

        SvmLightClassifierCustomizer customizer = (SvmLightClassifierCustomizer) _customizer;

        short[] cats = _catsMapping.keys();
        TShortArrayList l = new TShortArrayList(cats);
        l.sort();
        TShortArrayListIterator it = new TShortArrayListIterator(l);
        while (it.hasNext()) {
            short catID = it.next();

            String path = (String) _catsMapping.get(catID);

            File f = new File(customizer.getTempPath() + Os.pathSeparator()
                    + path);
            f.mkdirs();

            // Generate test data
            String test = customizer.getTempPath() + Os.pathSeparator() + path
                    + Os.pathSeparator() + "test_" + docID + ".txt";
            try {
                SvmLightLearner.generateClassificationData(test, testIndex,
                        docID, catID);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            String model = _classifierDataDir + Os.pathSeparator() + path
                    + Os.pathSeparator() + "model.txt";
            String prediction = customizer.getTempPath() + Os.pathSeparator()
                    + path + Os.pathSeparator() + "prediction_" + docID
                    + ".txt";

            // Call SVMLight classifier
            String[] command = new String[]{
                    customizer.getSvmLightExecutablePath(), test, model,
                    prediction};

            builder.command(command);
            Process classP;
            try {
                classP = builder.start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            if (customizer.isPrintingOutput()) {
                // any error message?
                StreamRedirect errorRedirect = new StreamRedirect(
                        classP.getErrorStream(), "svm_light ERR");

                // any output?
                StreamRedirect outputRedirect = new StreamRedirect(
                        classP.getInputStream(), "svm_light OUT");

                // kick them off
                errorRedirect.start();
                outputRedirect.start();
            }

            // any error???
            int exitVal;
            try {
                exitVal = classP.waitFor();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            if (customizer.isPrintingOutput())
                JatecsLogger.status().println(
                        "svm_light exit value: " + exitVal);

            // Decode results obtained from svm_light.
            try {
                decodeResults(prediction, testIndex, catID, res);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            classP.destroy();

            // Delete prediction file.
            if (customizer.isDeletingPredictionFiles()) {
                File predFile = new File(prediction);
                predFile.delete();
            }

            // Delete test file.
            if (customizer.isDeletingTestFiles()) {
                File testFile = new File(test);
                testFile.delete();
            }
        }

        return res;
    }

    protected void decodeResults(String inputFile, IIndex index, short catID,
                                 ClassificationResult res) throws Exception {
        BufferedReader in = new BufferedReader(new FileReader(inputFile), 4096);

        String line = in.readLine();
        assert (line != null);
        assert (!line.equals(""));

        double score = Double.parseDouble(line);
        res.categoryID.add(catID);
        res.score.add(score);

        in.close();
    }

    public ClassifierRange getClassifierRange(short catID) {
        ClassifierRange r = new ClassifierRange();
        r.border = 0;
        r.minimum = Double.MIN_VALUE;
        r.maximum = Double.MAX_VALUE;

        return r;
    }

    @Override
    public int getCategoryCount() {
        return this._catsMapping.size();
    }

    @Override
    public IShortIterator getCategories() {
        TShortArrayList l = new TShortArrayList();
        for (short i = 0; i < this._catsMapping.size(); i++)
            l.add(i);

        return new TShortArrayListIterator(l);
    }

    public ClassificationResult[] classify(IIndex testIndex, short catID) {
        if (_customizer == null)
            throw new RuntimeErrorException(new Error(
                    "Classifier customizer not set."),
                    "Classifier customizer not set.");

        ClassificationResult[] r = new ClassificationResult[testIndex
                .getDocumentDB().getDocumentsCount()];

        SvmLightClassifierCustomizer customizer = (SvmLightClassifierCustomizer) _customizer;

        String path = (String) _catsMapping.get(catID);

        File f = new File(customizer.getTempPath() + Os.pathSeparator() + path);
        f.mkdirs();

        // generate test data.
        String test = customizer.getTempPath() + Os.pathSeparator() + path
                + Os.pathSeparator() + "test.txt";
        try {
            SvmLightLearner.generateClassificationData(test, testIndex, catID);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        String model = _classifierDataDir + Os.pathSeparator() + path
                + Os.pathSeparator() + "model.txt";
        String prediction = customizer.getTempPath() + Os.pathSeparator()
                + path + Os.pathSeparator() + "prediction.txt";

        // Call SVMLight classifier
        String[] command = new String[]{
                customizer.getSvmLightExecutablePath(), test, model, prediction};

        ProcessBuilder builder = new ProcessBuilder();
        builder.command(command);
        Process classP;
        try {
            classP = builder.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (customizer.isPrintingOutput()) {
            // any error message?
            StreamRedirect errorRedirect = new StreamRedirect(
                    classP.getErrorStream(), "svm_light ERR");

            // any output?
            StreamRedirect outputRedirect = new StreamRedirect(
                    classP.getInputStream(), "svm_light OUT");

            // kick them off
            errorRedirect.start();
            outputRedirect.start();
        }

        // any error???
        int exitVal;
        try {
            exitVal = classP.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (customizer.isPrintingOutput())
            JatecsLogger.status().println("svm_light exit value: " + exitVal);

        // Decode results obtained from svm_light.
        try {
            decodeResults(prediction, testIndex, catID, r);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        classP.destroy();

        // Delete prediction file.
        if (customizer.isDeletingPredictionFiles()) {
            File predFile = new File(prediction);
            predFile.delete();
        }

        // Delete test file.
        if (customizer.isDeletingTestFiles()) {
            File testFile = new File(test);
            testFile.delete();
        }

        return r;
    }

    protected void decodeResults(String inputFile, IIndex index, short catID,
                                 ClassificationResult[] res) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(inputFile), 4096);

        String line = in.readLine();
        assert (line != null);
        assert (!line.equals(""));
        int doc = 0;
        while (line != null) {
            if (line.equals("")) {
                line = in.readLine();
                continue;
            }

            ClassificationResult r = new ClassificationResult();

            double score = Double.parseDouble(line);
            r.categoryID.add(catID);
            r.score.add(score);
            r.documentID = doc;
            res[doc] = r;

            doc++;
            line = in.readLine();
        }

        in.close();
    }
}
