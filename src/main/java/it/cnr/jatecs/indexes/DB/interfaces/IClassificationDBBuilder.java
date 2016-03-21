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

public interface IClassificationDBBuilder {

    /**
     * Mark a document as belonging to a specific category.
     *
     * @param document The document ID.
     * @param category The category ID.
     */
    public void setDocumentCategory(int document, short category);

    /**
     * Mark a document as belonging to a specific category. If primary is true, the category is the primary
     * category for this document, otherwise the category is a secondary one for the document.
     *
     * @param document The document ID.
     * @param category The category ID.
     * @param primary Indicate if the category is primary or secondary.
     */
    public void setDocumentCategory(int document, short category, boolean primary);


    /**
     * Get the resulting classification DB.
     *
     * @return The resulting classification DB:
     */
    public IClassificationDB getClassificationDB();
}
