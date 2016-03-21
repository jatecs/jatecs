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

import gnu.trove.TShortHashSet;
import it.cnr.jatecs.utils.interfaces.INameable;
import it.cnr.jatecs.utils.interfaces.INamed;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

public interface IIndex extends INamed, INameable {

    /**
     * @return the features db to which this db refers
     */
    public IFeatureDB getFeatureDB();

    /**
     * @return the documents db to which this db refers
     */
    public IDocumentDB getDocumentDB();

    /**
     * @return the categories db to which this db refers
     */
    public ICategoryDB getCategoryDB();

    /**
     * remove some features from the db
     *
     * @param removedFeatures an iterator on the features to be removed
     */
    public void removeFeatures(IIntIterator removedFeatures);

    /**
     * remove some documents from the db, EVENTUALLY compacting the features ids.
     * ATTENTION: this method HAS effects on ALL the dbs,
     *
     * @param removedDocuments   an iterator on the documents to be removed
     * @param compressFeaturesId true to compact the features id, false to keep
     *                           them sparse
     */
    public void removeDocuments(IIntIterator removedDocuments, boolean compressFeaturesId);

    /**
     * remove some categories from the db, compacting the ids.
     * ATTENTION: this method HAS effects on ALL the dbs, and categories have to be sorted
     *
     * @param removedCategories an iterator on the categories (sorted) to be removed
     */
    public void removeCategories(IShortIterator removedCategories);

    /**
     * @return the domain db to which this db refers
     */
    public IDomainDB getDomainDB();

    /**
     * @return the content db to which this db refers
     */
    public IContentDB getContentDB();

    /**
     * @return the weighting db to which this db refers
     */
    public IWeightingDB getWeightingDB();

    /**
     * @return the classification db to which this db refers
     */
    public IClassificationDB getClassificationDB();

    /**
     * @param document the document id
     * @param category the category id
     * @return the length of the document, considering the eventual local representation
     * for the category
     */
    public int getDocumentLength(int document, short category);

    /**
     * @param document the document id
     * @param category the category id
     * @return the number of features that compose the document, considering the eventual
     * local representation for the category
     */
    public int getDocumentFeaturesCount(int document, short category);

    /**
     * @param document the document id
     * @param category the category id
     * @return an iterator on the features composing the document, considering the eventual
     * local representation for the category
     */
    public IIntIterator getDocumentFeatures(int document, short category);

    /**
     * @param document the document id
     * @param feature  the feature id
     * @param category the category id
     * @return true if the document contains the feature, considering the eventual
     * local representation for the category
     */
    public boolean hasDocumentFeature(int document, int feature, short category);

    /**
     * @param document the document id
     * @param feature  the feature id
     * @param category the category id
     * @return the frequency of the feature in the document, considering the eventual
     * local representation for the category
     */
    public int getDocumentFeatureFrequency(int document, int feature, short category);

    public double getDocumentFeatureWeight(int document, int feature, short category);

    /**
     * @param feature  the feature id
     * @param category the category id
     * @return the number of documents that contain the feature, considering the eventual
     * local representation for the category
     */
    public int getFeatureDocumentsCount(int feature, short category);

    /**
     * @param feature  the feature id
     * @param category the category id
     * @return an iterator on the documents that contain the feature, considering the eventual
     * local representation for the category
     */
    public IIntIterator getFeatureDocuments(int feature, short category);

    /**
     * Get the number of documents which share both the given feature ID and the given category ID.
     *
     * @param featureID  The feature ID.
     * @param categoryID The category ID.
     * @return The number of documents which share feature and category ID.
     */
    public int getFeatureCategoryDocumentsCount(int featureID, short categoryID);

    /**
     * Get the list of documents which share both the given feature ID and the given category IDs.
     *
     * @param featureID  The feature ID.
     * @param categoryID The category ID.
     * @return The list of documents which share the given feature and category IDs.
     */
    public IIntIterator getFeatureCategoryDocuments(int featureID, short categoryID);


    /**
     * @return a deep clone of the db, all linked db are deep cloned too
     */
    public IIndex cloneIndex();

    /**
     * Remove categories that do not have positive examples in the index
     *
     * @return The category IDs of the removed categories
     */
    public TShortHashSet cleanCategories();
}
