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

package it.cnr.jatecs.indexes.DB.interfaces;

import it.cnr.jatecs.indexing.corpus.CorpusCategory;

public interface IIndexBuilder {

    /**
     * Add a document to this index.
     *
     * @param documentName The document name.
     * @param featureNames The raw string features composing the document.
     * @param categoryNames The set of categories to which this document belong to.
     * @return The new assigned document ID.
     */
    public int addDocument(String documentName, String[] featureNames,
                           CorpusCategory[] categoryNames);

    /**
     * Add a document to this index.
     *
     * @param documentName The document name.
     * @param featureNames The raw string features composing the document.
     * @param categoryNames The set of categories to which this document belong to.
     * @return The new assigned document ID.
     */
    public int addDocument(String documentName, String[] featureNames,
                           String[] categoryNames);

    /**
     * Add to an existant document a set of features and the corresponding frequencies inside that document.
     * @param docID The document ID.
     * @param featureNames The set of features to add.
     * @param occurrences The frequencies of the features inside the document.
     */
    public void addDocumentFeatures(int docID, String[] featureNames,
                                    int[] occurrences);

    /**
     * Get the resulting index.
     *
     * @return The resulting index.
     */
    public IIndex getIndex();
}