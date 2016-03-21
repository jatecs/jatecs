package apps.transferLearning;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import apps.utils.Utils;
import it.cnr.jatecs.classification.ClassificationMode;
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDB;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveClassificationDBType;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveContentDBType;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveReadWriteHelper;
import it.cnr.jatecs.indexing.tsr.ITsrFunction;
import it.cnr.jatecs.io.FileSystemStorageManager;
import it.cnr.jatecs.representation.transfer.dci.CosineDCF;
import it.cnr.jatecs.representation.transfer.dci.DCFtype;
import it.cnr.jatecs.representation.transfer.dci.DCIcustomizer;
import it.cnr.jatecs.representation.transfer.dci.DistributionalCorrespondeceIndexing;
import it.cnr.jatecs.representation.transfer.dci.GaussianDCF;
import it.cnr.jatecs.representation.transfer.dci.IDistributionalCorrespondenceFunction;
import it.cnr.jatecs.representation.transfer.dci.LinearDCF;
import it.cnr.jatecs.representation.transfer.dci.MutualInformationDCF;
import it.cnr.jatecs.representation.transfer.dci.PointwiseMutualInformationDCF;
import it.cnr.jatecs.representation.transfer.dci.PolynomialDCF;
import it.cnr.jatecs.representation.transfer.dci.WordTranslationOracle;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.iterators.IntArrayIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;

public class DCImain {
	
	public static String indexPath_s;
	public static String sourceTrainName;
	public static String sourceUnlabelName;
	public static String indexPath_t;
	public static String targetTestName;
	public static String targetUnlabeleName;
	public static boolean transduction;
	public static DCFtype distModel;
	public static String svmconfig;
	public static String resultsPath;
	public static short onlyCat=-1;
	public static double fs_ratio=1.0;
	public static double unlabel_ratio=1.0;
	public static double kernel_bias;
	public static double kernel_param;
	
	public static IIndex train_s;
	public static IIndex unlabel_s;
	public static IIndex test_t;
	public static IIndex unlabel_t;

	public static void main(String[] args) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, InterruptedException, ExecutionException {
		//checking parameter errors
		DCIcustomizer customizer=paramCheck(args);
		
		//reading indexes
		readIndexes();
		
		//reduce unlabel size (if requested)
		unlabelSizeReduction();
		
		//select most important features in source-side (if requested)
		featureSelection();
		
		showDescriptions(train_s, unlabel_s, unlabel_t, test_t);
		
		//retain only selected cat (if requested)
		retainCategory();
		
		IDistributionalCorrespondenceFunction distModel_source=instantiateDistributionalModel(unlabel_s);
		IDistributionalCorrespondenceFunction distModel_target=instantiateDistributionalModel(unlabel_t);
		
		//revectorizing indexes
		DistributionalCorrespondeceIndexing dci = new DistributionalCorrespondeceIndexing(train_s, 
				distModel_source, distModel_target, customizer);

		dci.compute();
		IIndex latentTraining = dci.getLatentTrainIndex();
		
		//train learner
		IClassifier classifier = Utils.trainSVMlight(latentTraining, svmconfig);
		
		//prepare test
		IIndex latentTest = dci.projectTargetIndex(test_t);
		
		//classification
		IClassificationDB predictions = classification(dci, classifier, latentTest);

		//evaluation
		IClassificationDB trueValues = test_t.getClassificationDB();
		Utils.evaluation(predictions, trueValues, resultsPath, targetTestName);

	}
	
	private static IClassificationDB classification(
			DistributionalCorrespondeceIndexing dci, IClassifier classifier, IIndex indexTesting) throws IOException {
		return Utils.testClassifier(indexTesting,
				classifier, resultsPath, targetTestName + "_SVMlib.cla", ClassificationMode.PER_CATEGORY);
	}

	private static void retainCategory() {
		if(onlyCat!=-1){
			train_s = Utils.leaveOnlyCat(train_s, onlyCat);
			test_t  = Utils.leaveOnlyCat(test_t,  onlyCat);
		}
	}

