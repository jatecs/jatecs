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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class SvmORClassifier extends BaseClassifier {

    String _inputTrainingDir;
    String _outputTestDir;

    String _model;
    TShortObjectHashMap<String> _catsMapping;

    HashMap<Integer, ClassificationResult> _predicted = null;

    SvmORClassifier() {
        _inputTrainingDir = "Input_dir_not_setted";
        _outputTestDir = "Output_test_dir_not_setted";
        _model = "";
        _customizer = new SvmORClassifierCustomizer();
        _catsMapping = new TShortObjectHashMap<String>();
        _predicted = new HashMap<Integer, ClassificationResult>();
    }

    public ClassificationResult classify(IIndex testIndex, int docID) {
        ClassificationResult res = new ClassificationResult(testIndex
                .getCategoryDB().getCategoriesCount());
        res.documentID = docID;

        ProcessBuilder builder = new ProcessBuilder();

        // Generate test data
        String test = _outputTestDir + Os.pathSeparator() + "test" + docID
                + ".txt";
        File fil = new File(test);
        // if (!fil.exists()) {
        try {
            SvmORLearner.generateTestData(test, testIndex, docID);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // }

        File f = new File(_outputTestDir + Os.pathSeparator());
        f.mkdirs();

        String model = _model;

        // Call LibSVM OR modified classifier
        String[] comm = {
                ((SvmORClassifierCustomizer) _customizer)._libsvm_orPath
                        + Os.pathSeparator() + "svm-predict", test, model,
                "/dev/stdout"};

        builder.command(comm);
        Process classP;
        try {
            classP = builder.start();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (((SvmORClassifierCustomizer) _customizer)._printOutputSvm) {
            // any error message?
            StreamRedirect errorRedirect = new StreamRedirect(
                    classP.getErrorStream(), "libsvm_or ERR");

            // any output?
            // StreamRedirect outputRedirect = new StreamRedirect(classP
            // .getInputStream(), "");

            // kick them off
            errorRedirect.start();
            // outputRedirect.start();
        }

        // any error???
        int exitVal;
        try {
            exitVal = classP.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        try {
            byte[] score = new byte[1];
            classP.getInputStream().read(score);
            short catID = testIndex.getCategoryDB().getCategory(
                    new String(score));
            boolean found = false;
            IShortIterator catIt = testIndex.getCategoryDB().getCategories();
            while (catIt.hasNext()) {
                short category = catIt.next();
                res.categoryID.add(category);
                if (category == catID) {
                    found = true;
                    res.score.add(1.0);
                } else
                    res.score.add(-1.0);
            }

            assert (found);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (((SvmORClassifierCustomizer) _customizer)._printOutputSvm)
            JatecsLogger.status().println("libsvm_or exit value: " + exitVal);

        classP.destroy();

        fil.delete();

        return res;
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
        return 1;
    }

    @Override
    public IShortIterator getCategories() {
        TShortArrayList l = new TShortArrayList();
        l.add((short) 0);
        return new TShortArrayListIterator(l);
    }

    protected void finalize() throws Throwable {
        try {
            File f = new File(_outputTestDir);
            Os.deleteDirectory(f);

        } finally {
            super.finalize();
        }
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
