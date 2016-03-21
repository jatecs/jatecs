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

import gnu.trove.TShortDoubleHashMap;
import it.cnr.jatecs.classification.ClassificationMode;
import it.cnr.jatecs.classification.ClassificationResult;
import it.cnr.jatecs.classification.ClassificationScoreDB;
import it.cnr.jatecs.classification.ClassifierRange;
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDB;
import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDBBuilder;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveClassificationDBBuilder;
import it.cnr.jatecs.module.JatecsModule;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.TextualProgressBar;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

public class Classifier extends JatecsModule {

    private IClassifier _classifier;

    private ClassificationMode _mode;

    private IClassificationDB _classificationDB;

    private boolean _saveConfidences;
    private ClassificationScoreDB _classification;

    private TShortDoubleHashMap _thresholds;

    public Classifier(IIndex testIndex, IClassifier classifier) {
        this(testIndex, classifier, false);
    }

    public Classifier(IIndex testIndex, IClassifier classifier,
                      boolean saveConfidences) {
        super(testIndex, Classifier.class.getName());
        _classifier = classifier;
        _mode = ClassificationMode.PER_DOCUMENT;
        _classificationDB = null;
        _classification = null;
        _saveConfidences = saveConfidences;
        _thresholds = null;
    }

    public void setClassificationMode(ClassificationMode mode) {
        _mode = mode;
    }

    public IClassificationDB getClassificationDB() {
        return _classificationDB;
    }

    public ClassificationScoreDB getConfidences() {
        return _classification;
    }

    protected void classifyPerDocument() {
        IIntIterator docs = index().getDocumentDB().getDocuments();

        TextualProgressBar bar = new TextualProgressBar("Classify documents");
        bar.signal(0);
        int numComputed = 0;
        int numToCompute = index().getDocumentDB().getDocumentsCount();

        IClassificationDBBuilder builder = new TroveClassificationDBBuilder(
                index().getDocumentDB(), index().getCategoryDB());

        if (_saveConfidences)
            _classification = new ClassificationScoreDB(index().getDocumentDB()
                    .getDocumentsCount());

        docs.begin();
        while (docs.hasNext()) {
            int docID = docs.next();
            ClassificationResult res = _classifier.classify(index(), docID);

            for (int i = 0; i < res.categoryID.size(); i++) {
                short catID = res.categoryID.get(i);
                ClassifierRange cr = _classifier.getClassifierRange(catID);

                double threshold = cr.border;
                if (_thresholds != null && _thresholds.containsKey(catID)) {
                    threshold = _thresholds.get(catID);
                }

                if (res.score.get(i) >= threshold) {
                    builder.setDocumentCategory(docID, catID);
                }
                if (_saveConfidences) {
                    _classification.insertScore(docID,
                            res.categoryID.get(i), res.score.get(i), cr);
                }
            }

            numComputed++;

            // Signal the status of actual operation.
            bar.signal((numComputed * 100) / numToCompute);
        }

        bar.signal(100);

        _classificationDB = builder.getClassificationDB();
    }

    protected void classifyPerCategory() {
        IShortIterator cats = index().getCategoryDB().getCategories();

        TextualProgressBar bar = new TextualProgressBar("Classify documents");
        bar.signal(0);
        int numComputed = 0;
        int numToCompute = index().getCategoryDB().getCategoriesCount();

        IClassificationDBBuilder builder = new TroveClassificationDBBuilder(
                index().getDocumentDB(), index().getCategoryDB());

        if (_saveConfidences)
            _classification = new ClassificationScoreDB(index().getDocumentDB()
                    .getDocumentsCount());

        cats.begin();
        while (cats.hasNext()) {
            short catID = cats.next();
            ClassificationResult[] r = _classifier.classify(index(), catID);

            ClassifierRange cr = _classifier.getClassifierRange(catID);

            double threshold = cr.border;
            if (_thresholds != null && _thresholds.containsKey(catID)) {
                threshold = _thresholds.get(catID);
            }

            for (int i = 0; i < r.length; i++) {
                ClassificationResult res = r[i];

                if (res.score.get(0) >= threshold) {
                    builder.setDocumentCategory(res.documentID, catID);
                }
                if (_saveConfidences) {
                    _classification.insertScore(res.documentID, catID,
                            res.score.get(0), cr);
                }
            }

            numComputed++;

            // Signal the status of actual operation.
            bar.signal((numComputed * 100) / numToCompute);
        }

        bar.signal(100);

        _classificationDB = builder.getClassificationDB();
    }

    @Override
    protected void processModule() {
        long start = System.currentTimeMillis();

        if (_mode == ClassificationMode.PER_DOCUMENT) {
            JatecsLogger.status().info("Classifying a document at a time.");
            classifyPerDocument();
        } else {
            JatecsLogger.status().info("Classifying a category at a time.");
            classifyPerCategory();
        }
        long end = System.currentTimeMillis();
        JatecsLogger.status().println(
                "Time for classification: " + (end - start) + " milliseconds");
    }

    public void setCustomThresholds(TShortDoubleHashMap thresholds) {
        this._thresholds = thresholds;
    }

}
