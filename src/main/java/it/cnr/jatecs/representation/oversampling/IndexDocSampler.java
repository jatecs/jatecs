package it.cnr.jatecs.representation.oversampling;

import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.representation.vector.SparseVector;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

public class IndexDocSampler extends AbstractBaselineDocSampler{

	public IndexDocSampler(IIndex index, short catid) {
		init(index, catid);
	}

	protected void positiveSampleDoc(int docID, GenericIndexBuilderRows indexbuilder){
		simpleReplicateSampleDoc(docID, indexbuilder);
	}
	
	protected void negativeSampleDoc(int docID, GenericIndexBuilderRows indexbuilder){
		simpleReplicateSampleDoc(docID, indexbuilder);
	}
	
	protected void simpleReplicateSampleDoc(int docID, GenericIndexBuilderRows indexbuilder){
		String oldName=_index.getDocumentDB().getDocumentName(docID);
		String docName=super.generateNewName(oldName, indexbuilder);
		SparseVector docRep=_sampler.getDocumentFrequencies(docID);
		IShortIterator docCats=_index.getClassificationDB().getDocumentCategories(docID);
		indexbuilder.addDocumentRawFrequencies(docName, docRep, docCats);			
	}
	
}
