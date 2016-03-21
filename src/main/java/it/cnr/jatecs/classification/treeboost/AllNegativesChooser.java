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

public class AllNegativesChooser implements INegativesChooser {

    protected IIndex _index;


    public void initialize(IIndex index) {
        _index = index;
    }

    public void release() {
        _index = null;
    }

    public TIntArrayListIterator selectNegatives(String category) {
        TIntArrayList l = new TIntArrayList();

        short catID = _index.getCategoryDB().getCategory(category);
        IIntIterator it = _index.getDocumentDB().getDocuments();
        while (it.hasNext()) {
            int docID = it.next();
            if (_index.getClassificationDB().hasDocumentCategory(docID, catID))
                continue;

            l.add(docID);
        }

        return new TIntArrayListIterator(l);
    }
}
