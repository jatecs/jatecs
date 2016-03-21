package apps.dataset;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import apps.utils.Utils;
import it.cnr.jatecs.indexes.DB.generic.GenericIndex;
import it.cnr.jatecs.indexes.DB.generic.MultilingualIndex;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.interfaces.IIndexBuilder;
import it.cnr.jatecs.indexes.DB.interfaces.IMultilingualIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveCategoryDBBuilder;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveContentDBBuilder;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveDocumentLanguageDB;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveLanguagesDB;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveMainIndexBuilder;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveWeightingDBBuilder;
import it.cnr.jatecs.indexes.utils.LanguageLabel;
import it.cnr.jatecs.indexing.corpus.CorpusCategory;
import it.cnr.jatecs.indexing.corpus.CorpusDocument;
import it.cnr.jatecs.indexing.corpus.DocumentType;
import it.cnr.jatecs.indexing.corpus.WebisCLS1.WebisCLS1CorpusReader;
import it.cnr.jatecs.utils.Os;
import it.cnr.jatecs.utils.iterators.IntArrayIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

public class IndexWebisCLS1 {
	
	private static final String catBooks="books"; 
	private static final String catDVD="dvd";
	private static final String catMusic="music";
	
	private static final String corpusTrain="train";
	private static final String corpusTest="test";
	private static final String corpusUnlabel="unlabeled";
	
	private static final String classPositive="positive";
	
	private static final String dirSentimentAnalysisTask="SentimentAnalysis";
	private static final String dirTopicClassificationTask="TopicClassification";
	private static final String bar=Os.pathSeparator();
	
	private static String[] productCategories = {catBooks, catDVD, catMusic};
	private static String[] corpusTypes = {corpusTrain, corpusTest, corpusUnlabel};
	
	private static boolean computeTFIDF=false;
	
	private static LanguageLabel[] langs = {LanguageLabel.en, LanguageLabel.de, LanguageLabel.fr, LanguageLabel.ja};
	private static LanguageLabel sourceLang = LanguageLabel.en;
	private static LanguageLabel[] targetlangs = {LanguageLabel.de, LanguageLabel.fr, LanguageLabel.ja};
	

	/**
	 * Creates the Jatecs index for processed documents of the Webis-CLS-10 sentiment corpus in 
	 * http://www.uni-weimar.de/en/media/chairs/webis/research/corpora/corpus-webis-cls-10/
	 * Reference: 
	 * 
	 * [1] Peter Prettenhofer and Benno Stein. Cross-Language Text Classification using Structural 
	 * Correspondence Learning. In 48th Annual Meeting of the Association of Computational 
	 * Linguistics (ACL 10), pages 1118-1127, July 2010. Association for Computational Linguistics
	 * 
	 * @param args
	 * @throws IOException 	
	 */
	public static void main(String[] args) throws IOException {
		
			
			if(args.length!=3){
				System.err.println("Parameter Error"+
						"\n\t<InputBase>: processed corpus from webis-CLS-10"+
						"\n\t<OutputDir>"+
						"\n\t<Tfidf (true o false)>");
				System.exit(0);
			}
			
			String inputBase = args[0];
			String outBase= args[1];
			computeTFIDF = Boolean.parseBoolean(args[2]);			
			
			//-----------------------------------------------------------------------------------
			
			//generate the base indexes for Sentiment Analysis
			generateBaseSAindexes(inputBase, outBase);
			
			//generate each lang-category domain folder
			generateCrossDomainCrossLingualSAtasks(outBase);
			
			//-----------------------------------------------------------------------------------
			//generate the base indexes for topic classification
			generateBaseTopicClassificationIndexes(inputBase, outBase);
			
			//generate the monolingual topic classification tasks (to establish a upper-bound)	
			generateMonolingualTopicTasks(outBase);
			
			//generate the cross-lingual topic classification tasks (English is always the source, 
			//German, French, and Japanese are the target languages) 		
			generateCrossLingualTopicTasks(outBase);
			
			System.out.println("Done!");
		
	}
	
