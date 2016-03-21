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

package it.cnr.jatecs.indexes.DB.troveCompact;

import gnu.trove.TIntArrayList;
import gnu.trove.TShortArrayList;
import it.cnr.jatecs.indexes.DB.interfaces.ICategoryDB;
import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDB;
import it.cnr.jatecs.indexes.DB.interfaces.IDocumentDB;
import it.cnr.jatecs.utils.iterators.EmptyIntIterator;
import it.cnr.jatecs.utils.iterators.TIntArrayListIterator;
import it.cnr.jatecs.utils.iterators.TShortArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.util.Vector;

public class TroveClassificationILDB implements IClassificationDB {

    protected String _name;

    protected ICategoryDB _categoriesDB;
    protected IDocumentDB _documentsDB;
    protected Vector<TIntArrayList> _categoriesDocuments;
    protected Vector<Vector<Boolean>> _categoriesDocumentsPrimary;

    public TroveClassificationILDB(IDocumentDB documentsDB, ICategoryDB categoriesDB) {
        super();
        _name = "generic";
        _documentsDB = documentsDB;
        _categoriesDB = categoriesDB;
        int size = categoriesDB.getCategoriesCount();
        _categoriesDocuments = new Vector<TIntArrayList>(size);
        _categoriesDocumentsPrimary = new Vector<Vector<Boolean>>(size);
        for (int i = 0; i < size; ++i) {
            _categoriesDocuments.add(new TIntArrayList());
            _categoriesDocumentsPrimary.add(new Vector<Boolean>());
        }

    }

    public ICategoryDB getCategoryDB() {
        return _categoriesDB;
    }

    public IDocumentDB getDocumentDB() {
        return _documentsDB;
    }

    public int getCategoryDocumentsCount(short category) {
        if (category < _categoriesDocuments.size())
            return _categoriesDocuments.get(category).size();
        else
            return 0;
    }

    public IIntIterator getCategoryDocuments(short category) {
        if (category < _categoriesDocuments.size())
            return new TIntArrayListIterator(_categoriesDocuments.get(category));
        else
            return new EmptyIntIterator();
    }

    public boolean hasDocumentCategory(int document, short category) {
        if (category < _categoriesDocuments.size())
            return (_categoriesDocuments.get(category).binarySearch(document)) >= 0;
        else
            return false;
    }

    public int getDocumentCategoriesCount(int document) {
        IShortIterator catIt = _categoriesDB.getCategories();
        int count = 0;
        while (catIt.hasNext()) {
            if (hasDocumentCategory(document, catIt.next()))
                ++count;
        }
        return count;
    }

    public IShortIterator getDocumentCategories(int document) {
        IShortIterator catIt = _categoriesDB.getCategories();
        short category = 0;
        TShortArrayList categories = new TShortArrayList();
        while (catIt.hasNext()) {
            category = catIt.next();
            if (hasDocumentCategory(document, category))
                categories.add(category);
        }
        return new TShortArrayListIterator(categories);
    }

    public String getName() {
        return _name;
    }

    public void setName(String name) {
        _name = name;
    }

    public void removeCategories(IShortIterator removedCategories) {
        int shift = 0;
        while (removedCategories.hasNext()) {
            short catID = removedCategories.next();
            _categoriesDocuments.remove(catID - shift);
            _categoriesDocumentsPrimary.remove(catID - shift);
            ++shift;
        }
    }

    public void removeDocuments(IIntIterator removedDocuments) {
        for (int i = 0; i < _categoriesDocuments.size(); ++i) {
            TIntArrayList docs = _categoriesDocuments.get(i);
            Vector<Boolean> docsPrimary = _categoriesDocumentsPrimary.get(i);
            int j = 0;
            int shift = 0;
            int doc;
            int rem;
            if (j < docs.size() && removedDocuments.hasNext()) {
                doc = docs.getQuick(j);
                rem = removedDocuments.next();

                while (true) {
                    if (doc == rem) {
                        docs.remove(j);
                        docsPrimary.remove(j);
                        if (j < docs.size() && removedDocuments.hasNext()) {
                            doc = docs.getQuick(j);
                            rem = removedDocuments.next();
                            ++shift;
                        } else
                            break;
                    } else if (doc > rem) {
                        if (removedDocuments.hasNext()) {
                            rem = removedDocuments.next();
                            ++shift;
                        } else
                            break;
                    } else {
                        docs.setQuick(j, doc - shift);
                        ++j;
                        if (j < docs.size())
                            doc = docs.getQuick(j);
                        else
                            break;
                    }
                }
                ++shift;
            }
            while (j < docs.size()) {
                docs.setQuick(j, docs.getQuick(j) - shift);
                ++j;
            }
            removedDocuments.begin();
        }
    }

    @SuppressWarnings("unchecked")
    public IClassificationDB cloneDB(ICategoryDB categoriesDB, IDocumentDB documentsDB) {
        TroveClassificationILDB classificationDB = new TroveClassificationILDB(documentsDB, categoriesDB);
        classificationDB._name = new String(_name);

        classificationDB._categoriesDocuments = new Vector<TIntArrayList>(_categoriesDocuments.size());
        classificationDB._categoriesDocumentsPrimary = new Vector<Vector<Boolean>>(_categoriesDocumentsPrimary.size());
        for (int i = 0; i < _categoriesDocuments.size(); ++i) {
            classificationDB._categoriesDocuments.add((TIntArrayList) _categoriesDocuments.get(i).clone());
            classificationDB._categoriesDocumentsPrimary.add((Vector<Boolean>) _categoriesDocumentsPrimary.get(i).clone());
        }
        return classificationDB;
    }

    public boolean isPrimaryCategory(int document, short category) {
        if (category < _categoriesDocumentsPrimary.size()) {
            int pos = _categoriesDocuments.get(category).binarySearch(document);
            if (pos < 0)
                return false;
            else
                return _categoriesDocumentsPrimary.get(category).get(pos);
        } else
            return false;
    }
}

