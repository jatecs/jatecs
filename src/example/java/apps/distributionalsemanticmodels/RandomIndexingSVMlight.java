package apps.distributionalsemanticmodels;

import java.io.IOException;

import apps.utils.Utils;
import it.cnr.jatecs.classification.ClassificationMode;
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDB;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.representation.randomprojections.RandomIndexing;
import it.cnr.jatecs.representation.vector.IMatrix.MATRIX_MODE;

public class RandomIndexingSVMlight {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		if (args.length != 7) {
			System.out
					.println("Error.\n\t[index path] [training index] [testing index] "
							+ "[latent dimensionality] [nonZeros for indexes] [svmlight_conf] [output path]");
			System.exit(0);
		}

		String indexPath = args[0];
		String indexTrainingFile = args[1];
		String indexTestingFile = args[2];
		int latent_dim = Integer.parseInt(args[3]);
		int nonZeros = Integer.parseInt(args[4]);
		String svmlightConf = args[5];
		String resultsPath = args[6];
		
		// open index and language tags
		IIndex indexTraining = Utils.readIndex(indexPath, indexTrainingFile);
		IIndex indexTesting = Utils.readIndex(indexPath, indexTestingFile);		

		if(latent_dim==-1){
			latent_dim=indexTraining.getFeatureDB().getFeaturesCount();
		}
		
		// obtain latent indexes		
		RandomIndexing randomIndexing = new RandomIndexing(indexTraining, latent_dim, nonZeros);
		randomIndexing.setMatrixMode(nonZeros==2? MATRIX_MODE.SPARSE_MATRIX : MATRIX_MODE.DENSE_MATRIX);
		
		randomIndexing.project();
		indexTraining = randomIndexing.getLatentTrainindex();
		
		indexTesting = randomIndexing.getLatentTestindex(indexTesting);
		
		randomIndexing.clearResources();

		// learning
		IClassifier classifier = Utils.trainSVMlight(indexTraining, svmlightConf);
		
		// CLASSIFICATION
		IClassificationDB predictions = Utils.testClassifier(indexTesting,
				classifier, resultsPath, indexTestingFile + "_SVMlib.cla", ClassificationMode.PER_CATEGORY);
		
		// Evaluation
		// ---------------------------------------------------------------------------
		IClassificationDB trueValues = Utils.readClassification(indexPath, indexTestingFile);
		Utils.evaluation(predictions, trueValues, resultsPath, indexTestingFile);
		}

}
