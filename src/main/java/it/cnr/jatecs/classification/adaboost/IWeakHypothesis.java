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

package it.cnr.jatecs.classification.adaboost;

public interface IWeakHypothesis {

    /**
     * Get the critical boosting data for the specified category ID.
     *
     * @param catID The category ID in boosting internal representation.
     * @return The weak hypothesis data values for the specified category ID.
     */
    public HypothesisData value(short catID);

    /**
     * Set the critical boosting values for the specified category ID.
     *
     * @param catID The category ID specified as boosting internal reprsentation.
     * @param d     The boosting values for specified category ID.
     */
    public void setValue(short catID, HypothesisData d);

    /**
     * Get the number of classifiers stored in the hypothesis.
     *
     * @return The number of classifiers stored in the model.
     */
    public int getClassifiersCount();

}
