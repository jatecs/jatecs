package it.cnr.jatecs.representation.oversampling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import it.cnr.jatecs.classification.knn.SimilarDocument;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.utils.DocSet;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.iterators.IntArrayIterator;

public class SMOTE_ENN extends SMOTE{

	public SMOTE_ENN(IIndex index, int k, short catid) {
		super(index, k, catid);		
	}

	@Override
	public IIndex resample(int oversampling, int undersampling) {
		//perform the SMOTE oversampling
		IIndex resampled = super.resample(oversampling, undersampling);
		super.clear();
		
		//removes all misclassified documents according to a 3-NN check
		removeMisclassifiedKnn(3, resampled, _uniqueCatID);
		
		return resampled;		
	}

	public static void removeMisclassifiedKnn(int k, IIndex resampled, short uniqueCatID) {
		DocSet documentIDs=DocSet.genDocset(resampled.getDocumentDB());
		HashMap<Integer, List<SimilarDocument>> neigbours = computeNN(resampled, documentIDs, k);
		
		List<Integer> misclassified=new ArrayList<Integer>();
		
		int minoritarianRemoved=0;
		int mayoritarianRemoved=0;
		
		for(int docID : documentIDs.asList()){
			boolean docCategory=resampled.getClassificationDB().hasDocumentCategory(docID, uniqueCatID);
			int agree=0;
			int total=0;
			for(SimilarDocument neig_i : neigbours.get(docID)){
				int neigID=neig_i.docID;
				boolean neigCategory=resampled.getClassificationDB().hasDocumentCategory(neigID, uniqueCatID);
				if(docCategory==neigCategory)
					agree++;
				total++;
			}
			if(agree<=total/2){
				misclassified.add(docID);
				if(docCategory)
					minoritarianRemoved++;
				else
					mayoritarianRemoved++;
			}
		}
		
		int positives=resampled.getClassificationDB().getCategoryDocumentsCount(uniqueCatID);
		int negatives=resampled.getDocumentDB().getDocumentsCount()-positives;
		JatecsLogger.status().println("Removing " + minoritarianRemoved+"/"+positives + " from the minoritarian class");
		JatecsLogger.status().println("Removing " + mayoritarianRemoved+"/"+negatives + " from the mayoritarian class");
		
		Collections.sort(misclassified);
		resampled.removeDocuments(IntArrayIterator.List2IntArrayIterator(misclassified), false);	
		
	}
}
