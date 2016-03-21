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
import it.cnr.jatecs.classification.ClassificationScoreDB;
import it.cnr.jatecs.classification.ClassifierRange;
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.indexes.DB.interfaces.ICategoryDB;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.module.JatecsModule;
import it.cnr.jatecs.utils.IOperationStatusListener;
import it.cnr.jatecs.utils.TextualProgressBar;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

public class InteractiveClassifier extends JatecsModule {

    private IClassifier _classifier;


    private ClassificationMode _mode;

    private TShortArrayList _validCategories;

    private ClassificationScoreDB _lastClassification;


    private IOperationStatusListener _listener;


    public InteractiveClassifier(IIndex testIndex, IClassifier classifier) {
        this(testIndex, classifier, testIndex.getCategoryDB());
    }

    public InteractiveClassifier(IIndex testIndex, IClassifier classifier, ICategoryDB validCategories) {
        super(testIndex, InteractiveClassifier.class.getName());
        _classifier = classifier;
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
        _lastClassification = null;
    }


    public void setOperationStatusListener(IOperationStatusListener listener) {
        _listener = listener;
    }


    public ClassificationScoreDB getLastClassification() {
        return _lastClassification;
    }


    public void setClassificationMode(ClassificationMode mode) {
        _mode = mode;
    }

    protected void classifyPerDocument() {
        IIntIterator docs = index().getDocumentDB().getDocuments();

        TextualProgressBar bar = new TextualProgressBar("Classify documents");
        bar.signal(0);
        if (_listener != null)
            _listener.operationStatus(0);

        int numComputed = 0;
        int numToCompute = index().getDocumentDB().getDocumentsCount();


        _lastClassification = new ClassificationScoreDB(index().getDocumentDB().getDocumentsCount());


        docs.begin();
        while (docs.hasNext()) {
            int docID = docs.next();
            ClassificationResult res = _classifier.classify(index(), docID);

            for (int i = 0; i < res.categoryID.size(); i++) {
                ClassifierRange cr = _classifier.getClassifierRange(res.categoryID.get(i));
                _lastClassification.insertScore(docID, res.categoryID.get(i), res.score.get(i), cr);
            }

            numComputed++;

            // Signal the status of actual operation.
            bar.signal((numComputed * 100) / numToCompute);

            if (_listener != null)
                _listener.operationStatus((numComputed * 100) / numToCompute);
        }


        bar.signal(100);
        if (_listener != null)
            _listener.operationStatus(100);

    }


    protected void classifyPerCategory() {
        IShortIterator cats = index().getCategoryDB().getCategories();

        TextualProgressBar bar = new TextualProgressBar("Classify documents");
        bar.signal(0);
        int numComputed = 0;
        int numToCompute = index().getCategoryDB().getCategoriesCount();

        _lastClassification = new ClassificationScoreDB(index().getDocumentDB().getDocumentsCount());

        cats.begin();
        while (cats.hasNext()) {
            short catID = cats.next();
            ClassificationResult[] r = _classifier.classify(index(), catID);

            for (int i = 0; i < r.length; i++) {
                ClassificationResult res = r[i];

                ClassifierRange cr = _classifier.getClassifierRange(res.categoryID.get(0));
                _lastClassification.insertScore(res.documentID, catID, res.score.get(0), cr);
            }

            numComputed++;

            // Signal the status of actual operation.
            bar.signal((numComputed * 100) / numToCompute);
        }


        bar.signal(100);
    }


    @Override
    protected void processModule() {
        if (_mode == ClassificationMode.PER_DOCUMENT)
            classifyPerDocument();
        else
            classifyPerCategory();


    }

}
