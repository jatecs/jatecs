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

package it.cnr.jatecs.classification.logistic_regression;

import gnu.trove.TShortArrayList;
import it.cnr.jatecs.classification.BaseClassifier;
import it.cnr.jatecs.classification.ClassificationResult;
import it.cnr.jatecs.classification.ClassifierRange;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.iterators.TShortArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

public class ModRegularizedLogisticRegressionClassifier extends BaseClassifier {
    final double[][] weigths;

    ModRegularizedLogisticRegressionClassifier(int numCategories) {
        weigths = new double[numCategories][];
    }

    public ClassificationResult classify(IIndex testIndex, int docID) {
        IIntIterator feats = testIndex.getContentDB()
                .getDocumentFeatures(docID);
        ClassificationResult res = new ClassificationResult();
        res.documentID = docID;

        IShortIterator cats = testIndex.getCategoryDB().getCategories();
        while (cats.hasNext()) {
            short catID = cats.next();
            double score = 0;
            feats.begin();
            while (feats.hasNext()) {
                int featID = feats.next();
                double w = testIndex.getWeightingDB().getDocumentFeatureWeight(
                        docID, featID);
                score += w * weigths[catID][featID];
            }

            res.categoryID.add(catID);
            res.score.add(score);
        }

        return res;
    }

    public ClassifierRange getClassifierRange(short catID) {
        ClassifierRange cr = new ClassifierRange();
        cr.border = 0;
        cr.maximum = Double.MAX_VALUE;
        cr.minimum = -Double.MAX_VALUE;

        return cr;
    }

    @Override
    public int getCategoryCount() {
        return weigths.length;
    }

    @Override
    public IShortIterator getCategories() {
        TShortArrayList l = new TShortArrayList();
        for (short i = 0; i < weigths.length; i++)
            l.add(i);

        return new TShortArrayListIterator(l);
    }
}
