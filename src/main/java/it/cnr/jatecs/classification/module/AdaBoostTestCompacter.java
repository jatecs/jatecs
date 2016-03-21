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

import it.cnr.jatecs.classification.adaboost.AdaBoostClassifier;
import it.cnr.jatecs.classification.adaboost.AdaBoostIndexCompacter;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.module.JatecsModule;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.Pair;

public class AdaBoostTestCompacter extends JatecsModule {

    protected IIndex _trainingIndex, _testIndex;
    protected AdaBoostClassifier _classifier;

    protected Pair<IIndex, AdaBoostClassifier> _res;


    public AdaBoostTestCompacter(IIndex trainingIndex, IIndex testIndex, AdaBoostClassifier classifier) {
        super(null, AdaBoostTestCompacter.class.getName());

        _trainingIndex = trainingIndex;
        _testIndex = testIndex;
        _classifier = classifier;
    }

    @Override
    protected void processModule() {
        JatecsLogger.status().print("Computing compact test index and updating classification model...");
        AdaBoostIndexCompacter c = new AdaBoostIndexCompacter();
        _res = c.compactIndex(_classifier, _trainingIndex, _testIndex);
        setIndex(_res.getFirst());
        JatecsLogger.status().println("done.");
    }


    public IIndex compactIndex() {
        return _res.getFirst();
    }


    public AdaBoostClassifier compactClassifier() {
        return _res.getSecond();
    }
}
