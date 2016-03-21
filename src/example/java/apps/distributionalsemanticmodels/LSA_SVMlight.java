package apps.distributionalsemanticmodels;

import java.io.IOException;

import apps.utils.Utils;
import it.cnr.jatecs.classification.ClassificationMode;
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDB;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.representation.lsa.LSA;
import it.cnr.jatecs.representation.lsa.SVDlibcCustomizer;

public class LSA_SVMlight {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		if (args.length != 7) {
			System.out
					.println("Error.\n\t[index path] [training index] [testing index] "
							+ "[latent dimensionality] [svmlight_conf] [svdlibc path] [output path]");
			System.exit(0);
		}

		String indexPath = args[0];
		String indexTrainingFile = args[1];
		String indexTestingFile = args[2];
		int latent_dim = Integer.parseInt(args[3]);
		String svmlightConf = args[4];
		String svdlibc = args[5];
		String resultsPath = args[6];
	
		// open index and language tags
		IIndex indexTraining = Utils.readIndex(indexPath, indexTrainingFile);
		IIndex indexTesting = Utils.readIndex(indexPath, indexTestingFile);		

		// obtain latent indexes
		SVDlibcCustomizer customizer = new SVDlibcCustomizer(svdlibc, latent_dim);
		customizer.setUseWeights();
		LSA LatentSemanticAnalysis = new LSA(indexTraining, customizer);
		
		LatentSemanticAnalysis.project();
		indexTraining = LatentSemanticAnalysis.getLatentTrainindex();
		
		indexTesting = LatentSemanticAnalysis.getLatentTestindex(indexTesting);
		
		LatentSemanticAnalysis.clearResources();

		// learning
		IClassifier classifier = Utils.trainSVMlight(indexTraining, svmlightConf);
	
		// CLASSIFICATION
		IClassificationDB predictions = Utils.testClassifier(indexTesting,
				classifier, resultsPath, indexTestingFile + "_SVMlib.cla", ClassificationMode.PER_CATEGORY);
	
		// Evaluation
		// ---------------------------------------------------------------------------
		IClassificationDB trueValues = Utils.readClassification(indexPath, indexTestingFile);
		Utils.evaluation(predictions, trueValues, resultsPath, indexTestingFile);// learning	
	}

}
