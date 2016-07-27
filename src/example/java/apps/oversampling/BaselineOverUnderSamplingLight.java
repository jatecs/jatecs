package apps.oversampling;

import it.cnr.jatecs.classification.ClassificationMode;
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDB;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveClassificationDBType;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveContentDBType;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveReadWriteHelper;
import it.cnr.jatecs.indexes.utils.DocSet;
import it.cnr.jatecs.representation.oversampling.AbstractBaselineDocSampler;
import it.cnr.jatecs.representation.oversampling.BorderSMOTE;
import it.cnr.jatecs.representation.oversampling.IndexDocSampler;
import it.cnr.jatecs.representation.oversampling.SMOTE;
import it.cnr.jatecs.representation.oversampling.SMOTE_ENN;
import it.cnr.jatecs.representation.vector.IndexVectorizer;
import it.cnr.jatecs.representation.vector.SparseVector;
import it.cnr.jatecs.utils.JatecsLogger;
import apps.utils.Utils;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class BaselineOverUnderSamplingLight {

	//the "light" version resuses pre-calculated models
	public static void main(String[] args) throws IOException {
		//param check
		int argc=0;
		String path=args[argc++];
		String trainname=args[argc++];
		String testname=args[argc++];
		int oversampling=Integer.parseInt(args[argc++]);
		String svmconf=args[argc++];
		String result=args[argc++];
		String method=args[argc++];
		
		//index reading
		IIndex train = Utils.readIndex(path, trainname);
		IIndex test = Utils.readIndex(path, testname);
		
		JatecsLogger.status().println("Lightweight version: assumes the index to be already processed (only one class, feature selection, ...)");
		checkOneclassIndex(train);
		checkOneclassIndex(test);		
		datasetDescription(train);
		
		// if the sampling is -1 then select all positive or negative documentsdocuments
		int posDocs=train.getClassificationDB().getCategoryDocumentsCount((short)0);
		int negDocs=train.getDocumentDB().getDocumentsCount()-posDocs;
		if(oversampling==-1 || oversampling < posDocs) oversampling=posDocs;
		
		int undersampling = negDocs;
				
		//over/under-sampling
		if(oversampling!=posDocs || undersampling!=negDocs){
			JatecsLogger.status().println("Resampling " + oversampling+"/"+undersampling);
			AbstractBaselineDocSampler ids=initBaselinemethod(method, train);
			train = ids.resample(oversampling, undersampling);
		}
		else{
			JatecsLogger.status().println("Nothing to do! no resampling, nor undersampling to be applied.");
		}
		
		datasetDescription(train);
		
		switch (method) {
		//SMOTE-based methods operate on a weighted index, not on binary representations, there is therefore no need
		//for reweighting
			case "SMOTE":
			case "SMOTE-ENN":
			case "BorderSMOTE":
				JatecsLogger.status().println("Don't reweight");
				break;
			case "replicate":
				JatecsLogger.status().println("Reweighting");
				train=Utils.weightTFIDFtrain(train);
				test=Utils.weightTFIDFtest(train, test);
				break;

			default:
				System.err.println("Error in weightingt, method<"+method+"> unknow");
				System.exit(0);
			break;	
		}
		
		// learning
		IClassifier classifier = Utils.trainSVMlight(train, svmconf);

		// CLASSIFICATION
		IClassificationDB predictions = Utils.testClassifier(test, classifier,  ClassificationMode.PER_CATEGORY);

		// Evaluation
		// ---------------------------------------------------------------------------
		IClassificationDB trueValues = test.getClassificationDB();
		Utils.evaluation(predictions, trueValues, result, test+".out");
	}
	
	private static AbstractBaselineDocSampler initBaselinemethod(String method, IIndex train) {
		AbstractBaselineDocSampler sampler=null;
		switch(method){
			case "replicate":
				sampler=new IndexDocSampler(train, (short)0); break;
			case "SMOTE":
				sampler=new SMOTE(train, 5, (short)0); break;
			case "SMOTE-ENN":
				sampler=new SMOTE_ENN(train, 5, (short)0); break;
			case "BorderSMOTE":
				sampler=new BorderSMOTE(train, 5, (short)0); break;
			default:
				System.err.println("Error: method <"+method+"> not suported. Allowed: (replicate|SMOTE|SMOTE-ENN)");
				System.exit(0);
		}
		
		return sampler;
	}
	
	private static void datasetDescription(IIndex index) throws IOException{
		short cat=(short)0;
		int D=index.getDocumentDB().getDocumentsCount();
		int pos=index.getClassificationDB().getCategoryDocumentsCount(cat);
		int neg=D-pos;
		JatecsLogger.status().println("Documents: "+D+"\t[(+)" + pos + ", (-)"+neg+"]");
	}
	
	private static void checkOneclassIndex(IIndex index) {
		int nC = index.getCategoryDB().getCategoriesCount();
		if(nC!=1){
			System.err.println("Error: index must have only one class.");
			System.exit(0);
		}
	}

}
