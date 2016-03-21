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

package it.cnr.jatecs.classification.treeboost;

import gnu.trove.TIntArrayList;
import gnu.trove.TShortIntHashMap;
import it.cnr.jatecs.clustering.ClusterDocumentDescriptor;
import it.cnr.jatecs.clustering.interfaces.IDocumentClusterizer;
import it.cnr.jatecs.clustering.kmeans.KMeansDocumentClusterizer;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.iterators.TIntArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.util.Vector;

public class ClusteringNegativesChooser implements INegativesChooser {

    protected IDocumentClusterizer _clusterizer;


    /**
     * Given a category, indicate the minimum number of positives contained in a cluster to consider the
     * cluster valid for the considered category.
     */
    protected int _minimumPositivesPerCluster;


    protected Vector<ClusterDocumentDescriptor> _clusters;

    protected TIntArrayList[] _catsClusters;

    protected IIndex _index;


    public ClusteringNegativesChooser() {
        _clusterizer = new KMeansDocumentClusterizer();
        _minimumPositivesPerCluster = 1;
        _clusters = null;
        _catsClusters = null;
        _index = null;
    }


    public void initialize(IIndex index) {
        _index = index;

        // Get clusters from index.
        _clusters = _clusterizer.clusterize(index);

        _catsClusters = new TIntArrayList[index.getCategoryDB().getCategoriesCount()];
        for (int i = 0; i < _catsClusters.length; i++)
            _catsClusters[i] = new TIntArrayList();

        for (int i = 0; i < _clusters.size(); i++) {
            ClusterDocumentDescriptor cd = _clusters.get(i);

            TShortIntHashMap map = new TShortIntHashMap();

            // DEBUG
            System.out.println("The cluster " + i + " contains " + cd.documents.size() + " documents.");

            IIntIterator it = new TIntArrayListIterator(cd.documents);
            while (it.hasNext()) {
                int docID = it.next();

                IShortIterator cats = index.getClassificationDB().getDocumentCategories(docID);
                while (cats.hasNext()) {
                    short catID = cats.next();
                    if (map.containsKey(catID))
                        map.put(catID, map.get(catID) + 1);
                    else
                        map.put(catID, 1);
                }
            }

            short[] keys = map.keys();
            for (int j = 0; j < keys.length; j++) {
                int numPositives = map.get(keys[j]);
                if (numPositives < _minimumPositivesPerCluster)
                    continue;

                // This cluster is a "good" cluster. Save it.
                _catsClusters[keys[j]].add(i);
            }

        }

    }


    public void release() {
        _catsClusters = null;
        _clusters = null;
        _index = null;
    }


    public TIntArrayListIterator selectNegatives(String category) {
        short catID = _index.getCategoryDB().getCategory(category);

        TIntArrayList negatives = new TIntArrayList();

        for (int i = 0; i < _catsClusters[catID].size(); i++) {
            int cluster = _catsClusters[catID].get(i);

            ClusterDocumentDescriptor cd = _clusters.get(cluster);
            IIntIterator it = new TIntArrayListIterator(cd.documents);
            while (it.hasNext()) {
                int docID = it.next();
                if (_index.getClassificationDB().hasDocumentCategory(docID, catID))
                    // Positive document. Skip it.
                    continue;

                negatives.add(docID);
            }
        }

        return new TIntArrayListIterator(negatives);
    }

}
