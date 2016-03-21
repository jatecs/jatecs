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

import it.cnr.jatecs.indexes.DB.interfaces.IDocumentDB;
import it.cnr.jatecs.indexes.DB.interfaces.IDocumentDBBuilder;

public class TroveDocumentsDBBuilder implements IDocumentDBBuilder {

    protected TroveDocumentsDB _documentsDB;

    public TroveDocumentsDBBuilder() {
        super();
        _documentsDB = new TroveDocumentsDB();
    }


    public TroveDocumentsDBBuilder(TroveDocumentsDB db) {
        super();
        _documentsDB = db;
    }

    public int addDocument(String documentName) {
        if (_documentsDB._documentsMap.containsKey(documentName))
            throw new RuntimeException("Duplicate document: " + documentName);
        int document = _documentsDB._documentsMap.size();
        _documentsDB._documentsMap.put(documentName, document);
        _documentsDB._documentsRMap.add(documentName);
        return document;
    }

    public IDocumentDB getDocumentDB() {
        return _documentsDB;
    }
}
