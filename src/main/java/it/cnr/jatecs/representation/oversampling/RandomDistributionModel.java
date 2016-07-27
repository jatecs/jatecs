package it.cnr.jatecs.representation.oversampling;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;

import it.cnr.jatecs.indexes.DB.generic.GenericIndex;
import it.cnr.jatecs.indexes.DB.interfaces.IContentDB;
import it.cnr.jatecs.indexes.DB.interfaces.IFeatureDB;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.interfaces.IWeightingDB;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveClassificationDBBuilder;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveContentDBBuilder;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveDocumentsDBBuilder;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveFeatureDBBuilder;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveWeightingDBBuilder;
import it.cnr.jatecs.indexes.utils.DocSet;
import it.cnr.jatecs.representation.vector.DenseVector;
import it.cnr.jatecs.representation.vector.IVector;
import it.cnr.jatecs.representation.vector.IndexVectorizer;
import it.cnr.jatecs.representation.vector.SparseVector;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

public class RandomDistributionModel implements IRandomDistributionModel{
	
	protected Hashtable<Integer, IVector> _featVecSet;
	protected IIndex _eventIndex;
	private Hashtable<Integer,Integer> _docIDdimMap;
	private IFeatureDB _latentSpace;
	private int _latentDimensions;
	
	public RandomDistributionModel(IIndex index, int latentDimensions){
		_eventIndex=takeEvents(index, latentDimensions);
		_latentDimensions=latentDimensions;
		_featVecSet = new Hashtable<Integer, IVector>();		
		_latentSpace=createLatentSpace();
		_docIDdimMap=mapEventID_dim(_eventIndex);
	}	
	
	//map each of the event-IDs with a dimension in the space
	private static Hashtable<Integer, Integer> mapEventID_dim(IIndex eventIndex) {
		IIntIterator docsit = eventIndex.getDocumentDB().getDocuments();
		int dimpos=0;
		Hashtable<Integer, Integer> docIDdimMap=new Hashtable<>();
		while(docsit.hasNext()){
			int docid = docsit.next();
			docIDdimMap.put(docid, dimpos++);
		}
		return docIDdimMap;
	}

	private IIndex takeEvents(IIndex index, int n_events) {
		if(n_events==index.getDocumentDB().getDocumentsCount())
			return index.cloneIndex();
		
		Random rand=new Random();
		List<Integer> alldocIDs=new ArrayList<Integer>();
		TroveDocumentsDBBuilder events=new TroveDocumentsDBBuilder();
		TroveContentDBBuilder content=new TroveContentDBBuilder(events.getDocumentDB(), index.getFeatureDB());
		TroveWeightingDBBuilder weighting=new TroveWeightingDBBuilder(content.getContentDB());
		TroveClassificationDBBuilder classification=new TroveClassificationDBBuilder(events.getDocumentDB(), index.getCategoryDB());
		
		for(int i = 0; i < n_events; i++){
			if(alldocIDs.isEmpty())
				alldocIDs=DocSet.genDocset(index.getDocumentDB()).asList();
			int remainingDocs=alldocIDs.size();
			int randDocID=alldocIDs.remove(rand.nextInt(remainingDocs));
			String docName=index.getDocumentDB().getDocumentName(randDocID);
			int eventID = events.addDocument("Event_"+i+"_fromDocName_"+docName);
			
			IIntIterator docFeats=index.getContentDB().getDocumentFeatures(randDocID);
			while(docFeats.hasNext()){
				int featID=docFeats.next();
				int count=index.getContentDB().getDocumentFeatureFrequency(randDocID, featID);
				if(count>0){
					double weight=index.getWeightingDB().getDocumentFeatureWeight(randDocID, featID);
					
					content.setDocumentFeatureFrequency(eventID, featID, count);					
					weighting.setDocumentFeatureWeight(eventID, featID, weight);
				}
				IShortIterator doccats=index.getClassificationDB().getDocumentCategories(randDocID);
				while(doccats.hasNext())
					classification.setDocumentCategory(eventID, doccats.next());				
			}			
		}		
		
		return new GenericIndex(index.getFeatureDB(), 
				events.getDocumentDB(), 
				index.getCategoryDB(), 
				index.getDomainDB(), 
				content.getContentDB(), 
				weighting.getWeightingDB(), 
				classification.getClassificationDB());
	}

	private IFeatureDB createLatentSpace() {
		TroveFeatureDBBuilder latent=new TroveFeatureDBBuilder();
		for(int i = 0; i < _latentDimensions; i++){
			latent.addFeature("latent_"+i);
		}
		return latent.getFeatureDB();
	}

	private int numFeatures(){
		return _eventIndex.getFeatureDB().getFeaturesCount();
	}

	@Override
	public synchronized IVector getFeatureVector(int featID) {
		if(!_featVecSet.contains(featID))
			_featVecSet.put(featID, computeFeatureVector(featID));
		return _featVecSet.get(featID);
	}

	private IVector computeFeatureVector(int featID) {
		IVector featureVector=storeMemoryFeatureVector(featID);
		
		IIntIterator docit = _eventIndex.getContentDB().getFeatureDocuments(featID);
		while(docit.hasNext()){
			int docID = docit.next();
			//int count = _content.getDocumentFeatureFrequency(docID, featID);
			double weight = _eventIndex.getWeightingDB().getDocumentFeatureWeight(docID, featID);
			if(weight!=0){
				int dimpos = _docIDdimMap.get(docID);
				featureVector.set(dimpos, weight);
			}
		}
		
		//check with l1 instead of l2
		featureVector.normalize();
		
		return featureVector;
	}
	
	//initializes a feature vector for a given feature as dense or sparse depending on its initial load
	private IVector storeMemoryFeatureVector(int featID){
		int nD=_eventIndex.getDocumentDB().getDocumentsCount();
		int featDocsCount=_eventIndex.getContentDB().getFeatureDocumentsCount(featID);
		boolean sparse=(featDocsCount*1.0/nD)<0.5;		
		if(sparse)
			return new SparseVector(_latentDimensions);
		else
			return new DenseVector(_latentDimensions);
	}

	@Override
	public IIntIterator getFeatureSpace() {
		return _eventIndex.getFeatureDB().getFeatures();
	}

	@Override
	public IFeatureDB getLatentFeatureSpace() {
		return _latentSpace;
	}


	@Override
	public int getLatentFeatureSpaceSize() {
		return _latentDimensions;
	}


	@Override
	public int getFeatureSpaceSize() {
		return numFeatures();
	}
	
	
	
	
}
