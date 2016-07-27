package it.cnr.jatecs.representation.oversampling;

import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.utils.DocSet;
import it.cnr.jatecs.representation.vector.IndexVectorizer;

import java.util.List;

public abstract class AbstractBaselineDocSampler {
	protected short _uniqueCatID;
	private int _absoluteIDcounter=0;
	protected IIndex _index;
	protected IndexVectorizer _sampler;

	protected void init(IIndex index, short catid) {
		_index=index;
		_absoluteIDcounter=0;
		_uniqueCatID=catid;
		_sampler=new IndexVectorizer(_index);
	}

	public IIndex resample(int oversampling, int undersampling) {		
		GenericIndexBuilderRows indexbuilder=new GenericIndexBuilderRows(_index.getDomainDB());
		
		DocSet positiveDocIDs=DocSet.genCatDocset(_index, _uniqueCatID);
		DocSet negativeDocIDs=DocSet.minus(DocSet.genDocset(_index.getDocumentDB()), positiveDocIDs);
		
		sampling(undersampling, negativeDocIDs, indexbuilder, false);
		sampling(oversampling, positiveDocIDs, indexbuilder, true);
		
		return indexbuilder.getIndex();
	}
	
	protected void sampling(int sampling, DocSet docIDs, GenericIndexBuilderRows indexbuilder, boolean positive){
		int added=0;
		
		//first sample round is performed iteratively to assure all documents are selected at least once
		if(sampling >= docIDs.size()){
			List<Integer> posdocList=docIDs.asList();
			for(int i=0; i < posdocList.size() && added < sampling; i++, added++)
				simpleReplicateSampleDoc(posdocList.get(i), indexbuilder);
		}
		
		//complete the random sampling
		for(int docID : docIDs.randomSampling(sampling - added))
			sampleDoc(docID, indexbuilder, positive);
	}
	
	private void sampleDoc(int docID, GenericIndexBuilderRows indexbuilder, boolean positive) {		
		if(positive)
			positiveSampleDoc(docID, indexbuilder);
		else
			negativeSampleDoc(docID, indexbuilder);
	}

	abstract void positiveSampleDoc(int docID, GenericIndexBuilderRows indexbuilder);
	
	abstract void negativeSampleDoc(int docID, GenericIndexBuilderRows indexbuilder);
	
	abstract void simpleReplicateSampleDoc(int docID, GenericIndexBuilderRows indexbuilder);
	
	public String generateNewName(String name, GenericIndexBuilderRows indexbuilder){
		if(!indexbuilder.hasDocumentNamed(name))
			return name;
		else return name+"_"+_absoluteIDcounter++;
	}
	
}
