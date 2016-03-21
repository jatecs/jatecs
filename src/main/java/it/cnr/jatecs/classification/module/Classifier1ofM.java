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
import it.cnr.jatecs.classification.ClassificationResult;
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.evaluation.ClassificationComparer;
import it.cnr.jatecs.evaluation.ContingencyTableDataManager;
import it.cnr.jatecs.evaluation.ContingencyTableSet;
import it.cnr.jatecs.evaluation.util.EvaluationReport;
import it.cnr.jatecs.indexes.DB.interfaces.ICategoryDB;
import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDB;
import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDBBuilder;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveClassificationDBBuilder;
import it.cnr.jatecs.module.JatecsModule;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.TextualProgressBar;
import it.cnr.jatecs.utils.iterators.TShortArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.io.IOException;

public class Classifier1ofM extends JatecsModule {

    public boolean _useInternalEvaluation = true;
    private IClassifier _classifier;
    private String _classifierDir;
    private TShortArrayList _validCategories;
    private IClassificationDB _classificationDB;

    public Classifier1ofM(IIndex testIndex, IClassifier classifier,
                          String classifierDir) {
        this(testIndex, classifier, classifierDir, testIndex.getCategoryDB());
    }

    public Classifier1ofM(IIndex testIndex, IClassifier classifier,
                          String classifierDir, ICategoryDB validCategories) {
        super(testIndex, Classifier1ofM.class.getName());
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
    }

    public IClassificationDB getClassificationDB() {
        return _classificationDB;
    }

    @Override
    protected void processModule() {
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
            ClassificationResult res = _classifier.classify(index(), docID);

            double minScore = -Double.MAX_VALUE;
            short assignedCategory = _validCategories.getQuick(0);
            for (short i = 0; i < res.categoryID.size(); i++) {
                if (res.score.get(i) > minScore && _validCategories.contains(i)) {
                    assignedCategory = res.categoryID.get(i);
                    minScore = res.score.get(i);
                }
            }
            builder.setDocumentCategory(docID, assignedCategory);

            numComputed++;

            // Signal the status of actual operation.
            bar.signal((numComputed * 100) / numToCompute);
        }

        bar.signal(100);
        _classificationDB = builder.getClassificationDB();
        if (_useInternalEvaluation) {
            ClassificationComparer cc = new ClassificationComparer(builder
                    .getClassificationDB(), index().getClassificationDB(),
                    new TShortArrayListIterator(_validCategories));

            ContingencyTableSet tableSet = cc.evaluate();

            String fname = _classifierDir;
            try {
                ContingencyTableDataManager.writeContingencyTableSet(fname,
                        tableSet);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            JatecsLogger.status().print(EvaluationReport.printReport(tableSet));
        }
    }

}
