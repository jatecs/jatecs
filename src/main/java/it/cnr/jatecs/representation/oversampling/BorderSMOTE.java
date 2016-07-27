package it.cnr.jatecs.representation.oversampling;

import it.cnr.jatecs.classification.knn.SimilarDocument;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.JatecsLogger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class BorderSMOTE extends SMOTE{
	
	private int _k;

	public BorderSMOTE(IIndex index, int k, short catid) {
		super(index, k, catid);
		_k=k;
		searchDangerSet();
	}
	
	private void searchDangerSet(){
		HashSet<Integer> DANGER=new HashSet<Integer>();
		for(int docid : this._nearest_neigbours.keySet()){
			int positiveNeighbours=0;
			for(SimilarDocument neighbourid : this._nearest_neigbours.get(docid)){
				if(minoritarian(neighbourid.docID))
					positiveNeighbours++;
			}
			if(positiveNeighbours<(_k/2.0)){
				DANGER.add(docid);
			}
		}
		
		JatecsLogger.status().println("The DANGER set contains " + DANGER.size()+"/"+this._nearest_neigbours.size()+" positive examples.");
		
		//clean the nearest neigbours so that only documents in DANGER are considered and only minoritarian nearest neighbours
		List<Integer> docids = new ArrayList<Integer>(this._nearest_neigbours.keySet());
		for(int docid: docids){
			if(!DANGER.contains(docid)){
				this._nearest_neigbours.remove(docid);
			}
			else{
				List<SimilarDocument> neigbours=this._nearest_neigbours.get(docid);
				for(int i = 0; i < neigbours.size(); ){
					int nn_id=neigbours.get(i).docID;
					if(!minoritarian(nn_id))
						neigbours.remove(i);
					else i++;					
				}
			}
		}
	}
	
	public boolean minoritarian(int docid){
		return this._index.getClassificationDB().hasDocumentCategory(docid, _uniqueCatID);
	}

}
