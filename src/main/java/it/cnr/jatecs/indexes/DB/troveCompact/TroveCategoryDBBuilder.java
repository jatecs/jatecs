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
import it.cnr.jatecs.indexes.DB.interfaces.ICategoryDB;
import it.cnr.jatecs.indexes.DB.interfaces.ICategoryDBBuilder;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

public class TroveCategoryDBBuilder implements ICategoryDBBuilder {

    protected TroveCategoryDB _categoryDB;

    public TroveCategoryDBBuilder() {
        super();
        _categoryDB = new TroveCategoryDB();
    }

    public void setCategories(String[] categories) {
        int shift = 0;
        for (int i = 0; i < categories.length; ++i) {
            if (categories[i].equals("") || categories[i] == null) {
                ++shift;
                continue;
            }

            if (!_categoryDB._categoryMap.containsKey(categories[i])) {
                _categoryDB._categoryMap.put(categories[i - shift],
                        (short) (i - shift));
                _categoryDB._categoryRMap.add(categories[i - shift]);
                _categoryDB._categoryHierarchy.add(new TShortArrayList());
            } else
                ++shift;
        }
    }

    public void addCategory(String catName) {
        if (!_categoryDB._categoryMap.containsKey(catName)) {
            int i = _categoryDB._categoryMap.size();
            _categoryDB._categoryMap.put(catName, (short) i);
            _categoryDB._categoryRMap.add(catName);
            _categoryDB._categoryHierarchy.add(new TShortArrayList());
        }
    }

    public void setParentCategory(String childName, String parentName) {
        if (_categoryDB._categoryMap.containsKey(childName)
                && _categoryDB._categoryMap.containsKey(parentName)) {
            short child = _categoryDB._categoryMap.get(childName);
            short parent = _categoryDB._categoryMap.get(parentName);
            if (!_categoryDB._categoryHierarchy.get(child).contains(parent)) {
                _categoryDB._categoryHierarchy.get(child).add(parent);
                _categoryDB._categoryHierarchy.get(child).sort();
                checkLoops(child, parent);
                if (_categoryDB._childs.containsKey(parent)) {
                    TShortArrayList childs = (TShortArrayList) _categoryDB._childs
                            .get(parent);
                    childs.add(child);
                    childs.sort();
                }
            }
        }
    }

    private void checkLoops(short child, short parent) {
        if (child == parent)
            throw new RuntimeException("Category hierarchy can't contain loops: " + _categoryDB.getCategoryName(child));
        IShortIterator cats = _categoryDB.getParentCategories(parent);
        while (cats.hasNext()) {
            checkLoops(child, cats.next());
        }
    }

    public ICategoryDB getCategoryDB() {
        return _categoryDB;
    }
}
