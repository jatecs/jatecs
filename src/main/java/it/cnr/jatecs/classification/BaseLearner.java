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

import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.interfaces.ILearner;
import it.cnr.jatecs.classification.interfaces.ILearnerRuntimeCustomizer;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;

import java.util.Vector;

public abstract class BaseLearner implements ILearner {

    protected ILearnerRuntimeCustomizer _customizer = null;

    public abstract IClassifier mergeClassifiers(Vector<IClassifier> classifiers);

    public abstract IClassifier build(IIndex trainingIndex);

    public ILearnerRuntimeCustomizer getRuntimeCustomizer() {
        return _customizer;
    }

    public void setRuntimeCustomizer(ILearnerRuntimeCustomizer customizer) {
        _customizer = customizer;
    }

    public abstract void setRuntimeCustomizer(
            Vector<ILearnerRuntimeCustomizer> customizers);

    public abstract ILearnerRuntimeCustomizer getRuntimeCustomizer(short catID);

}
