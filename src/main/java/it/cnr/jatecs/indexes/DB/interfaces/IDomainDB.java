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

public interface IDomainDB extends INamed, INameable {

    /**
     * @return the categories db to which this db refers
     */
    public ICategoryDB getCategoryDB();

    /**
     * @return the features db to which this db refers
     */
    public IFeatureDB getFeatureDB();

    /**
     * @return true if the db uses a local representation
     */
    public boolean hasLocalRepresentation();

    /**
     * @param category the category id
     * @return the number of features valid for the category
     */
    public int getCategoryFeaturesCount(short category);

    /**
     * @param category the category id
     * @returnan iterator on the valid features for the category
     */
    public IIntIterator getCategoryFeatures(short category);

    /**
     * @param category the category id
     * @param feature  the feature id
     * @return the if the feature is valid for the category
     */
    public boolean hasCategoryFeature(short category, int feature);

    /**
     * remove some features from the db, compacting the ids.
     * ATTENTION: this method has no effects on the features db, always call its
     * removeFeatures method after this to keep the two dbs aligned
     *
     * @param removedFeatures an iterator on the features to be removed
     *                       them sparse
     */
    public void removeFeatures(IIntIterator removedFeatures);

    /**
     * remove some features only for a category
     *
     * @param category          the category id
     * @param removedFeatures an iterator on the features to be removed
     */
    public void removeCategoryFeatures(short category, IIntIterator removedFeatures);

    /**
     * remove some categories from the db, compacting the ids.
     * ATTENTION: this method has no effects on the categories db, always call its
     * removedCategories method after this to keep the two dbs aligned
     *
     * @param removedCategories an iterator on the categories to be removed
     */
    public void removeCategories(IShortIterator removedCategories);

    /**
     * @param categoriesDB the categories db to be linked by the clone
     * @param featuresDB   the features db to be linked by the clone
     * @return a deep clone of the db
     */
    public IDomainDB cloneDB(ICategoryDB categoriesDB, IFeatureDB featuresDB);
}