package apps.distributionalsemanticmodels;

import apps.utils.Utils;
import it.cnr.jatecs.classification.ClassificationMode;
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.indexes.DB.interfaces.IMultilingualIndex;
import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDB;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.representation.randomprojections.LightweightRandomIndexing;
import java.io.IOException;

public class LightweightRandomIndexingApp {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		if (args.length != 6) {
			System.out
					.println("Error.\n\t[index path] [training index] [testing index] "
							+ "[latent dimensionality] [svmperf_conf] [output path]");
			System.exit(0);
		}

		String indexPath = args[0];
		String indexTrainingFile = args[1];
		String indexTestingFile = args[2];
		int latent_dim = Integer.parseInt(args[3]);
		String svmperfConf = args[4];
		String resultsPath = args[5];
		
		// open index and language tags
		IMultilingualIndex indexCLTraining = Utils.readMultilingualIndex(indexPath, indexTrainingFile);
		IMultilingualIndex indexCLTesting = Utils.readMultilingualIndex(indexPath, indexTestingFile);

		// obtain latent indexes		
		if(latent_dim==-1){
			latent_dim=indexCLTraining.getFeatureDB().getFeaturesCount();
		}
		
		LightweightRandomIndexing randomIndexing = new LightweightRandomIndexing(indexCLTraining, latent_dim);
		
		IIndex indexTraining = randomIndexing.getLatentTrainindex();

		IIndex indexTesting = randomIndexing.getLatentTestindex(indexCLTesting);

		randomIndexing.clearResources();

		// learning
		IClassifier classifier = Utils.trainSVMlight(indexTraining, svmperfConf);

		// CLASSIFICATION
		IClassificationDB predictions = Utils.testClassifier(indexTesting,
				classifier, resultsPath, indexTestingFile + "_SVMlib.cla", ClassificationMode.PER_CATEGORY);

		// Evaluation
		// ---------------------------------------------------------------------------
		IClassificationDB trueValues = Utils.readClassification(indexPath, indexTestingFile);
		Utils.evaluation(predictions, trueValues, resultsPath, indexTestingFile);
	}

}
