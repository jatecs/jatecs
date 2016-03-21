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
import it.cnr.jatecs.indexes.DB.interfaces.IContentDB;
import it.cnr.jatecs.indexes.DB.interfaces.IDocumentDB;
import it.cnr.jatecs.indexes.DB.interfaces.IFeatureDB;
import it.cnr.jatecs.io.IStorageManager;
import it.cnr.jatecs.io.RamStorageManager;

import org.junit.Test;

public class TroveContentDBRWTest {

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

	protected IFeatureDB buildFeaturesDB() {
		TroveFeatureDBBuilder builder = new TroveFeatureDBBuilder();

		builder.addFeature("feat1");
		builder.addFeature("feat2");
		builder.addFeature("feat3");
		builder.addFeature("feat4");
		builder.addFeature("feat5");

		return builder.getFeatureDB();
	}

	protected IContentDB buildContentDB(IDocumentDB documentsDB,
			IFeatureDB featuresDB) {
		TroveContentDBBuilder builder = new TroveContentDBBuilder(documentsDB,
				featuresDB);
		for (int i = 0; i < 5; i++) {
			builder.setDocumentFeatureFrequency(i, i, (i + 1));
		}
		return builder.getContentDB();
	}

	@Test
	public void writeTest() {
		IDocumentDB docsDB = buildDocumentsDB();
		IFeatureDB featuresDB = buildFeaturesDB();
		IContentDB contentDB = buildContentDB(docsDB, featuresDB);
		TroveContentDBRW dbRW = new TroveContentDBRW(
				contentDB.getDocumentDB(),
				contentDB.getFeatureDB());
		IStorageManager storageManager = getStorageManager();
		String dbName = "content";

		try {
			dbRW.write(storageManager, contentDB, dbName, true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalStateException);
		}

		try {
			storageManager.open();
			dbRW.write(null, contentDB, dbName, true);
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
			dbRW.write(storageManager, contentDB, null, true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		try {
			dbRW.write(storageManager, contentDB, "", true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		dbRW.write(storageManager, contentDB, dbName, true);
		IContentDB read = dbRW.read(storageManager, dbName);
		assertTrue(read.getName().equals(contentDB.getName()));
		assertTrue(read.getDocumentLength(0) == 1);
	}
	
	
	@Test
	public void readTest() {
		IDocumentDB docsDB = buildDocumentsDB();
		IFeatureDB featuresDB = buildFeaturesDB();
		IContentDB contentDB = buildContentDB(docsDB, featuresDB);
		TroveContentDBRW dbRW = new TroveContentDBRW(
				contentDB.getDocumentDB(),
				contentDB.getFeatureDB());
		IStorageManager storageManager = getStorageManager();
		String dbName = "content";
		
		try {
			dbRW.read(storageManager, "unknown");
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalStateException);
		}

		try {
			storageManager.open();
			dbRW.write(storageManager, contentDB, dbName, true);
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
		
		IContentDB read = dbRW.read(storageManager, dbName);
		assertTrue(read.getName().equals(contentDB.getName()));
		for (int i = 0; i < 5; i++) {
			assertTrue(read.getDocumentFeaturesCount(i) == 1);
			assertTrue(read.getDocumentFeaturesCount(i) == contentDB.getDocumentFeaturesCount(i));
			assertTrue(read.getDocumentFeatureFrequency(i, i) == (i+1));
			assertTrue(read.getDocumentFeatureFrequency(i, i) == contentDB.getDocumentFeatureFrequency(i, i));
		}
		
		
	}
}
