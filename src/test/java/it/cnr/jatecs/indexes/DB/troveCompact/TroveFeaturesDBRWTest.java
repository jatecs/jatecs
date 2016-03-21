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
import it.cnr.jatecs.indexes.DB.interfaces.IFeatureDB;
import it.cnr.jatecs.io.IStorageManager;
import it.cnr.jatecs.io.RamStorageManager;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;

import org.junit.Test;

public class TroveFeaturesDBRWTest {

	protected TroveFeaturesDBRW getDB() {
		return new TroveFeaturesDBRW();
	}

	protected IStorageManager getStorageManager() {
		return new RamStorageManager();
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

	@Test
	public void writeTest() {
		TroveFeaturesDBRW dbRW = getDB();
		IStorageManager storageManager = getStorageManager();
		IFeatureDB featuresDB = buildFeaturesDB();
		String dbName = "features";

		try {
			dbRW.write(storageManager, featuresDB, dbName, true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalStateException);
		}

		try {
			storageManager.open();
			dbRW.write(null, featuresDB, dbName, true);
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
			dbRW.write(storageManager, featuresDB, null, true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		try {
			dbRW.write(storageManager, featuresDB, "", true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		dbRW.write(storageManager, featuresDB, dbName, true);
		IFeatureDB featuresDBRead = dbRW.read(storageManager, dbName);
		assertTrue(featuresDBRead.getFeaturesCount() == 5);
	}

	@Test
	public void readTest() {
		TroveFeaturesDBRW dbRW = getDB();
		IStorageManager storageManager = getStorageManager();
		IFeatureDB featuresDB = buildFeaturesDB();
		String dbName = "features";

		try {
			dbRW.read(storageManager, "unknown");
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalStateException);
		}

		try {
			storageManager.open();
			dbRW.write(storageManager, featuresDB, dbName, true);
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

		IFeatureDB read = dbRW.read(storageManager, dbName);
		assertTrue(read.getFeaturesCount() == featuresDB.getFeaturesCount());
		IIntIterator it = read.getFeatures();
		while (it.hasNext()) {
			int featID = it.next();
			assertTrue(read.getFeatureName(featID).equals(
					featuresDB.getFeatureName(featID)));
		}
	}
}
