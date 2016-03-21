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

public interface IFeatureDB extends INamed, INameable {

    /**
     * @param feature the feature id
     * @return the feature name
     */
    public String getFeatureName(int feature);

    /**
     * @param featureName the feature name
     * @return the feature id
     */
    public int getFeature(String featureName);

    /**
     * @return the number of features in the db
     */
    public int getFeaturesCount();

    /**
     * @return an iterator on the features contained in the db
     */
    public IIntIterator getFeatures();

    /**
     * @param feature the feature id
     * @return true if the feature is in the db
     */
    public boolean isValidFeature(int feature);

    /**
     * remove some features from the db
     *
     * @param removedFeatures an iterator on the features to be removed
     */
    public void removeFeatures(IIntIterator removedFeatures);

    /**
     * @return a deep clone of the db
     */
    public IFeatureDB cloneDB();

}