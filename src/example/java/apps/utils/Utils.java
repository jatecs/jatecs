package apps.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import it.cnr.jatecs.classification.ClassificationMode;
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.module.Classifier;
import it.cnr.jatecs.classification.svm.SvmLearner;
import it.cnr.jatecs.classification.svmlight.SvmLightClassifierCustomizer;
import it.cnr.jatecs.classification.svmlight.SvmLightLearner;
import it.cnr.jatecs.classification.svmlight.SvmLightLearnerCustomizer;
import it.cnr.jatecs.evaluation.ClassificationComparer;
import it.cnr.jatecs.evaluation.ContingencyTableDataManager;
import it.cnr.jatecs.evaluation.ContingencyTableSet;
import it.cnr.jatecs.evaluation.util.EvaluationReport;
import it.cnr.jatecs.indexes.DB.generic.GenericIndex;
import it.cnr.jatecs.indexes.DB.generic.MultilingualIndex;
import it.cnr.jatecs.indexes.DB.interfaces.ICategoryDB;
import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDB;
import it.cnr.jatecs.indexes.DB.interfaces.IFeatureDB;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.interfaces.IMultilingualIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveCategoryDBBuilder;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveClassificationDBBuilder;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveClassificationDBType;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveContentDBBuilder;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveContentDBType;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveDocumentLanguageDB;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveDocumentsDBBuilder;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveDomainDB;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveFeatureDBBuilder;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveLanguagesDB;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveMultilingualReadWriteHelper;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveReadWriteHelper;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveWeightingDBBuilder;
import it.cnr.jatecs.indexes.utils.LanguageLabel;
import it.cnr.jatecs.indexing.tsr.InformationGain;
import it.cnr.jatecs.indexing.tsr.RoundRobinTSR;
import it.cnr.jatecs.indexing.weighting.IWeighting;
import it.cnr.jatecs.indexing.weighting.TfNormalizedIdf;
import it.cnr.jatecs.io.FileSystemStorageManager;
import it.cnr.jatecs.utils.Os;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

public class Utils {

	public static IIndex readIndex(String indexPath, String indexName) throws IOException {
		FileSystemStorageManager storageManager = new FileSystemStorageManager(indexPath, false);
		storageManager.open();
		IIndex index = TroveReadWriteHelper.readIndex(storageManager, indexName, TroveContentDBType.Full,
				TroveClassificationDBType.Full);
		storageManager.close();
		return index;
	}

	public static IMultilingualIndex readMultilingualIndex(String indexPath, String indexName) throws IOException {
		FileSystemStorageManager storageManager = new FileSystemStorageManager(indexPath, false);
		storageManager.open();
		IMultilingualIndex index = TroveMultilingualReadWriteHelper.readIndex(storageManager, indexName,
				TroveContentDBType.Full, TroveClassificationDBType.Full);
		storageManager.close();
		return index;
	}

	public static IClassificationDB readClassification(String indexPath, String indexName) throws IOException {
		FileSystemStorageManager storageManager = new FileSystemStorageManager(indexPath, false);
		storageManager.open();
		IClassificationDB classification = TroveReadWriteHelper.readClassification(storageManager, indexName);
		storageManager.close();
		return classification;
	}

	public static void evaluation(IClassificationDB predictions, IClassificationDB trueValues,
			String experimentResultsDir, String indexTestingFile) throws IOException {
		ClassificationComparer flatComparer = new ClassificationComparer(predictions, trueValues);
		ContingencyTableSet tableSet = flatComparer.evaluate(false);
		String flatResultPath = experimentResultsDir + Os.pathSeparator() + "flatEvalTable_" + indexTestingFile;
		writeComparison(tableSet, predictions, trueValues, flatResultPath);
	}

	public static void writeMultilingualIndex(IMultilingualIndex multilingIndex, String indexPath, String indexName,
			boolean override) throws IOException {
		FileSystemStorageManager storageManager = new FileSystemStorageManager(indexPath, true);
		storageManager.open();
		TroveMultilingualReadWriteHelper.writeIndex(storageManager, multilingIndex, indexName, override);
		storageManager.close();
	}

	public static void writeComparison(ContingencyTableSet tableSet, IClassificationDB predictions,
			IClassificationDB trueValues, String resultPath) throws IOException {
		tableSet.setName(predictions.getName());
		ContingencyTableDataManager.writeContingencyTableSet(resultPath, tableSet);
		String report = EvaluationReport.printReport(tableSet, trueValues.getCategoryDB());
		String summaryPath = resultPath + ".txt";
		FileWriter writer = new FileWriter(summaryPath);
		writer.write(report);
		writer.close();
	}

