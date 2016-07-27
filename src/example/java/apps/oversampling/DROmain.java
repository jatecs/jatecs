package apps.oversampling;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.apache.commons.cli.*;

import apps.utils.Utils;
import it.cnr.jatecs.classification.ClassificationMode;
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.indexes.DB.interfaces.*;
import it.cnr.jatecs.indexes.DB.troveCompact.*;
import it.cnr.jatecs.indexes.utils.IndexStatistics;
import it.cnr.jatecs.representation.oversampling.*;
import it.cnr.jatecs.representation.transfer.*;
import it.cnr.jatecs.representation.vector.IndexVectorizer;
import it.cnr.jatecs.representation.vector.SparseVector;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

public class DROmain {
	
	private static String indexPath;
	private static String indexTrainName;
	private static String indexTestName;
	private static String svmconfig;
	private static String resultPath;
	private static short selCat;
	private static double fs_ratio;	
	private static int latentDimensions;		
	private static boolean performRI;
	private static boolean cleanTraining;

	public static void main(String[] args) throws IOException, InterruptedException, ExecutionException, ClassNotFoundException, InstantiationException, IllegalAccessException {
		DRIcustomizer customizer = paramCheck(args);	
		
		msg("Reading training index");
		IIndex train = Utils.readIndex(indexPath, indexTrainName);
		//IIndex dist = TroveReadWriteHelper.readIndex(indexPath, indexDistModel, TroveContentDBType.Full, TroveClassificationDBType.Full);
		IIndex test = Utils.readIndex(indexPath, indexTestName);
		
		msg("Feature selection with ratio " + fs_ratio);
		if(selCat!=-1){
			train = Utils.leaveOnlyCat(train, selCat);
			test  = Utils.leaveOnlyCat(test,  selCat);
		}
		if(fs_ratio>0 && fs_ratio<1)
			Utils.featureSelectionRR(train, test, fs_ratio);
		
		selCat=train.getCategoryDB().getCategories().next();
		showDetails(train, selCat);	
		
		int catPositiveExamples=train.getClassificationDB().getCategoryDocumentsCount(selCat);
		customizer._trainReplicants = Math.max(customizer._trainReplicants, catPositiveExamples);
		
		msg("Instantiating distributional model");
		IIndex dist=train.cloneIndex();
		if(latentDimensions<0)
			latentDimensions=dist.getDocumentDB().getDocumentsCount();
		
		if(performRI){
			msg("Random Indexing of distributional model...");
			RandomIndexer randomIndexing = new RandomIndexer();			
			RandomIndexDictionary docDic=randomIndexing.getDocumentRandomDictionary(dist.getDocumentDB(), latentDimensions, 2, false);
			dist=randomIndexing.reindexFeatures(dist, docDic);
			showDetails(dist, selCat);
		}
		IRandomDistributionModel distmodel=new RandomDistributionModel(dist, latentDimensions);
		
		msg("Instantiating distributional random indexing model");
		DistributionalRandomOversampling dro=new DistributionalRandomOversampling(distmodel, customizer);
		
		dro.setSupervisedWeighting(train);		
		IIndex latentTraining=dro.compute(train, false);				
		IIndex latentTest =dro.compute(test, true);
		
		msg("dri matrices: ");
		showDetails(latentTraining, selCat);
		showDetails(latentTest, selCat);
		
		latentTraining=ampliateWithBOW(latentTraining, train);
		latentTest=ampliateWithBOW(latentTest, test);
		msg("dri bow-extended matrices: ");
		showDetails(latentTraining, selCat);
		showDetails(latentTest, selCat);/**/
		
		latentTraining=Utils.weightTFIDFtrain(latentTraining);
		latentTest=Utils.weightTFIDFtest(latentTraining, latentTest);
		
		if(cleanTraining){
			msg("Cleaning training data ENN.");
			SMOTE_ENN.removeMisclassifiedKnn(3, latentTraining, (short)0);
		}
		
		//train learner
		IClassifier classifier = Utils.trainSVMlight(latentTraining, svmconfig);

		//classification
		IClassificationDB predictions = Utils.testClassifier(latentTest, classifier, resultPath, null, ClassificationMode.PER_CATEGORY);
		
		//evaluation
		evaluation(predictions, test, latentTest, customizer, resultPath, indexTestName);
		

	}
	