	private static void generateCrossLingualTopicTasks(String outBase) throws IOException {
		String sourceIndexPath = outBase+bar+dirTopicClassificationTask+bar+sourceLang;
		IMultilingualIndex sourceTrain = Utils.readMultilingualIndex(sourceIndexPath, corpusTrain);
		IMultilingualIndex sourceUnlabel = Utils.readMultilingualIndex(sourceIndexPath, corpusUnlabel);
		
		sourceUnlabel = asDependentIndex(sourceTrain, sourceUnlabel, false);
		
		for(LanguageLabel targetLang : targetlangs){			
			System.out.println("Generating indexes for language " + targetLang);
			String langString = langString(targetLang);										
			
			String targetIndexPath = outBase+bar+dirTopicClassificationTask+bar+langString;
			IMultilingualIndex targetTest = Utils.readMultilingualIndex(targetIndexPath, corpusTest);
			IMultilingualIndex targetUnlabel = Utils.readMultilingualIndex(targetIndexPath, corpusUnlabel);
			
			targetTest = asDependentIndex(targetUnlabel, targetTest, true);
			
			char sourcePre = sourceLang.toString().charAt(0);
			char targetPre = langString.charAt(0);
			String taskPrefix = (""+sourcePre+targetPre).toUpperCase();
			
			String taskPath = outBase+bar+"Task"+dirTopicClassificationTask+bar+taskPrefix;
			Utils.writeMultilingualIndex(sourceTrain, taskPath, "source"+corpusTrain, true);
			Utils.writeMultilingualIndex(sourceUnlabel, taskPath, "source"+corpusUnlabel, true);
			Utils.writeMultilingualIndex(targetTest, taskPath, "target"+corpusTest, true);
			Utils.writeMultilingualIndex(targetUnlabel, taskPath, "target"+corpusUnlabel, true);					
		}		
	}

	private static void generateMonolingualTopicTasks(String outBase) throws IOException {
		for(LanguageLabel lang : langs){
			System.out.println("Generating indexes for language " + lang);
			String langString = langString(lang);
			String indexPath = outBase+bar+dirTopicClassificationTask+bar+langString;
			IMultilingualIndex train = Utils.readMultilingualIndex(indexPath, corpusTrain);
			IMultilingualIndex test = Utils.readMultilingualIndex(indexPath, corpusTest);
			
			test = asDependentIndex(train, test, true);
			
			char langPre = langString.charAt(0);
			String taskPrefix = "Mono"+((""+langPre).toUpperCase());
			
			String taskPath = outBase+bar+"Task"+dirTopicClassificationTask+bar+taskPrefix;
			Utils.writeMultilingualIndex(train, taskPath, corpusTrain, true);
			Utils.writeMultilingualIndex(test, taskPath, corpusTest, true);					
		}		
	}

	private static void generateBaseTopicClassificationIndexes(String inputBase, String outBase) throws IOException {
		TroveCategoryDBBuilder topicCatBuilder = new TroveCategoryDBBuilder();
		topicCatBuilder.addCategory(catBooks);
		topicCatBuilder.addCategory(catDVD);
		topicCatBuilder.addCategory(catMusic);		
		
		System.out.println("Generating Topic Classification base Indexes");
		int maxDocuments=2000;
		
		for(LanguageLabel lang : langs){				
			System.out.println("Generating indexes for language " + lang);
			String langString = langString(lang);
			
			String outPath = outBase+bar+dirTopicClassificationTask+bar+langString;
			String inputDir=inputBase+bar+langString;
			
			for(String corpusType:corpusTypes){
				System.out.println("\t\tCorpus Type " + corpusType);
				
				if(corpusType.equals(corpusUnlabel)){
					//there are >50,000 unlabeled documents for all languages but for French
					//according to [1] 60,000 unlabeled docs are taken for topic classification
					//this means 20,000 for each topic. For French however we take 
					//(first)25,000 + (all)9,000 + (all)16,000, so the maximum num of docs for Frenc is 25,000
					if(langString.equals(LanguageLabel.fr)){
						maxDocuments=25000;
					}
					else{
						maxDocuments=20000;
					}
				}
				else{
					//take 2,000x3 = 6,000 train/test docs
					maxDocuments=2000;
				}
							
				generateTCTaskIndexFromFile(inputDir, corpusType, topicCatBuilder, getDocType(corpusType), lang, outPath, corpusType, maxDocuments);
			}
		}		
	}

