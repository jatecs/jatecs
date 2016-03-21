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
import it.cnr.jatecs.evaluation.ClassificationComparer;
import it.cnr.jatecs.evaluation.ConfusionMatrix;
import it.cnr.jatecs.evaluation.ConfusionMatrixDataManager;
import it.cnr.jatecs.evaluation.util.EvaluationReport;
import it.cnr.jatecs.indexes.DB.interfaces.ICategoryDB;
import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDBBuilder;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveClassificationDBBuilder;
import it.cnr.jatecs.module.JatecsModule;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.TextualProgressBar;
import it.cnr.jatecs.utils.iterators.TShortArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.io.FileOutputStream;
import java.io.IOException;

public class SingleLabelClassifierWithResults extends JatecsModule {

    private IClassifier _classifier;

    private String _classifierDir;

    private ClassificationMode _mode;

    private TShortArrayList _validCategories;

    public SingleLabelClassifierWithResults(IIndex testIndex,
                                            IClassifier classifier, String classifierDir) {
        this(testIndex, classifier, classifierDir, testIndex.getCategoryDB());
    }

    public SingleLabelClassifierWithResults(IIndex testIndex,
                                            IClassifier classifier, String classifierDir,
                                            ICategoryDB validCategories) {
        super(testIndex, Classifier.class.getName());
        _classifier = classifier;
        _classifierDir = classifierDir;
        _validCategories = new TShortArrayList();
        IShortIterator catIt = validCategories.getCategories();
        while (catIt.hasNext()) {
            short cat = catIt.next();
            String catName = validCategories.getCategoryName(cat);
            short indexCat = index().getCategoryDB().getCategory(catName);
            if (indexCat >= 0)
                _validCategories.add(indexCat);
        }
        _mode = ClassificationMode.PER_DOCUMENT;

    }

    public void setClassificationMode(ClassificationMode mode) {
        _mode = mode;
    }

    protected void classifyPerDocument() {
        IIntIterator docs = index().getDocumentDB().getDocuments();

        TextualProgressBar bar = new TextualProgressBar("Classify documents");
        bar.signal(0);
        int numComputed = 0;
        int numToCompute = index().getDocumentDB().getDocumentsCount();

        IClassificationDBBuilder builder = new TroveClassificationDBBuilder(
                index().getDocumentDB(), index().getCategoryDB());

        docs.begin();
        while (docs.hasNext()) {
            int docID = docs.next();
            System.out.println("DocID: " + docID);
            ClassificationResult res = _classifier.classify(index(), docID);

            for (int i = 0; i < res.categoryID.size(); i++) {
                ClassifierRange cr = _classifier
                        .getClassifierRange(res.categoryID.get(i));
                if (res.score.get(i) >= cr.border)
                    builder.setDocumentCategory(docID, res.categoryID.get(i));
            }

            numComputed++;

            // Signal the status of actual operation.
            bar.signal((numComputed * 100) / numToCompute);
        }

        bar.signal(100);

        ClassificationComparer cc = new ClassificationComparer(builder
                .getClassificationDB(), index().getClassificationDB(),
                new TShortArrayListIterator(_validCategories));

        ConfusionMatrix cm = cc.evaluateSingleLabel();

        String fname = _classifierDir;
        try {
            ConfusionMatrixDataManager.writeConfusionMatrix(fname, cm);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String res = EvaluationReport
                .printReport(cm, index().getCategoryDB());
        System.out.print(res);
        FileOutputStream os;
        try {
            os = new FileOutputStream(_classifierDir + "/_out.txt");

            os.write(res.getBytes());
            os.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void classifyPerCategory() {
        IShortIterator cats = index().getCategoryDB().getCategories();

        TextualProgressBar bar = new TextualProgressBar("Classify documents");
        bar.signal(0);
        int numComputed = 0;
        int numToCompute = index().getCategoryDB().getCategoriesCount();

        IClassificationDBBuilder builder = new TroveClassificationDBBuilder(
                index().getDocumentDB(), index().getCategoryDB());

        cats.begin();
        while (cats.hasNext()) {
            short catID = cats.next();
            ClassificationResult[] r = _classifier.classify(index(), catID);

            for (int i = 0; i < r.length; i++) {
                ClassificationResult res = r[i];

                ClassifierRange cr = _classifier
                        .getClassifierRange(res.categoryID.get(0));
                if (res.score.get(0) >= cr.border)
                    builder.setDocumentCategory(res.documentID, catID);
            }

            numComputed++;

            // Signal the status of actual operation.
            bar.signal((numComputed * 100) / numToCompute);
        }

        bar.signal(100);

        ClassificationComparer cc = new ClassificationComparer(builder
                .getClassificationDB(), index().getClassificationDB(),
                new TShortArrayListIterator(_validCategories));

        ConfusionMatrix cm = cc.evaluateSingleLabel();

        String fname = _classifierDir;
        try {
            ConfusionMatrixDataManager.writeConfusionMatrix(fname, cm);
        } catch (IOException e) {
            throw new RuntimeException(e);

        }

        String res = EvaluationReport
                .printReport(cm, index().getCategoryDB());
        System.out.print(res);
        try {
            FileOutputStream os = new FileOutputStream(_classifierDir
                    + "/_out.txt");
            os.write(res.getBytes());
            os.close();
        } catch (IOException e) {
            throw new RuntimeException(e);

        }
    }

    @Override
    protected void processModule()

    {
        long start = System.currentTimeMillis();

        if (_mode == ClassificationMode.PER_DOCUMENT)
            classifyPerDocument();
        else
            classifyPerCategory();

        long end = System.currentTimeMillis();
        JatecsLogger.status().println(
                "Time for classification: " + (end - start) + " milliseconds");
    }

}
