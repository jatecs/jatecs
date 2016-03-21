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

import it.cnr.jatecs.classification.ClassificationResult;
import it.cnr.jatecs.classification.ClassifierRange;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

public interface IClassifier {

    /**
     * Classify the given document ID over the set of stored categories in the
     * classifier. These can be less than the total number of categories stored
     * in the model (i.e. in a hierarchical context, some subtrees of categories
     * can be cut due to filter action of root category of subtree).
     *
     * @param testIndex The index used to access the representation of the given
     *                  document.
     * @param document  The document ID to classify.
     * @return An object representing the results of classification.
     */
    public ClassificationResult classify(IIndex testIndex, int document);

    /**
     * Classify all the documents in the index respect to specified category.
     *
     * @param testIndex The index used to access the test documents.
     * @param category  The category ID which the documents must be classified.
     * @return A set of classification results, one for each document tested.
     */
    public ClassificationResult[] classify(IIndex testIndex, short category);

    /**
     * Get the classifier limits to consider for a given category ID when try to
     * understand how a document was classified by that particular classifier.
     *
     * @param category The category ID which indicate the binary classifier to query.
     * @return An object representing the range limits useful for judging a
     * document classification task givne by this classifier.
     */
    public ClassifierRange getClassifierRange(short category);

    /**
     * Get the number of categories covered by the classifier.
     *
     * @return The number of categories covered by the classifier.
     */
    public int getCategoryCount();

    /**
     * Get the list of valid categories for the classification
     *
     * @return The list of valid categories.
     */
    public IShortIterator getCategories();

    /**
     * Get the runtime customizer used at classification time.
     *
     * @return The runtime customizer used.
     */
    public IClassifierRuntimeCustomizer getRuntimeCustomizer();

    /**
     * Set the runtime customizer to use at classification time. By setting a
     * proper customizer you can tune the runtime parameters of the algorithm.
     *
     * @param customizer The customizer to use.
     */
    public void setRuntimeCustomizer(IClassifierRuntimeCustomizer customizer);

    /**
     * Use this method to release any resource used by classifier.
     */
    public void destroy();
}
