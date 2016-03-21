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

package it.cnr.jatecs.indexes.DB.troveCompact;

import gnu.trove.TIntIntHashMap;
import it.cnr.jatecs.indexes.DB.interfaces.IDocumentDB;
import it.cnr.jatecs.utils.iterators.RangeIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Vector;

public class TroveDocumentsDB implements IDocumentDB {

    protected HashMap<String, Integer> _documentsMap;
    protected Vector<String> _documentsRMap;
    protected String _name;

    public TroveDocumentsDB() {
        super();
        _documentsMap = new HashMap<String, Integer>();
        _documentsRMap = new Vector<String>();
        _name = "generic";
    }

    public String getDocumentName(int document) {
        return _documentsRMap.get(document);
    }

    public int getDocument(String documentName) {
        if (_documentsMap.containsKey(documentName))
            return _documentsMap.get(documentName);
        else
            return -1;
    }

    public int getDocumentsCount() {
        return _documentsRMap.size();
    }

    public IIntIterator getDocuments() {
        return new RangeIntIterator(0, _documentsRMap.size());
    }

    public boolean isValidDocument(int document) {
        return (document >= 0) ? ((document < _documentsRMap.size()) ? true
                : false) : false;
    }

    public String getName() {
        return _name;
    }

    public void setName(String name) {
        _name = name;
    }

    public void removeDocuments(IIntIterator removedDocuments) {
        int shift = 0;
        int lastGoodDocument = 0;
        int totalDocuments = _documentsMap.size();
        TIntIntHashMap documentsRemap = new TIntIntHashMap();
        while (removedDocuments.hasNext()) {
            int removedDocument = removedDocuments.next();
            while (lastGoodDocument < removedDocument) {
                documentsRemap.put(lastGoodDocument, lastGoodDocument - shift);
                ++lastGoodDocument;
            }
            lastGoodDocument = removedDocument + 1;
            int removedDocumentPosition = removedDocument - shift;
            _documentsMap.remove(_documentsRMap.get(removedDocumentPosition));
            _documentsRMap.remove(removedDocumentPosition);
            ++shift;
        }

        while (lastGoodDocument < totalDocuments) {
            documentsRemap.put(lastGoodDocument, lastGoodDocument - shift);
            ++lastGoodDocument;
        }

        Iterator<Entry<String, Integer>> mapIter = _documentsMap.entrySet()
                .iterator();
        while (mapIter.hasNext()) {
            Entry<String, Integer> entry = mapIter.next();
            int value = entry.getValue();
            int newvalue = documentsRemap.get(value);
            entry.setValue(newvalue);
        }

    }

    @SuppressWarnings("unchecked")
    public IDocumentDB cloneDB() {
        TroveDocumentsDB documentsDB = new TroveDocumentsDB();
        documentsDB._name = new String(_name);

        documentsDB._documentsMap = new HashMap<String, Integer>();
        Iterator<Entry<String, Integer>> mapIter = _documentsMap.entrySet()
                .iterator();
        while (mapIter.hasNext()) {
            Entry<String, Integer> entry = mapIter.next();
            documentsDB._documentsMap.put(entry.getKey(), entry.getValue());
        }

        documentsDB._documentsRMap = (Vector<String>) _documentsRMap.clone();
        return documentsDB;
    }
}
