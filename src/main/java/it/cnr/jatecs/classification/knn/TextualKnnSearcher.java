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

package it.cnr.jatecs.classification.knn;

import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexing.similarity.EuclideanDistance;
import it.cnr.jatecs.indexing.similarity.IBaseSimilarityFunction;
import it.cnr.jatecs.indexing.similarity.ISimilarityFunction;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;

import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

public class TextualKnnSearcher implements IKnnSearcher {

    protected boolean _sameIndexes;
    protected ISimilarityFunction _similarity;
    protected float[][] _matrixSimilarity;

    public TextualKnnSearcher() {
        _similarity = new EuclideanDistance();
        _sameIndexes = false;
        _matrixSimilarity = null;
    }

    public Vector<SimilarDocument> search(IIndex testIndex, int docID, IIndex trainingIndex, int numSimilar) {
        Vector<SimilarDocument> docs = new Vector<SimilarDocument>();

        TreeSet<SimilarDocument> sorted = new TreeSet<SimilarDocument>(new SimilarDocumentComparator(_similarity));

        IIntIterator it = trainingIndex.getDocumentDB().getDocuments();
        while (it.hasNext()) {
            int d = it.next();
            SimilarDocument sd = new SimilarDocument();
            sd.docID = d;

            if (_matrixSimilarity != null)
                sd.score = _matrixSimilarity[d][docID];
            else
                sd.score = _similarity.compute(docID, testIndex, d, trainingIndex);

            sorted.add(sd);
            if (sorted.size() > numSimilar)
                sorted.remove(sorted.first());
        }

        Iterator<SimilarDocument> itSim = sorted.iterator();
        while (itSim.hasNext()) {
            SimilarDocument sd = itSim.next();
            docs.add(sd);
        }

        return docs;
    }

    public void setUseSameIndexesData(boolean sameIndexesData) {
        _sameIndexes = sameIndexesData;
    }

    public boolean useSameIndexesData() {
        return _sameIndexes;
    }

    public void setSimilarityMatrix(float[][] matrix) {
        _matrixSimilarity = matrix;
    }

    public IBaseSimilarityFunction getSimilarityFunction() {
        return _similarity;
    }

    public void setSimilarityFunction(ISimilarityFunction func) {
        _similarity = func;
    }

}
