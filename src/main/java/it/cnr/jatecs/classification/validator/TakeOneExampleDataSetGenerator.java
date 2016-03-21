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

package it.cnr.jatecs.classification.validator;

import gnu.trove.TIntArrayList;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.Pair;
import it.cnr.jatecs.utils.iterators.TIntArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;

public class TakeOneExampleDataSetGenerator implements IDataSetGenerator {

    protected IIndex _index;
    protected IIntIterator _documents;

    public void begin(IIndex index) {
        _index = index;

        _documents = _index.getDocumentDB().getDocuments();
    }

    public boolean hasNext() {
        return _documents.hasNext();
    }

    public Pair<IIndex, IIndex> next() {
        assert (_documents.hasNext());
        int docID = _documents.next();

        TIntArrayList docsToRemove = new TIntArrayList();
        docsToRemove.add(docID);
        IIndex training = _index.cloneIndex();
        training.removeDocuments(new TIntArrayListIterator(docsToRemove), false);

        IIndex test = _index.cloneIndex();
        docsToRemove = new TIntArrayList();
        IIntIterator docs = test.getDocumentDB().getDocuments();
        while (docs.hasNext()) {
            int doc = docs.next();
            if (doc == docID)
                continue;

            docsToRemove.add(doc);
        }
        test.removeDocuments(new TIntArrayListIterator(docsToRemove), false);

        Pair<IIndex, IIndex> res = new Pair<IIndex, IIndex>(training, test);
        return res;
    }

}
