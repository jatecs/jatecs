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

import it.cnr.jatecs.indexes.DB.interfaces.ICategoryDB;
import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDB;
import it.cnr.jatecs.indexes.DB.interfaces.IDocumentDB;
import it.cnr.jatecs.io.IStorageManager;
import it.cnr.jatecs.io.RamStorageManager;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TroveClassificationDBRWTest {

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

	protected ICategoryDB buildCategoriesDB() {
		TroveCategoryDBBuilder builder = new TroveCategoryDBBuilder();
		builder.addCategory("cat1");
		builder.addCategory("cat2");
		builder.addCategory("cat3");
		builder.addCategory("cat4");
		builder.setParentCategory("cat3", "cat2");
		builder.setParentCategory("cat4", "cat2");
		return builder.getCategoryDB();
	}

	protected IClassificationDB buildClassificationDB(IDocumentDB documentsDB,
													  ICategoryDB catsDB) {
		TroveClassificationDBBuilder builder = new TroveClassificationDBBuilder(
				documentsDB, catsDB);
		builder.setDocumentCategory(documentsDB.getDocument("doc1"),
				catsDB.getCategory("cat2"));
		builder.setDocumentCategory(documentsDB.getDocument("doc1"),
				catsDB.getCategory("cat4"));
		return builder.getClassificationDB();
	}

	@Test
	public void writeTest() {
		IDocumentDB docsDB = buildDocumentsDB();
		ICategoryDB catsDB = buildCategoriesDB();
		IClassificationDB classificationDB = buildClassificationDB(docsDB,
				catsDB);
		TroveClassificationDBRW dbRW = new TroveClassificationDBRW(
				classificationDB.getDocumentDB(),
				classificationDB.getCategoryDB());
		IStorageManager storageManager = getStorageManager();
		String dbName = "classification";

		try {
			dbRW.write(storageManager, classificationDB, dbName, true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalStateException);
		}

		try {
			storageManager.open();
			dbRW.write(null, classificationDB, dbName, true);
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
			dbRW.write(storageManager, classificationDB, null, true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		try {
			dbRW.write(storageManager, classificationDB, "", true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		dbRW.write(storageManager, classificationDB, dbName, true);
		IClassificationDB read = dbRW.read(storageManager, dbName,
				TroveClassificationDBType.Default);
		assertTrue(read.getName().equals(classificationDB.getName()));
		assertTrue(read.getCategoryDocumentsCount(catsDB.getCategory("cat4")) == classificationDB
				.getCategoryDocumentsCount(catsDB.getCategory("cat4")));
	}

	@Test
	public void readTest() {
		IDocumentDB docsDB = buildDocumentsDB();
		ICategoryDB catsDB = buildCategoriesDB();
		IClassificationDB classificationDB = buildClassificationDB(docsDB,
				catsDB);
		TroveClassificationDBRW dbRW = new TroveClassificationDBRW(
				classificationDB.getDocumentDB(),
				classificationDB.getCategoryDB());
		IStorageManager storageManager = getStorageManager();
		String dbName = "classification";

		try {
			dbRW.read(storageManager, "unknown", TroveClassificationDBType.Default);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalStateException);
		}

		try {
			storageManager.open();
			dbRW.write(storageManager, classificationDB, dbName, true);
		} catch (Exception e) {
			fail();
		}

		try {
			dbRW.read((IStorageManager) null, dbName, TroveClassificationDBType.Default);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof NullPointerException);
		}

		try {
			dbRW.read(storageManager, null, TroveClassificationDBType.Default);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		try {
			dbRW.read(storageManager, "", TroveClassificationDBType.Default);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		try {
			dbRW.read(storageManager, "unknown", TroveClassificationDBType.Default);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		IClassificationDB read = dbRW.read(storageManager, dbName, TroveClassificationDBType.Default);
		assertTrue(read.getName().equals(classificationDB.getName()));
		for (int i = 1; i <= 5; i++) {
			if (i == 2 || i == 4)
				assertTrue(read.getCategoryDocumentsCount(read.getCategoryDB().getCategory("cat"+i)) == 1);
			else
				assertTrue(read.getCategoryDocumentsCount(read.getCategoryDB().getCategory("cat"+i)) == 0);

			assertTrue(read.getCategoryDocumentsCount(catsDB.getCategory("cat"+i)) == classificationDB
					.getCategoryDocumentsCount(catsDB.getCategory("cat"+i)));
		}


	}
}
