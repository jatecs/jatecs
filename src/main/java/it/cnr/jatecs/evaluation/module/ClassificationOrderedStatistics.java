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

package it.cnr.jatecs.evaluation.module;

import it.cnr.jatecs.classification.thresholdboost.ThresholdBoostClassifierCustomizer;
import it.cnr.jatecs.evaluation.ContingencyTable;
import it.cnr.jatecs.evaluation.ContingencyTableDataManager;
import it.cnr.jatecs.evaluation.ContingencyTableSet;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.module.JatecsModule;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.Os;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.io.IOException;
import java.util.Iterator;
import java.util.TreeSet;

public class ClassificationOrderedStatistics extends JatecsModule {

    private String _ctable;
    private ThresholdBoostClassifierCustomizer _cust;
    public ClassificationOrderedStatistics(IIndex trainingIndex, String ctable, ThresholdBoostClassifierCustomizer cust) {
        super(trainingIndex, ClassificationOrderedStatistics.class.getName());
        _ctable = ctable;
        _cust = cust;
    }

    @Override
    protected void processModule() {
        ContingencyTableSet ts;
        try {
            ts = ContingencyTableDataManager.readContingencyTableSet(_ctable);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        TreeSet<Category> ord = new TreeSet<Category>();
        IShortIterator cats = index().getCategoryDB().getCategories();
        while (cats.hasNext()) {
            short catID = cats.next();
            Category c = new Category();
            c.catID = catID;
            c.numPositives = index().getClassificationDB().getCategoryDocumentsCount(catID);
            ord.add(c);
        }

        ContingencyTableSet cts = new ContingencyTableSet();
        Iterator<Category> it = ord.iterator();

        String msg = "Rank\tCatID\tName\tF1\tPrecision\tRecall\tMicroF1\tThreshold\n";
        int count = 1;
        while (it.hasNext()) {
            Category c = it.next();
            ContingencyTable ct = ts.getCategoryContingencyTable(c.catID);
            cts.addContingenyTable(c.catID, ct);

            ContingencyTable global = cts.getGlobalContingencyTable();

            msg += "" + (count++) + "\t" + c.catID + "\t" + index().getCategoryDB().getCategoryName(c.catID) +
                    " \t" + Os.generateDouble(ct.f1(), 4) + "\t" + Os.generateDouble(ct.precision(), 4) + "\t" + Os.generateDouble(ct.recall(), 4) +
                    "\t" + Os.generateDouble(global.f1(), 4) +
                    //"\ttp="+global.tp()+"\ttn="+global.tn()+"\tfp="+global.fp()+"\tfn="+global.fn()+
                    "\t" + _cust.getClassifierRange(c.catID).border + "\n";
        }

        JatecsLogger.status().println(msg);
    }

    class Category implements Comparable<Category> {
        short catID;
        int numPositives;


        public int compareTo(Category o) {
            if (numPositives < o.numPositives)
                return 1;
            else if (numPositives > o.numPositives)
                return -1;
            else {
                if (catID < o.catID)
                    return 1;
                else if (catID > o.catID)
                    return -1;
                else
                    return 0;
            }
        }


        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Category))
                return false;

            return compareTo((Category) obj) == 0;
        }


    }

}
