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

import gnu.trove.TShortArrayList;
import it.cnr.jatecs.classification.OptimalConfiguration;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;

import java.util.Vector;

public interface IClassifierOptimizer {

    /**
     * Optimize a given classifier by finding the best combination of learner and classifier runtime
     * configuration values.
     *
     * @param training   The training index to use in learning phase.
     * @param validation The validation index to use in test phase.
     * @return A pair containing the best combination of runtime configuration parameters.
     */
    public OptimalConfiguration optimizeFor(ILearner learner, IIndex training, IIndex validation, TShortArrayList validCategories);

    public void assignBestLearnerConfiguration(ILearnerRuntimeCustomizer target, TShortArrayList externalCategories, Vector<ILearnerRuntimeCustomizer> customizers, TShortArrayList internalCategories);

    public void assignBestClassifierConfiguration(IClassifierRuntimeCustomizer target, TShortArrayList externalCategories, Vector<IClassifierRuntimeCustomizer> customizers, TShortArrayList internalCategories);
}
