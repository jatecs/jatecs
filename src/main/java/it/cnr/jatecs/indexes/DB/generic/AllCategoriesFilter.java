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

package it.cnr.jatecs.indexes.DB.generic;

import it.cnr.jatecs.indexes.DB.interfaces.ICategoryDB;
import it.cnr.jatecs.indexes.DB.interfaces.ICategoryFilter;

/**
 * A category filter which can be used to get all categories sets as
 * specified in {@link CategorySetType} enumeration.
 *
 * @author Tiziano Fagni
 */
public class AllCategoriesFilter implements ICategoryFilter {

    private CategorySetType categorySetType;

    /**
     * Build a new instance with the categories set type set to
     * {@link CategorySetType#ALL_CATEGORIES}.
     */
    public AllCategoriesFilter() {
        categorySetType = CategorySetType.ALL_CATEGORIES;
    }


    /**
     * Get the category set used.
     *
     * @return The category set used.
     */
    public CategorySetType getCategorySetType() {
        return categorySetType;
    }


    /**
     * Set the category set type.
     *
     * @param categorySetType The category set type.
     * @throws NullPointerException Raised if the specified category set is 'null'.
     */
    public void setCategorySetType(CategorySetType categorySetType) {

        if (categorySetType == null)
            throw new NullPointerException("The specified category set is 'null'");

        this.categorySetType = categorySetType;
    }


    @Override
    public boolean isAvailable(ICategoryDB catsDB, short catID) {
        if (catsDB == null)
            throw new NullPointerException("The specified categories DB is 'null'");

        if (!catsDB.isValidCategory(catID))
            throw new IllegalArgumentException("The catID value is invalid: " + catID);

        switch (categorySetType) {
            case ALL_CATEGORIES:
                return true;
            case INTERNAL_CATEGORIES:
                return isInternalCategory(catsDB, catID);
            case LEAFS_CATEGORIES:
                return isLeafCategory(catsDB, catID);
            default:
                throw new IllegalStateException("Unknown state");
        }

    }


    private boolean isLeafCategory(ICategoryDB catsDB, short catID) {
        int cont = catsDB.getChildCategoriesCount(catID);
        return cont == 0;
    }


    private boolean isInternalCategory(ICategoryDB catsDB, short catID) {
        int cont = catsDB.getChildCategoriesCount(catID);
        return cont != 0;
    }

}
