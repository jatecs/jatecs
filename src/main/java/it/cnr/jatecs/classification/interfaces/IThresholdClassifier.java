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

package it.cnr.jatecs.classification.interfaces;

import it.cnr.jatecs.classification.ClassifierRange;

public interface IThresholdClassifier {

    /**
     * Set the valid range values to apply to a determined category ID.
     *
     * @param classifier The classifier to tune.
     * @param catID      The category ID.
     * @param range      The values which determine the valid interval values for
     *                   category "catID".
     */
    public void setClassifierRange(short catID, ClassifierRange range);

    /**
     * Get the classifier limits to consider for a given category ID when try to
     * understand how a document was classified by that particular classifier.
     *
     * @param catID The category ID which indicate the binary classifier to query.
     * @return An object representing the range limits useful for judging a
     * document classfication task givne by this classifier.
     */
    public ClassifierRange getClassifierRange(short catID);

    /**
     * Get the number of classifiers stored in the model.
     *
     * @return The number of classifiers stored in the model.
     */
    public int getClassifiersCount();

    public void reserveMemoryFor(int numProfiles);
}
