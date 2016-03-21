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

package it.cnr.jatecs.classification.weightedboost;

import it.cnr.jatecs.indexing.discretization.DiscreteBin;

import java.util.TreeSet;

public class WeakLearnerParams {

    private TreeSet<DiscreteBin>[] bins;
    private Integer maxNumberOfBins = null;


    @SuppressWarnings("unchecked")
    public WeakLearnerParams() {
        bins = new TreeSet[0];
    }


    public TreeSet<DiscreteBin>[] getBins() {
        return bins;
    }


    public void setBins(TreeSet<DiscreteBin>[] bins) {
        this.bins = bins;
        maxNumberOfBins = null;
    }


    public int getMaxNumberOfBins() {
        if (maxNumberOfBins == null) {
            maxNumberOfBins = 0;
            for (int i = 0; i < bins.length; i++) {
                if (bins[i].size() > maxNumberOfBins)
                    maxNumberOfBins = bins[i].size();
            }
        }

        return maxNumberOfBins;
    }
}
