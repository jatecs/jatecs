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

package it.cnr.jatecs.satc.rank;

import gnu.trove.TIntArrayList;
import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntHashSet;
import it.cnr.jatecs.classification.ClassificationScoreDB;
import it.cnr.jatecs.satc.interfaces.IStaticRank;
import it.cnr.jatecs.utils.Ranker;

abstract public class StaticRank implements IStaticRank {

    int trainSize;
    int testSize;
    ClassificationScoreDB classification;
    TIntHashSet categoriesFilter;

    public StaticRank(int trainSize, ClassificationScoreDB classification, TIntHashSet categoriesFilter) {
        this.trainSize = trainSize;
        this.classification = classification;
        this.testSize = classification.getDocumentCount();
        if (categoriesFilter == null || categoriesFilter.isEmpty()) {
            this.categoriesFilter = new TIntHashSet((int) (testSize + testSize * 0.25), (float) 0.75);
            for (short i = 0; i < classification.getDocumentScoresAsSet(0).size(); i++) {
                this.categoriesFilter.add(i);
            }
        } else {
            this.categoriesFilter = categoriesFilter;
        }
    }

    public TIntArrayList getRank(TIntDoubleHashMap table) {
        Ranker r = new Ranker();
        return r.get(table);
    }

    @Override
    public TIntArrayList getMacroRank() {
        //System.out.println(Arrays.toString(getRank(getMacroTable()).toNativeArray()));
        return getRank(getMacroTable());
    }

    @Override
    public TIntArrayList getMicroRank() {
        return getRank(getMicroTable());
    }

    @Override
    abstract public TIntDoubleHashMap getMacroTable();

    @Override
    abstract public TIntDoubleHashMap getMicroTable();


}
