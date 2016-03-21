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

import gnu.trove.TShortArrayList;
import gnu.trove.TShortObjectHashMap;
import gnu.trove.TShortShortHashMap;
import it.cnr.jatecs.indexes.DB.interfaces.ICategoryDB;
import it.cnr.jatecs.utils.iterators.RangeShortIterator;
import it.cnr.jatecs.utils.iterators.TShortArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Vector;

public class TroveCategoryDB implements ICategoryDB {

    protected String _name;
    protected HashMap<String, Short> _categoryMap;
    protected Vector<String> _categoryRMap;
    protected Vector<TShortArrayList> _categoryHierarchy;
    protected TShortObjectHashMap<TShortArrayList> _childs;
    public TroveCategoryDB() {
        super();
        _categoryMap = new HashMap<String, Short>();
        _categoryRMap = new Vector<String>();
        _categoryHierarchy = new Vector<TShortArrayList>();
        _name = "generic";
        _childs = new TShortObjectHashMap<TShortArrayList>();
    }

    public String getCategoryName(short category) {
        return _categoryRMap.get(category);
    }

    public short getCategory(String categoryName) {
        if (_categoryMap.containsKey(categoryName))
            return _categoryMap.get(categoryName);
        else
            return -1;
    }

    public int getCategoriesCount() {
        return _categoryRMap.size();
    }

    public IShortIterator getCategories() {
        return new RangeShortIterator((short) 0, (short) _categoryRMap.size());
    }

    public boolean isValidCategory(short category) {
        return (category >= 0) ? ((category < _categoryRMap.size()) ? true
                : false) : false;
    }

    public IShortIterator getParentCategories(short category) {
        try {
            return new TShortArrayListIterator(_categoryHierarchy.get(category));
        } catch (Exception e) {
            return new TShortArrayListIterator(new TShortArrayList());
        }

    }

    public IShortIterator getChildCategories(short category) {
        if (!_childs.containsKey(category)) {
            TShortArrayList childs = new TShortArrayList();
            IShortIterator ist = getCategories();
            while (ist.hasNext()) {
                short child = ist.next();
                IShortIterator istp = getParentCategories(child);
                while (istp.hasNext()) {
                    if (category == istp.next())
                        childs.add(child);
                }
            }
            childs.sort();
            _childs.put(category, childs);
        }
        return new TShortArrayListIterator(
                (TShortArrayList) _childs.get(category));
    }

    public int getChildCategoriesCount(short category) {
        if (!_childs.containsKey(category))
            getChildCategories(category);
        return ((TShortArrayList) _childs.get(category)).size();
    }

    public boolean hasChildCategories(short category) {
        IShortIterator ist = getCategories();
        while (ist.hasNext()) {
            short child = ist.next();
            IShortIterator istp = getParentCategories(child);
            while (istp.hasNext()) {
                if (category == istp.next())
                    return true;
            }
        }
        return false;
    }

    public IShortIterator getSiblingCategories(short category) {
        TShortArrayList siblings = new TShortArrayList();
        IShortIterator istp = getParentCategories(category);
        if (istp.hasNext()) {
            while (istp.hasNext()) {
                short parent = istp.next();
                IShortIterator istc = getChildCategories(parent);
                while (istc.hasNext()) {
                    siblings.add(istc.next());
                }
            }
        } else {
            istp = getRootCategories();
            while (istp.hasNext())
                siblings.add(istp.next());

        }
        siblings.sort();
        int pos = siblings.binarySearch(category);
        siblings.remove(pos);

        return new TShortArrayListIterator(siblings);
    }

    public String getName() {
        return _name;
    }

    public void setName(String name) {
        _name = name;
    }

    public void removeCategories(IShortIterator removedCategories) {
        _childs.clear();
        int shift = 0;
        short lastGoodCategory = 0;
        short totalCategories = (short) _categoryMap.size();
        TShortShortHashMap categoryRemap = new TShortShortHashMap();
        while (removedCategories.hasNext()) {
            short removedCategory = removedCategories.next();
            while (lastGoodCategory < removedCategory) {
                categoryRemap.put(lastGoodCategory,
                        (short) (lastGoodCategory - shift));
                ++lastGoodCategory;
            }
            lastGoodCategory = (short) (removedCategory + 1);
            short removedCategoryPosition = (short) (removedCategory - shift);
            _categoryMap.remove(_categoryRMap.get(removedCategoryPosition));
            _categoryRMap.remove(removedCategoryPosition);
            _categoryHierarchy.remove(removedCategoryPosition);
            ++shift;
        }

        while (lastGoodCategory < totalCategories) {
            categoryRemap.put(lastGoodCategory,
                    (short) (lastGoodCategory - shift));
            ++lastGoodCategory;
        }

        Iterator<Entry<String, Short>> mapIter = _categoryMap.entrySet()
                .iterator();
        while (mapIter.hasNext()) {
            Entry<String, Short> entry = mapIter.next();
            short value = entry.getValue();
            short newvalue = categoryRemap.get(value);
            entry.setValue(newvalue);
        }

        for (int i = 0; i < _categoryHierarchy.size(); ++i) {
            TShortArrayList hier = _categoryHierarchy.get(i);
            int pos = 0;
            while (pos < hier.size()) {
                short cat = hier.getQuick(pos);
                if (categoryRemap.containsKey(cat)) {
                    hier.setQuick(pos, categoryRemap.get(cat));
                    ++pos;
                } else
                    hier.remove(pos);
            }
        }
    }

    public IShortIterator getRootCategories() {
        TShortArrayList parents = new TShortArrayList();

        IShortIterator cats = getCategories();
        while (cats.hasNext()) {
            short catID = cats.next();
            IShortIterator parentsIt = getParentCategories(catID);
            if (parentsIt.hasNext())
                continue;
            else
                // It has no parents.
                parents.add(catID);
        }

        parents.sort();
        return new TShortArrayListIterator(parents);
    }

    @SuppressWarnings("unchecked")
    public ICategoryDB cloneDB() {
        TroveCategoryDB categoriesDB = new TroveCategoryDB();
        categoriesDB._name = new String(_name);

        categoriesDB._categoryMap = new HashMap<String, Short>();

        Iterator<Entry<String, Short>> mapIter = _categoryMap.entrySet()
                .iterator();
        while (mapIter.hasNext()) {
            Entry<String, Short> entry = mapIter.next();
            categoriesDB._categoryMap.put(entry.getKey(), entry.getValue());
        }

        categoriesDB._categoryRMap = (Vector<String>) _categoryRMap.clone();

        categoriesDB._categoryHierarchy = new Vector<TShortArrayList>(
                _categoryHierarchy.size());
        for (int i = 0; i < _categoryHierarchy.size(); ++i)
            categoriesDB._categoryHierarchy
                    .add((TShortArrayList) _categoryHierarchy.get(i).clone());

        return categoriesDB;
    }

}
