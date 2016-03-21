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

public class ContingencyTableSet implements INamed, INameable {

    protected String _name;
    protected ContingencyTable _globalTable;
    protected TShortShortHashMap _categoriesIds;
    protected Vector<ContingencyTable> _categoriesTables;

    public ContingencyTableSet() {
        this("generic");
    }

    public ContingencyTableSet(String name) {
        super();
        _name = name;
        _globalTable = new ContingencyTable();
        _categoriesIds = new TShortShortHashMap();
        _categoriesTables = new Vector<ContingencyTable>();
    }

    public void addTP(short category) {
        ContingencyTable catTable;
        if (!_categoriesIds.containsKey(category)) {
            _categoriesIds.put(category, (short) _categoriesTables.size());
            catTable = new ContingencyTable();
            _categoriesTables.add(catTable);
        } else
            catTable = getCategoryContingencyTable(category);

        ++catTable._tp;
        ++_globalTable._tp;
    }

    public void addTN(short category) {
        ContingencyTable catTable;
        if (!_categoriesIds.containsKey(category)) {
            _categoriesIds.put(category, (short) _categoriesTables.size());
            catTable = new ContingencyTable();
            _categoriesTables.add(catTable);
        } else
            catTable = getCategoryContingencyTable(category);

        ++catTable._tn;
        ++_globalTable._tn;
    }

    public void addFP(short category) {
        ContingencyTable catTable;
        if (!_categoriesIds.containsKey(category)) {
            _categoriesIds.put(category, (short) _categoriesTables.size());
            catTable = new ContingencyTable();
            _categoriesTables.add(catTable);
        } else
            catTable = getCategoryContingencyTable(category);

        ++catTable._fp;
        ++_globalTable._fp;
    }

    public void addFN(short category) {
        ContingencyTable catTable;
        if (!_categoriesIds.containsKey(category)) {
            _categoriesIds.put(category, (short) _categoriesTables.size());
            catTable = new ContingencyTable();
            _categoriesTables.add(catTable);
        } else
            catTable = getCategoryContingencyTable(category);

        ++catTable._fn;
        ++_globalTable._fn;
    }

    public void addContingenyTable(short category, ContingencyTable table) {
        ContingencyTable catTable;
        if (!_categoriesIds.containsKey(category)) {
            _categoriesIds.put(category, (short) _categoriesTables.size());
            catTable = new ContingencyTable(table.getName());
            _categoriesTables.add(catTable);
        } else
            catTable = getCategoryContingencyTable(category);

        catTable._fn += table._fn;
        catTable._tn += table._tn;
        catTable._fp += table._fp;
        catTable._tp += table._tp;

        _globalTable._fn += table._fn;
        _globalTable._tn += table._tn;
        _globalTable._fp += table._fp;
        _globalTable._tp += table._tp;
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

    public ContingencyTable getGlobalContingencyTable() {
        return _globalTable;
    }

    public ContingencyTable getCategoryContingencyTable(short category) {
        if (_categoriesIds.containsKey(category))
            return _categoriesTables.get(_categoriesIds.get(category));
        else
            return null;
    }

    public String getName() {
        return _name;
    }

    public void setName(String name) {
        _name = name;
    }

    public double macroPrecision() {
        int count = _categoriesTables.size();
        double sum = 0.0;
        for (int i = 0; i < count; ++i)
            sum += _categoriesTables.get(i).precision();
        return sum / count;
    }

    public double macroRecall() {
        int count = _categoriesTables.size();
        double sum = 0.0;
        for (int i = 0; i < count; ++i)
            sum += _categoriesTables.get(i).recall();
        return sum / count;
    }

    public double macroF1() {
        return macroF(1.0);
    }

    public double macroF(double beta) {
        int count = _categoriesTables.size();
        double sum = 0.0;
        for (int i = 0; i < count; ++i)
            sum += _categoriesTables.get(i).f(beta);
        return sum / count;
    }

    public double macroAccuracy() {
        int count = _categoriesTables.size();
        double sum = 0.0;
        for (int i = 0; i < count; ++i)
            sum += _categoriesTables.get(i).accuracy();
        return sum / count;
    }

    public double macroError() {
        return 1.0 - macroAccuracy();
    }

    public Object macroPd() {
        int count = _categoriesTables.size();
        double sum = 0.0;
        for (int i = 0; i < count; ++i)
            sum += _categoriesTables.get(i).pd();
        return sum / count;
    }


    public double macroSpecificity() {
        int count = _categoriesTables.size();
        double sum = 0.0;
        for (int i = 0; i < count; ++i)
            sum += _categoriesTables.get(i).specificity();
        return sum / count;
    }

    public double macroRoc() {
        int count = _categoriesTables.size();
        double sum = 0.0;
        for (int i = 0; i < count; ++i)
            sum += _categoriesTables.get(i).roc();
        return sum / count;
    }
}
