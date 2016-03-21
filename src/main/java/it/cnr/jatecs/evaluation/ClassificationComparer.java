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

package it.cnr.jatecs.evaluation;

import gnu.trove.TIntHashSet;
import gnu.trove.TShortArrayList;
import gnu.trove.TShortHashSet;
import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDB;
import it.cnr.jatecs.utils.iterators.TShortArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * This comparer, differently from {@link HierarchicalClassificationComparer}, evaluates all the codes
 * in the taxonomy in the same way, giving to internal nodes no special behaviour.
 */
public class ClassificationComparer {

    protected IClassificationDB _experiment;
    protected IClassificationDB _goldStandard;
    protected TShortArrayList _validCategories;
    protected ContingencyTableSet _tableSet;
    private ArrayList<TShortHashSet> _experimentArray;
    private HashMap<Short, String> _categoryNames;
    private ArrayList<TShortHashSet> _goldStandardArray;

    public ClassificationComparer(IClassificationDB experiment,
                                  IClassificationDB goldStandard) {
        this(experiment, goldStandard, goldStandard.getCategoryDB()
                .getCategories());
    }

    public ClassificationComparer(IClassificationDB evaluated,
                                  IClassificationDB target, IShortIterator validCategories) {
        super();
        _validCategories = new TShortArrayList();
        while (validCategories.hasNext())
            _validCategories.add(validCategories.next());
        validCategories.begin();
        _experiment = evaluated;
        _goldStandard = target;
    }

    /**
     * @param evaluated
     * @param target
     * @param validCategories
     * @param fastDataStruct  If true create "primitive" data structures for the datasets,
     *                        in order to perform faster evaluations
     */
    // FIXME implies that document IDs are from 0 to dataset size
    public ClassificationComparer(IClassificationDB evaluated,
                                  IClassificationDB target, IShortIterator validCategories,
                                  boolean fastDataStruct) {
        this(evaluated, target, validCategories);
        _categoryNames = new HashMap<Short, String>();
        while (validCategories.hasNext()) {
            short category = validCategories.next();
            _categoryNames.put(category, _experiment.getCategoryDB()
                    .getCategoryName(category));
        }
        validCategories.begin();
        int docNum = _experiment.getDocumentDB().getDocumentsCount();
        _experimentArray = new ArrayList<TShortHashSet>(docNum);
        _goldStandardArray = new ArrayList<TShortHashSet>(docNum);
        IIntIterator docIt = _experiment.getDocumentDB().getDocuments();
        while (docIt.hasNext()) {
            int document = docIt.next();
            TShortHashSet expCats = new TShortHashSet();
            _experimentArray.add(expCats);
            TShortHashSet goldCats = new TShortHashSet();
            _goldStandardArray.add(goldCats);
            while (validCategories.hasNext()) {
                short category = validCategories.next();
                if (_experiment.hasDocumentCategory(document, category)) {
                    expCats.add(category);
                }
                if (_goldStandard.hasDocumentCategory(document, category)) {
                    goldCats.add(category);
                }
            }
            validCategories.begin();
        }
    }

    public IClassificationDB getEvaluated() {
        return _experiment;
    }

    public IClassificationDB getTarget() {
        return _goldStandard;
    }

    public ContingencyTableSet evaluate() {
        return evaluate(false);
    }

    public ContingencyTableSet evaluate(boolean onlyLeaves) {
        ContingencyTableSet tableSet = new ContingencyTableSet();
        IIntIterator docIt = _experiment.getDocumentDB().getDocuments();
        IShortIterator catIt = new TShortArrayListIterator(_validCategories);

        while (catIt.hasNext()) {
            short category = catIt.next();
            if (onlyLeaves
                    && _experiment.getCategoryDB().hasChildCategories(
                    category))
                continue;
            ContingencyTable table = new ContingencyTable(_experiment
                    .getCategoryDB().getCategoryName(category));
            docIt.begin();
            while (docIt.hasNext()) {
                int document = docIt.next();
                if (_experiment.hasDocumentCategory(document, category)) {
                    if (_goldStandard.hasDocumentCategory(document, category))
                        table.addTP();
                    else
                        table.addFP();
                } else {
                    if (_goldStandard.hasDocumentCategory(document, category))
                        table.addFN();
                    else
                        table.addTN();
                }
            }
            tableSet.addContingenyTable(category, table);
        }
        return tableSet;
    }


    public ContingencyTableSet evaluateFast() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    /**
     * Fast evaluation on all valid categories, mixing datasets. Used in Semi-Automated TC (satc).
     * Evaluation made on all categories, not only leaves.
     *
     * @param trueDocuments document IDs for which the evaluation is made on the gold standard
     * @return
     */
    // FIXME implies that document IDs are from 0 to dataset size
    public ContingencyTableSet evaluateMixedFast(TIntHashSet trueDocuments) {
        ContingencyTableSet tableSet = new ContingencyTableSet();
        IShortIterator catIt = new TShortArrayListIterator(_validCategories);
        HashMap<Short, ContingencyTable> tables = new HashMap<Short, ContingencyTable>();
        while (catIt.hasNext()) {
            short category = catIt.next();
            tables.put(category,
                    new ContingencyTable(_categoryNames.get(category)));
        }
        catIt.begin();

        IIntIterator docIt = _experiment.getDocumentDB().getDocuments();
        while (docIt.hasNext()) {
            int document = docIt.next();
            TShortHashSet expCats = _experimentArray.get(document);
            TShortHashSet goldCats = _goldStandardArray.get(document);
            if (trueDocuments.contains(document)) {
                while (catIt.hasNext()) {
                    short category = catIt.next();
                    ContingencyTable table = tables.get(category);
                    if (goldCats.contains(category)) {
                        table.addTP();
                    } else {
                        table.addTN();
                    }
                }
                catIt.begin();
            } else {
                while (catIt.hasNext()) {
                    short category = catIt.next();
                    ContingencyTable table = tables.get(category);
                    boolean inGold = goldCats.contains(category) ? true : false;
                    if (expCats.contains(category)) {
                        if (inGold)
                            table.addTP();
                        else
                            table.addFP();
                    } else {
                        if (inGold)
                            table.addFN();
                        else
                            table.addTN();
                    }
                }
                catIt.begin();
            }
        }

        while (catIt.hasNext()) {
            short category = catIt.next();
            tableSet.addContingenyTable(category, tables.get(category));
        }
        catIt.begin();

        return tableSet;
    }

    /**
     * Compute the confusion matrix for a single-label experiment. You must be
     * sure that each document has assigned one and only one category.
     * <p>
     * NOTE: Currently the confusion matrix is computed using all categories
     * available on experiment classification DB.
     *
     * @return The confusion matrix.
     */
    public ConfusionMatrix evaluateSingleLabel() {
        ConfusionMatrix cm = new ConfusionMatrix(_experiment.getCategoryDB()
                .getCategoriesCount());
        IIntIterator docIt = _experiment.getDocumentDB().getDocuments();

        while (docIt.hasNext()) {
            int docID = docIt.next();
            IShortIterator docCats = _experiment.getDocumentCategories(docID);
            assert (docCats.hasNext());
            short catID = docCats.next();

            IShortIterator goldDocCats = _goldStandard
                    .getDocumentCategories(docID);
            assert (goldDocCats.hasNext());
            short trueCatID = goldDocCats.next();

            cm.setError(trueCatID, catID, cm.getError(trueCatID, catID) + 1);
        }

        return cm;
    }
}
