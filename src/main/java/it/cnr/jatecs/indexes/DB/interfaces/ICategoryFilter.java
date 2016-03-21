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

/**
 * Used to filter in some way the set of categories available in a DB.
 *
 * @author Tiziano Fagni
 */
public interface ICategoryFilter {

    /**
     * Indicates if the specified category ID is available for processing
     * in the given categories DB.
     *
     * @param catsDB The categories DB.
     * @param catID  The categoryID to check.
     * @return True if the category is available for processing, false otherwise.
     * @throws IllegalArgumentException Raised if the specified category ID is invalid.
     * @throws NullPointerException     Raised if the specified categories DB is 'null'.
     */
    public boolean isAvailable(ICategoryDB catsDB, short catID);
}
