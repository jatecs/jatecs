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
import gnu.trove.TIntArrayList;
import it.cnr.jatecs.indexes.DB.interfaces.ICategoryDB;
import it.cnr.jatecs.indexes.DB.interfaces.IDomainDB;
import it.cnr.jatecs.indexes.DB.interfaces.IFeatureDB;
import it.cnr.jatecs.io.IStorageManager;
import it.cnr.jatecs.io.RamStorageManager;
import it.cnr.jatecs.utils.iterators.TIntArrayListIterator;

import org.junit.Test;

public class TroveDomainDBRWTest {
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

	protected IFeatureDB buildFeaturesDB() {
		TroveFeatureDBBuilder builder = new TroveFeatureDBBuilder();

		builder.addFeature("feat1");
		builder.addFeature("feat2");
		builder.addFeature("feat3");
		builder.addFeature("feat4");
		builder.addFeature("feat5");

		return builder.getFeatureDB();
	}

	protected IDomainDB buildDomainDB(ICategoryDB catsDB, IFeatureDB featsDB) {
		TroveDomainDB domainDB = new TroveDomainDB(catsDB, featsDB);
		TIntArrayList removedFeatures = new TIntArrayList();
		removedFeatures.add(0);
		removedFeatures.add(1);
		domainDB.removeCategoryFeatures((short) 0, new TIntArrayListIterator(
				removedFeatures));
		return domainDB;
	}

	@Test
	public void writeTest() {
		ICategoryDB catsDB = buildCategoriesDB();
		IFeatureDB featuresDB = buildFeaturesDB();
		IDomainDB domainDB = buildDomainDB(catsDB, featuresDB);
		TroveDomainDBRW dbRW = new TroveDomainDBRW(domainDB.getCategoryDB(),
				domainDB.getFeatureDB());
		IStorageManager storageManager = getStorageManager();
		String dbName = "domain";

		try {
			dbRW.write(storageManager, domainDB, dbName, true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalStateException);
		}

		try {
			storageManager.open();
			dbRW.write(null, domainDB, dbName, true);
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
			dbRW.write(storageManager, domainDB, null, true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		try {
			dbRW.write(storageManager, domainDB, "", true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		dbRW.write(storageManager, domainDB, dbName, true);
		IDomainDB read = dbRW.read(storageManager, dbName);
		assertTrue(read.getName().equals(domainDB.getName()));
		assertTrue(domainDB.getCategoryFeaturesCount((short) 0) == 3);
	}

	@Test
	public void readTest() {
		ICategoryDB catsDB = buildCategoriesDB();
		IFeatureDB featuresDB = buildFeaturesDB();
		IDomainDB domainDB = buildDomainDB(catsDB, featuresDB);
		TroveDomainDBRW dbRW = new TroveDomainDBRW(domainDB.getCategoryDB(),
				domainDB.getFeatureDB());
		IStorageManager storageManager = getStorageManager();
		String dbName = "domain";
		
		try {
			dbRW.read(storageManager, "unknown");
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalStateException);
		}

		try {
			storageManager.open();
			dbRW.write(storageManager, domainDB, dbName, true);
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
		
		IDomainDB read = dbRW.read(storageManager, dbName);
		assertTrue(read.getName().equals(domainDB.getName()));
		
		assertTrue(read.hasCategoryFeature((short)0, 2));
		assertTrue(read.hasCategoryFeature((short)0, 3));
		assertTrue(read.hasCategoryFeature((short)0, 4));
		assertTrue(!read.hasCategoryFeature((short)0, 1));
		assertTrue(!read.hasCategoryFeature((short)0, 0));
	}
}
