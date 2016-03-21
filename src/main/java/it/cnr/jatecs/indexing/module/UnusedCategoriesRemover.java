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

package it.cnr.jatecs.indexing.module;

import gnu.trove.TShortArrayList;
import gnu.trove.TShortShortHashMap;
import it.cnr.jatecs.indexes.DB.generic.GenericIndex;
import it.cnr.jatecs.indexes.DB.interfaces.ICategoryDB;
import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDB;
import it.cnr.jatecs.indexes.DB.interfaces.IDomainDB;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.module.JatecsModule;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.iterators.FilteredShortIterator;
import it.cnr.jatecs.utils.iterators.TShortArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

public class UnusedCategoriesRemover extends JatecsModule {

    private IIndex _index;

    private TShortShortHashMap _originalToNew;

    private TShortShortHashMap _newToOriginal;

    private TShortArrayList _usedCategories;

    private int _limit;

    public UnusedCategoriesRemover(IIndex index) {
        this(index, 0);
    }

    public UnusedCategoriesRemover(IIndex index, int limit) {
        super(index, "UnusedCategoriesRemover");
        _index = null;
        _newToOriginal = null;
        _originalToNew = null;
        _usedCategories = null;
        _limit = limit;
    }

    public IIndex getProcessedIndex() {
        return _index;
    }

    public TShortShortHashMap getNewToOriginalMapping() {
        return (TShortShortHashMap) _newToOriginal.clone();
    }

    public TShortShortHashMap getOriginalToNewMapping() {
        return (TShortShortHashMap) _originalToNew.clone();
    }

    @Override
    protected void processModule() {
        IIndex source = index();
        ICategoryDB categoriesDB = source.getCategoryDB().cloneDB();
        IClassificationDB classificationDB = source.getClassificationDB().cloneDB(categoriesDB, source.getDocumentDB());
        IDomainDB domainDB = source.getDomainDB().cloneDB(categoriesDB, source.getFeatureDB());
        _index = new GenericIndex(source.getFeatureDB(), source.getDocumentDB(),
                categoriesDB, domainDB, source.getContentDB(),
                source.getWeightingDB(), classificationDB);
        _usedCategories = computeUsedCategories(_index);
        FilteredShortIterator unusedCategories = new FilteredShortIterator(categoriesDB.getCategories(), new TShortArrayListIterator(_usedCategories), true);
        _newToOriginal = new TShortShortHashMap();
        _originalToNew = new TShortShortHashMap();
        JatecsLogger.status().println("Used categories mapping:");
        for (short i = 0; i < _usedCategories.size(); ++i) {
            short usedCategory = _usedCategories.getQuick(i);
            JatecsLogger.status().println(source.getCategoryDB().getCategoryName(usedCategory) + " (" + usedCategory + " -> " + i + ")");
            _newToOriginal.put(i, usedCategory);
            _originalToNew.put(usedCategory, i);
        }
        JatecsLogger.status().println("done.");
        _index.removeCategories(unusedCategories);
    }

    public IShortIterator getUsedCategories() {
        return new TShortArrayListIterator(_usedCategories);
    }

    public IShortIterator getUnusedCategories() {
        return new FilteredShortIterator(index().getCategoryDB().getCategories(), new TShortArrayListIterator(_usedCategories), true);
    }

    private TShortArrayList computeUsedCategories(IIndex index) {
        TShortArrayList usedCategories = new TShortArrayList();
        ICategoryDB categoriesDB = index.getCategoryDB();
        IClassificationDB classificationDB = index.getClassificationDB();
        IShortIterator categories = categoriesDB.getCategories();
        while (categories.hasNext()) {
            short category = categories.next();
            IShortIterator childCategories = categoriesDB.getChildCategories(category);
            IIntIterator documents = classificationDB.getCategoryDocuments(category);
            if (_limit == 0) {
                boolean hasItsOwnDocuments = false;
                while (documents.hasNext()) {
                    int document = documents.next();
                    boolean found = true;
                    childCategories.begin();
                    while (childCategories.hasNext()) {
                        short childCategory = childCategories.next();
                        if (classificationDB.hasDocumentCategory(document, childCategory)) {
                            found = false;
                            break;
                        }
                    }
                    if (found) {
                        hasItsOwnDocuments = true;
                        break;
                    }
                }
                if (hasItsOwnDocuments)
                    usedCategories.add(category);
            } else {
                int hasItsOwnDocuments = 0;
                while (documents.hasNext()) {
                    int document = documents.next();
                    boolean found = true;
                    childCategories.begin();
                    while (childCategories.hasNext()) {
                        short childCategory = childCategories.next();
                        if (classificationDB.hasDocumentCategory(document, childCategory)) {
                            found = false;
                            break;
                        }
                    }
                    if (found) {
                        ++hasItsOwnDocuments;
                        if (hasItsOwnDocuments > _limit)
                            break;
                    }
                }
                if (hasItsOwnDocuments > _limit)
                    usedCategories.add(category);
            }
        }
        return usedCategories;
    }

}
