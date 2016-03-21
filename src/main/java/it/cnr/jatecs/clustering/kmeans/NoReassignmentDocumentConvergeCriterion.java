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

package it.cnr.jatecs.clustering.kmeans;

import gnu.trove.TIntArrayList;
import it.cnr.jatecs.clustering.DocumentCentroid;
import it.cnr.jatecs.clustering.utils.Clustering;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.iterators.TIntArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;

public class NoReassignmentDocumentConvergeCriterion implements IDocumentConvergeCriterion {


    protected IIndex _index;
    protected IIntIterator _documents;
    TIntArrayList _assignments;


    public NoReassignmentDocumentConvergeCriterion() {
        _assignments = new TIntArrayList();
    }


    public void beginEvaluation(IIntIterator documents, IIndex index) {
        _assignments.clear();
        documents.begin();
        while (documents.hasNext()) {
            documents.next();
            _assignments.add(-1);
        }

        _documents = documents;
        _index = index;
    }

    public boolean isConverging(DocumentCentroid[] centroids) {

        boolean reassigned = false;

        for (int i = 0; i < centroids.length; i++) {
            DocumentCentroid c = centroids[i];
            for (int j = 0; j < c.documents.size(); j++) {
                int docID = c.documents.get(j);
                if (_assignments.get(docID) != i) {
                    // At least one document was reassigned.
                    reassigned = true;

                    _assignments.set(docID, i);
                }
            }

            c.features = Clustering.computeDocumentCentroid(new TIntArrayListIterator(c.documents), _index);
        }

        if (!reassigned)
            // Convergence was reached.
            return true;
        else
            // More iterations needed.
            return false;
    }

}