	private static void unlabelSizeReduction() {
		if(unlabel_ratio > 0.0 && unlabel_ratio <= 1.0){
			if(unlabel_ratio < 1.0){
				unlabelSizeReduction(unlabel_s, unlabel_ratio);
				unlabelSizeReduction(unlabel_t, unlabel_ratio);
				unlabel_s=Utils.weightTFIDFtrain(unlabel_s);
				unlabel_t=Utils.weightTFIDFtrain(unlabel_t);
			}				
		}
		else error("Error. Unlabel-reduction ratio parameter out of range. Should be in [0,1]");
	}

	public static void readIndexes() throws IOException {
		JatecsLogger.status().println("Reading indexes");
		
		FileSystemStorageManager storageManager = new FileSystemStorageManager(
				indexPath_s, false);
		
		JatecsLogger.status().println("\tSource Training");
		storageManager.open();
		train_s = TroveReadWriteHelper.readIndex(storageManager, sourceTrainName, TroveContentDBType.Full, TroveClassificationDBType.Full);
		storageManager.close();
		
		JatecsLogger.status().println("\tSource Unlabeled");
		storageManager.open();
		unlabel_s = TroveReadWriteHelper.readIndex(storageManager, sourceUnlabelName, TroveContentDBType.Full, TroveClassificationDBType.Full);
		storageManager.close();
		
		storageManager = new FileSystemStorageManager(indexPath_t, false);
		
		JatecsLogger.status().println("\tTarget Test");
		storageManager.open();
		test_t = TroveReadWriteHelper.readIndex(storageManager, targetTestName, TroveContentDBType.Full, TroveClassificationDBType.Full);
		storageManager.close();
		
		JatecsLogger.status().println("\tTarget Unlabeled");
		storageManager.open();
		unlabel_t = TroveReadWriteHelper.readIndex(storageManager, targetUnlabeleName, TroveContentDBType.Full, TroveClassificationDBType.Full);
		storageManager.close();

	}

	private static void featureSelection() {
		if(fs_ratio > 0.0 && fs_ratio <= 1.0){
			if(fs_ratio < 1.0)
				Utils.featureSelectionRR(train_s, unlabel_s, fs_ratio);
		}
		else error("Error. Ratio parameter for feature selection out of range. Should be in [0,1]");
	}

	private static void unlabelSizeReduction(IIndex unlabel, double ratio) {
		List<Integer> pos_docs = new ArrayList<Integer>();
		List<Integer> neg_docs = new ArrayList<Integer>();
		
		short cat=0;
		IIntIterator docsit = unlabel.getDocumentDB().getDocuments();
		while(docsit.hasNext()){
			int docid=docsit.next();
			boolean inCat = unlabel.getClassificationDB().hasDocumentCategory(docid, cat);
			if(inCat)
				pos_docs.add(docid);
			else
				neg_docs.add(docid);
		}
		int positives=pos_docs.size();
		int negatives=neg_docs.size();
		while(pos_docs.size() > (1.0-ratio)*positives)
			pos_docs.remove(0);
		while(neg_docs.size() > (1.0-ratio)*negatives)
			neg_docs.remove(0);
		List<Integer> toRemove=new ArrayList<Integer>();
		toRemove.addAll(pos_docs);
		toRemove.addAll(neg_docs);
		Collections.sort(toRemove);
		unlabel.removeDocuments(IntArrayIterator.List2IntArrayIterator(toRemove), false);
	}

