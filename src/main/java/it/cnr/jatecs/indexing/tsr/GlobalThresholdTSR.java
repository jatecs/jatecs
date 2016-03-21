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
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.Os;
import it.cnr.jatecs.utils.TextualProgressBar;
import it.cnr.jatecs.utils.iterators.TIntArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;

public class GlobalThresholdTSR implements ITsr {

    protected double _threshold;

    protected ITsrFunction _function;

    /**
     * removes features with a tsr score under the given threshold
     *
     * @param threshold the TSR threshold
     * @param function  the GLOBAL (invariant on categories) tsr function
     */
    public GlobalThresholdTSR(double threshold, ITsrFunction function) {
        super();
        _threshold = threshold;
        _function = function;
    }

    public void computeTSR(IIndex index) {
        TextualProgressBar bar = new TextualProgressBar(
                "Compute global threshold ("
                        + Os.generateDoubleString(_threshold, 3)
                        + ") TSR with " + _function.getClass().getName());
        int total = index.getFeatureDB().getFeaturesCount();
        int step = 0;

        TIntArrayList toRemove = new TIntArrayList();

        IIntIterator it = index.getFeatureDB().getFeatures();
        while (it.hasNext()) {
            int featID = it.next();

            double score = _function.compute((short) 0, featID, index);

            if (score < _threshold)
                toRemove.add(featID);

            step++;
            bar.signal((step * 100) / total);
        }

        bar.signal(100);

        toRemove.sort();

        // Remove the worst features.
        JatecsLogger.status().print(
                "Removing " + toRemove.size() + " features...");
        index.removeFeatures(new TIntArrayListIterator(toRemove));
        JatecsLogger.status().println(
                "done. Now the DB contains "
                        + index.getFeatureDB().getFeaturesCount()
                        + " feature(s).");
    }

}
