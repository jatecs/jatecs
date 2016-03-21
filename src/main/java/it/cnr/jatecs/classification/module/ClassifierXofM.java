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
import it.cnr.jatecs.classification.ClassifierRange;
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.indexes.DB.interfaces.ICategoryDB;
import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDB;
import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDBBuilder;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveClassificationDBBuilder;
import it.cnr.jatecs.module.JatecsModule;
import it.cnr.jatecs.utils.TextualProgressBar;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

public class ClassifierXofM extends JatecsModule {

    private IClassifier _classifier;

    private TShortArrayList _validCategories;

    private IClassificationDB _classificationDB;
    private int _x;
    private BoundCondition _boundCondition;

    public ClassifierXofM(IIndex testIndex, IClassifier classifier) {
        this(testIndex, classifier, testIndex.getCategoryDB());
    }

    public ClassifierXofM(IIndex testIndex, IClassifier classifier, ICategoryDB validCategories) {
        super(testIndex, ClassifierXofM.class.getName());
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
        _x = 1;
        _boundCondition = BoundCondition.ExactlyX;
        _classificationDB = null;
    }

    public int getX() {
        return _x;
    }

    public void setX(int x) {
        _x = x;
    }

    public BoundCondition getBoundCondition() {
        return _boundCondition;
    }

    public void setBoundCondition(BoundCondition boundCondition) {
        _boundCondition = boundCondition;
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

        IClassificationDBBuilder builder = new TroveClassificationDBBuilder(index().getDocumentDB(), index().getCategoryDB());

        TShortArrayList assignedCats = new TShortArrayList();
        TShortArrayList bestCats = new TShortArrayList();
        int minPosition = 0;

        docs.begin();
        while (docs.hasNext()) {
            int docID = docs.next();
            if (index().getContentDB().getDocumentFeaturesCount(docID) == 0)
                continue;
            ClassificationResult res = _classifier.classify(index(), docID);

            assignedCats.clear();

            for (short i = 0; i < res.categoryID.size(); i++) {
                ClassifierRange cr = _classifier.getClassifierRange(res.categoryID.get(i));
                if (res.score.get(i) >= cr.border && _validCategories.contains(res.categoryID.get(i)))
                    assignedCats.add(res.categoryID.get(i));
            }

            double minScore = Double.MIN_VALUE;
            bestCats.clear();
            minPosition = 0;
            for (short i = 0; i < res.categoryID.size(); i++) {
                if (_validCategories.contains(i)) {
                    if (bestCats.size() < _x) {
                        bestCats.add(i);
                        double catScore = res.score.get(bestCats.getQuick(i));
                        if (catScore < minScore) {
                            minScore = catScore;
                            minPosition = i;
                        }
                    } else {
                        if (res.score.get(i) > minScore) {
                            bestCats.setQuick(minPosition, i);
                            minScore = Double.MAX_VALUE;
                            for (int j = 0; j < bestCats.size(); ++j) {
                                double catScore = res.score.get(bestCats.getQuick(j));
                                if (catScore < minScore) {
                                    minScore = catScore;
                                    minPosition = j;
                                }
                            }
                        }
                    }
                }
            }

            if (_boundCondition == BoundCondition.ExactlyX) {
                for (int j = 0; j < bestCats.size(); ++j) {
                    builder.setDocumentCategory(docID, bestCats.getQuick(j));
                }
            } else if (_boundCondition == BoundCondition.AtLeastX) {
                for (int j = 0; j < bestCats.size(); ++j) {
                    short cat = bestCats.getQuick(j);
                    builder.setDocumentCategory(docID, cat);
                    if (assignedCats.contains(cat)) {
                        int idx = assignedCats.indexOf(cat);
                        assignedCats.remove(idx);
                    }
                }
                for (int j = 0; j < assignedCats.size(); ++j) {
                    short cat = assignedCats.getQuick(j);
                    builder.setDocumentCategory(docID, cat);
                }
            } else if (_boundCondition == BoundCondition.AtMostX) {
                for (int j = 0; j < assignedCats.size(); ++j) {
                    short cat = assignedCats.getQuick(j);
                    if (bestCats.contains(cat))
                        builder.setDocumentCategory(docID, cat);
                }
            }

            numComputed++;

            // Signal the status of actual operation.
            bar.signal((numComputed * 100) / numToCompute);
        }

        bar.signal(100);

        _classificationDB = builder.getClassificationDB();
    }
}