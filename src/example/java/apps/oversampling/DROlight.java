package apps.oversampling;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.apache.commons.cli.*;

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
import apps.utils.Utils;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

public class DROlight {
	
	private static String indexPath;
	private static String indexTrainName;
	private static String indexTestName;
	private static String svmconfig;
	private static String resultPath;
	private static short selCat;
	private static int latentDimensions;	

	//the "light" version resuses pre-calculated models
	public static void main(String[] args) throws IOException, InterruptedException, ExecutionException, ClassNotFoundException, InstantiationException, IllegalAccessException {
		DRIcustomizer customizer = paramCheck(args);	
		
		msg("Reading training index");
		IIndex train = Utils.readIndex(indexPath, indexTrainName);
		IIndex test = Utils.readIndex(indexPath, indexTestName);
		
		IIndex latentTraining, latentTest;
		checkOneclassIndex(train);
		checkOneclassIndex(test);
		
		msg("Light version... indexes are assumed to be processed (one class, feature selection, ...)");
		selCat=train.getCategoryDB().getCategories().next();
		showDetails(train, selCat);	
		
		int catPositiveExamples=train.getClassificationDB().getCategoryDocumentsCount(selCat);
		if(customizer._trainReplicants > catPositiveExamples){			
			msg("Instantiating distributional model");
			IIndex dist=train.cloneIndex();
			if(latentDimensions<0)
				latentDimensions=dist.getDocumentDB().getDocumentsCount();
			
			IRandomDistributionModel distmodel=new RandomDistributionModel(dist, latentDimensions);
			
			msg("Instantiating distributional random indexing model");
			DistributionalRandomOversampling dro=new DistributionalRandomOversampling(distmodel, customizer);
			
			dro.setSupervisedWeighting(train);		
			latentTraining=dro.compute(train, false);				
			latentTest =dro.compute(test, true);
			
			msg("dri matrices: ");
			showDetails(latentTraining, selCat);
			showDetails(latentTest, selCat);
			
			latentTraining=ampliateWithBOW(latentTraining, train);
			latentTest=ampliateWithBOW(latentTest, test);
			msg("dri bow-extended matrices: ");
			showDetails(latentTraining, selCat);
			showDetails(latentTest, selCat);
			
			latentTraining=Utils.weightTFIDFtrain(latentTraining);
			latentTest=Utils.weightTFIDFtest(latentTraining, latentTest);
		}
		else{
			JatecsLogger.status().println("Oversampling will not be performed: the selected category already has a larger number of documents than requested.");
			latentTraining=train;
			latentTest=test;
		}
		
		//train learner
		IClassifier classifier = Utils.trainSVMlight(latentTraining, svmconfig);

		//classification
		IClassificationDB predictions = Utils.testClassifier(latentTest, classifier, resultPath, null, ClassificationMode.PER_CATEGORY);
		
		//evaluation
		evaluation(predictions, test, latentTest, customizer, resultPath, indexTestName);
		

	}
	
	private static void checkOneclassIndex(IIndex index) {
		int nC = index.getCategoryDB().getCategoriesCount();
		if(nC!=1){
			System.err.println("Error: index must have only one class.");
			System.exit(0);
		}
	}

	private static void evaluation(IClassificationDB predictions, IIndex test, IIndex latentTest, DRIcustomizer customizer, String resultPath, String indexTestName) throws IOException {
		IClassificationDB trueValues = latentTest.getClassificationDB();
		Utils.evaluation(predictions, trueValues, resultPath, indexTestName);/**/
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
		}/**/
		
		
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
		options.addOption(OptionBuilder.withArgName("index").hasArg().withDescription("test index name").create("te"));
		options.addOption(OptionBuilder.withArgName("file").hasArg().withDescription("svmlight config file").create("svm"));
		options.addOption(OptionBuilder.withArgName("path").hasArg().withDescription("result paht").create("o"));
		options.addOption(OptionBuilder.withArgName("num").hasArg().withDescription("latent dimensions (number of columns)").create("l"));
		options.addOption(OptionBuilder.withArgName("num").hasArg().withDescription("training replicas for minoritarian-class documents").create("rr"));
		options.addOption(OptionBuilder.withArgName("num").hasArg().withDescription("test replicas for committee").create("ss"));
		options.addOption(OptionBuilder.withArgName("path").hasArg().withDescription("persistent doc-prob model (tries to load if exists, saves if not)").create("model"));
		options.addOption(OptionBuilder.withArgName("mode").hasArg().withDescription("determines the density of the latent space (PROP=same density; HIGH=high density; VERYHIGH)").create("density"));
		options.addOption(OptionBuilder.withArgName("num").hasArg().withDescription("number of threads (default 10)").create("nthreads"));
		
		DRIcustomizer customizer=new DRIcustomizer();
		CommandLineParser parser = new BasicParser();		
		
		try {
			CommandLine cmd = parser.parse(options, args);			
			
			indexPath=cmd.getOptionValue("path");
			indexTrainName=cmd.getOptionValue("tr");
			indexTestName=cmd.getOptionValue("te");			
			svmconfig = cmd.getOptionValue("svm");
			customizer._undersampling_ratio = -1.0;
			customizer._trainReplicants=Integer.parseInt(cmd.getOptionValue("rr"));
			customizer._testReplicants=Integer.parseInt(cmd.getOptionValue("ss", "1"));
			customizer._latentDensity=cmd.getOptionValue("density", "PROP");
			customizer._useSoftmax=false;
			customizer._loadSaveProbmodelPath=cmd.getOptionValue("model", null);
			DistributionalRandomOversampling.MAX_THREADS=Integer.parseInt(cmd.getOptionValue("nthreads", ""+DistributionalRandomOversampling.MAX_THREADS));
			JatecsLogger.status().println("Setting the number of threads to " + DistributionalRandomOversampling.MAX_THREADS);
			latentDimensions=Integer.parseInt(cmd.getOptionValue("l", "-1"));	
			
			resultPath = cmd.getOptionValue("o");
			selCat = (short)0;
			
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