	private static void evaluation(IClassificationDB predictions, IIndex test, IIndex latentTest, DRIcustomizer customizer, String resultPath, String indexTestName) throws IOException {
		IClassificationDB trueValues = null;
		if(customizer._testReplicants>1){
			predictions = decomposePredictions(predictions, test.getDocumentDB(), test.getCategoryDB());
			trueValues = test.getClassificationDB();			
		}
		else trueValues = latentTest.getClassificationDB();		
		
		Utils.evaluation(predictions, trueValues, resultPath, indexTestName);
	}

	private static IClassificationDB decomposePredictions(
			IClassificationDB predictions, IDocumentDB orig_docs, ICategoryDB orig_class) {

		TroveClassificationDBBuilder classif=new TroveClassificationDBBuilder(orig_docs, orig_class);
		IDocumentDB latent_docs=predictions.getDocumentDB();
		
		IShortIterator cats=orig_class.getCategories();
		while(cats.hasNext()){
			short catID=cats.next();
			IIntIterator docsit=orig_docs.getDocuments();
			while(docsit.hasNext()){
				int docid=docsit.next();
				String doc_name=orig_docs.getDocumentName(docid);
				
				int doc_version=0;
				int latent_docid;
				int positivos=0;
				int negativos=0;
				while((latent_docid = latent_docs.getDocument(doc_name+"_"+doc_version))!=-1){				
					if(predictions.hasDocumentCategory(latent_docid, catID))
						positivos++;
					else
						negativos++;
					doc_version++;
				}
				
				if(positivos>negativos)
					classif.setDocumentCategory(docid, catID);
			}
		}
		
		return classif.getClassificationDB();
		
	}
	
	private static IIndex ampliateWithBOW(IIndex indexDRO, IIndex indexBoW){
		
		//join both feature spaces
		TroveFeatureDBBuilder joinFeatSpace=new TroveFeatureDBBuilder();
		int nFeatsDRO=indexDRO.getFeatureDB().getFeaturesCount();
		int nFeatsBow=indexBoW.getFeatureDB().getFeaturesCount();
		int joinFeats=nFeatsBow+nFeatsDRO;
		for(int i = 0; i < joinFeats; i++){
			joinFeatSpace.addFeature("JoinFeat_"+i);
		}
		TroveDomainDB domain=new TroveDomainDB(indexDRO.getCategoryDB(), joinFeatSpace.getFeatureDB());
		
		//join bow index vectors with all trials of dri
		GenericIndexBuilderRows proj=new GenericIndexBuilderRows(domain);
		
		IndexVectorizer bowVectorizer=new IndexVectorizer(indexBoW);
		IndexVectorizer driVectorizer=new IndexVectorizer(indexDRO);
		
		IIntIterator docs=indexDRO.getDocumentDB().getDocuments();
		while(docs.hasNext()){
			int driDocID=docs.next();
			SparseVector docvecDRI=driVectorizer.getDocumentFrequencies(driDocID);			
			String driDocname=indexDRO.getDocumentDB().getDocumentName(driDocID);
			
			String origname=driDocname.substring(0, driDocname.lastIndexOf("_"));
			int bowDocID=indexBoW.getDocumentDB().getDocument(origname);
			if(bowDocID!=-1){
				SparseVector docvecBoW=bowVectorizer.getDocumentFrequencies(bowDocID);
				SparseVector joinDocvec=SparseVector.concatenate(docvecBoW, docvecDRI);
				proj.addDocumentRawFrequencies(driDocname, joinDocvec, indexDRO.getClassificationDB().getDocumentCategories(driDocID));
			}
			else{
				JatecsLogger.status().println("Error joining Bow-Dri: Document "+driDocname+" not found!");
			}
		}	
		
		return proj.getIndex();
	}

