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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import it.cnr.jatecs.indexes.DB.interfaces.IDocumentDB;
import it.cnr.jatecs.io.IStorageManager;
import it.cnr.jatecs.io.RamStorageManager;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;

import org.junit.Test;

public class TroveDocumentsDBRWTest {

	protected TroveDocumentsDBRW getDB() {
		return new TroveDocumentsDBRW();
	}
	
	protected IStorageManager getStorageManager() {
		return new RamStorageManager();
	}
	
	protected IDocumentDB buildDocumentsDB() {
		TroveDocumentsDBBuilder builder = new TroveDocumentsDBBuilder();
		
		builder.addDocument("doc1");
		builder.addDocument("doc2");
		builder.addDocument("doc3");
		builder.addDocument("doc4");
		builder.addDocument("doc5");
		
		return builder.getDocumentDB();
	}
	
	
	@Test
	public void writeTest() {
		TroveDocumentsDBRW dbRW = getDB();
		IStorageManager storageManager = getStorageManager();
		IDocumentDB documentsDB = buildDocumentsDB();
		String dbName = "documents";
		
		
		try {
			dbRW.write(storageManager, documentsDB, dbName, true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalStateException);
		}

		try {
			storageManager.open();
			dbRW.write(null, documentsDB, dbName, true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof NullPointerException);
		}

		try {
			dbRW.write(storageManager, null, dbName, true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof NullPointerException);
		}

		try {
			dbRW.write(storageManager, documentsDB, null, true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		try {
			dbRW.write(storageManager, documentsDB, "", true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}
		
		dbRW.write(storageManager, documentsDB, dbName, true);
		IDocumentDB documentsDBRead = dbRW.read(storageManager, dbName);
		assertTrue(documentsDBRead.getDocumentsCount() == 5);
	}
	
	
	@Test
	public void readTest() {
		TroveDocumentsDBRW dbRW = getDB();
		IStorageManager storageManager = getStorageManager();
		IDocumentDB documentsDB = buildDocumentsDB();
		String dbName = "documents";

		try {
			dbRW.read(storageManager, "unknown");
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalStateException);
		}

		try {
			storageManager.open();
			dbRW.write(storageManager, documentsDB, dbName, true);
		} catch (Exception e) {
			fail();
		}

		try {
			dbRW.read((IStorageManager) null, dbName);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof NullPointerException);
		}

		try {
			dbRW.read(storageManager, null);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		try {
			dbRW.read(storageManager, "");
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		try {
			dbRW.read(storageManager, "unknown");
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}
		
		IDocumentDB read = dbRW.read(storageManager, dbName);
		assertTrue(read.getDocumentsCount() == documentsDB.getDocumentsCount());
		IIntIterator it = read.getDocuments();
		while (it.hasNext()) {
			int docID = it.next();
			assertTrue(read.getDocumentName(docID).equals(documentsDB.getDocumentName(docID)));
		}
	}
}
