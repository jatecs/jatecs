package apps.dataset;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import apps.utils.Utils;
import it.cnr.jatecs.indexes.DB.generic.GenericIndex;
import it.cnr.jatecs.indexes.DB.generic.MultilingualIndex;
import it.cnr.jatecs.indexes.DB.interfaces.IFeatureDB;
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

/**
 * Creates the Jatecs-indexes for the Blizter et al.'s MultiDomainSentiment dataset:
 * http://www.cs.jhu.edu/~mdredze/datasets/sentiment/
 * */
public class IndexMultiDomainSentimentBlizter{
	
	private static final String catBooks="books"; 
	private static final String catDVD="dvd";
	private static final String catElectronics="electronics";
	private static final String catKitchen="kitchen";
	
	private static final String corpusPositive="positive.review";
	private static final String corpusNegative="negative.review";
	private static final String corpusUnlabel="unlabeled.review";
	
	private static final String classPositive="positive";
	private static final String bar=Os.pathSeparator();
	
	private static String[] productCategories = {catBooks, catDVD, catElectronics, catKitchen};
	private static String[] corpusTypes = {corpusPositive, corpusNegative, corpusUnlabel};
	
	public static void main(String[] args) throws IOException {		
			
			if(args.length!=2){
				System.err.println("Parameter Error"+
						"\n\t<InputBase>: processed corpus from webis-CLS-10"+
						"\n\t<OutputDir>");
				System.exit(0);
			}
			
			String inputBase = args[0];
			String outBase= args[1];
		
			
			//-----------------------------------------------------------------------------------			
			//generate the base indexes for Sentiment Analysis
			generateBaseSAindexes(inputBase, outBase);
		
			//generate the cross-domain sentiment tasks, each folder (i.e., domain, for example "books") will contain
			//four indexes: the sourceTrain and sourceUnlabel, and the targetUnlabeled and targetTest
			generateCrossDomainSAtasks(outBase);
			
			System.out.println("Done!");
		
	}
	
	//the domain comprises 2 sets of data: (L)labeledReviews and (U)unlabeledReviews. 4 indexes are created: 
	//	(L)sourceTrain and (U)dependent-sourceUnlabeled
	//	(U)targetUnlabeled and (L) dependent-targetTest
	private static void generateCrossDomainSAtasks(String baseSApath) throws IOException {
		for(String prodCat : productCategories){
			
			String sourceIndexPath = baseSApath+bar+"Base"+prodCat;
			IMultilingualIndex positives = Utils.readMultilingualIndex(sourceIndexPath, corpusPositive);
			IMultilingualIndex negatives = Utils.readMultilingualIndex(sourceIndexPath, corpusNegative);
			IMultilingualIndex unlabel = Utils.readMultilingualIndex(sourceIndexPath, corpusUnlabel);
			
			IMultilingualIndex posNegReviews = Utils.joinIndexes(positives, negatives, LanguageLabel.en, LanguageLabel.en);
			posNegReviews=Utils.weightTFIDFtrainMultilingual(posNegReviews);

			IMultilingualIndex sourceUnlabel = asDependentIndex(posNegReviews, unlabel, true);
			IMultilingualIndex targetTest = asDependentIndex(unlabel, posNegReviews, true);
			
			String taskPath = baseSApath+bar+prodCat;
			Utils.writeMultilingualIndex(posNegReviews, taskPath, "sourceTrain", true);
			Utils.writeMultilingualIndex(sourceUnlabel, taskPath, "sourceUnlabel", true);
			Utils.writeMultilingualIndex(unlabel, taskPath, "targetUnlabel", true);
			Utils.writeMultilingualIndex(targetTest, taskPath, "targetTest", true);
		}		
	}

