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

import it.cnr.jatecs.classification.BaseClassifier;
import it.cnr.jatecs.classification.ClassificationResult;
import it.cnr.jatecs.classification.ClassifierRange;
import it.cnr.jatecs.indexes.DB.interfaces.ICategoryDB;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.util.Vector;

public class WMVSingleLabelKnnCommitteeClassifier extends BaseClassifier {

    protected IIndex _training;

    protected Vector<SingleLabelKnnClassifier> _classifiers;
    protected ICategoryDB _catsDB;

    public WMVSingleLabelKnnCommitteeClassifier(IIndex training, ICategoryDB catsDB) {
        _catsDB = catsDB;
        _classifiers = new Vector<SingleLabelKnnClassifier>();
        _customizer = new SingleLabelKnnCommitteeClassifierCustomizer();
        _training = training;
    }


    public Vector<SingleLabelKnnClassifier> getClassifiers() {
        return _classifiers;
    }

    public ClassificationResult classify(IIndex testIndex, int docID) {
        SingleLabelKnnCommitteeClassifierCustomizer cust = (SingleLabelKnnCommitteeClassifierCustomizer) getRuntimeCustomizer();
        Vector<SimilarDocument> simDocs = cust._searcher.search(testIndex, docID, _training, cust.getNumSimilar());


        double[] scores = new double[_training.getCategoryDB().getCategoriesCount()];
        for (int i = 0; i < scores.length; i++) scores[i] = -Double.MAX_VALUE;

        for (int i = 0; i < _classifiers.size(); i++) {
            SingleLabelKnnClassifier cl = _classifiers.get(i);
            SingleLabelKnnClassifierCustomizer cu = (SingleLabelKnnClassifierCustomizer) cl.getRuntimeCustomizer();
            cu._searcher.setUseSameIndexesData(true);
            double curScore = 0;
            for (int j = 0; j < simDocs.size(); j++) {
                SimilarDocument sd = simDocs.get(j);
                double similarity = cust.getKnnSearcher().getSimilarityFunction().compute(docID, testIndex, sd.docID, _training);
                assert (similarity >= 0 && similarity <= 1);
                ClassificationResult cr = cl.classify(_training, sd.docID);
                short assignedCatID = 0;
                for (int z = 0; z < cr.categoryID.size(); z++) {
                    short catID = cr.categoryID.get(z);
                    double sc = cr.score.get(z);
                    if (sc > cl.getClassifierRange(catID).border) {
                        assignedCatID = catID;
                        break;
                    }
                }

                int val = -1;
                if (_training.getClassificationDB().hasDocumentCategory(sd.docID, assignedCatID))
                    val = +1;

                curScore += (similarity) * val;
            }

            cu._searcher.setUseSameIndexesData(false);

            ClassificationResult cr = cl.classify(testIndex, docID);
            short assignedCatID = 0;
            for (int z = 0; z < cr.categoryID.size(); z++) {
                short catID = cr.categoryID.get(z);
                double sc = cr.score.get(z);
                if (sc > cl.getClassifierRange(catID).border) {
                    assignedCatID = catID;
                    break;
                }
            }
            if (scores[assignedCatID] == -Double.MAX_VALUE) scores[assignedCatID] = 0;
            scores[assignedCatID] += curScore;
        }

        double max = -Double.MAX_VALUE;
        int catID = 0;
        for (int i = 0; i < scores.length; i++) {
            if (scores[i] > max) {
                max = scores[i];
                catID = i;
            }
        }

        ClassificationResult cr = new ClassificationResult();
        cr.documentID = docID;
        for (int i = 0; i < scores.length; i++) {
            if (i == catID) {
                cr.categoryID.add((short) catID);
                cr.score.add(1);
            } else {
                cr.categoryID.add((short) i);
                cr.score.add(0);
            }
        }

        return cr;
    }

    public ClassifierRange getClassifierRange(short catID) {
        SingleLabelKnnCommitteeClassifierCustomizer cust = (SingleLabelKnnCommitteeClassifierCustomizer) _customizer;
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

}
