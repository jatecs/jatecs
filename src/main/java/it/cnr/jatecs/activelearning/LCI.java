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

import it.cnr.jatecs.classification.ClassificationScoreDB;
import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDB;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;

/**
 * Label Cardinality Inconsistency sampling strategy proposed in
 * "X. Li and Y. Guo. Active learning with multi-label svm classification. In IJCAI, 2013"
 *
 * @author giacomo
 */
public class LCI extends ALpoolRank {

    public LCI(ClassificationScoreDB confidenceUnlabelled, IIndex trainingSet,
               IClassificationDB classificationUnlabelled) {
        super(confidenceUnlabelled, trainingSet);

        int docsCount = trainingSet.getDocumentDB().getDocumentsCount();
        double avgCatsTrain = 0.0;
        IIntIterator docIt = trainingSet.getDocumentDB().getDocuments();
        docIt.begin();
        while (docIt.hasNext()) {
            avgCatsTrain += trainingSet.getClassificationDB()
                    .getDocumentCategoriesCount(docIt.next());
        }
        avgCatsTrain /= docsCount;

        docsCount = classificationUnlabelled.getDocumentDB()
                .getDocumentsCount();
        docIt = classificationUnlabelled.getDocumentDB().getDocuments();
        docIt.begin();
        while (docIt.hasNext()) {
            int docId = docIt.next();
            double value = Math.abs(classificationUnlabelled
                    .getDocumentCategoriesCount(docId) - avgCatsTrain);
            rankingMap.put(docId, value);
            updateMax(docId, value);
        }

    }

}
