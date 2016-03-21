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
import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDB;
import it.cnr.jatecs.indexes.DB.interfaces.IContentDB;
import it.cnr.jatecs.indexes.DB.interfaces.IDocumentDB;
import it.cnr.jatecs.indexes.DB.interfaces.IDomainDB;
import it.cnr.jatecs.indexes.DB.interfaces.IFeatureDB;
import it.cnr.jatecs.indexes.DB.interfaces.IWeightingDB;
import it.cnr.jatecs.io.IStorageManager;
import it.cnr.jatecs.io.RamStorageManager;
import it.cnr.jatecs.utils.iterators.TIntArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import org.junit.Test;

public class TroveReadWriteHelperTest {

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

	protected IContentDB buildContentDB(IDocumentDB documentsDB,
			IFeatureDB featuresDB) {
		TroveContentDBBuilder builder = new TroveContentDBBuilder(documentsDB,
				featuresDB);
		for (int i = 0; i < 5; i++) {
			builder.setDocumentFeatureFrequency(i, i, (i + 1));
		}
		return builder.getContentDB();
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

	protected IDomainDB buildDomainDB(ICategoryDB catsDB, IFeatureDB featsDB) {
		TroveDomainDB domainDB = new TroveDomainDB(catsDB, featsDB);
		TIntArrayList removedFeatures = new TIntArrayList();
		removedFeatures.add(0);
		removedFeatures.add(1);
		domainDB.removeCategoryFeatures((short) 0, new TIntArrayListIterator(
				removedFeatures));
		return domainDB;
	}

	protected IWeightingDB buildWeightingDB(IContentDB contentDB) {
		TroveWeightingDBBuilder builder = new TroveWeightingDBBuilder(contentDB);
		for (int i = 0; i < 5; i++) {
			builder.setDocumentFeatureWeight(i, i, i + 1);
		}

		return builder.getWeightingDB();
	}

	@Test
	public void featuresDBWriteTest() {
		IFeatureDB featuresDB = buildFeaturesDB();
		IStorageManager storageManager = getStorageManager();

		String dbName = "features";

		try {
			TroveReadWriteHelper.writeFeatures(storageManager, featuresDB,
					dbName, true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalStateException);
		}

		try {
			storageManager.open();
			TroveReadWriteHelper.writeFeatures(null, featuresDB, dbName, true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof NullPointerException);
		}

		try {
			TroveReadWriteHelper.writeFeatures(storageManager, null, dbName,
					true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof NullPointerException);
		}

		try {
			TroveReadWriteHelper.writeFeatures(storageManager, featuresDB,
					null, true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		try {
			TroveReadWriteHelper.writeFeatures(storageManager, featuresDB, "",
					true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		TroveReadWriteHelper.writeFeatures(storageManager, featuresDB, dbName,
				true);
		IFeatureDB featuresDBRead = TroveReadWriteHelper.readFeatures(
				storageManager, dbName);
		assertTrue(featuresDBRead.getFeaturesCount() == 5);
	}

	@Test
	public void featuresDBReadTest() {
		IStorageManager storageManager = getStorageManager();
		IFeatureDB featuresDB = buildFeaturesDB();
		String dbName = "features";

		try {
			TroveReadWriteHelper.readFeatures(storageManager, "unknown");
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalStateException);
		}

		try {
			storageManager.open();
			TroveReadWriteHelper.writeFeatures(storageManager, featuresDB,
					dbName, true);
		} catch (Exception e) {
			fail();
		}

		try {
			TroveReadWriteHelper.readFeatures((IStorageManager) null, dbName);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof NullPointerException);
		}

		try {
			TroveReadWriteHelper.readFeatures(storageManager, null);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		try {
			TroveReadWriteHelper.readFeatures(storageManager, "");
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		try {
			TroveReadWriteHelper.readFeatures(storageManager, "unknown");
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		IFeatureDB read = TroveReadWriteHelper.readFeatures(storageManager,
				dbName);
		assertTrue(read.getFeaturesCount() == featuresDB.getFeaturesCount());
		IIntIterator it = read.getFeatures();
		while (it.hasNext()) {
			int featID = it.next();
			assertTrue(read.getFeatureName(featID).equals(
					featuresDB.getFeatureName(featID)));
		}
	}

	@Test
	public void documentsDBWriteTest() {
		IStorageManager storageManager = getStorageManager();
		IDocumentDB documentsDB = buildDocumentsDB();
		String dbName = "documents";

		try {
			TroveReadWriteHelper.writeDocuments(storageManager, documentsDB,
					dbName, true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalStateException);
		}

		try {
			storageManager.open();
			TroveReadWriteHelper
					.writeDocuments(null, documentsDB, dbName, true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof NullPointerException);
		}

		try {
			TroveReadWriteHelper.writeDocuments(storageManager, null, dbName,
					true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof NullPointerException);
		}

		try {
			TroveReadWriteHelper.writeDocuments(storageManager, documentsDB,
					null, true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		try {
			TroveReadWriteHelper.writeDocuments(storageManager, documentsDB,
					"", true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		TroveReadWriteHelper.writeDocuments(storageManager, documentsDB,
				dbName, true);
		IDocumentDB documentsDBRead = TroveReadWriteHelper.readDocuments(
				storageManager, dbName);
		assertTrue(documentsDBRead.getDocumentsCount() == 5);
	}

	@Test
	public void documentsDBReadTest() {
		IStorageManager storageManager = getStorageManager();
		IDocumentDB documentsDB = buildDocumentsDB();
		String dbName = "documents";

		try {
			TroveReadWriteHelper.readDocuments(storageManager, "unknown");
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalStateException);
		}

		try {
			storageManager.open();
			TroveReadWriteHelper.writeDocuments(storageManager, documentsDB,
					dbName, true);
		} catch (Exception e) {
			fail();
		}

		try {
			TroveReadWriteHelper.readDocuments((IStorageManager) null, dbName);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof NullPointerException);
		}

		try {
			TroveReadWriteHelper.readDocuments(storageManager, null);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		try {
			TroveReadWriteHelper.readDocuments(storageManager, "");
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		try {
			TroveReadWriteHelper.readDocuments(storageManager, "unknown");
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		IDocumentDB read = TroveReadWriteHelper.readDocuments(storageManager,
				dbName);
		assertTrue(read.getDocumentsCount() == documentsDB.getDocumentsCount());
		IIntIterator it = read.getDocuments();
		while (it.hasNext()) {
			int docID = it.next();
			assertTrue(read.getDocumentName(docID).equals(
					documentsDB.getDocumentName(docID)));
		}
	}

	@Test
	public void categoriesDBWriteTest() {
		IStorageManager storageManager = getStorageManager();
		ICategoryDB catsDB = buildCategoriesDB();
		String dbName = "categories";

		try {
			TroveReadWriteHelper.writeCategories(storageManager, catsDB,
					dbName, true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalStateException);
		}

		try {
			storageManager.open();
			TroveReadWriteHelper.writeCategories(null, catsDB, dbName, true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof NullPointerException);
		}

		try {
			TroveReadWriteHelper.writeCategories(storageManager, null, dbName,
					true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof NullPointerException);
		}

		try {
			TroveReadWriteHelper.writeCategories(storageManager, catsDB, null,
					true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		try {
			TroveReadWriteHelper.writeCategories(storageManager, catsDB, "",
					true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		TroveReadWriteHelper.writeCategories(storageManager, catsDB, dbName,
				true);
		ICategoryDB catsDBRead = TroveReadWriteHelper.readCategories(
				storageManager, dbName);
		assertTrue(catsDB.getCategoriesCount() == catsDBRead
				.getCategoriesCount());

		ICategoryDB catsDB2 = buildCategoriesDB2();
		TroveReadWriteHelper.writeCategories(storageManager, catsDB, dbName,
				false);
		catsDBRead = TroveReadWriteHelper
				.readCategories(storageManager, dbName);
		assertTrue(catsDB2.getCategoriesCount() != catsDBRead
				.getCategoriesCount());

	}

	@Test
	public void categoriesDBReadTest() {
		IStorageManager storageManager = getStorageManager();
		ICategoryDB catsDB = buildCategoriesDB();
		String dbName = "categories";

		try {
			TroveReadWriteHelper.readCategories(storageManager, "unknown");
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalStateException);
		}

		try {
			storageManager.open();
			TroveReadWriteHelper.writeCategories(storageManager, catsDB,
					dbName, true);
		} catch (Exception e) {
			fail();
		}

		try {
			TroveReadWriteHelper.readCategories((IStorageManager) null, dbName);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof NullPointerException);
		}

		try {
			TroveReadWriteHelper.readCategories(storageManager, null);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		try {
			TroveReadWriteHelper.readCategories(storageManager, "");
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		try {
			TroveReadWriteHelper.readCategories(storageManager, "unknown");
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		try {
			ICategoryDB read = TroveReadWriteHelper.readCategories(
					storageManager, "categories");
			assertTrue(read.getCategoriesCount() == catsDB.getCategoriesCount());
			IShortIterator it = read.getCategories();
			while (it.hasNext()) {
				short catID = it.next();
				assertTrue(catsDB.isValidCategory(catID));
				assertTrue(catsDB.getCategoryName(catID).equals(
						read.getCategoryName(catID)));
				assertTrue(catsDB.getChildCategoriesCount(catID) == read
						.getChildCategoriesCount(catID));
				IShortIterator children = read.getChildCategories(catID);
				if (children.hasNext()) {
					assertTrue(catsDB.hasChildCategories(catID));
				}
			}

		} catch (Exception e) {
			fail();
		}
	}

	@Test
	public void classificationDBWriteTest() {
		IDocumentDB docsDB = buildDocumentsDB();
		ICategoryDB catsDB = buildCategoriesDB();
		IClassificationDB classificationDB = buildClassificationDB(docsDB,
				catsDB);
		IStorageManager storageManager = getStorageManager();
		String dbName = "classification";

		try {
			TroveReadWriteHelper.writeClassification(storageManager,
					classificationDB, dbName, true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalStateException);
		}

		try {
			storageManager.open();
			TroveReadWriteHelper.writeClassification(null, classificationDB,
					dbName, true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof NullPointerException);
		}

		try {
			TroveReadWriteHelper.writeClassification(storageManager, null,
					dbName, true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof NullPointerException);
		}

		try {
			TroveReadWriteHelper.writeClassification(storageManager,
					classificationDB, null, true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		try {
			TroveReadWriteHelper.writeClassification(storageManager,
					classificationDB, "", true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		TroveReadWriteHelper.writeClassification(storageManager,
				classificationDB, dbName, true);
		IClassificationDB read = TroveReadWriteHelper.readClassification(
				storageManager, dbName, docsDB, catsDB,
				TroveClassificationDBType.Default);
		assertTrue(read.getName().equals(classificationDB.getName()));
		assertTrue(read.getCategoryDocumentsCount(catsDB.getCategory("cat4")) == classificationDB
				.getCategoryDocumentsCount(catsDB.getCategory("cat4")));
	}

	@Test
	public void classificationDBReadTest() {
		IDocumentDB docsDB = buildDocumentsDB();
		ICategoryDB catsDB = buildCategoriesDB();
		IClassificationDB classificationDB = buildClassificationDB(docsDB,
				catsDB);
		IStorageManager storageManager = getStorageManager();
		String dbName = "classification";

		try {
			TroveReadWriteHelper.readClassification(storageManager, "unknown",
					TroveClassificationDBType.Default);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalStateException);
		}

		try {
			storageManager.open();
			TroveReadWriteHelper.writeClassification(storageManager,
					classificationDB, dbName, true);
		} catch (Exception e) {
			fail();
		}

		try {
			TroveReadWriteHelper.readClassification((IStorageManager) null,
					dbName, TroveClassificationDBType.Default);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof NullPointerException);
		}

		try {
			TroveReadWriteHelper.readClassification(storageManager, null,
					TroveClassificationDBType.Default);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		try {
			TroveReadWriteHelper.readClassification(storageManager, "",
					TroveClassificationDBType.Default);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		try {
			TroveReadWriteHelper.readClassification(storageManager, "unknown",
					TroveClassificationDBType.Default);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		IClassificationDB read = TroveReadWriteHelper.readClassification(
				storageManager, dbName, docsDB, catsDB,
				TroveClassificationDBType.Default);
		assertTrue(read.getName().equals(classificationDB.getName()));
		for (int i = 1; i <= 5; i++) {
			if (i == 2 || i == 4)
				assertTrue(read.getCategoryDocumentsCount(read.getCategoryDB()
						.getCategory("cat" + i)) == 1);
			else
				assertTrue(read.getCategoryDocumentsCount(read.getCategoryDB()
						.getCategory("cat" + i)) == 0);

			assertTrue(read.getCategoryDocumentsCount(catsDB.getCategory("cat"
					+ i)) == classificationDB.getCategoryDocumentsCount(catsDB
					.getCategory("cat" + i)));
		}

	}

	@Test
	public void contentDBWriteTest() {
		IDocumentDB docsDB = buildDocumentsDB();
		IFeatureDB featuresDB = buildFeaturesDB();
		IContentDB contentDB = buildContentDB(docsDB, featuresDB);
		IStorageManager storageManager = getStorageManager();
		String dbName = "content";

		try {
			TroveReadWriteHelper.writeContent(storageManager, contentDB,
					dbName, true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalStateException);
		}

		try {
			storageManager.open();
			TroveReadWriteHelper.writeContent(null, contentDB, dbName, true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof NullPointerException);
		}

		try {
			TroveReadWriteHelper.writeContent(storageManager, null, dbName,
					true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof NullPointerException);
		}

		try {
			TroveReadWriteHelper.writeContent(storageManager, contentDB, null,
					true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		try {
			TroveReadWriteHelper.writeContent(storageManager, contentDB, "",
					true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		TroveReadWriteHelper.writeContent(storageManager, contentDB, dbName,
				true);
		IContentDB read = TroveReadWriteHelper.readContent(storageManager,
				dbName, docsDB, featuresDB);
		assertTrue(read.getName().equals(contentDB.getName()));
		assertTrue(read.getDocumentLength(0) == 1);
	}

	@Test
	public void contentDBReadTest() {
		IDocumentDB docsDB = buildDocumentsDB();
		IFeatureDB featuresDB = buildFeaturesDB();
		IContentDB contentDB = buildContentDB(docsDB, featuresDB);
		IStorageManager storageManager = getStorageManager();
		String dbName = "content";

		try {
			TroveReadWriteHelper.readContent(storageManager, "unknown");
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalStateException);
		}

		try {
			storageManager.open();
			TroveReadWriteHelper.writeContent(storageManager, contentDB,
					dbName, true);
		} catch (Exception e) {
			fail();
		}

		try {
			TroveReadWriteHelper.readContent((IStorageManager) null, dbName);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof NullPointerException);
		}

		try {
			TroveReadWriteHelper.readContent(storageManager, null);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		try {
			TroveReadWriteHelper.readContent(storageManager, "");
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		try {
			TroveReadWriteHelper.readContent(storageManager, "unknown");
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		IContentDB read = TroveReadWriteHelper.readContent(storageManager,
				dbName, docsDB, featuresDB);
		assertTrue(read.getName().equals(contentDB.getName()));
		for (int i = 0; i < 5; i++) {
			assertTrue(read.getDocumentFeaturesCount(i) == 1);
			assertTrue(read.getDocumentFeaturesCount(i) == contentDB
					.getDocumentFeaturesCount(i));
			assertTrue(read.getDocumentFeatureFrequency(i, i) == (i + 1));
			assertTrue(read.getDocumentFeatureFrequency(i, i) == contentDB
					.getDocumentFeatureFrequency(i, i));
		}
	}

	@Test
	public void domainDBWriteTest() {
		ICategoryDB catsDB = buildCategoriesDB();
		IFeatureDB featuresDB = buildFeaturesDB();
		IDomainDB domainDB = buildDomainDB(catsDB, featuresDB);
		IStorageManager storageManager = getStorageManager();
		String dbName = "domain";

		try {
			TroveReadWriteHelper.writeDomain(storageManager, domainDB, dbName,
					true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalStateException);
		}

		try {
			storageManager.open();
			TroveReadWriteHelper.writeDomain(null, domainDB, dbName, true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof NullPointerException);
		}

		try {
			TroveReadWriteHelper
					.writeDomain(storageManager, null, dbName, true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof NullPointerException);
		}

		try {
			TroveReadWriteHelper.writeDomain(storageManager, domainDB, null,
					true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		try {
			TroveReadWriteHelper
					.writeDomain(storageManager, domainDB, "", true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		TroveReadWriteHelper
				.writeDomain(storageManager, domainDB, dbName, true);
		IDomainDB read = TroveReadWriteHelper.readDomain(storageManager,
				dbName, catsDB, featuresDB);
		assertTrue(read.getName().equals(domainDB.getName()));
		assertTrue(domainDB.getCategoryFeaturesCount((short) 0) == 3);
	}

	@Test
	public void domainDBReadTest() {
		ICategoryDB catsDB = buildCategoriesDB();
		IFeatureDB featuresDB = buildFeaturesDB();
		IDomainDB domainDB = buildDomainDB(catsDB, featuresDB);
		IStorageManager storageManager = getStorageManager();
		String dbName = "domain";

		try {
			TroveReadWriteHelper.readDomain(storageManager, "unknown");
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalStateException);
		}

		try {
			storageManager.open();
			TroveReadWriteHelper.writeDomain(storageManager, domainDB, dbName,
					true);
		} catch (Exception e) {
			fail();
		}

		try {
			TroveReadWriteHelper.readDomain((IStorageManager) null, dbName);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof NullPointerException);
		}

		try {
			TroveReadWriteHelper.readDomain(storageManager, null);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		try {
			TroveReadWriteHelper.readDomain(storageManager, "");
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		try {
			TroveReadWriteHelper.readDomain(storageManager, "unknown");
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		IDomainDB read = TroveReadWriteHelper.readDomain(storageManager,
				dbName, catsDB, featuresDB);
		assertTrue(read.getName().equals(domainDB.getName()));

		assertTrue(read.hasCategoryFeature((short) 0, 2));
		assertTrue(read.hasCategoryFeature((short) 0, 3));
		assertTrue(read.hasCategoryFeature((short) 0, 4));
		assertTrue(!read.hasCategoryFeature((short) 0, 1));
		assertTrue(!read.hasCategoryFeature((short) 0, 0));
	}

	@Test
	public void weightingDBWriteTest() {
		IDocumentDB docsDB = buildDocumentsDB();
		IFeatureDB featuresDB = buildFeaturesDB();
		IContentDB contentDB = buildContentDB(docsDB, featuresDB);
		IWeightingDB weightingDB = buildWeightingDB(contentDB);
		IStorageManager storageManager = getStorageManager();
		String dbName = "weighting";

		try {
			TroveReadWriteHelper.writeWeighting(storageManager, weightingDB,
					dbName, true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalStateException);
		}

		try {
			storageManager.open();
			TroveReadWriteHelper
					.writeWeighting(null, weightingDB, dbName, true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof NullPointerException);
		}

		try {
			TroveReadWriteHelper.writeWeighting(storageManager, null, dbName,
					true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof NullPointerException);
		}

		try {
			TroveReadWriteHelper.writeWeighting(storageManager, weightingDB,
					null, true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		try {
			TroveReadWriteHelper.writeWeighting(storageManager, weightingDB,
					"", true);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		TroveReadWriteHelper.writeWeighting(storageManager, weightingDB,
				dbName, true);
		IWeightingDB read = TroveReadWriteHelper.readWeighting(storageManager,
				dbName, contentDB);
		assertTrue(read.getName().equals(contentDB.getName()));
	}

	@Test
	public void weightingDBReadTest() {
		IDocumentDB docsDB = buildDocumentsDB();
		IFeatureDB featuresDB = buildFeaturesDB();
		IContentDB contentDB = buildContentDB(docsDB, featuresDB);
		IWeightingDB weightingDB = buildWeightingDB(contentDB);
		IStorageManager storageManager = getStorageManager();
		String dbName = "weighting";

		try {
			TroveReadWriteHelper.readWeighting(storageManager, "unknown");
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalStateException);
		}

		try {
			storageManager.open();
			TroveReadWriteHelper.writeWeighting(storageManager, weightingDB,
					dbName, true);
		} catch (Exception e) {
			fail();
		}

		try {
			TroveReadWriteHelper.readWeighting((IStorageManager) null, dbName);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof NullPointerException);
		}

		try {
			TroveReadWriteHelper.readWeighting(storageManager, null);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		try {
			TroveReadWriteHelper.readWeighting(storageManager, "");
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		try {
			TroveReadWriteHelper.readWeighting(storageManager, "unknown");
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		IWeightingDB read = TroveReadWriteHelper.readWeighting(storageManager,
				dbName, contentDB);
		assertTrue(read.getName().equals(weightingDB.getName()));

		for (int i = 0; i < 5; i++) {
			assertTrue(read.getDocumentFeatureWeight(i, i) == (i + 1));
			assertTrue(read.getDocumentFeatureWeight(i, i) == weightingDB
					.getDocumentFeatureWeight(i, i));
		}
	}
}
