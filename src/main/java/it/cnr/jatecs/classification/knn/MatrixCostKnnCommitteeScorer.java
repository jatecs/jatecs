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

import it.cnr.jatecs.classification.ClassificationResult;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.weighting.interfaces.IWeighting3D;

import java.util.Vector;

public class MatrixCostKnnCommitteeScorer implements IKnnCommitteeScorer {

    protected Vector<IWeighting3D> _matrixes;


    public MatrixCostKnnCommitteeScorer() {
        _matrixes = new Vector<IWeighting3D>();
    }


    public Vector<IWeighting3D> getMatrixes() {
        return _matrixes;
    }


    public ClassificationResult computeScore(KnnCommitteeClassifier cl,
                                             Vector<ClassificationResult> results, IIndex testIndex, int docID) {
        if (results.size() != _matrixes.size())
            throw new RuntimeException("The number of matrixes and classifiers must be the same");

        ClassificationResult cr = new ClassificationResult();
        cr.documentID = docID;

        for (int i = 0; i < results.get(0).categoryID.size(); i++) {
            cr.categoryID.add(results.get(0).categoryID.get(i));
            cr.score.add(0);
        }


        for (int i = 0; i < results.size(); i++) {
            ClassificationResult res = results.get(i);

            for (int j = 0; j < res.score.size(); j++) {
                double val = cr.score.get(j) + (res.score.get(j) * _matrixes.get(i).getWeight(res.categoryID.get(j), docID, 0));
                res.score.set(j, val);
            }
        }

        return cr;
    }

}
