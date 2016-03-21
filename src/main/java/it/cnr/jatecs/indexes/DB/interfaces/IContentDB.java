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

import it.cnr.jatecs.utils.interfaces.INameable;
import it.cnr.jatecs.utils.interfaces.INamed;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;

public interface IContentDB extends INamed, INameable {

    /**
     * @return the features db to which this db refers
     */
    public IFeatureDB getFeatureDB();

    /**
     * @return the documents db to which this db refers
     */
    public IDocumentDB getDocumentDB();

    /**
     * @param document the document id
     * @return the length of the document
     */
    public int getDocumentLength(int document);

    /**
     * @param document the document id
     * @return the number of features that compose the document
     */
    public int getDocumentFeaturesCount(int document);

    /**
     * @param document the document id
     * @return an iterator on the features composing the document
     */
    public IIntIterator getDocumentFeatures(int document);

    /**
     * @param feature the feature id
     * @return the number of documents that contain the feature
     */
    public int getFeatureDocumentsCount(int feature);

    /**
     * @param feature the feature id
     * @return an iterator on the documents that contain the feature
     */
    public IIntIterator getFeatureDocuments(int feature);

    /**
     * @param document the document id
     * @param feature  the feature id
     * @return true if the document contains the feature
     */
    public boolean hasDocumentFeature(int document, int feature);

    /**
     * @return an iterator on the features that do not appear in any document
     */
    public IIntIterator getUnusedFeatures();

    /**
     * @param document the document id
     * @param feature  the feature id
     * @return the frequency of the feature in the document
     */
    public int getDocumentFeatureFrequency(int document, int feature);

    /**
     * remove some documents from the db, compacting the ids. ATTENTION: this
     * method has no effects on the documents db, always call its
     * removeDocuments method after this to keep the two dbs aligned
     *
     * @param removedDocuments an iterator on the documents to be removed
     */
    public void removeDocuments(IIntIterator removedDocuments);

    /**
     * remove some features from the db, compacting the ids. ATTENTION: this
     * method has no effects on the features db, always call its removeFeatures
     * method after this to keep the two dbs aligned
     *
     * @param removedFeatures an iterator on the features to be removed
     */
    public void removeFeatures(IIntIterator removedFeatures);

    /**
     * @param docDB  the documents db to be linked by the clone
     * @param featDB the features db to be linked by the clone
     * @return a deep clone of the db
     */
    public IContentDB cloneDB(IDocumentDB docDB, IFeatureDB featDB);

}