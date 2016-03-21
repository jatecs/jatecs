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
import it.cnr.jatecs.classification.ClassificationScoreDB;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;

/**
 * A PoolRank is the abstraction of a class that builds a rank of documents in
 * an unlabeled set with respect an active learning method.
 *
 * @author giacomo
 */
public abstract class PoolRank {

    /**
     * Generate a ranking of the unlabelled documents in confidenceUnlabelled
     *
     * @param confidenceUnlabelled Classification of unlabelled examples
     * @param trainingSet          current training set of the classifier
     */
    public PoolRank(ClassificationScoreDB confidenceUnlabelled,
                    IIndex trainingSet) {
    }

    /**
     * Return the documents IDs in the unlabelled examples for retraining and
     * evaluating in terms of macro F1
     *
     * @param n Select the first n
     * @return Ordered document IDs
     */
    abstract public TIntArrayList getFirstMacro(int n);

    /**
     * Return the documents IDs in the unlabelled examples for retraining and
     * evaluating in terms of micro F1
     *
     * @param n Select the first n
     * @return Ordered document IDs
     */
    abstract public TIntArrayList getFirstMicro(int n);

}
