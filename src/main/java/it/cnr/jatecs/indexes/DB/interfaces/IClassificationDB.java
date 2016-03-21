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
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

public interface IClassificationDB extends INamed, INameable {

    /**
     * @return the categories db to which this db refers
     */
    public ICategoryDB getCategoryDB();

    /**
     * @return the documents db to which this db refers
     */
    public IDocumentDB getDocumentDB();

    /**
     * @param document the document id
     * @return the number of categories the given document belongs to
     */
    public int getDocumentCategoriesCount(int document);

    /**
     * @param document the document id
     * @return an iterator on the categories the given document belongs to
     */
    public IShortIterator getDocumentCategories(int document);

    /**
     * @param document the document id
     * @param category the category id
     * @return true if the document belongs to the category
     */
    public boolean hasDocumentCategory(int document, short category);

    /**
     * @param category the category id
     * @return the number of documents assigned to the given category
     */
    public int getCategoryDocumentsCount(short category);

    /**
     * @param category the category id
     * @return an iterator on the documents assigned to the given category
     */
    public IIntIterator getCategoryDocuments(short category);

    /**
     * remove some categories from the db, compacting the ids. ATTENTION: this
     * method has no effects on the categories db, always call its
     * removedCategories method after this to keep the two dbs aligned
     *
     * @param removedCategories an iterator on the categories to be removed
     */
    public void removeCategories(IShortIterator removedCategories);

    /**
     * remove some documents from the db, compacting the ids. ATTENTION: this
     * method has no effects on the documents db, always call its
     * removeDocuments method after this to keep the two dbs aligned
     *
     * @param removedDocuments an iterator on the documents to be removed
     */
    public void removeDocuments(IIntIterator removedDocuments);

    /**
     * @param categoriesDB the categories db to be linked by the clone
     * @param documentsDB  the documents db to be linked by the clone
     * @return a deep clone of the db
     */
    public IClassificationDB cloneDB(ICategoryDB categoriesDB,
                                     IDocumentDB documentsDB);


    /**
     * Indicate if the specified category is primary for given document. If the document is not positive for
     * the specified category, the method will return false.
     *
     * @param document The document to check.
     * @param category The category to check.
     * @return True if the category is primary, false otherwise.
     */
    public boolean isPrimaryCategory(int document, short category);
}