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

package it.cnr.jatecs.indexing.tsr;

import gnu.trove.TIntArrayList;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.TextualProgressBar;
import it.cnr.jatecs.utils.iterators.TIntArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.util.TreeSet;

public class LocalTSR implements ITsr {

    protected ITsrFunction _func;

    protected int _numBestFeatureForCategory;


    protected double _minThreshold;

    public LocalTSR(ITsrFunction func) {
        _func = func;
        _numBestFeatureForCategory = 40;
    }

    /**
     * Set the minimum threshold value used to decide if a feature will be
     * erased. The value of threshold must range from 0 (excluded) and 100
     * (excluded) and indicate the percentage of features that must be deleted
     * from the original features contained in the initial index. The features
     * are ordered according to their local TEF value: only the (100-threshold)%
     * of the features with higher local TEF value, at least in one category,
     * will be kept back in the resulting index.
     *
     * @param threshold The value of threshold.
     */
    public void setMinimumThreshold(double threshold) {
        if (threshold <= 0)
            throw new RuntimeException(
                    "The value of TSR threshold must be greater than 0.");

        if (threshold >= 100)
            throw new RuntimeException(
                    "The value of TSR threshold must be lower than 100.");

        _minThreshold = 1.0 - (threshold / 100.0);
    }

    public void setNumberOfBestFeaturesForCategory(int numFeatures) {
        _numBestFeatureForCategory = numFeatures;
    }

    public void computeTSR(IIndex index) {
        TextualProgressBar bar = new TextualProgressBar(
                "Compute local TSR with " + _func.getClass().getName());
        int total = index.getCategoryDB().getCategoriesCount();
        int step = 0;

        IShortIterator itCats = index.getCategoryDB().getCategories();
        while (itCats.hasNext()) {
            short catID = itCats.next();
            if (index.getClassificationDB().getCategoryDocumentsCount(catID) == 0)
                continue;

            TreeSet<FeatureEntry> best = new TreeSet<FeatureEntry>();
            TIntArrayList toRemove = new TIntArrayList();

            // For each valid feature in this category compute TEF.
            IIntIterator itFeats = index.getDomainDB().getCategoryFeatures(
                    catID);
            while (itFeats.hasNext()) {
                int featID = itFeats.next();
                double tef = _func.compute(catID, featID, index);

                FeatureEntry fe = new FeatureEntry();
                fe.featureID = featID;
                fe.score = tef;
                best.add(fe);
                if (best.size() > _numBestFeatureForCategory) {
                    toRemove.add(best.first().featureID);
                    best.remove(best.first());

                }
            }

            // Remove the worst features.
            index.getDomainDB().removeCategoryFeatures(catID,
                    new TIntArrayListIterator(toRemove));

            step++;
            bar.signal((step * 100) / total);
        }

        bar.signal(100);
    }

}