	public static IClassificationDB testClassifier(IIndex indexTesting, IClassifier classifier,
			String experimentResultsDir, String writeClassResults, ClassificationMode mode) throws IOException {
		Classifier classifierModule = new Classifier(indexTesting, classifier, false);
		classifierModule.setClassificationMode(mode);
		classifierModule.exec();

		IClassificationDB testClassification = classifierModule.getClassificationDB();
		if (experimentResultsDir != null && writeClassResults != null) {
			FileSystemStorageManager storageManager = new FileSystemStorageManager(experimentResultsDir, false);
			storageManager.open();
			TroveReadWriteHelper.writeClassification(storageManager, testClassification, writeClassResults, true);
			storageManager.close();
		}
		return testClassification;
	}
	
	public static IClassificationDB testClassifier(IIndex indexTesting, IClassifier classifier, ClassificationMode mode) throws IOException {
		return testClassifier(indexTesting, classifier, null, null, mode);
	}

	public static IClassifier trainSVMlight(IIndex indexTraining, String svmlightConfigPath)
			throws FileNotFoundException, IOException {
		Properties svmlightconf = readProperties(svmlightConfigPath);
		String path_svmlight = svmlightconf.getProperty("SVMlight_PATH");
		SvmLightLearnerCustomizer customizer = new SvmLightLearnerCustomizer(
				path_svmlight + Os.pathSeparator() + "svm_learn");
		customizer.setDeleteTrainingFiles(true);
		customizer.printSvmLightOutput(true);
		SvmLightLearner learner = new SvmLightLearner();
		learner.setRuntimeCustomizer(customizer);

		IClassifier classifier = learner.build(indexTraining);

		SvmLightClassifierCustomizer classifierCustomizer = new SvmLightClassifierCustomizer(
				path_svmlight + Os.pathSeparator() + "svm_classify");
		classifierCustomizer.setDeletePredictionsFiles(true);
		classifierCustomizer.setDeleteTestFiles(true);
		classifierCustomizer.printSvmLightOutput(false);
		classifier.setRuntimeCustomizer(classifierCustomizer);

		return classifier;
	}


