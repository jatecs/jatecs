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
import it.cnr.jatecs.indexing.similarity.IBaseSimilarityFunction;

import java.util.Vector;

public interface IKnnSearcher {

    /**
     * Compute the most "numSimilar" documents to document "docID". The information of "docID" can be retrieved
     * from "testIndex", while the most similar documents must be searched with "trainingIndex".
     *
     * @param testIndex     The index of the document to be matched.
     * @param docID         The ID of the document to be matched.
     * @param trainingIndex The index containing documents to be searched to match "docID".
     * @param numSimilar    The number of similar documents to retrieve.
     * @return A vector containing the most "numSimilar" similar documents of "docID" .
     */
    public Vector<SimilarDocument> search(IIndex testIndex, int docID, IIndex trainingIndex, int numSimilar);


    /**
     * Get the similarity function used by this searcher.
     *
     * @return The similarity function usd by this searcher.
     */
    public IBaseSimilarityFunction getSimilarityFunction();


    /**
     * Indicate if the indexes used in {@link #search(IIndex, int, IIndex, int)} method contain or not the
     * same data.
     *
     * @return True if the indexes used in {@link #search(IIndex, int, IIndex, int)} contain the same data,
     * false otherwise.
     */
    public boolean useSameIndexesData();


    /**
     * Set the property which says if the indexes used in {@link #search(IIndex, int, IIndex, int)} method contain the
     * same data.
     *
     * @param sameIndexesData True if the indexes used in {@link #search(IIndex, int, IIndex, int)} method contain the
     *                        same data, false otherwise.
     */
    public void setUseSameIndexesData(boolean sameIndexesData);
}
