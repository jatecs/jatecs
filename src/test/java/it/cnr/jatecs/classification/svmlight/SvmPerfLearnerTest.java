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

package it.cnr.jatecs.classification.svmlight;

import static org.junit.Assert.assertTrue;
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveCategoryDBBuilder;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveMainIndexBuilder;
import it.cnr.jatecs.indexing.tsr.GSS;
import it.cnr.jatecs.indexing.tsr.ITsrFunction;
import it.cnr.jatecs.indexing.tsr.LocalTSR;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.Os;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import org.junit.Test;

public class SvmPerfLearnerTest {

	private IIndex getIndex() {
		TroveCategoryDBBuilder catBuilder = new TroveCategoryDBBuilder();
		catBuilder.addCategory("c1");
		catBuilder.addCategory("c2");
		catBuilder.addCategory("c3");
		TroveMainIndexBuilder builder = new TroveMainIndexBuilder(
				catBuilder.getCategoryDB());
		builder.addDocument("d1", new String[] { "f1", "f2", "f3" },
				new String[] { "c1" });
		builder.addDocument("d2", new String[] { "f1", "f2", "f4" },
				new String[] { "c1" });
		builder.addDocument("d3", new String[] { "f3", "f5", "f6" },
				new String[] { "c2" });
		builder.addDocument("d4", new String[] { "f3", "f4", "f5" },
				new String[] { "c2" });
		builder.addDocument("d5", new String[] { "f5", "f6", "f7" },
				new String[] { "c3" });
		builder.addDocument("d6", new String[] { "f1", "f7", "f8" },
				new String[] { "c3" });

		return builder.getIndex();
	}

	@Test
	public void testBuildGlobalIndex() {
		String path = System.getenv("SVMPERF_PATH");
		if (path == null) {
			JatecsLogger
					.execution()
					.warning(
							"Can't run the SvmPerfLearnerTest.testBuildGlobalIndex test. You must set the SVMPERF_PATH environment variable pointing to the directoty containing the svm_perf_learn executable."
									+ Os.newline());
			return;
		}
		SvmPerfLearnerCustomizer customizer = new SvmPerfLearnerCustomizer(path
				+ Os.pathSeparator() + "svm_perf_learn");
		customizer.printSvmPerfOutput(true);

		SvmPerfLearner learner = new SvmPerfLearner();
		learner.setRuntimeCustomizer(customizer);

		IIndex training = getIndex();

		IClassifier classifier = learner.build(training);

		assertTrue(classifier != null);

		SvmPerfClassifierCustomizer classifierCustomizer = new SvmPerfClassifierCustomizer(
				path + Os.pathSeparator() + "svm_perf_classify");
		classifierCustomizer.printSvmPerfOutput(true);

		classifier.setRuntimeCustomizer(classifierCustomizer);

		IShortIterator cats = training.getCategoryDB().getCategories();
		while (cats.hasNext()) {
			short catID = cats.next();
			classifier.classify(training, catID);
		}
	}

	@Test
	public void testBuildLocalIndex() {
		String path = System.getenv("SVMPERF_PATH");
		if (path == null) {
			JatecsLogger
					.execution()
					.warning(
							"Can't run the SvmPerfLearnerTest.testBuildLocalIndex test. You must set the SVMPERF_PATH environment variable pointing to the directoty containing the svm_perf_learn executable."
									+ Os.newline());
			return;
		}
		SvmPerfLearnerCustomizer customizer = new SvmPerfLearnerCustomizer(path
				+ Os.pathSeparator() + "svm_perf_learn");
		customizer.printSvmPerfOutput(true);

		SvmPerfLearner learner = new SvmPerfLearner();
		learner.setRuntimeCustomizer(customizer);

		IIndex training = getIndex();

		ITsrFunction func = new GSS();
		LocalTSR tsr = new LocalTSR(func);

		tsr.setNumberOfBestFeaturesForCategory(2);

		tsr.computeTSR(training);

		IClassifier classifier = learner.build(training);

		assertTrue(classifier != null);

		SvmPerfClassifierCustomizer classifierCustomizer = new SvmPerfClassifierCustomizer(
				path + Os.pathSeparator() + "svm_perf_classify");
		classifierCustomizer.printSvmPerfOutput(true);

		classifier.setRuntimeCustomizer(classifierCustomizer);

		IShortIterator cats = training.getCategoryDB().getCategories();
		while (cats.hasNext()) {
			short catID = cats.next();
			classifier.classify(training, catID);
		}
	}

}
