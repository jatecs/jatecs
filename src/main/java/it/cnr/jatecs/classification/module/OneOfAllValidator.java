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

package it.cnr.jatecs.classification.module;

import gnu.trove.TShortArrayList;
import it.cnr.jatecs.classification.ClassificationMode;
import it.cnr.jatecs.classification.ClassificationResult;
import it.cnr.jatecs.classification.ClassifierRange;
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.interfaces.IClassifierRuntimeCustomizer;
import it.cnr.jatecs.classification.interfaces.ILearner;
import it.cnr.jatecs.classification.validator.IDataSetGenerator;
import it.cnr.jatecs.evaluation.ContingencyTableSet;
import it.cnr.jatecs.evaluation.HierarchicalClassificationComparer;
import it.cnr.jatecs.indexes.DB.interfaces.ICategoryDB;
import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDB;
import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDBBuilder;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveClassificationDBBuilder;
import it.cnr.jatecs.module.JatecsModule;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.Os;
import it.cnr.jatecs.utils.Pair;
import it.cnr.jatecs.utils.iterators.TShortArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class OneOfAllValidator extends JatecsModule {

    protected ILearner _learner;
    protected IDataSetGenerator _dsGenerator;
    protected TShortArrayList _validCategories;
    protected String _filename;
    protected List<IClassificationDB> _systemRes;
    Vector<IClassifierRuntimeCustomizer> _cust;
    private ClassificationMode _mode;

    public OneOfAllValidator(IIndex index, ILearner learner,
                             IDataSetGenerator generator) {
        super(index, OneOfAllValidator.class.getName());
        _learner = learner;
        _mode = ClassificationMode.PER_DOCUMENT;
        _dsGenerator = generator;

        _validCategories = new TShortArrayList();
        IShortIterator catIt = index().getCategoryDB().getCategories();
        while (catIt.hasNext()) {
            short cat = catIt.next();
            String catName = index().getCategoryDB().getCategoryName(cat);
            short indexCat = index().getCategoryDB().getCategory(catName);
            if (indexCat >= 0)
                _validCategories.add(indexCat);
        }

        _cust = new Vector<IClassifierRuntimeCustomizer>();
    }

    public OneOfAllValidator(IIndex index, ILearner learner,
                             IDataSetGenerator generator, ICategoryDB validCategories) {
        super(index, OneOfAllValidator.class.getName());
        _learner = learner;
        _mode = ClassificationMode.PER_DOCUMENT;
        _dsGenerator = generator;

        _validCategories = new TShortArrayList();
        IShortIterator catIt = validCategories.getCategories();
        while (catIt.hasNext()) {
            short cat = catIt.next();
            String catName = validCategories.getCategoryName(cat);
            short indexCat = index().getCategoryDB().getCategory(catName);
            if (indexCat >= 0)
                _validCategories.add(indexCat);
        }

        _cust = new Vector<IClassifierRuntimeCustomizer>();
    }

    public void setOutputFilename(String fname) {
        _filename = fname;
    }

    public void addClassifierConfiguration(IClassifierRuntimeCustomizer c) {
        _cust.add(c);
    }

    @Override
    protected void processModule() {
        Vector<IClassificationDBBuilder> builders = new Vector<IClassificationDBBuilder>(
                _cust.size());
        for (int i = 0; i < _cust.size(); i++)
            builders.add(new TroveClassificationDBBuilder(index()
                    .getDocumentDB(), index().getCategoryDB()));

        _systemRes = new ArrayList<IClassificationDB>();

        // Initialize dataset generator.
        _dsGenerator.begin(index());

        int count = 0;
        while (_dsGenerator.hasNext()) {
            // Generate the pair training/test set.
            Pair<IIndex, IIndex> ds = _dsGenerator.next();

            long start = System.currentTimeMillis();

            // Generate the classifier.
            IClassifier classifier = _learner.build(ds.getFirst());

            long end = System.currentTimeMillis();

            System.out.println("The training time is: " + (end - start));

            for (int i = 0; i < builders.size(); i++) {

                IClassificationDBBuilder builder = builders.get(i);
                classifier.setRuntimeCustomizer(_cust.get(i));

                start = System.currentTimeMillis();

                // Test generated classifier.
                if (_mode == ClassificationMode.PER_DOCUMENT)
                    classifyPerDocument(ds.getSecond(), classifier, builder,
                            count);
                else
                    classifyPerCategory(ds.getSecond(), classifier, builder,
                            count);

                end = System.currentTimeMillis();

                System.out.println("The classification time is: "
                        + (end - start));

                JatecsLogger.status().println(
                        "Classified configuration " + (i + 1));

            }

            classifier.destroy();

            count++;
        }

        String msg = "";
        for (int i = 0; i < builders.size(); i++) {

            IClassificationDBBuilder builder = builders.get(i);
            _systemRes.add(builder.getClassificationDB());
            HierarchicalClassificationComparer cc = new HierarchicalClassificationComparer(
                    builder.getClassificationDB(), index()
                    .getClassificationDB(),
                    new TShortArrayListIterator(_validCategories));

            ContingencyTableSet tableSet = cc.evaluate();

            // Print average results.
            double microF1 = 0;
            double macroF1 = 0;
            double microAccuracy = 0;
            double macroAccuracy = 0;

            microF1 = tableSet.getGlobalContingencyTable().f1();
            macroF1 = tableSet.macroF1();
            microAccuracy = tableSet.getGlobalContingencyTable().accuracy();
            macroAccuracy = tableSet.macroAccuracy();

            JatecsLogger
                    .status()
                    .println(
                            "\nConfiguration "
                                    + (i + 1)
                                    + ". Tested "
                                    + count
                                    + " configuration(s).The results are the following:");
            JatecsLogger.status().println("MicroF1 = " + microF1);
            JatecsLogger.status().println("MacroF1 = " + macroF1);
            JatecsLogger.status().println("Micro accuracy = " + microAccuracy);
            JatecsLogger.status().println("Macro accuracy = " + macroAccuracy);

            msg += "\nConfiguration " + (i + 1) + ". Tested " + count
                    + " configuration(s).The results are the following:"
                    + Os.newline();
            msg += "MicroF1 = " + microF1 + Os.newline();
            msg += "MacroF1 = " + macroF1 + Os.newline();
            msg += "Micro accuracy = " + microAccuracy + Os.newline();
            msg += "Macro accuracy = " + macroAccuracy + Os.newline();

        }

        if (_filename != null) {
            File f = new File(_filename);
            File dir = f.getParentFile();
            if (!dir.exists())
                dir.mkdirs();
            BufferedWriter writer;
            try {
                writer = new BufferedWriter(new FileWriter(f));

                writer.write(msg);
                writer.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    protected void classifyPerDocument(IIndex test, IClassifier classifier,
                                       IClassificationDBBuilder builder, int idx) {
        IIntIterator docs = test.getDocumentDB().getDocuments();

        docs.begin();
        while (docs.hasNext()) {
            int docID = docs.next();
            ClassificationResult res = classifier.classify(test, docID);

            for (int i = 0; i < res.categoryID.size(); i++) {
                ClassifierRange cr = classifier
                        .getClassifierRange(res.categoryID.get(i));
                if (res.score.get(i) >= cr.border)
                    builder.setDocumentCategory(idx, res.categoryID.get(i));
            }
        }

    }

    protected void classifyPerCategory(IIndex test, IClassifier classifier,
                                       IClassificationDBBuilder builder, int idx) {
        IShortIterator cats = test.getCategoryDB().getCategories();

        cats.begin();
        while (cats.hasNext()) {
            short catID = cats.next();
            ClassificationResult[] r = classifier.classify(test, catID);

            for (int i = 0; i < r.length; i++) {
                ClassificationResult res = r[i];

                ClassifierRange cr = classifier
                        .getClassifierRange(res.categoryID.get(0));
                if (res.score.get(0) >= cr.border)
                    builder.setDocumentCategory(idx, catID);
            }

        }

    }

    public List<IClassificationDB> getSystemResults() {
        return _systemRes;
    }

}
