package apps.distributionalsemanticmodels;

import java.io.IOException;

import apps.utils.Utils;
import it.cnr.jatecs.classification.ClassificationMode;
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDB;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.interfaces.IMultilingualIndex;
import it.cnr.jatecs.representation.lsa.MDMcomparable;
import it.cnr.jatecs.representation.lsa.SVDlibcCustomizer;

public class MultilingualDomainModelsSVMlight {

	public static void main(String[] args) throws IOException {
		if (args.length < 8) {
			System.out
					.println("Error.\n\t[index path] [training index] [testing index] "
							+ "[latent dimensionality] [svmlight conf file] [svdlibc exe] [Multilingual|BoW] [output path]");
			System.exit(0);
		}

		String indexPath = args[0];
		String indexTrainingFile = args[1];
		String indexTestingFile = args[2];
		int latent_dim = Integer.parseInt(args[3]);
		String svmLightConfigFile = args[4];
		String svdlibc_exe = args[5];
		boolean multilingual=true;
		if(args[6].equals("Multilingual"))
			multilingual=true;
		else if(args[6].equals("BoW"))
			multilingual=false;
		else{
			System.err.println("Param " + args[6] + " must be 'Multilingual' or 'BoW'");
			System.exit(0);
		}
		String resultsPath = args[7];		
		
		// open index and language tags
		IMultilingualIndex training = Utils.readMultilingualIndex(indexPath, indexTrainingFile);
		IMultilingualIndex test = Utils.readMultilingualIndex(indexPath, indexTestingFile);
		
		// obtain latent indexes
		SVDlibcCustomizer customizer = new SVDlibcCustomizer(svdlibc_exe, latent_dim);
		customizer.setUseFrequencies();
		MDMcomparable MultilingualDomainModel = new MDMcomparable(training, customizer);
		if(multilingual)
			MultilingualDomainModel.computeDasMDM();
		else
			MultilingualDomainModel.computeDasBOW();
		
		MultilingualDomainModel.project();
		IIndex latentTraining = MultilingualDomainModel.getLatentTrainindex();
		
		IIndex latentTest = MultilingualDomainModel.getLatentTestindex(test);
		
		MultilingualDomainModel.clearResources();

		// learning
		IClassifier classifier = Utils.trainSVMlight(latentTraining, svmLightConfigFile);

		// CLASSIFICATION
		IClassificationDB predictions = Utils.testClassifier(latentTest,
				classifier, resultsPath, indexTestingFile + "_SVMlib.cla", ClassificationMode.PER_CATEGORY);
		// Evaluation
		// ---------------------------------------------------------------------------
		IClassificationDB trueValues = Utils.readClassification(indexPath, indexTestingFile);
		Utils.evaluation(predictions, trueValues, resultsPath, indexTestingFile);	

	}

}
