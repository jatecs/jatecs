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

package it.cnr.jatecs.classification.logistic_regression;

import it.cnr.jatecs.classification.interfaces.ILearnerRuntimeCustomizer;

public class ModRegularizedLogisticRegressionLearnerCustomizer implements
        ILearnerRuntimeCustomizer {
    /**
     * The number of iterations.
     */
    public int K = 100;

    /**
     * Tolerance parameter. For text classification problems a proper value
     * should be 0.1.
     */
    public double epsilon = 0.1;

    /**
     * THe regularization parameter. Usually computed with k-fold optimization.
     * The default value (0.001) should be appropriate in most situations for
     * this type of algorithm.
     */
    public double lambda = 0.001;

    public double getCK(int numIterations, int currentIteration) {
        double r = (double) numIterations / 50.0;
        return Math.max(0, 1 - r);
    }

    public ILearnerRuntimeCustomizer cloneObject() {
        return null;
    }

}
