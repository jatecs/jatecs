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

public interface IWeightingDB extends INamed, INameable {

    /**
     * @return the content db to which this db refers
     */
    public IContentDB getContentDB();

    /**
     * @return the default weight for a feature appearing in a document
     */
    public double getDefaultWeight();

    /**
     * @return the weight for a feature not appearing in a document
     */
    public double getNoWeight();

    /**
     * @param document the document id
     * @param feature  the feature id
     * @return the weight of the feature in the document
     */
    public double getDocumentFeatureWeight(int document, int feature);

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
     * @param removedFeaturess an iterator on the features to be removed
     */
    public void removeFeatures(IIntIterator removedFeaturess);

    /**
     * @param contentDB the content db to be linked by the clone
     * @return a deep clone of the db
     */
    public IWeightingDB cloneDB(IContentDB contentDB);

}