	private static void showDescriptions(IIndex train_s, IIndex unlabel_s,
			IIndex unlabel_t, IIndex test_t) {
		showIndexCounters(train_s, "Source Train");
		showIndexCounters(unlabel_s, "Source Unlabel");
		showIndexCounters(unlabel_t, "Target Unlabel");
		showIndexCounters(test_t, "Target Test");
	}
	private static void showIndexCounters(IIndex ind, String description){
		int nD = ind.getDocumentDB().getDocumentsCount();
		int nF = ind.getFeatureDB().getFeaturesCount();
		int pos = ind.getClassificationDB().getCategoryDocumentsCount((short)0);
		double balance=pos*1.0/nD;
		JatecsLogger.status().println(description+": nD="+nD+", nF="+nF+", balance="+balance);
	}

	public static IDistributionalCorrespondenceFunction instantiateDistributionalModel(
			IIndex unlabel_index) {
		IDistributionalCorrespondenceFunction model=null;
		JatecsLogger.status().print("Instantiating " + distModel.toString() + " distributional model...");
		
		switch(distModel){
		case linear:
			model=new LinearDCF(unlabel_index);
			break;
		case pmi:
			model=new PointwiseMutualInformationDCF(unlabel_index);
			break;
		case mi:
			model=new MutualInformationDCF(unlabel_index);
			break;
		case cosine:
			model=new CosineDCF(unlabel_index);
			break;
		case polynomial:
			JatecsLogger.status().print("\tk_bias="+kernel_bias+", k_exp="+kernel_param);
			model=new PolynomialDCF(unlabel_index, kernel_bias, kernel_param);			
			break;
		case gaussian:
			JatecsLogger.status().print("\tk_sigma="+kernel_param);
			model=new GaussianDCF(unlabel_index, kernel_param);
			break;
		default:
			JatecsLogger.status().println("Error: distributional model <"+distModel.toString()+"> not available.");
			System.exit(0);
			break;
		}
		JatecsLogger.status().println("[done]");
		
		return model;
	}

