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

import it.cnr.jatecs.evaluation.ContingencyTableDataManager;
import it.cnr.jatecs.evaluation.ContingencyTableSet;
import it.cnr.jatecs.module.JatecsModule;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;

public class ContingencyTablesMerger extends JatecsModule {

    private Hashtable<String, String> _tables;

    public ContingencyTablesMerger() {
        super(null, ContingencyTablesMerger.class.getName());

        _tables = new Hashtable<String, String>();
    }

    public void addContingencyTable(String path) {
        _tables.put(path, path);
    }

    public void addContingencyTablesRange(String path,
                                          String substitutionVariable, int begin, int end) {
        for (int i = begin; i <= end; i++) {
            path = path.replaceAll(substitutionVariable, "" + i);
            _tables.put(path, path);
        }
    }

    @Override
    protected void processModule() {
        ContingencyTableSet res = new ContingencyTableSet();

        Iterator<String> it = _tables.values().iterator();
        while (it.hasNext()) {
            String dir = it.next();
            ContingencyTableSet ct;
            try {
                ct = ContingencyTableDataManager.readContingencyTableSet(dir);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            IShortIterator cats = ct.getEvaluatedCategories();
            while (cats.hasNext()) {
                short catID = cats.next();
                res.addContingenyTable(catID,
                        ct.getCategoryContingencyTable(catID));
            }
        }
    }

}
