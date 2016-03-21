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
import it.cnr.jatecs.utils.TextualProgressBar;
import it.cnr.jatecs.utils.iterators.TIntArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;

import java.util.TreeSet;

public class GlobalTSR implements ITsr {

    protected ITsrFunction _func;
    protected int _numBestFeature;
    protected IGlobalTSRPolicy _global;

    public GlobalTSR(ITsrFunction function, IGlobalTSRPolicy global) {
        _func = function;
        _global = global;
        _numBestFeature = 2000;
    }

    public void setNumberOfBestFeatures(int numFeatures) {
        _numBestFeature = numFeatures;
    }

    public void computeTSR(IIndex index) {
        TextualProgressBar bar = new TextualProgressBar(
                "Compute global TSR with " + _func.getClass().getName());
        int total = index.getFeatureDB().getFeaturesCount();
        int step = 0;

        TreeSet<FeatureEntry> best = new TreeSet<FeatureEntry>();
        TIntArrayList toRemove = new TIntArrayList();

        IIntIterator it = index.getFeatureDB().getFeatures();
        while (it.hasNext()) {
            int featID = it.next();

            double[] scores = new double[index.getCategoryDB()
                    .getCategoriesCount()];

            for (short catID = 0; catID < scores.length; catID++) {
                if (!index.getDomainDB().hasCategoryFeature(catID, featID))
                    scores[catID] = 0;
                else
                    scores[catID] = _func.compute(catID, featID, index);
            }

            // Compute feature global value.
            double score = _global.compute(scores, index);

            FeatureEntry fe = new FeatureEntry();
            fe.featureID = featID;
            fe.score = score;
            best.add(fe);
            if (best.size() > _numBestFeature) {
                toRemove.add(best.first().featureID);
                best.remove(best.first());

            }

            step++;
            bar.signal((step * 100) / total);
        }

        bar.signal(100);

        toRemove.sort();

        // Remove the worst features.
        JatecsLogger.status().print("Removing worst features...");
        index.removeFeatures(new TIntArrayListIterator(toRemove));

        JatecsLogger.status().println(
                "done. Now the DB contains "
                        + index.getFeatureDB().getFeaturesCount()
                        + " feature(s).");
    }

}
