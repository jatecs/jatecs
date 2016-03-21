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
import it.cnr.jatecs.classification.ClassifierRange;
import it.cnr.jatecs.indexes.DB.interfaces.ICategoryDB;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.util.Vector;

public class KnnCommitteeClassifier extends KnnBaseClassifier {

    protected Vector<KnnBaseClassifier> _classifiers;
    protected ICategoryDB _catsDB;

    public KnnCommitteeClassifier(ICategoryDB catsDB) {
        _catsDB = catsDB;
        _classifiers = new Vector<KnnBaseClassifier>();
        _customizer = new KnnCommitteeClassifierCustomizer();
    }

    public Vector<KnnBaseClassifier> getClassifiers() {
        return _classifiers;
    }

    public ClassificationResult classify(IIndex testIndex, int docID) {
        KnnCommitteeClassifierCustomizer cust = (KnnCommitteeClassifierCustomizer) _customizer;

        Vector<ClassificationResult> results = new Vector<ClassificationResult>();
        for (int i = 0; i < _classifiers.size(); i++) {
            ClassificationResult res = _classifiers.get(i).classify(testIndex,
                    docID);
            results.add(res);
        }

        ClassificationResult r = cust._scorer.computeScore(this, results,
                testIndex, docID);

        return r;
    }

    public ClassifierRange getClassifierRange(short catID) {
        KnnCommitteeClassifierCustomizer cust = (KnnCommitteeClassifierCustomizer) _customizer;
        return cust.getClassifierRange(catID);
    }

    @Override
    public int getCategoryCount() {
        return _catsDB.getCategoriesCount();
    }

    @Override
    public IShortIterator getCategories() {
        return _catsDB.getCategories();
    }

    @Override
    public void setTrainingIndex(IIndex training) {
        for (int i = 0; i < getClassifiers().size(); i++) {
            KnnBaseClassifier cl = (KnnBaseClassifier) getClassifiers().get(i);
            if (cl != null) {
                cl.setTrainingIndex(training);
            }
        }
    }

}
