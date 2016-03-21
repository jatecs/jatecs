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

package it.cnr.jatecs.classification.treeboost;

import gnu.trove.TIntArrayList;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.iterators.TIntArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

public class SiblingNegativesChooser implements INegativesChooser {

    protected IIndex _globalIndex;

    public TIntArrayListIterator selectNegatives(String category) {

        TIntArrayList negatives = new TIntArrayList();

        short id = _globalIndex.getCategoryDB().getCategory(category);
        IShortIterator cats = _globalIndex.getCategoryDB()
                .getSiblingCategories(id);
        while (cats.hasNext()) {
            short catID = cats.next();

            IIntIterator docsNegative = _globalIndex.getClassificationDB()
                    .getCategoryDocuments(catID);
            while (docsNegative.hasNext()) {
                int docID = docsNegative.next();
                if (!negatives.contains(docID)
                        && !_globalIndex.getClassificationDB()
                        .hasDocumentCategory(docID, id))
                    negatives.add(docID);
            }
        }

        return new TIntArrayListIterator(negatives);
    }

    public void initialize(IIndex index) {
        assert (index != null);

        _globalIndex = index;
    }

    public void release() {

    }

}