	private static void generateCrossDomainCrossLingualSAtasks(String baseSApath) throws IOException {
		for(LanguageLabel lang : langs){
			String langString = langString(lang);
			for(String dom : productCategories){
				String indexPath = baseSApath+bar+dirSentimentAnalysisTask+bar+langString+bar+dom;
				IMultilingualIndex sourceTrain = Utils.readMultilingualIndex(indexPath, corpusTrain);
				IMultilingualIndex sourceUnlabel = Utils.readMultilingualIndex(indexPath, corpusUnlabel);
				sourceTrain=Utils.weightTFIDFtrainMultilingual(sourceTrain);
				sourceUnlabel = asDependentIndex(sourceTrain, sourceUnlabel, false);
				
				IMultilingualIndex targetTest = Utils.readMultilingualIndex(indexPath, corpusTest);
				IMultilingualIndex targetUnlabel = Utils.readMultilingualIndex(indexPath, corpusUnlabel);
				targetUnlabel = Utils.weightTFIDFtrainMultilingual(targetUnlabel);
				targetTest = asDependentIndex(targetUnlabel, targetTest, true);
				
				char langAcronym = langString.charAt(0);
				char domAcronym = dom.charAt(0);
				String taskPrefix = (""+langAcronym+domAcronym).toUpperCase();
				
				System.out.println("Generating indexes for task "+taskPrefix);
				
				String taskPath = baseSApath+bar+"TaskCrossCross"+dirSentimentAnalysisTask+bar+taskPrefix;
				Utils.writeMultilingualIndex(sourceTrain, taskPath, "source"+corpusTrain, true);
				Utils.writeMultilingualIndex(sourceUnlabel, taskPath, "source"+corpusUnlabel, true);
				Utils.writeMultilingualIndex(targetTest, taskPath, "target"+corpusTest, true);
				Utils.writeMultilingualIndex(targetUnlabel, taskPath, "target"+corpusUnlabel, true);					
		
			}	
		}
	}


	private static void generateBaseSAindexes(String inputBase, String outBase) throws IOException{
		TroveCategoryDBBuilder sentimentCatBuilder = new TroveCategoryDBBuilder();
		sentimentCatBuilder.addCategory(classPositive);
		
		int maxDocuments=50000;
		
		System.out.println("Generating Sentiment Analysis Indexes");		
		for(LanguageLabel lang : langs){
			System.out.println("Generating indexes for language " + lang);
			String langString = langString(lang);
			for(String prodCat : productCategories){			
				System.out.println("\tProduct Category " + prodCat);					
				
				String inputDir=inputBase+bar+langString+bar+prodCat;
				String outPath = outBase+bar+dirSentimentAnalysisTask+bar+langString+bar+prodCat;
				
				for(String corpusType:corpusTypes){												
					String file = corpusType+".processed";
					System.out.println("\t\tCorpus Type " + corpusType + ": in " + inputDir+"/"+file);
					generateSATaskIndexFromFile(inputDir, file, sentimentCatBuilder, getDocType(corpusType), lang, outPath, corpusType, maxDocuments);
					
				}
				if(lang!=LanguageLabel.en){
					String translation_path = inputDir+bar+"trans"+bar+LanguageLabel.en.toString()+bar+prodCat;
					String file = "test.processed";
					System.out.println("\t\tReading translation in " + translation_path+bar+file);
					generateSATaskIndexFromFile(translation_path, file, sentimentCatBuilder, DocumentType.TEST, lang, outPath, "trans_"+lang.toString()+"_en.processed", maxDocuments);
				}
			}
		}
	}
	
