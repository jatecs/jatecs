package it.cnr.jatecs.representation.vector;

import java.util.HashMap;

import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;

enum Field{
	Frequency,
	Weight,
	Boolean
}

public class IndexVectorizer {
	
	private IIndex index;
	private HashMap<Integer,Integer> docDim;
	private HashMap<Integer,Integer> featDim;
	
	public IndexVectorizer(IIndex index){
		this.index=index;
		this.docDim=new HashMap<>();
		this.featDim=new HashMap<>();
		
		indexIdsDims(this.index.getDocumentDB().getDocuments(), docDim);
		indexIdsDims(this.index.getFeatureDB().getFeatures(), featDim);
	}

	private static void indexIdsDims(IIntIterator ids_it, HashMap<Integer,Integer> idDim) {
		int dim=0;
		ids_it.begin();
		while(ids_it.hasNext())
			idDim.put(ids_it.next(), dim++);		
	}

	public SparseVector getDocumentFrequencies(int docID){
		return getDocumentRepresentation(docID, Field.Frequency);
	}
	
	public SparseVector getDocumentWeights(int docID){
		return getDocumentRepresentation(docID, Field.Weight);
	}
	
	public SparseVector getDocumentBoolean(int docID){
		return getDocumentRepresentation(docID, Field.Boolean);
	}
	
	public SparseVector getFeatureFrequencies(int featID){
		return getFeatureRepresentation(featID, Field.Frequency);
	}
	
	public SparseVector getFeatureWeights(int featID){
		return getFeatureRepresentation(featID, Field.Weight);
	}
	
	public SparseVector getFeatureBoolean(int featID){
		return getFeatureRepresentation(featID, Field.Boolean);
	}
	
	private SparseVector getDocumentRepresentation(int docID, Field field){
		int nF=index.getFeatureDB().getFeaturesCount();
		SparseVector docrep=new SparseVector(nF);
		IIntIterator docfeats=index.getContentDB().getDocumentFeatures(docID);
		while(docfeats.hasNext()){
			int featID=docfeats.next();			
			double val=getMatrixValue(docID, featID, field);			
			int dim=featDim.get(featID);
			docrep.set(dim, val);
		}
		return docrep;
	}
	
	private SparseVector getFeatureRepresentation(int featID, Field field){
		int nD=index.getDocumentDB().getDocumentsCount();
		SparseVector featrep=new SparseVector(nD);
		IIntIterator featdocs=index.getContentDB().getFeatureDocuments(featID);
		while(featdocs.hasNext()){
			int docID=featdocs.next();			
			double val=getMatrixValue(docID, featID, field);			
			int dim=docDim.get(docID);
			featrep.set(dim, val);
		}
		return featrep;
	}
	
	private double getMatrixValue(int docID, int featID, Field field){
		double val=0;
		switch (field) {
		case Frequency:
			val=index.getContentDB().getDocumentFeatureFrequency(docID, featID);
			break;
		case Weight:
			val=index.getWeightingDB().getDocumentFeatureWeight(docID, featID);
			break;
		case Boolean:
			val=1.0;
			break;
		}
		return val;
	}
	
	public int getDocumentDimension(int docID){
		return this.docDim.get(docID);
	}
	
	public int getFeatureDimension(int featID){
		return this.featDim.get(featDim);
	}

	public int getDocumentsCount() {
		return index.getDocumentDB().getDocumentsCount();
	}

	public int getFeaturesCount() {
		return index.getFeatureDB().getFeaturesCount();
	}
	
	public IIndex index(){
		return this.index;
	}
	
}
