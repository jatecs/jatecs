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

package it.cnr.jatecs.classification;

import it.cnr.jatecs.classification.adaboost.AdaBoostDataManager;
import it.cnr.jatecs.classification.adaboost.AdaBoostLearner;
import it.cnr.jatecs.classification.adaboost.AdaBoostLearnerCustomizer;
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveCategoryDBBuilder;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveMainIndexBuilder;
import it.cnr.jatecs.io.RamStorageManager;

import java.io.IOException;

import org.junit.Test;

public class AdaBoostDataManagerTest {

	protected IIndex buildIndex() {
		TroveCategoryDBBuilder catsBuilder = new TroveCategoryDBBuilder();
		catsBuilder.addCategory("cat1");
		catsBuilder.addCategory("cat2");
		catsBuilder.addCategory("cat3");
		catsBuilder.addCategory("cat4");
		catsBuilder.setParentCategory("cat2", "cat1");
		catsBuilder.setParentCategory("cat3", "cat1");

		TroveMainIndexBuilder ib = new TroveMainIndexBuilder(
				catsBuilder.getCategoryDB());

		ib.addDocument("doc1", new String[] { "This", "is", "a", "test",
				"very", "short" }, new String[] { "cat4" });
		ib.addDocument("doc2", new String[] { "This", "is", "another", "test",
				"very", "short", "slightly", "different" },
				new String[] { "cat2" });
		ib.addDocument("doc3", new String[] { "A", "very", "nice", "day", "in",
				"Pisa" }, new String[] { "cat3" });

		return ib.getIndex();
	}

	@Test
	public void writeLearnerTest() {
		IIndex trainingIndex = buildIndex();
		RamStorageManager storageManager = new RamStorageManager();
		try {
			storageManager.open();
		} catch (IOException e) {
			throw new RuntimeException("Bug in test code", e);
		}
		AdaBoostLearner learner = new AdaBoostLearner();
		AdaBoostLearnerCustomizer customizer = (AdaBoostLearnerCustomizer) learner
				.getRuntimeCustomizer();
		learner.setRuntimeCustomizer(customizer);
		IClassifier classifier = learner.build(trainingIndex);
		AdaBoostDataManager dataManager = new AdaBoostDataManager();
		dataManager.write(storageManager, "cl1", classifier);
	}

	@Test
	public void readLearnerTest() {
		IIndex trainingIndex = buildIndex();
		RamStorageManager storageManager = new RamStorageManager();
		try {
			storageManager.open();
		} catch (IOException e) {
			throw new RuntimeException("Bug in test code", e);
		}
		AdaBoostLearner learner = new AdaBoostLearner();
		AdaBoostLearnerCustomizer customizer = (AdaBoostLearnerCustomizer) learner
				.getRuntimeCustomizer();
		learner.setRuntimeCustomizer(customizer);
		IClassifier classifier = learner.build(trainingIndex);
		AdaBoostDataManager dataManager = new AdaBoostDataManager();
		dataManager.write(storageManager, "cl1", classifier);

		IClassifier readClassifier = dataManager.read(storageManager, "cl1");
		assert (readClassifier != null);
	}
}