	private static IMultilingualIndex asDependentIndex(IMultilingualIndex weightedBaseIndex, IMultilingualIndex dependent, boolean reweight){
		TroveContentDBBuilder contentDB = new TroveContentDBBuilder(dependent.getDocumentDB(), weightedBaseIndex.getFeatureDB());
		TroveWeightingDBBuilder weightingDB = new TroveWeightingDBBuilder(weightedBaseIndex.getContentDB());
		
		IIntIterator docit = dependent.getDocumentDB().getDocuments();
		while(docit.hasNext()){
			int docid = docit.next();
			IIntIterator featit = dependent.getContentDB().getDocumentFeatures(docid);
			while(featit.hasNext()){
				int featidDep = featit.next();
				String featname = dependent.getFeatureDB().getFeatureName(featidDep);
				int featidBase = weightedBaseIndex.getFeatureDB().getFeature(featname);
				if(featidBase!=-1){
					int count = dependent.getContentDB().getDocumentFeatureFrequency(docid, featidDep);					
					if(count > 0){
						contentDB.setDocumentFeatureFrequency(docid, featidBase, count);					
					}
				}
			}
		}
		
		GenericIndex dep=new GenericIndex(weightedBaseIndex.getFeatureDB(), 
				dependent.getDocumentDB(), 
				dependent.getCategoryDB(), 
				weightedBaseIndex.getDomainDB(), 
				contentDB.getContentDB(), 
				weightingDB.getWeightingDB(), 
				dependent.getClassificationDB());
		
		MultilingualIndex cldep = new MultilingualIndex(dep, dependent.getDocumentLanguageDB());
		
		if(reweight)
			return Utils.weightTFIDFtestMultilingual(weightedBaseIndex, cldep);
		else return cldep;
	}
	
	private static DocumentType getDocType(String docType){
		switch(docType){
			case "train":return DocumentType.TRAINING;
			case "test":return DocumentType.TEST;
			default: return DocumentType.VALIDATION;
		}
		
	}
	
	private static String langString(LanguageLabel lang){
		switch(lang){
			case ja:
				return "jp";
			default:
				return lang.toString();					
		}
	}
	
	private static IMultilingualIndex generateSATaskIndexFromFile(String inputDir, String file, TroveCategoryDBBuilder builder, DocumentType type, LanguageLabel lang, String outPath, String outName, int maxDocuments) throws IOException{
		WebisCLS1CorpusReader reader = new WebisCLS1CorpusReader(builder.getCategoryDB());
		reader.setSentimentAnalysisTask();
		reader.setDocumentType(type);
		reader.setInputFile(inputDir, file);
		reader.setDocNamePrefix(lang.toString()+"_"+type.toString());
		
		IIndexBuilder indexBuilder = new TroveMainIndexBuilder(builder.getCategoryDB());
		reader.begin();
		CorpusDocument doc = null;
		int docsAdded=0;
		while((doc = reader.next())!=null && docsAdded++ < maxDocuments){					
			indexBuilder.addDocument(doc.name(), doc.content().split("\\s+"), getCats(doc));
		}
		
		if(type==DocumentType.VALIDATION){
			removeWordsInOnly1Doc(indexBuilder.getIndex());			
		}
		removeMeaninglessWords(indexBuilder.getIndex());
		
		IMultilingualIndex index = addLangLabels(indexBuilder.getIndex(), lang);
		
		if(computeTFIDF/* && type!=DocumentType.TEST*/)
			index=Utils.weightTFIDFtrainMultilingual(index);
		
		if(outPath!=null){
			showIndexSumary(index);
			Utils.writeMultilingualIndex(index, outPath, outName, true);
		}
		
		return index;
		
	}
	
	private static void removeMeaninglessWords(IIndex index) {
		System.out.println("Deleting meaningless words");
		int initialSize=index.getFeatureDB().getFeaturesCount();
		ArrayList<Integer> meaningless = new ArrayList<Integer>();
		IIntIterator featit = index.getFeatureDB().getFeatures();
		int progress=0;
		while(featit.hasNext()){
			int featid=featit.next();
			String featname = index.getFeatureDB().getFeatureName(featid);
			if(!meaningfulTerm(featname)){
				meaningless.add(featid);
			}
			if(++progress%100==0)
				System.out.println("prog " + (progress*100.0/initialSize));
		}
		System.out.println("Deleting...");	
		index.removeFeatures(IntArrayIterator.List2IntArrayIterator(meaningless));
		int finalSize=index.getFeatureDB().getFeaturesCount();
		System.out.println("Removed = " + (initialSize-finalSize)+"/"+initialSize);		
		
	}
	
	private static boolean meaningfulTerm(String featname){		
		boolean isMark = featname.matches("['\"\\(\\)?!¡¿\\.,;\\-0-9]+");
		if(isMark)
			return false;
		
		int length=featname.length();
		if(length < 3){
			boolean isOccidental = featname.matches("[a-z]{1,2}");
			if(isOccidental)
				return false;
		}
		
		return true;/**/
	}

