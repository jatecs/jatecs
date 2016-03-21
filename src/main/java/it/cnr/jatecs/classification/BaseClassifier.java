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

package it.cnr.jatecs.classification;

import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.interfaces.IClassifierRuntimeCustomizer;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;

public abstract class BaseClassifier implements IClassifier {

    protected IClassifierRuntimeCustomizer _customizer = null;

    public IClassifierRuntimeCustomizer getRuntimeCustomizer() {
        return _customizer;
    }

    public void setRuntimeCustomizer(IClassifierRuntimeCustomizer customizer) {
        _customizer = customizer;
    }

    public ClassificationResult[] classify(IIndex testIndex, short catID) {

        ClassificationResult[] r = new ClassificationResult[testIndex
                .getDocumentDB().getDocumentsCount()];

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
}
