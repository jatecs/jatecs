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

import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.TextualProgressBar;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.util.Vector;

public class MostSimilarPositiveProcessor extends KnnDocumentWeightProcessor {

    public MostSimilarPositiveProcessor(IIndex index, IKnnSearcher searcher, String outputDir) {
        super(index, searcher, outputDir);
    }

    @Override
    public void computeWeights() {
        TextualProgressBar b = new TextualProgressBar("Computing weights of each training document");
        int numToCompute = _index.getDocumentDB().getDocumentsCount();
        b.signal(0);
        IIntIterator docs = _index.getDocumentDB().getDocuments();
        IShortIterator cats = _index.getCategoryDB().getCategories();
        int computed = 0;
        while (docs.hasNext()) {
            int docID = docs.next();
            Vector<SimilarDocument> similar = _searcher.search(_index, docID, _index, _k);
            cats.begin();
            while (cats.hasNext()) {
                short catID = cats.next();

                int positives = 0;
                for (int i = 0; i < similar.size(); i++) {
                    SimilarDocument sd = similar.get(i);
                    if (_index.getClassificationDB().hasDocumentCategory(sd.docID, catID))
                        positives++;
                }

                double score = (double) positives / (double) similar.size();
                _matrix.setWeight(score, catID, docID, 0);
            }

            computed++;

            b.signal((computed * 100) / numToCompute);
        }

        b.signal(100);
    }

}
