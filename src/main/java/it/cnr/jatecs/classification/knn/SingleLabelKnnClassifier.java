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
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.interfaces.IClassifierRuntimeCustomizer;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

public class SingleLabelKnnClassifier implements IClassifier {
    protected IIndex _training;
    private SingleLabelKnnClassifierCustomizer _customizer;
    public SingleLabelKnnClassifier(IIndex training) {
        _training = training;
        _customizer = new SingleLabelKnnClassifierCustomizer();
    }

    public ClassificationResult classify(IIndex testIndex, int docID) {
        ClassificationResult res = new ClassificationResult();
        res.documentID = docID;

        SingleLabelKnnClassifierCustomizer cust = (SingleLabelKnnClassifierCustomizer) getRuntimeCustomizer();
        int maxKValue = cust.getK();
        Vector<SimilarDocument> allSimilar = cust._searcher.search(testIndex, docID, _training, maxKValue);

        double scores[] = new double[_training.getCategoryDB().getCategoriesCount()];
        double avgs[] = new double[_training.getCategoryDB().getCategoriesCount()];
        for (int i = 0; i < scores.length; i++) {
            scores[i] = 0;
            avgs[i] = 0;
        }

        //double norm = 0;
        for (int i = 0; i < allSimilar.size(); i++) {
            SimilarDocument doc = allSimilar.get(i);
            IShortIterator cats = _training.getClassificationDB().getDocumentCategories(doc.docID);
            while (cats.hasNext()) {
                short catID = cats.next();
                scores[catID] += doc.score;
                //norm += doc.score;
            }
        }

        TreeSet<Key> map = new TreeSet<Key>();
        for (int i = 0; i < scores.length; i++) {
            Key k = new Key();
            k.catID = (short) i;
            k.score = scores[i];
            map.add(k);
        }

		/*double avg = 0;
		int cont = 0;
		while (map.size() > 0)
		{
			Key key = map.last();
			map.remove(key);
			res.categoryID.add(key.catID);
			res.score.add(key.score);
			if (cont <= 1)
				avg += key.score;
			cont++;
		}

		avg /= 2;
		cust._defaultMargin = avg;*/


        Key k = map.last();
        assert (map.remove(k));

        double avg = 0;
        Iterator<Key> it = map.iterator();
        while (it.hasNext()) {
            Key key = it.next();
            avg += key.score;
        }
        avg /= map.size();
        res.categoryID.add(k.catID);
        res.score.add(k.score - avg);

        it = map.iterator();
        while (it.hasNext()) {
            Key key = it.next();
            if (key.catID != k.catID) {
                res.categoryID.add(key.catID);
                res.score.add(0);
            }
        }

        return res;
    }

    protected Vector<SimilarDocument> selectSimilar(Vector<SimilarDocument> sd, int numDocuments) {
        Vector<SimilarDocument> similar = new Vector<SimilarDocument>();
        for (int i = 0; i < numDocuments; i++)
            similar.add(sd.get(i));

        return similar;
    }

    public ClassifierRange getClassifierRange(short catID)

    {
        SingleLabelKnnClassifierCustomizer cust = (SingleLabelKnnClassifierCustomizer) _customizer;

        return cust.getClassifierRange(catID);
    }

    public int getCategoryCount() {
        return _training.getCategoryDB().getCategoriesCount();
    }

    public IShortIterator getCategories() {
        return _training.getCategoryDB().getCategories();
    }

    public ClassificationResult[] classify(IIndex testIndex, short catID) {
        ClassificationResult[] r = new ClassificationResult[testIndex.getDocumentDB().getDocumentsCount()];

        IIntIterator it = testIndex.getDocumentDB().getDocuments();
        while (it.hasNext()) {
            int docID = it.next();
            ClassificationResult res = classify(testIndex, docID);

            for (int i = 0; i < res.categoryID.size(); i++) {
                short cat = res.categoryID.get(i);
                if (cat == catID) {
                    ClassificationResult result = new ClassificationResult();
                    result.documentID = docID;
                    result.categoryID.add(catID);
                    result.score.add(res.score.get(i));

                    r[docID] = result;
                    break;
                }
            }

            if (r[docID] == null) {
                ClassificationResult result = new ClassificationResult();
                result.documentID = docID;
                result.categoryID.add(catID);
                result.score.add(getClassifierRange(catID).minimum);
                r[docID] = result;
            }
        }

        return r;
    }

    public void destroy() {

    }

    public IClassifierRuntimeCustomizer getRuntimeCustomizer() {
        return _customizer;
    }

    public void setRuntimeCustomizer(IClassifierRuntimeCustomizer customizer) {
        _customizer = (SingleLabelKnnClassifierCustomizer) customizer;

    }

    class Key implements Comparable<Key> {
        short catID;
        double score;

        public int compareTo(Key o) {
            if (score < o.score)
                return -1;
            else if (score > o.score)
                return 1;
            else {
                if (catID < o.catID)
                    return -1;
                else if (catID > o.catID)
                    return 1;
                else
                    return 0;

            }
        }

        @Override
        public boolean equals(Object obj) {
            Key k = (Key) obj;
            if (obj == null)
                return false;

            return compareTo(k) == 0;
        }


    }

}
