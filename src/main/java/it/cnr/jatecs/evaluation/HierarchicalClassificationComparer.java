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

import gnu.trove.TShortArrayList;
import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDB;
import it.cnr.jatecs.utils.iterators.TShortArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

/**
 * This comparer evaluates internal nodes in a special way, by giving an
 * evaluation to internal nodes only if they act as "other" nodes, i.e. a
 * document is assigned to internal node but it is not assigned to any child of
 * the node.
 *
 */
public class HierarchicalClassificationComparer {


    protected IClassificationDB _experiment;
    protected IClassificationDB _goldStandard;
    protected TShortArrayList _validCategories;
    protected ContingencyTableSet _tableSet;

    public HierarchicalClassificationComparer(IClassificationDB experiment,
                                              IClassificationDB goldStandard) {
        this(experiment, goldStandard, goldStandard.getCategoryDB()
                .getCategories());
    }

    public HierarchicalClassificationComparer(IClassificationDB evaluated,
                                              IClassificationDB target, IShortIterator validCategories) {
        super();
        _validCategories = new TShortArrayList();
        while (validCategories.hasNext())
            _validCategories.add(validCategories.next());
        _experiment = evaluated;
        _goldStandard = target;
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
            ContingencyTable table = new ContingencyTable(_experiment
                    .getCategoryDB().getCategoryName(category));
            if (!_goldStandard.getCategoryDB().hasChildCategories(category)) {
                docIt.begin();
                while (docIt.hasNext()) {
                    int document = docIt.next();
                    if (_experiment.hasDocumentCategory(document, category)) {
                        if (_goldStandard.hasDocumentCategory(document,
                                category))
                            table.addTP();
                        else
                            table.addFP();
                    } else {
                        if (_goldStandard.hasDocumentCategory(document,
                                category))
                            table.addFN();
                        else
                            table.addTN();
                    }
                }
                tableSet.addContingenyTable(category, table);
            } else if (!onlyLeaves) {
                boolean hasItsOwnDocuments = false;
                IShortIterator childs = _goldStandard.getCategoryDB()
                        .getChildCategories(category);
                docIt.begin();
                while (docIt.hasNext()) {
                    int document = docIt.next();
                    boolean goldStandardValue = true;
                    if (_goldStandard.hasDocumentCategory(document, category)) {
                        childs.begin();
                        while (childs.hasNext()) {
                            short child = childs.next();
                            if (_goldStandard.hasDocumentCategory(document,
                                    child)) {
                                goldStandardValue = false;
                                break;
                            }
                        }
                    } else
                        goldStandardValue = false;
                    if (goldStandardValue)
                        hasItsOwnDocuments = true;

                    boolean experimentValue = true;
                    if (_experiment.hasDocumentCategory(document, category)) {
                        childs.begin();
                        while (childs.hasNext()) {
                            short child = childs.next();
                            if (_experiment
                                    .hasDocumentCategory(document, child)) {
                                experimentValue = false;
                                break;
                            }
                        }
                    } else
                        experimentValue = false;

                    if (experimentValue) {
                        if (goldStandardValue)
                            table.addTP();
                        else
                            table.addFP();
                    } else {
                        if (goldStandardValue)
                            table.addFN();
                        else
                            table.addTN();
                    }
                }
                if (hasItsOwnDocuments)
                    tableSet.addContingenyTable(category, table);
            }

        }
        return tableSet;
    }

    /**
     * Compute the confusion matrix for a single-label experiment. You must be
     * sure that each document has assigned one and only one category.
     * <p>
     * NOTE: Currently the confusion matrix is computed using all the categories
     * (internal and leaf nodes) available on experiment classification DB. The
     * internal nodes will be given no useful results.
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