	private static void removeWordsInOnly1Doc(IIndex index) {
		System.out.println("Deleting words in only one unlabeled document");
		int initialSize=index.getFeatureDB().getFeaturesCount();
		ArrayList<Integer> infrequentWords = new ArrayList<Integer>();
		IIntIterator featit = index.getFeatureDB().getFeatures();
		int progress=0;
		while(featit.hasNext()){
			int featid=featit.next();
			int count = index.getContentDB().getFeatureDocumentsCount(featid);
			if(count==1){
				infrequentWords.add(featid);
			}
			if(++progress%100==0)
				System.out.println("prog " + (progress*100.0/initialSize));
		}
		System.out.println("Deleting...");	
		index.removeFeatures(IntArrayIterator.List2IntArrayIterator(infrequentWords));
		int finalSize=index.getFeatureDB().getFeaturesCount();
		System.out.println("Removed = " + (initialSize-finalSize)+"/"+initialSize);		
	}

	private static IMultilingualIndex generateTCTaskIndexFromFile(String inputDir, String docType, TroveCategoryDBBuilder builder, DocumentType type, LanguageLabel lang, String outPath, String outName, int maxDocuments) throws IOException{
		WebisCLS1CorpusReader reader = new WebisCLS1CorpusReader(builder.getCategoryDB());		
		reader.setDocumentType(type);	
		
		IIndexBuilder indexBuilder = new TroveMainIndexBuilder(builder.getCategoryDB());
		
		for(String cat : productCategories){
			reader.setDocNamePrefix(lang.toString()+"_"+type.toString()+cat);
			String file = cat+Os.pathSeparator()+docType+".processed";
			reader.setInputFile(inputDir, file);
			reader.begin();
			reader.setCategoryClassificationTask(cat);
			CorpusDocument doc = null;
			int docsAdded=0;
			while((doc = reader.next())!=null && docsAdded++ < maxDocuments){					
				indexBuilder.addDocument(doc.name(), doc.content().split("\\s+"), getCats(doc));
			}
		}		
		
		if(type==DocumentType.VALIDATION){
			removeWordsInOnly1Doc(indexBuilder.getIndex());
			removeMeaninglessWords(indexBuilder.getIndex());
		}
		
		IMultilingualIndex index = addLangLabels(indexBuilder.getIndex(), lang);
		
		if(computeTFIDF && type!=DocumentType.TEST)
			index=Utils.weightTFIDFtrainMultilingual(index);
		
		if(outPath!=null){
			showIndexSumary(index);
			Utils.writeMultilingualIndex(index, outPath, outName, true);
		}
		
		return index;
		
	}
	
	private static void showIndexSumary(IMultilingualIndex index) {
		int nD=index.getDocumentDB().getDocumentsCount();
		int nF=index.getFeatureDB().getFeaturesCount();
		int nC=index.getCategoryDB().getCategoriesCount();
		IShortIterator cats=index.getCategoryDB().getCategories();
		System.out.println("nD="+nD);
		System.out.println("nF="+nF);
		System.out.println("nC="+nC);
		while(cats.hasNext()){
			short catid = cats.next();
			String catname = index.getCategoryDB().getCategoryName(catid);
			int nDocsC = index.getClassificationDB().getCategoryDocumentsCount(catid);
			System.out.println("\t"+catname+": " + nDocsC);
		}		
	}
	
	private static String[] getCats(CorpusDocument doc){
		List<CorpusCategory> cats = doc.categories();
		String[] catnames = new String[cats.size()];
		int i= 0;
		for(CorpusCategory cat:cats){
			catnames[i++]=cat.name;
		}
		return catnames;
	}
	
	private static IMultilingualIndex addLangLabels(IIndex index, LanguageLabel lang){
		TroveDocumentLanguageDB langDB = new TroveDocumentLanguageDB(index.getDocumentDB(), new TroveLanguagesDB());
		IIntIterator docs = index.getDocumentDB().getDocuments();
		while(docs.hasNext()){
			int docid = docs.next();
			langDB.indexDocLang(docid, lang);
		}
		MultilingualIndex clindex = new MultilingualIndex(index, langDB);
		return clindex;
	}
	
}
	