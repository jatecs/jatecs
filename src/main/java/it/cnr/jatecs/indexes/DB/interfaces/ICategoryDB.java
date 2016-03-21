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
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

public interface ICategoryDB extends INamed, INameable {

    /**
     * @param category the category id
     * @return the category name given its id
     */
    public String getCategoryName(short category);

    /**
     * @param categoryName the category name
     * @return the category id given its name
     */
    public short getCategory(String categoryName);

    /**
     * @return the number of categories
     */
    public int getCategoriesCount();

    /**
     * @return an iterator on the categories id
     */
    public IShortIterator getCategories();

    /**
     * @param category the category id
     * @return true if the id is related to a know category
     */
    public boolean isValidCategory(short category);

    /**
     * @param category the category id
     * @return an iterator on the parents of the given category
     */
    public IShortIterator getParentCategories(short category);

    /**
     * @param category the category id
     * @return an iterator on the children of the given category
     */
    public IShortIterator getChildCategories(short category);

    /**
     * @param category the category id
     * @return the number of the childs of the given category
     */
    public int getChildCategoriesCount(short category);

    /**
     * @param category the category id
     * @return true if category has childs (non-leaf category)
     */
    public boolean hasChildCategories(short category);

    /**
     * @param category the category id
     * @return an iterator on the siblings of the given category
     */
    public IShortIterator getSiblingCategories(short category);

    /**
     * @return an iterator on the categories that have no parents
     */
    public IShortIterator getRootCategories();

    /**
     * remove some categories from the db, compacting the ids.
     *
     * @param removedCategories an iterator on the categories to be removed
     */
    public void removeCategories(IShortIterator removedCategories);

    /**
     * @return a deep clone of the db
     */
    public ICategoryDB cloneDB();

}