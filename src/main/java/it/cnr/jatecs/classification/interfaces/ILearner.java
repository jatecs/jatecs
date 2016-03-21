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

import it.cnr.jatecs.indexes.DB.interfaces.IIndex;

import java.util.Vector;

public interface ILearner {

    /**
     * Build a new classifier for the data contained in the "trainingIndex" variable.
     *
     * @return An object representing the model constructed for the classifier.
     */
    public IClassifier build(IIndex trainingIndex);

    /**
     * Merge all classifiers provided in "classifiers" parameter into a single classifier. The passed classifiers
     * MUST have stored data for only one category, so the resulting classifier will contain data for n categories
     * (if the passed classsifiers are n).
     *
     * @param classifiers The list of classifiers to merge.
     * @return A merged classsifier.
     */
    public IClassifier mergeClassifiers(Vector<IClassifier> classifiers);

    /**
     * Set the rutime customizer to use at learning time. By setting a proper customizer you can tune the
     * runtime parameters of the learning algorithm.
     *
     * @param customizer The customizer to use.
     */
    public void setRuntimeCustomizer(ILearnerRuntimeCustomizer customizer);

    /**
     * Get the runtime customizer used at learning time.
     *
     * @return The runtime customizer used.
     */
    public ILearnerRuntimeCustomizer getRuntimeCustomizer();

    /**
     * Set the runtime customizer of this learner as the union of the passed customizers. Each passed customizers must have
     * data related to only one category.
     *
     * @param customizers The customizers to set.
     */
    public void setRuntimeCustomizer(Vector<ILearnerRuntimeCustomizer> customizers);

    /**
     * Get the runtime customizer only of the specified category.
     *
     * @param catID The category ID.
     * @return A runtime customizer only useful for specified category.
     */
    public ILearnerRuntimeCustomizer getRuntimeCustomizer(short catID);
}
