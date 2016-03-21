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
import it.cnr.jatecs.utils.iterators.EmptyShortIterator;
import it.cnr.jatecs.utils.iterators.TIntArrayListIterator;
import it.cnr.jatecs.utils.iterators.TShortArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.util.Vector;

public class TroveClassificationDB implements IClassificationDB {

    public TroveClassificationDB(IDocumentDB documentsDB, ICategoryDB categoriesDB) {
        super();
        _name = "generic";
        _documentsDB = documentsDB;
        _categoriesDB = categoriesDB;
        int size = documentsDB.getDocumentsCount();
        _documentsCategories = new Vector<TShortArrayList>(size);
        _documentsCatsPrimary = new Vector<Vector<Boolean>>(size);
        for(int i=0;i<size;++i)
        {
            _documentsCategories.add(new TShortArrayList());
            _documentsCatsPrimary.add(new Vector<Boolean>());
        }
    }

    protected String _name;

    protected ICategoryDB _categoriesDB;
    protected IDocumentDB _documentsDB;
    protected Vector<TShortArrayList> _documentsCategories;
    protected Vector<Vector<Boolean>> _documentsCatsPrimary;

    public ICategoryDB getCategoryDB() {
        return _categoriesDB;
    }

    public IDocumentDB getDocumentDB() {
        return _documentsDB;
    }

    public int getDocumentCategoriesCount(int document) {
        if(document<_documentsCategories.size())
            return _documentsCategories.get(document).size();
        else
            return 0;
    }

    public IShortIterator getDocumentCategories(int document) {
        if(document<_documentsCategories.size())
            return new TShortArrayListIterator(_documentsCategories.get(document));
        else
            return new EmptyShortIterator();
    }

    public boolean hasDocumentCategory(int document, short category) {
        if(document<_documentsCategories.size())
            return (_documentsCategories.get(document).binarySearch(category))>=0;
        else
            return false;
    }

    public int getCategoryDocumentsCount(short category) {
        IIntIterator docIt = _documentsDB.getDocuments();
        int count = 0;
        while(docIt.hasNext()) {
            if(hasDocumentCategory(docIt.next(), category))
                ++count;
        }
        return count;
    }

    public IIntIterator getCategoryDocuments(short category) {
        IIntIterator docIt = _documentsDB.getDocuments();
        int document = 0;
        TIntArrayList documents = new TIntArrayList();
        while(docIt.hasNext()) {
            document = docIt.next();
            if(hasDocumentCategory(document, category))
                documents.add(document);
        }
        return new TIntArrayListIterator(documents);
    }

    public String getName() {
        return _name;
    }

    public void setName(String name) {
        _name = name;
    }

    public void removeDocuments(IIntIterator removedDocuments) {
        int shift=0;
        while(removedDocuments.hasNext()) {
            int doc = removedDocuments.next();
            _documentsCategories.remove(doc-shift);
            _documentsCatsPrimary.remove(doc-shift);
            ++shift;
        }
    }

    public void removeCategories(IShortIterator removedCategories) {
        for(int i=0;i<_documentsCategories.size();++i) {
            TShortArrayList cats = _documentsCategories.get(i);
            Vector<Boolean> catsPrimary = _documentsCatsPrimary.get(i);
            int j = 0;
            short shift = 0;
            short cat;
            short rem;
            if(j<cats.size()&&removedCategories.hasNext()) {
                cat = cats.getQuick(j);
                rem = removedCategories.next();

                while(true) {
                    if(cat==rem) {
                        cats.remove(j);
                        catsPrimary.remove(j);
                        if(j<cats.size()&&removedCategories.hasNext()) {
                            cat = cats.getQuick(j);
                            rem = removedCategories.next();
                            ++shift;
                        }
                        else
                            break;
                    }
                    else if(cat>rem) {
                        if(removedCategories.hasNext()) {
                            rem=removedCategories.next();
                            ++shift;
                        }
                        else
                            break;
                    }
                    else {
                        cats.setQuick(j,(short)(cat-shift));
                        ++j;
                        if(j<cats.size())
                            cat = cats.getQuick(j);
                        else
                            break;
                    }
                }
                ++shift;
            }
            while(j<cats.size()) {
                cats.setQuick(j,(short)(cats.getQuick(j)-shift));
                ++j;
            }
            removedCategories.begin();
        }
    }

    @SuppressWarnings("unchecked")
    public IClassificationDB cloneDB(ICategoryDB categoriesDB, IDocumentDB documentsDB) {
        TroveClassificationDB classificationDB = new TroveClassificationDB(documentsDB,categoriesDB);
        classificationDB._name = new String(_name);

        classificationDB._documentsCategories = new Vector<TShortArrayList>(_documentsCategories.size());
        classificationDB._documentsCatsPrimary = new Vector<Vector<Boolean>>(_documentsCategories.size());
        for(int i = 0;i<_documentsCategories.size();++i)
        {
            classificationDB._documentsCategories.add((TShortArrayList) _documentsCategories.get(i).clone());
            classificationDB._documentsCatsPrimary.add((Vector<Boolean>) _documentsCatsPrimary.get(i).clone());
        }
        return classificationDB;
    }

    public boolean isPrimaryCategory(int document, short category)
    {
        if (!hasDocumentCategory(document, category))
            return false;

        Vector<Boolean> cats = _documentsCatsPrimary.get(document);
        int pos = _documentsCategories.get(document).binarySearch(category);

        return cats.get(pos);
    }
}

