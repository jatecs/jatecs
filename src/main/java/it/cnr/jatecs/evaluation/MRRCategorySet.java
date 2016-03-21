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
import it.cnr.jatecs.utils.iterators.ShortArrayIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MRRCategorySet {
    private final List<MRRCategory> cats;
    private final TShortShortHashMap categoriesIds;

    public MRRCategorySet() {
        cats = new ArrayList<MRRCategory>();
        categoriesIds = new TShortShortHashMap();
    }

    public void putMRR(short catID, MRRCategory mrr) {
        if (categoriesIds.containsKey(catID)) {
            int idx = categoriesIds.get(catID);
            cats.set(idx, mrr);
        } else {
            categoriesIds.put(catID, (short) cats.size());
            cats.add(mrr);
        }
    }


    public MRRCategory getMRR(short catID) {
        if (categoriesIds.containsKey(catID)) {
            int idx = categoriesIds.get(catID);
            return cats.get(idx);
        } else
            return null;
    }


    public IShortIterator getEvaluatedCategories() {
        short[] cats = categoriesIds.keys().clone();
        Arrays.sort(cats);
        return new ShortArrayIterator(cats);
    }

    public int getEvaluatedCategoriesCount() {
        return categoriesIds.size();
    }


    public double getMacroMRRValue() {
        double mrr = 0;
        for (int i = 0; i < cats.size(); i++) {
            mrr += cats.get(i).getMRRValue();
        }

        mrr /= getEvaluatedCategoriesCount();
        return mrr;
    }


    public MRRCategory getGlobalMRR() {
        if (cats.size() == 0)
            throw new IllegalStateException("To compute global MRR table, you must have at least 1 MRR category handled by this categiry set.");
        MRRCategory gc = new MRRCategory(cats.get(0).getMaxNumRanks());
        for (int i = 0; i < gc.getMaxNumRanks() + 1; i++) {
            for (int j = 0; j < cats.size(); j++) {
                gc.addNToRank(i, cats.get(j).getRank(i));
            }
        }

        gc.setName("Global MRR");
        return gc;
    }
}