	private static void generateBaseSAindexes(String inputBase, String outBase) throws IOException{
		TroveCategoryDBBuilder sentimentCatBuilder = new TroveCategoryDBBuilder();
		sentimentCatBuilder.addCategory(classPositive);
		
		System.out.println("Generating Sentiment Analysis base Indexes");

		for(String prodCat : productCategories){			
			System.out.println("\tProduct Category " + prodCat);					
			
			String inputDir=inputBase+bar+prodCat;
			String outPath = outBase+bar+"Base"+prodCat;
			
			for(String corpusType:corpusTypes){
				System.out.println("\t\tCorpus Type " + corpusType);
				generateSATaskIndexFromFile(inputDir, corpusType, sentimentCatBuilder, getDocType(corpusType), outPath, corpusType);
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
			case corpusUnlabel:return DocumentType.VALIDATION;
			default: return DocumentType.TRAINING;
		}
		
	}
	
	private static IMultilingualIndex generateSATaskIndexFromFile(String inputDir, String file, TroveCategoryDBBuilder builder, DocumentType type, String outPath, String outName) throws IOException{
		WebisCLS1CorpusReader reader = new WebisCLS1CorpusReader(builder.getCategoryDB());
		reader.setSentimentAnalysisTask();
		reader.setDocumentType(type);
		reader.setInputFile(inputDir, file);
		reader.setDocNamePrefix(type.toString());
		
		IIndexBuilder indexBuilder = new TroveMainIndexBuilder(builder.getCategoryDB());
		reader.begin();
		CorpusDocument doc = null;		
		while((doc = reader.next())!=null){
			indexBuilder.addDocument(doc.name(), doc.content().split("\\s+"), getCats(doc));
		}
		
		if(type==DocumentType.VALIDATION){
			removeWordsInOnly1Doc(indexBuilder.getIndex());
		}
		
		IMultilingualIndex index = addLangLabels(indexBuilder.getIndex(), LanguageLabel.en);
		
		if(outPath!=null){
			showIndexSumary(index);
			Utils.writeMultilingualIndex(index, outPath, outName, true);
		}
		
		return index;
		
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
			if(++progress%1000==0)
				System.out.println("prog " + (progress*100.0/initialSize));
		}
		System.out.println("Deleting...");	
		index.removeFeatures(IntArrayIterator.List2IntArrayIterator(infrequentWords));
		int finalSize=index.getFeatureDB().getFeaturesCount();
		System.out.println("Removed = " + (initialSize-finalSize)+"/"+initialSize);		
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
	
	public static void writeIndexInWordCountFormat(IIndex index, String outpath, short cat, boolean positiveNegative, String foldname) throws IOException{
		File fw=new File(outpath);
		fw.mkdirs();
		
		FileWriter fw_pos = null;
		FileWriter fw_neg = null;		
		if(positiveNegative){
			fw_pos = new FileWriter(outpath+Os.pathSeparator()+corpusPositive);
			fw_neg = new FileWriter(outpath+Os.pathSeparator()+corpusNegative);
		}
		else{
			fw_pos = new FileWriter(outpath+Os.pathSeparator()+foldname);
			fw_neg = fw_pos;
		}
		
		String poslabel = "#label#:positive";
		String neglabel = "#label#:negative";
		
		IIntIterator docs = index.getDocumentDB().getDocuments();
		while(docs.hasNext()){
			int docid = docs.next();
			StringBuilder docrow = new StringBuilder();
			IIntIterator docfeats = index.getContentDB().getDocumentFeatures(docid);
			while(docfeats.hasNext()){
				int featid = docfeats.next();
				String featname = index.getFeatureDB().getFeatureName(featid);
				int count = index.getContentDB().getDocumentFeatureFrequency(docid, featid);
				docrow.append(featname).append(":").append(count).append(" ");
			}
			boolean hascat = index.getClassificationDB().hasDocumentCategory(docid, cat);
			if(hascat){
				docrow.append(poslabel);
				docrow.append("\n");
				fw_pos.write(docrow.toString());
			}
			else{
				docrow.append(neglabel);
				docrow.append("\n");
				fw_neg.write(docrow.toString());
			}			
		}
		
		fw_pos.close();
		fw_neg.close();
	}

	public static void createTrivialDicctionary(IFeatureDB sourcefeats, IFeatureDB targetfeats, String outpath) throws IOException{
		HashSet<String> terms = new HashSet<String>();
		addTerms(sourcefeats, terms);
		addTerms(targetfeats, terms);
		ArrayList<String> sorted_terms = new ArrayList<String>(terms);
		Collections.sort(sorted_terms);
		
		FileWriter fw = new FileWriter(outpath);
		for(String term:sorted_terms){
			String term_row = term + "\t" + term + "\n"; 
			fw.write(term_row);
		}
		fw.close();
	}
	
	private static void addTerms(IFeatureDB feats, HashSet<String> terms){
		IIntIterator featit = feats.getFeatures();
		while(featit.hasNext()){
			int featid = featit.next();
			String featname = feats.getFeatureName(featid);
			terms.add(featname);
		}
	}
	
}
	