	private static ITsrFunction getTSRfunction(String tsrFunctionName) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		Class<?> clazz = Class.forName(tsrFunctionName);
		ITsrFunction function = (ITsrFunction)clazz.newInstance();
		return function;
	}

	public static DCIcustomizer paramCheck(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException{		
		Options options = new Options();
		options.addOption("nu", false, "disable common words unification");
		options.addOption("s", false, "use only supervised information to select the pivots [default: uses also feature distorsion across domains]");
		options.addOption("dc", false, "fill dictionary online manually if any word is not present at runtime");
		options.addOption("nr", false, "do not rescale profile-vectors before normalizing [default does rescale]");
		options.addOption("clean", false, "clean features: removes (occidental) terms with mark-characters or length < 3");
		options.addOption("transduction", false, "perform transductive learning with target unlabeled docs");
		options.addOption(OptionBuilder.withArgName("path").hasArg().withDescription("source domain path").create("spath"));
		options.addOption(OptionBuilder.withArgName("index").hasArg().withDescription("source training index name").create("str"));
		options.addOption(OptionBuilder.withArgName("index").hasArg().withDescription("source unlabeled index name").create("su"));		
		options.addOption(OptionBuilder.withArgName("path").hasArg().withDescription("target domain path").create("tpath"));
		options.addOption(OptionBuilder.withArgName("index").hasArg().withDescription("target test index name").create("tts"));
		options.addOption(OptionBuilder.withArgName("index").hasArg().withDescription("target unlabeled index name").create("tu"));
		options.addOption(OptionBuilder.withArgName("num").hasArg().withDescription("number of pivots [default 100]").create("p"));
		options.addOption(OptionBuilder.withArgName("num").hasArg().withDescription("support threshold [default 30]").create("phi"));
		options.addOption(OptionBuilder.withArgName("class").hasArg().withDescription("term selection reduction function class to decide pivots candidates").create("tsr"));
		options.addOption(OptionBuilder.withArgName("file").hasArg().withDescription("svmlight config file").create("svm"));
		options.addOption(OptionBuilder.withArgName("file").hasArg().withDescription("word translator oracle dictionary file").create("d"));
		options.addOption(OptionBuilder.withArgName("path").hasArg().withDescription("result paht").create("o"));
		options.addOption(OptionBuilder.withArgName("categoryID").hasArg().withDescription("category").create("c"));
		options.addOption(OptionBuilder.withArgName("model").hasArg().withDescription("distributional model (pmi|linear|mi|f1|cosine|gaussian|polynomial|...) [default pmi]").create("dist"));
		options.addOption(OptionBuilder.withArgName("real").hasArg().withDescription("bias for polynomial kernel (default 0)").create("k_bias"));
		options.addOption(OptionBuilder.withArgName("real").hasArg().withDescription("parameter for polynomial kernel, and Gaussian kernel (default 1)").create("k_param"));
		options.addOption(OptionBuilder.withArgName("real").hasArg().withDescription("feature selection ratio [default ratio=1.0]").create("fs"));
		options.addOption(OptionBuilder.withArgName("real").hasArg().withDescription("unlabel reduction ratio [default ratio=1.0]").create("ured"));
		options.addOption(OptionBuilder.withArgName("num").hasArg().withDescription("number of parallel threads [default 1]").create("nthread"));
		
		DCIcustomizer customizer=new DCIcustomizer();
		CommandLineParser parser = new BasicParser();		
		
		try {
			CommandLine cmd = parser.parse(options, args);
			
			indexPath_s = cmd.getOptionValue("spath");
			sourceTrainName = cmd.getOptionValue("str");
			sourceUnlabelName = cmd.getOptionValue("su");
			indexPath_t = cmd.getOptionValue("tpath");
			targetTestName = cmd.getOptionValue("tts");
			targetUnlabeleName = cmd.getOptionValue("tu");
			
			if(cmd.hasOption("p"))
				customizer._num_pivots=Integer.parseInt(cmd.getOptionValue("p"));
			if(cmd.hasOption("phi"))
				customizer._phi = Integer.parseInt(cmd.getOptionValue("phi"));			
			customizer._crosscorrespondence = !cmd.hasOption("s");
			customizer._cleanfeats=cmd.hasOption("clean");
			customizer._rescale=!cmd.hasOption("nr");
			customizer._unification = !cmd.hasOption("nu");
			if(cmd.hasOption("tsr"))
				customizer._tsrFunction = getTSRfunction(cmd.getOptionValue("tsr"));
			
			kernel_bias = Double.parseDouble(cmd.getOptionValue("k_bias", "0.0"));
			kernel_param = Double.parseDouble(cmd.getOptionValue("k_param", "1.0"));
			if(cmd.hasOption("nthread"))
				customizer._nThreads = Integer.parseInt(cmd.getOptionValue("nthread"));			
			
			transduction = cmd.hasOption("transduction");
			fs_ratio = Double.parseDouble(cmd.getOptionValue("fs", "1.0"));
			unlabel_ratio = Double.parseDouble(cmd.getOptionValue("ured", "1.0"));
			distModel = DCFtype.valueOf(cmd.getOptionValue("dist", DCFtype.pmi.toString()));
			svmconfig = cmd.getOptionValue("svm");
			if(cmd.hasOption("d")){
				boolean constructDictionary=cmd.hasOption("dc");
				customizer._oracle = new WordTranslationOracle(cmd.getOptionValue("d"), constructDictionary);				
			}
			resultsPath = cmd.getOptionValue("o");
			if(cmd.hasOption("c")){
				onlyCat = Short.parseShort(cmd.getOptionValue("c"));
			}
		} catch (ParseException e) {
			System.err.println( "Parsing failed.  Reason: " + e.getMessage());
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp(DCImain.class.getName(), options);
			System.exit(0);
		} catch (IOException e) {
			System.err.println("Dictionary for word-translator oracle not found or not readable");
			System.exit(0);
		}
		
		return customizer;
		
	}
	
	public static void error(String msg){
		JatecsLogger.status().println("Fatal error: "+msg+"\nExecution failed.");
		System.exit(0);
	}
}
