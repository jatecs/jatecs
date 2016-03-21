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

import it.cnr.jatecs.classification.adaboost.InitialDistributionMatrixType;
import it.cnr.jatecs.classification.adaboost.LogitLoss;
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.module.Classifier;
import it.cnr.jatecs.classification.weightedboost.WeightedBoostLearner;
import it.cnr.jatecs.classification.weightedboost.WeightedBoostLearnerCustomizer;
import it.cnr.jatecs.classification.weightedboost.WeightedWeakLearner;
import it.cnr.jatecs.evaluation.ClassificationComparer;
import it.cnr.jatecs.evaluation.ContingencyTableSet;
import it.cnr.jatecs.evaluation.util.EvaluationReport;
import it.cnr.jatecs.indexes.DB.interfaces.ICategoryDB;
import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDB;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveCategoryDBBuilder;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveClassificationDBType;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveContentDBType;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveDependentIndexBuilder;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveMainIndexBuilder;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveReadWriteHelper;
import it.cnr.jatecs.indexing.corpus.BagOfWordsFeatureExtractor;
import it.cnr.jatecs.indexing.corpus.CorpusReader;
import it.cnr.jatecs.indexing.corpus.FeatureExtractor;
import it.cnr.jatecs.indexing.corpus.FileCategoryReader;
import it.cnr.jatecs.indexing.corpus.SetType;
import it.cnr.jatecs.indexing.corpus.Reuters21578.Reuters21578CorpusReader;
import it.cnr.jatecs.indexing.corpus.Reuters21578.Reuters21578SplitType;
import it.cnr.jatecs.indexing.discretization.EqualFrequencyDiscretizer;
import it.cnr.jatecs.indexing.module.FullIndexConstructor;
import it.cnr.jatecs.indexing.preprocessing.EnglishPorterStemming;
import it.cnr.jatecs.indexing.preprocessing.EnglishStopword;
import it.cnr.jatecs.indexing.tsr.GlobalTSR;
import it.cnr.jatecs.indexing.tsr.ITsrFunction;
import it.cnr.jatecs.indexing.tsr.InformationGain;
import it.cnr.jatecs.indexing.tsr.WeightedSumTSRPolicy;
import it.cnr.jatecs.indexing.weighting.IWeighting;
import it.cnr.jatecs.indexing.weighting.TfNormalizedIdf;
import it.cnr.jatecs.io.FileSystemStorageManager;
import it.cnr.jatecs.utils.Os;

import java.io.IOException;

public class TestWeightedBoost {

	public static void main(String[] args) throws IOException {

		if (args.length != 2) {
			System.out
					.println("Usage: IndexReuters21578 <corpusPath> <categoryFile>");
			return;
		}

		try {
			String corpusPath = args[0];
			String categoriesFile = args[1];

			// DOMAIN
			TroveCategoryDBBuilder categoryDBBuilder = new TroveCategoryDBBuilder();
			FileCategoryReader categoriesReader = new FileCategoryReader(
					categoriesFile, categoryDBBuilder);
			ICategoryDB categoryDB = categoriesReader.getCategoryDB();

			// CORPUS READER
			Reuters21578CorpusReader corpusReader = new Reuters21578CorpusReader(
					categoryDB);

			corpusReader.excludeDocumentsWithoutValidCategories(false);
			corpusReader.setInputDir(corpusPath);
			corpusReader.setSplitType(Reuters21578SplitType.APTE);

			FeatureExtractor extractor = new BagOfWordsFeatureExtractor();

			extractor.disableEntitiesSubstitution();
			extractor.disableSpecialTermsSubstitution();
			extractor.disableSpellChecking();
			extractor.disableTFFeatures();
			extractor.enableStemming(new EnglishPorterStemming());
			extractor.enableStopwordRemoval(new EnglishStopword());

			// TRAINING INDEX
			corpusReader.setDocumentSetType(SetType.TRAINING);
			IIndex training = indexTraining(categoryDB, corpusReader, extractor);

			// TEST INDEX
			corpusReader.setDocumentSetType(SetType.TEST);
			IIndex test = indexTest(corpusReader, extractor, training);

			WeightedBoostLearner learner = new WeightedBoostLearner();
			WeightedBoostLearnerCustomizer customizer = new WeightedBoostLearnerCustomizer();
			customizer.setNumIterations(500);
			customizer.setWeakLearner(new WeightedWeakLearner());
			customizer.setPerCategoryNormalization(true);
			customizer.setLossFunction(new LogitLoss());
			customizer
					.setInitialDistributionType(InitialDistributionMatrixType.UNIFORM);
			customizer.keepDistributionMatrix(false);
			// EqualWidthDiscretizer discretizer = new EqualWidthDiscretizer();
			// discretizer.setNumBins(4);
			EqualFrequencyDiscretizer discretizer = new EqualFrequencyDiscretizer();
			discretizer.setNumBins(4);
			customizer.setDiscretizer(discretizer);
			learner.setRuntimeCustomizer(customizer);

			IClassifier classifier = learner.build(training);
			Classifier classifierModule = new Classifier(test, classifier,
					false);
			classifierModule.exec();

			IClassificationDB testClassification = classifierModule
					.getClassificationDB();

			ClassificationComparer cc = new ClassificationComparer(
					testClassification, test.getClassificationDB());
			ContingencyTableSet ts = cc.evaluate();
			String results = EvaluationReport.printReport(ts,
					training.getCategoryDB());
			System.out.println(results);

		} catch (Exception e) {
			throw new RuntimeException("Error", e);
		}

	}