	private static void showDetails(IIndex index, short cat) {
		double density=0;
		int nD=index.getDocumentDB().getDocumentsCount();
		int nF=index.getFeatureDB().getFeaturesCount();
		int posDocs=index.getClassificationDB().getCategoryDocumentsCount(cat);
		
		for(IIntIterator docsit=index.getDocumentDB().getDocuments(); docsit.hasNext(); ){
			int docid=docsit.next();
			int docfeats=index.getContentDB().getDocumentFeaturesCount(docid);
			density+=docfeats*1.0/nF;
		}
		density/=nD;
		
		
		msg("Matrix " + nD+"x"+nF+", density="+density + ", balance="+posDocs*1.0/nD);
	}

	private static void msg(String msg){
		JatecsLogger.status().println(msg);
	}

	public static DRIcustomizer paramCheck(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException{		
		Options options = new Options();
		options.addOption(OptionBuilder.withArgName("path").hasArg().withDescription("index path").create("path"));
		options.addOption(OptionBuilder.withArgName("index").hasArg().withDescription("training index name").create("tr"));
		//options.addOption(OptionBuilder.withArgName("index").hasArg().withDescription("distributional model index name").create("dm"));
		options.addOption(OptionBuilder.withArgName("index").hasArg().withDescription("test index name").create("te"));
		options.addOption(OptionBuilder.withArgName("file").hasArg().withDescription("svmlight config file").create("svm"));
		options.addOption(OptionBuilder.withArgName("path").hasArg().withDescription("result paht").create("o"));
		options.addOption(OptionBuilder.withArgName("catID").hasArg().withDescription("performs binary classification only in the selected category").create("c"));
		options.addOption(OptionBuilder.withArgName("real [0,1]").hasArg().withDescription("feature selection ration").create("fs"));
		options.addOption(OptionBuilder.withArgName("num").hasArg().withDescription("latent dimensions (number of columns)").create("l"));
		options.addOption(OptionBuilder.withArgName("num").hasArg().withDescription("training replicas for minoritarian-class documents").create("rr"));
		options.addOption(OptionBuilder.withArgName("num").hasArg().withDescription("testing replicas").create("re"));
		options.addOption(OptionBuilder.withArgName("real").hasArg().withDescription("under-sampling ratio of majoritarian-class training documents wrt minoritarian-class replicas").create("u"));
		options.addOption("ri", false, "performs random indexing on the columns of the distribution model instead of document sampling");
		options.addOption("enn", false, "performs data cleaning in training documents (remove all documents misclassified by a committee of 3 nearest-neighbours)");
		options.addOption("softmax", false, "uses softmax to create the probability mass function");
		
		DRIcustomizer customizer=new DRIcustomizer();
		CommandLineParser parser = new BasicParser();		
		
		try {
			CommandLine cmd = parser.parse(options, args);			
			
			indexPath=cmd.getOptionValue("path");
			indexTrainName=cmd.getOptionValue("tr");
			//indexDistName=cmd.getOptionValue("dm");
			indexTestName=cmd.getOptionValue("te");			
			svmconfig = cmd.getOptionValue("svm");
			fs_ratio = Double.parseDouble(cmd.getOptionValue("fs", "1.0"));
			customizer._undersampling_ratio = Double.parseDouble(cmd.getOptionValue("u", "-1.0"));
			latentDimensions=Integer.parseInt(cmd.getOptionValue("l", "-1"));		
			customizer._trainReplicants=Integer.parseInt(cmd.getOptionValue("rr"));
			customizer._testReplicants=Integer.parseInt(cmd.getOptionValue("re", "1"));
			performRI=cmd.hasOption("ri");
			cleanTraining=cmd.hasOption("enn");
			customizer._useSoftmax=cmd.hasOption("softmax");
			
			resultPath = cmd.getOptionValue("o");
			if(cmd.hasOption("c"))
				selCat = Short.parseShort(cmd.getOptionValue("c", "-1"));	
			else
				error("The category must be indicated");
			
		} catch (ParseException e) {
			System.err.println( "Parsing failed.  Reason: " + e.getMessage());
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp(DROmain.class.getName(), options);
			System.exit(0);
		}
		
		return customizer;
		
	}
	
	public static void error(String msg){
		JatecsLogger.status().println("Fatal error: "+msg+"\nExecution failed.");
		System.exit(0);
	}
}
