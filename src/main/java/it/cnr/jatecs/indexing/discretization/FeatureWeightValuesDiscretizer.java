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

package it.cnr.jatecs.indexing.discretization;

import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;

import java.util.TreeSet;

public class FeatureWeightValuesDiscretizer {

    private IIndex index;
    private IValuesDiscretizer discretizer;

    public FeatureWeightValuesDiscretizer(IIndex index,
                                          IValuesDiscretizer discretizer) {
        if (index == null)
            throw new NullPointerException("The specified index is 'null'");
        if (discretizer == null)
            throw new NullPointerException(
                    "The specified discretizer is 'null'");

        this.index = index;
        this.discretizer = discretizer;
    }

    public IIndex getIndex() {
        return index;
    }

    public void setIndex(IIndex index) {
        this.index = index;
    }

    public IValuesDiscretizer getDiscretizer() {
        return discretizer;
    }

    public void setDiscretizer(IValuesDiscretizer discretizer) {
        this.discretizer = discretizer;
    }

    public TreeSet<DiscreteBin>[] discretizeWeights() {

        @SuppressWarnings("unchecked")
        TreeSet<DiscreteBin>[] ret = new TreeSet[getIndex().getFeatureDB()
                .getFeaturesCount()];

        double avgBins = 0;
        IIntIterator feats = getIndex().getFeatureDB().getFeatures();
        while (feats.hasNext()) {
            int featID = feats.next();
            IIntIterator docs = getIndex().getContentDB().getFeatureDocuments(
                    featID);
            double[] weights = new double[getIndex().getContentDB()
                    .getFeatureDocumentsCount(featID)];
            int i = 0;

            // Retrieve all weights.
            while (docs.hasNext()) {
                int docID = docs.next();
                weights[i] = getIndex().getWeightingDB()
                        .getDocumentFeatureWeight(docID, featID);
                i++;
            }

            // Compute discrete bins.
            if (discretizer instanceof ChiMergeDiscretizer) {
                ChiMergeDiscretizer chiD = (ChiMergeDiscretizer) discretizer;
                if (!(chiD.getStatsProvider() instanceof IndexFeatureStatsProvider))
                    throw new IllegalArgumentException(
                            "The stats provider is not instance of "
                                    + IndexFeatureStatsProvider.class.getName());
                IndexFeatureStatsProvider statsProvider = (IndexFeatureStatsProvider) chiD
                        .getStatsProvider();
                statsProvider.setFeatureID(featID);
            } else if (discretizer instanceof InformationGainEntropyDiscretizer) {
                InformationGainEntropyDiscretizer igd = (InformationGainEntropyDiscretizer) discretizer;
                if (!(igd.getStatsProvider() instanceof IndexFeatureStatsProvider))
                    throw new IllegalArgumentException(
                            "The stats provider is not instance of "
                                    + IndexFeatureStatsProvider.class.getName());
                IndexFeatureStatsProvider statsProvider = (IndexFeatureStatsProvider) igd
                        .getStatsProvider();
                statsProvider.setFeatureID(featID);
            }

            TreeSet<DiscreteBin> bins = null;
            if (weights.length == 0) {
                // No documents contain this feature.
                bins = new TreeSet<DiscreteBin>();
                DiscreteBin db = new DiscreteBin(0, 1);
                bins.add(db);
            } else {

                bins = discretizer.discretizeValues(weights);
            }
            ret[featID] = bins;
            avgBins += bins.size();
        }

        avgBins /= getIndex().getFeatureDB().getFeaturesCount();
        System.out.println("The avg num of bins is " + avgBins);

        return ret;
    }
}
