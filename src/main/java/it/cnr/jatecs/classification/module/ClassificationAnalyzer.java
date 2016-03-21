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
import it.cnr.jatecs.classification.ClassificationScoreDB;
import it.cnr.jatecs.classification.ClassifierRangeWithScore;
import it.cnr.jatecs.indexes.DB.interfaces.ICategoryDB;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.module.JatecsModule;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

public class ClassificationAnalyzer extends JatecsModule {

    protected ClassificationScoreDB _cl;

    protected IIndex _training;
    protected IIndex _test;

    protected TShortArrayList _validCategories;

    public ClassificationAnalyzer(IIndex trainingIndex, IIndex testIndex,
                                  ClassificationScoreDB classification, ICategoryDB validCategories) {
        super(testIndex, ClassificationAnalyzer.class.getName());
        _cl = classification;
        _training = trainingIndex;
        _test = testIndex;

        _validCategories = new TShortArrayList();
        IShortIterator catIt = validCategories.getCategories();
        while (catIt.hasNext()) {
            short cat = catIt.next();
            String catName = validCategories.getCategoryName(cat);
            short indexCat = index().getCategoryDB().getCategory(catName);
            if (indexCat >= 0)
                _validCategories.add(indexCat);
        }
        _validCategories.sort();
    }

    @Override
    protected void processModule() {
        assert (_training.getCategoryDB().getCategoriesCount() == _test
                .getCategoryDB().getCategoriesCount());
        Hashtable<Short, Integer> classif = new Hashtable<Short, Integer>();
        IIntIterator testDocs = _test.getDocumentDB().getDocuments();
        int wrong = 0;
        int empty = 0;
        while (testDocs.hasNext()) {
            int docID = testDocs.next();

            Vector<Short> together = new Vector<Short>();

            Set<Entry<Short, ClassifierRangeWithScore>> en = _cl
                    .getDocumentScoresAsSet(docID);
            Iterator<Entry<Short, ClassifierRangeWithScore>> it = en.iterator();
            while (it.hasNext()) {
                Entry<Short, ClassifierRangeWithScore> e = it.next();
                if (!_validCategories.contains(e.getKey()))
                    continue;

                if (e.getValue().score < e.getValue().border)
                    continue;

                if (classif.containsKey(e.getKey())) {
                    classif.put(e.getKey(), classif.get(e.getKey()) + 1);
                } else {
                    classif.put(e.getKey(), 1);
                }

                together.add(e.getKey());
            }

            if (together.size() > 1) {
                Hashtable<Integer, Integer> intersection = new Hashtable<Integer, Integer>();
                for (int i = 0; i < together.size(); i++) {
                    IIntIterator docs = _training.getClassificationDB()
                            .getCategoryDocuments(together.get(i));
                    while (docs.hasNext()) {
                        int doc = docs.next();
                        if (intersection.containsKey(doc))
                            intersection.put(doc, intersection.get(doc) + 1);
                        else
                            intersection.put(doc, 1);
                    }

                }

                // Count the documents with this pattern.
                Vector<Integer> listCorrects = new Vector<Integer>();
                Vector<Integer> listCorrectsExceptOne = new Vector<Integer>();
                Vector<Integer> listCorrectsExceptTwo = new Vector<Integer>();
                Iterator<Integer> itDocs = intersection.keySet().iterator();
                while (itDocs.hasNext()) {
                    int doc = itDocs.next();
                    int num = intersection.get(doc);

                    if (num == together.size()) {
                        listCorrects.add(doc);
                    }

                    if (num == (together.size() - 1))
                        listCorrectsExceptOne.add(doc);

                    if (num == (together.size() - 2))
                        listCorrectsExceptTwo.add(doc);
                }

                String msg = "Test doc: "
                        + _test.getDocumentDB().getDocumentName(docID)
                        + "\n\ttraining [" + listCorrects.size() + "]";
                for (int i = 0; i < listCorrects.size(); i++)
                    msg += " "
                            + _training.getDocumentDB().getDocumentName(
                            listCorrects.get(i));

                msg += "\n";

				/*
                 * msg += "\ttraining (-1) ["+listCorrectsExceptOne.size()+"]";
				 * for (int i = 0; i < listCorrectsExceptOne.size(); i++) msg += "
				 * "+_training.getDocumentsDB().getDocumentName(listCorrectsExceptOne.get(i));
				 * msg += "\n";
				 * 
				 * msg += "\ttraining (-2) ["+listCorrectsExceptTwo.size()+"]";
				 * for (int i = 0; i < listCorrectsExceptTwo.size(); i++) msg += "
				 * "+_training.getDocumentsDB().getDocumentName(listCorrectsExceptTwo.get(i));
				 * msg += "\n";
				 * 
				 * msg += "\n";
				 */
                JatecsLogger.status().print(msg);

                if (listCorrects.size() == 0)
                    wrong++;
            }

            if (together.size() == 0)
                empty++;
        }

        JatecsLogger.status().println("Probably wrong classified: " + wrong);
        JatecsLogger.status().println("Documents without categories: " + empty);

        IShortIterator cats = _training.getCategoryDB().getCategories();
        while (cats.hasNext()) {
            short catID = cats.next();
            if (!_validCategories.contains(catID))
                continue;
            int val = 0;
            if (classif.containsKey(catID))
                val = classif.get(catID);

            JatecsLogger.status().println(
                    "Category <"
                            + _training.getCategoryDB()
                            .getCategoryName(catID)
                            + ">: training-->"
                            + _training.getClassificationDB()
                            .getCategoryDocumentsCount(catID)
                            + " test-->" + val);
        }
    }

}