	public static Properties readProperties(String propFile) {
		Properties prop = new Properties();
		InputStream input = null;

		try {
			input = new FileInputStream(propFile);
			prop.load(input);
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return prop;
	}

	public static IIndex leaveOnlyCat(IIndex index, short cat) {

		TroveCategoryDBBuilder category = new TroveCategoryDBBuilder();
		String catname = index.getCategoryDB().getCategoryName(cat);
		category.addCategory(catname);
		short onlyCat = category.getCategoryDB().getCategory(catname);

		System.out.println(
				"Leaving only cat <" + catname + "> with " + index.getClassificationDB().getCategoryDocumentsCount(cat)
						+ "/" + index.getDocumentDB().getDocumentsCount() + " documents");

		TroveDomainDB domain = new TroveDomainDB(category.getCategoryDB(), index.getFeatureDB());

		TroveClassificationDBBuilder classification = new TroveClassificationDBBuilder(index.getDocumentDB(),
				category.getCategoryDB());
		for (IIntIterator docs = index.getDocumentDB().getDocuments(); docs.hasNext();) {
			int docid = docs.next();
			if (index.getClassificationDB().hasDocumentCategory(docid, cat)) {
				classification.setDocumentCategory(docid, onlyCat);
			}
		}

		return new GenericIndex(index.getFeatureDB(), index.getDocumentDB(), category.getCategoryDB(), domain,
				index.getContentDB(), index.getWeightingDB(), classification.getClassificationDB());
	}

	public static IIndex weightTFIDFtrain(IIndex trainingIndex) {
		IWeighting weightingtraining = new TfNormalizedIdf(trainingIndex);
		return weightingtraining.computeWeights(trainingIndex);
	}

	public static IIndex weightTFIDFtest(IIndex trainingIndex, IIndex testIndex) {
		IWeighting weighting = new TfNormalizedIdf(trainingIndex);
		return weighting.computeWeights(testIndex);
	}

	public static IMultilingualIndex weightTFIDFtrainMultilingual(IMultilingualIndex trainingIndex) {
		IIndex weightedIndex = weightTFIDFtrain(trainingIndex);
		return new MultilingualIndex(weightedIndex, trainingIndex.getDocumentLanguageDB());
	}

	public static IMultilingualIndex weightTFIDFtestMultilingual(IMultilingualIndex trainingIndex,
			IMultilingualIndex testIndex) {
		IIndex weightedIndex = weightTFIDFtest(trainingIndex, testIndex);
		return new MultilingualIndex(weightedIndex, testIndex.getDocumentLanguageDB());
	}

	public static void featureSelectionRR(IIndex trainIndex, IIndex testIndex, double ratio) {
		int nT = trainIndex.getFeatureDB().getFeaturesCount();
		RoundRobinTSR rrTSR = new RoundRobinTSR(new InformationGain());
		rrTSR.setNumberOfBestFeatures((int) (nT * ratio));
		rrTSR.computeTSR(trainIndex, testIndex);
	}

	public static IMultilingualIndex joinIndexes(IIndex sourceIndex, IIndex targetIndex, LanguageLabel sourceLang,
			LanguageLabel targetLang) {

		ICategoryDB categories = sourceIndex.getCategoryDB();
		TroveDocumentsDBBuilder documents = new TroveDocumentsDBBuilder();
		TroveFeatureDBBuilder features = new TroveFeatureDBBuilder();
		TroveContentDBBuilder content = new TroveContentDBBuilder(documents.getDocumentDB(), features.getFeatureDB());
		TroveWeightingDBBuilder weighting = new TroveWeightingDBBuilder(content.getContentDB());
		TroveDomainDB domain = new TroveDomainDB(categories, features.getFeatureDB());
		TroveClassificationDBBuilder classification = new TroveClassificationDBBuilder(documents.getDocumentDB(),
				categories);
		TroveLanguagesDB languages = new TroveLanguagesDB();
		TroveDocumentLanguageDB documentLanguages = new TroveDocumentLanguageDB(documents.getDocumentDB(), languages);

		indexFeatures(sourceIndex, features);
		indexFeatures(targetIndex, features);

		indexDocuments(sourceIndex, sourceLang, documents, features.getFeatureDB(), content, weighting, classification,
				languages, documentLanguages, "joinLeft_");
		indexDocuments(targetIndex, targetLang, documents, features.getFeatureDB(), content, weighting, classification,
				languages, documentLanguages, "joinRight_");

		IIndex index = new GenericIndex("Cross-Language Training Index", features.getFeatureDB(),
				documents.getDocumentDB(), categories, domain, content.getContentDB(), weighting.getWeightingDB(),
				classification.getClassificationDB());

		IMultilingualIndex clindex = new MultilingualIndex(index, documentLanguages);

		return clindex;
	}

	private static void indexFeatures(IIndex index, TroveFeatureDBBuilder features) {
		IIntIterator feats = index.getFeatureDB().getFeatures();
		while (feats.hasNext()) {
			int featID = feats.next();
			String featname = index.getFeatureDB().getFeatureName(featID);
			if (features.getFeatureDB().getFeature(featname) == -1)
				features.addFeature(featname);
		}
	}

	private static void indexDocuments(IIndex index, LanguageLabel lang, TroveDocumentsDBBuilder documents,
			IFeatureDB features, TroveContentDBBuilder content, TroveWeightingDBBuilder weighting,
			TroveClassificationDBBuilder classification, TroveLanguagesDB languages,
			TroveDocumentLanguageDB documentLanguages, String docPrefix) {

		IIntIterator docs = index.getDocumentDB().getDocuments();
		while (docs.hasNext()) {
			int docID = docs.next();
			String documentName = docPrefix + index.getDocumentDB().getDocumentName(docID);

			int realDocIndex = documents.addDocument(documentName);
			IIntIterator featsInDoc = index.getContentDB().getDocumentFeatures(docID);
			while (featsInDoc.hasNext()) {
				int featID = featsInDoc.next();
				String featName = index.getFeatureDB().getFeatureName(featID);
				int reaFeatIndex = features.getFeature(featName);

				int count = index.getContentDB().getDocumentFeatureFrequency(docID, featID);
				if (count != 0) {
					int old = content.getContentDB().getDocumentFeatureFrequency(realDocIndex, reaFeatIndex);
					content.setDocumentFeatureFrequency(realDocIndex, reaFeatIndex, old + count);
				}

				double weight = index.getWeightingDB().getDocumentFeatureWeight(docID, featID);
				if (weight != 0.0) {
					double old = weighting.getWeightingDB().getDocumentFeatureWeight(realDocIndex, reaFeatIndex);
					weighting.setDocumentFeatureWeight(realDocIndex, reaFeatIndex, old + weight);
				}

				languages.addLanguage(lang);
				documentLanguages.indexDocLang(realDocIndex, lang);
			}

			IShortIterator cats = index.getClassificationDB().getDocumentCategories(docID);
			while (cats.hasNext()) {
				short catID = cats.next();
				classification.setDocumentCategory(realDocIndex, catID, true);
			}
		}
	}

}
