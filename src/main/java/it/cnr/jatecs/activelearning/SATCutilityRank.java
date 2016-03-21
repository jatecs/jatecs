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

package it.cnr.jatecs.activelearning;

import gnu.trove.TIntArrayList;
import gnu.trove.TIntHashSet;
import it.cnr.jatecs.classification.ClassificationScoreDB;
import it.cnr.jatecs.evaluation.ContingencyTableSet;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.satc.interfaces.IGain;
import it.cnr.jatecs.satc.rank.UtilityBased;

/**
 * A ranking method that uses the utility theory-based semi-automated text
 * classification strategy.
 * <p>
 * "A utility-theoretic ranking method for semi-automated text classification"
 * Giacomo Berardi, Andrea Esuli, Fabrizio Sebastiani
 * SIGIR 2012
 *
 * @author giacomo
 */
public class SATCutilityRank extends PoolRank {

    private UtilityBased rankObject;

    public SATCutilityRank(ClassificationScoreDB confidenceUnlabelled,
                           IIndex trainingSet, ContingencyTableSet estimatedEvaluation,
                           TIntHashSet categoriesFilter, IGain gain, double[] probabilitySlopes) {
        super(confidenceUnlabelled, trainingSet);
        rankObject = new UtilityBased(trainingSet.getDocumentDB()
                .getDocumentsCount(), confidenceUnlabelled, categoriesFilter,
                UtilityBased.EstimationType.TEST, estimatedEvaluation, gain,
                probabilitySlopes, new double[0]);
    }

    @Override
    public TIntArrayList getFirstMacro(int n) {
        return rankObject.getMacroRank().subList(0, n);
    }

    @Override
    public TIntArrayList getFirstMicro(int n) {
        return rankObject.getMicroRank().subList(0, n);
    }

}
