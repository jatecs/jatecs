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

package it.cnr.jatecs.classification.knn;

import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.interfaces.ILearner;
import it.cnr.jatecs.classification.interfaces.ILearnerRuntimeCustomizer;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;

import java.util.Vector;

public class KnnCommitteeFakeLearner implements ILearner {

    protected KnnCommitteeClassifier _cl;

    public KnnCommitteeFakeLearner(KnnCommitteeClassifier cl) {
        _cl = cl;
    }

    public KnnCommitteeClassifier getKnnCommitteeClassifier() {
        return _cl;
    }

    public IClassifier build(IIndex trainingIndex) {
        for (int i = 0; i < _cl._classifiers.size(); i++)
            _cl._classifiers.get(i)._training = trainingIndex;

        return _cl;
    }

    public ILearnerRuntimeCustomizer getRuntimeCustomizer() {
        return null;
    }

    public void setRuntimeCustomizer(
            Vector<ILearnerRuntimeCustomizer> customizers) {
        throw new RuntimeException("Method not supported");
    }

    public ILearnerRuntimeCustomizer getRuntimeCustomizer(short catID) {
        throw new RuntimeException("Method not supported");
    }

    public IClassifier mergeClassifiers(Vector<IClassifier> classifiers) {
        throw new RuntimeException("Method not supported");
    }

    public void setRuntimeCustomizer(ILearnerRuntimeCustomizer customizer) {
    }

}
