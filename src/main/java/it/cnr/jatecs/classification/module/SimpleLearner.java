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

package it.cnr.jatecs.classification.module;

import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.interfaces.ILearner;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.module.JatecsModule;
import it.cnr.jatecs.utils.JatecsLogger;

public class SimpleLearner extends JatecsModule {


    protected ILearner _learner;


    protected IClassifier _classifier;


    /**
     * Construct a new classifier object for the specified learner. The classification model
     * will be constructed considering the passed training index.
     *
     * @param training    The training index.
     * @param learner     The learner object to use in the construction of classifier.
     * @param dataManager The object that must write the model on disk.
     */
    public SimpleLearner(IIndex training, ILearner learner) {
        super(training, SimpleLearner.class.getName());

        assert (learner != null);
        _learner = learner;
    }


    @Override
    protected void processModule() {
        JatecsLogger.status().println("Start constructing the learning model of " + _learner.getClass().getName() + " for " + index().getDocumentDB().getDocumentsCount() + " training document(s).");
        _classifier = _learner.build(index());
    }


    public IClassifier lastBuildedClassifier() {
        return _classifier;
    }
}
