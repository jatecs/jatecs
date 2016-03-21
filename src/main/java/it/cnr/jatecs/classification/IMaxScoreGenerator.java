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

package it.cnr.jatecs.classification;

/**
 * A max score generator for a set of specified categories.
 *
 * @author Tiziano Fagni
 */
public interface IMaxScoreGenerator {

    /**
     * Get the maximum score (the score minus the margin of a category classifier)
     * obtainable from a positive classification.
     *
     * @param cl    The classification results.
     * @param catID The category ID.
     * @return The maximum positive score for the specified category ID.
     * @throws NullPointerException     Raised if the specified classification results
     *                                  instance is 'null'.
     * @throws IllegalArgumentException Raised if the specified category ID is invalid.
     */
    public double getMaximumPositiveScore(ClassificationScoreDB cl, short catID);


    /**
     * Get the minimum score (the score minus the margin of a category classifier)
     * obtainable from a negative classification.
     *
     * @param cl    The classification results.
     * @param catID The category ID.
     * @return The maximum positive score for the specified category ID.
     * @throws NullPointerException     Raised if the specified classification results
     *                                  instance is 'null'.
     * @throws IllegalArgumentException Raised if the specified category ID is invalid.
     */
    public double getMaximumNegativeScore(ClassificationScoreDB cl, short catID);
}
