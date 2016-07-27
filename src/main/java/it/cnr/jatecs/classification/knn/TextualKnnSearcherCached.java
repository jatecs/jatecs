package it.cnr.jatecs.classification.knn;

import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexing.similarity.EuclideanDistance;
import it.cnr.jatecs.indexing.similarity.IBaseSimilarityFunction;
import it.cnr.jatecs.indexing.similarity.ISimilarityFunction;
import it.cnr.jatecs.representation.vector.SparseMatrix;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;

import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

public class TextualKnnSearcherCached {

	protected ISimilarityFunction _similarity;
	protected SparseMatrix _cache; 
	
	public TextualKnnSearcherCached(){
		_similarity = new EuclideanDistance();
	}	
	public TextualKnnSearcherCached(ISimilarityFunction similarity){
		_similarity = similarity;
	}
	
	public Vector<SimilarDocument> search(IIndex index, int docID, int k){	
		if(_cache==null){
			int nD=index.getDocumentDB().getDocumentsCount();
			_cache=new SparseMatrix(nD, nD);
		}
		
		Vector<SimilarDocument> docs = new Vector<SimilarDocument>();

		TreeSet<SimilarDocument> sorted = new TreeSet<SimilarDocument>(new SimilarDocumentComparator(_similarity));

		IIntIterator it = index.getDocumentDB().getDocuments();
		while (it.hasNext()){
			int docID_j = it.next();
			if(docID==docID_j) continue;
			
			double score = getFromCache(docID, docID_j, index);			
			
			SimilarDocument sd=new SimilarDocument(docID_j, score);
			sorted.add(sd);
			if (sorted.size() > k)
				sorted.remove(sorted.first());
		}

		Iterator<SimilarDocument> itSim = sorted.iterator();
		while (itSim.hasNext()){
			SimilarDocument sd = itSim.next();
			docs.add(sd);
		}

		return docs;
	}
	
	private double getFromCache(int di, int dj, IIndex index){
		if(di==dj)
			return 0;
		
		int dmin=Math.min(di, dj);
		int dmax=Math.max(di, dj);
		double distance=_cache.get(dmin, dmax);
		if(distance==0){
			distance=new Float(_similarity.compute(dmin, dmax, index));
			_cache.set(dmin, dmax, distance);
		}
		return distance;
	}

	public IBaseSimilarityFunction getSimilarityFunction()	{
		return _similarity;
	}
	public void clear() {
		this._cache.clear();
	}
	
}
