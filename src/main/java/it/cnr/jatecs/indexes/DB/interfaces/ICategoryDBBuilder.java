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

public interface ICategoryDBBuilder {

    /**
     * Set the category names to be stored in the destination category DB.
     *
     * @param categoryNames The category names to be set.
     */
    public void setCategories(String[] categoryNames);

    /**
     * Set a hierarchical relation parent/child for a couple of distinct category names.
     *
     * @param childName  The child category name.
     * @param parentName The parent category name.
     */
    public void setParentCategory(String childName, String parentName);

    /**
     * Get the resulting category DB built from the set of operations specified by {@link #setCategories(String[])}
     * and {@link #setParentCategory(String, String)} methods calls.
     *
     * @return The built category DB.
     */
    public ICategoryDB getCategoryDB();

}