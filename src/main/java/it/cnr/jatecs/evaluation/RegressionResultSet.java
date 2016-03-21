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

import gnu.trove.TShortShortHashMap;
import it.cnr.jatecs.utils.interfaces.INameable;
import it.cnr.jatecs.utils.interfaces.INamed;
import it.cnr.jatecs.utils.iterators.ShortArrayIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.util.Arrays;
import java.util.Vector;

public class RegressionResultSet implements INamed, INameable {

    protected String _name;
    protected RegressionResult _globalTable;
    protected TShortShortHashMap _categoriesIds;
    protected Vector<RegressionResult> _categoriesTables;
    private int _categoriesCount;

    public RegressionResultSet(int categoriesCount) {
        this("generic", categoriesCount);
    }

    public RegressionResultSet(String name, int categoriesCount) {
        super();
        _name = name;
        _categoriesCount = categoriesCount;
        _globalTable = new RegressionResult(name, categoriesCount);
        _categoriesIds = new TShortShortHashMap();
        _categoriesTables = new Vector<RegressionResult>();
    }


    public void add(short category, String catName, int distance) {
        RegressionResult catTable;
        if (!_categoriesIds.containsKey(category)) {
            _categoriesIds.put(category, (short) _categoriesTables.size());
            catTable = new RegressionResult(catName, _categoriesCount);
            _categoriesTables.add(catTable);
        } else
            catTable = getRegressionResult(category);

        catTable.add(distance);
        _globalTable.add(distance);
    }


    public void addRegressionResult(short category, RegressionResult table) {
        RegressionResult catTable;
        if (!_categoriesIds.containsKey(category)) {
            _categoriesIds.put(category, (short) _categoriesTables.size());
            catTable = new RegressionResult(table.getName(), _categoriesCount);
            _categoriesTables.add(catTable);
        } else
            catTable = getRegressionResult(category);

        for (int i = 0; i < _categoriesCount; ++i) {
            catTable.set(i, catTable.get(i) + table.get(i));
            _globalTable.set(i, _globalTable.get(i) + table.get(i));
        }
    }

    public void reset() {
        _globalTable.reset();
        for (int i = 0; i < _categoriesTables.size(); ++i)
            _categoriesTables.get(i).reset();
    }

    public IShortIterator getEvaluatedCategories() {
        short[] cats = _categoriesIds.keys().clone();
        Arrays.sort(cats);
        return new ShortArrayIterator(cats);
    }

    public int getEvaluatedCategoriesCount() {
        return _categoriesIds.size();
    }

    public RegressionResult getGlobalRegressionResult() {
        return _globalTable;
    }

    public RegressionResult getRegressionResult(short category) {
        return _categoriesTables.get(_categoriesIds.get(category));
    }

    public String getName() {
        return _name;
    }

    public void setName(String name) {
        _name = name;
    }
}
