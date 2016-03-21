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

public interface IDocumentDB extends INamed, INameable {

    /**
     * @param document the document id
     * @return the name of the document
     */
    public String getDocumentName(int document);

    /**
     * @param documentName the name of the document
     * @return the document id
     */
    public int getDocument(String documentName);

    /**
     * @return the number of documents in the db
     */
    public int getDocumentsCount();

    /**
     * @return an interator on the documents id
     */
    public IIntIterator getDocuments();

    /**
     * @param document the document id
     * @return the if the id belongs to a known document
     */
    public boolean isValidDocument(int document);

    /**
     * remove some documents from the db, compacting the ids.
     *
     * @param removedDocuments an iterator on the documents to be removed
     */
    public void removeDocuments(IIntIterator removedDocuments);

    /**
     * @return a deep clone of the db
     */
    public IDocumentDB cloneDB();
}