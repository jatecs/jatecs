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
import it.cnr.jatecs.classification.knn.KnnClassifierCustomizer.KnnType;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.util.Vector;

public class KnnClassifier extends KnnBaseClassifier {

    public KnnClassifier(IIndex training) {
        _training = training;
        _customizer = new KnnClassifierCustomizer();
    }

    public ClassificationResult classify(IIndex testIndex, int docID) {
        KnnClassifierCustomizer cust = (KnnClassifierCustomizer) _customizer;
        if (cust.getKnnType() == KnnType.CLASSIC)
            return classifyClassic(testIndex, docID);
        else
            return classifyGalavotti(testIndex, docID);
    }


    protected ClassificationResult classifyGalavotti(IIndex testIndex, int docID) {
        ClassificationResult res = new ClassificationResult();
        res.documentID = docID;

        KnnClassifierCustomizer cust = (KnnClassifierCustomizer) _customizer;

        int maxKValue = cust.getMaxKValue();
        Vector<SimilarDocument> allSimilar = cust._searcher.search(testIndex, docID, _training, maxKValue);

        // Find the k most similar documents to the wanted docID.
        IShortIterator cats = _training.getCategoryDB().getCategories();
        while (cats.hasNext()) {
            short catID = cats.next();

            Vector<SimilarDocument> similar = selectSimilar(allSimilar, cust.getK(catID));

            res.categoryID.add(catID);
            double score = 0;
            for (int i = 0; i < similar.size(); i++) {
                SimilarDocument doc = similar.get(i);

                if (_training.getClassificationDB().hasDocumentCategory(doc.docID, catID)) {
                    score += doc.score;
                } else {
                    score -= doc.score;
                }
            }

            score /= similar.size();

            res.score.add(score);
        }

        return res;
    }


    protected Vector<SimilarDocument> selectSimilar(Vector<SimilarDocument> sd, int numDocuments) {
        Vector<SimilarDocument> similar = new Vector<SimilarDocument>();
        for (int i = 0; i < numDocuments; i++)
            similar.add(sd.get(i));

        return similar;
    }


    protected ClassificationResult classifyClassic(IIndex testIndex, int docID) {
        ClassificationResult res = new ClassificationResult();
        res.documentID = docID;

        KnnClassifierCustomizer cust = (KnnClassifierCustomizer) _customizer;

        int maxKValue = cust.getMaxKValue();
        Vector<SimilarDocument> allSimilar = cust._searcher.search(testIndex, docID, _training, maxKValue);


        // Find the k most similar documents to the wanted docID.
        IShortIterator cats = _training.getCategoryDB().getCategories();
        while (cats.hasNext()) {
            short catID = cats.next();

            Vector<SimilarDocument> similar = selectSimilar(allSimilar, cust.getK(catID));

            res.categoryID.add(catID);
            double score = 0;
            for (int i = 0; i < similar.size(); i++) {
                SimilarDocument doc = similar.get(i);
                if (!(doc.score >= 0 && doc.score <= 1))
                    System.out.println("Score: " + doc.score);
                if (_training.getClassificationDB().hasDocumentCategory(doc.docID, catID)) {
                    score += doc.score;
                }
            }

            res.score.add(score);
        }

        return res;
    }


    public ClassifierRange getClassifierRange(short catID) {
        KnnClassifierCustomizer cust = (KnnClassifierCustomizer) _customizer;

        return cust.getClassifierRange(catID);
    }

    @Override
    public int getCategoryCount() {
        return _training.getCategoryDB().getCategoriesCount();
    }

    @Override
    public IShortIterator getCategories() {
        return _training.getCategoryDB().getCategories();
    }


}