	private static IIndex indexTraining(ICategoryDB categoryDB,
			CorpusReader corpusReader, FeatureExtractor extractor)
			throws Exception {
		TroveMainIndexBuilder trainingIndexBuilder = new TroveMainIndexBuilder(
				categoryDB);
		FullIndexConstructor traningIndexConstructor = new FullIndexConstructor(
				corpusReader, trainingIndexBuilder);

		traningIndexConstructor.setFeatureExtractor(extractor);

		traningIndexConstructor.exec();

		IIndex index = traningIndexConstructor.index();

		FileSystemStorageManager storageManager = new FileSystemStorageManager(
				Os.getTemporaryDirectory(), false);
		storageManager.open();
		TroveReadWriteHelper.writeIndex(storageManager, index, "index", true);
		index = TroveReadWriteHelper.readIndex(storageManager, "index",
				TroveContentDBType.Full, TroveClassificationDBType.Full);
		storageManager.close();

		int numFeatureToUse = index.getFeatureDB().getFeaturesCount();
		ITsrFunction tsrFunc = new InformationGain();
		GlobalTSR tsr = new GlobalTSR(tsrFunc, new WeightedSumTSRPolicy());
		tsr.setNumberOfBestFeatures(numFeatureToUse);
		tsr.computeTSR(index);

		/*
		 * BM25 weighting = new BM25(index); weighting.setUseOnlyTFPart(true);
		 */
		IWeighting weighting = new TfNormalizedIdf(index);

		index = weighting.computeWeights(index);

		return index;
	}

	private static IIndex indexTest(CorpusReader corpusReader,
			FeatureExtractor extractor, IIndex training) throws Exception {
		TroveDependentIndexBuilder testIndexBuilder = new TroveDependentIndexBuilder(
				training.getDomainDB());
		FullIndexConstructor testIndexConstructor = new FullIndexConstructor(
				corpusReader, testIndexBuilder);

		testIndexConstructor.setFeatureExtractor(extractor);

		testIndexConstructor.exec();

		IIndex index = testIndexConstructor.index();

		FileSystemStorageManager storageManager = new FileSystemStorageManager(
				Os.getTemporaryDirectory(), false);
		storageManager.open();
		TroveReadWriteHelper.writeIndex(storageManager, index, "index", true);
		index = TroveReadWriteHelper.readIndex(storageManager, "index",
				TroveContentDBType.Full, TroveClassificationDBType.Full);
		storageManager.close();

		/*
		 * BM25 weighting = new BM25(training);
		 * weighting.setUseOnlyTFPart(true);
		 */
		IWeighting weighting = new TfNormalizedIdf(training);

		index = weighting.computeWeights(index);

		return index;
	}
}
