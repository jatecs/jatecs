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
import it.cnr.jatecs.indexes.DB.interfaces.ICategoryDB;
import it.cnr.jatecs.io.IStorageManager;
import it.cnr.jatecs.io.RamStorageManager;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import org.junit.Test;

public class TroveCategoriesDBRWTest {

	protected TroveCategoriesDBRW getDB() {
		return new TroveCategoriesDBRW();
	}

	protected IStorageManager getStorageManager() {
		return new RamStorageManager();
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
	
	
	protected ICategoryDB buildCategoriesDB2() {
		TroveCategoryDBBuilder builder = new TroveCategoryDBBuilder();
		builder.addCategory("other");
		return builder.getCategoryDB();
	}

	@Test
	public void writeTest() {
		TroveCategoriesDBRW db = getDB();
		IStorageManager storageManager = getStorageManager();
		ICategoryDB catsDB = buildCategoriesDB();
		String dbName = "categories";

		try {
			db.write(storageManager, catsDB, dbName, true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalStateException);
		}

		try {
			storageManager.open();
			db.write(null, catsDB, dbName, true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof NullPointerException);
		}

		try {
			db.write(storageManager, null, dbName, true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof NullPointerException);
		}

		try {
			db.write(storageManager, catsDB, null, true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		try {
			db.write(storageManager, catsDB, "", true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		db.write(storageManager, catsDB, dbName, true);
		ICategoryDB catsDBRead = db.read(storageManager, dbName);
		assertTrue(catsDB.getCategoriesCount() == catsDBRead
				.getCategoriesCount());
		
		ICategoryDB catsDB2 = buildCategoriesDB2();
		db.write(storageManager, catsDB, dbName, false);
		catsDBRead = db.read(storageManager, dbName);
		assertTrue(catsDB2.getCategoriesCount() != catsDBRead
				.getCategoriesCount());
		
	}

	@Test
	public void readTest() {
		TroveCategoriesDBRW db = getDB();
		IStorageManager storageManager = getStorageManager();
		ICategoryDB catsDB = buildCategoriesDB();
		String dbName = "categories";

		try {
			db.read(storageManager, "unknown");
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalStateException);
		}

		try {
			storageManager.open();
			db.write(storageManager, catsDB, dbName, true);
		} catch (Exception e) {
			fail();
		}

		try {
			db.read((IStorageManager) null, dbName);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof NullPointerException);
		}

		try {
			db.read(storageManager, null);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		try {
			db.read(storageManager, "");
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		try {
			db.read(storageManager, "unknown");
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		try {
			ICategoryDB read = db.read(storageManager, "categories");
			assertTrue(read.getCategoriesCount() == catsDB.getCategoriesCount());
			IShortIterator it = read.getCategories();
			while (it.hasNext()) {
				short catID = it.next();
				assertTrue(catsDB.isValidCategory(catID));
				assertTrue(catsDB.getCategoryName(catID).equals(
						read.getCategoryName(catID)));
				assertTrue(catsDB.getChildCategoriesCount(catID) == read.getChildCategoriesCount(catID));
				IShortIterator children = read.getChildCategories(catID);
				if (children.hasNext()) {
					assertTrue(catsDB.hasChildCategories(catID));
				}
			}

		} catch (Exception e) {
			fail();
		}
	}
}
