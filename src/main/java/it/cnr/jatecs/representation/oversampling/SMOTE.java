package it.cnr.jatecs.representation.oversampling;

import it.cnr.jatecs.classification.knn.SimilarDocument;
import it.cnr.jatecs.classification.knn.TextualKnnSearcherCached;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.utils.DocSet;
import it.cnr.jatecs.indexing.similarity.EuclideanSquareDistance;
import it.cnr.jatecs.representation.vector.IndexVectorizer;
import it.cnr.jatecs.representation.vector.SparseMatrix;
import it.cnr.jatecs.representation.vector.SparseVector;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

public class SMOTE extends AbstractBaselineDocSampler{
	
	protected HashMap<Integer, List<SimilarDocument>> _nearest_neigbours;
	//private IndexVectorizer _vecindex;
	private Random _rand;

	public SMOTE(IIndex index, int k, short catid) {
		init(index, catid);		
		_rand=new Random();
		_nearest_neigbours=computeNN(_index, DocSet.genCatDocset(_index, catid), k);
	}	
	
	protected static HashMap<Integer, List<SimilarDocument>> computeNN(IIndex index, DocSet forDocuments, int k) {
		HashMap<Integer, List<SimilarDocument>> nearest_neigbours=new HashMap<>();
		List<Integer> doclist=forDocuments.asList();
		
		int progress=0;
		int totalsteps=doclist.size();
		JatecsLogger.status().println("Knn for " + totalsteps + " documents: ");
		
		TextualKnnSearcherCached knn=new TextualKnnSearcherCached(new EuclideanSquareDistance());
		
		for(int docID_i : doclist){
			List<SimilarDocument> neigbours=knn.search(index, docID_i, k);
			nearest_neigbours.put(docID_i,neigbours);
			
			if(progress++%(Math.max(totalsteps/10,1))==0)
				JatecsLogger.status().print("..."+(progress*100/totalsteps)+"%");
		}
		JatecsLogger.status().println("");
		
		knn.clear();
		
		return nearest_neigbours;
	}
	
	protected static HashMap<Integer, List<DocSim>> computeNNfaster(IIndex index, DocSet forDocuments, int k) {
		int nD=index.getDocumentDB().getDocumentsCount();
		SparseMatrix _distcache=new SparseMatrix(nD, nD);
		
		HashMap<Integer, List<DocSim>> nearest_neigbours=new HashMap<>();
		
		List<Integer> alldocuments=DocSet.genDocset(index.getDocumentDB()).asList();
		
		IndexVectorizer docvectorizer = new IndexVectorizer(index);
		HashMap<Integer, SparseVector> docVector = new HashMap<>();
		for(int docid:alldocuments){
			docVector.put(docid, docvectorizer.getDocumentWeights(docid));
		}
		
		List<Integer> doclist=forDocuments.asList();
		int progress=0;
		int totalsteps=doclist.size();
		JatecsLogger.status().println("Knn for " + totalsteps + " documents: ");
		for(int docID_i : doclist){
			TreeSet<DocSim> neighbours = new TreeSet<DocSim>();
			
			double distThreshold = Double.MAX_VALUE;
			SparseVector docvec_i=docVector.get(docID_i);
			
			for(int docID_j : alldocuments){
				if(docID_i==docID_j) continue;
				
				double distance = _distcache.get(Math.min(docID_i, docID_j), Math.max(docID_i, docID_j));
				if(distance==0){				
					distance = euclideanSquaredDistanceThresholded(docvec_i, docVector.get(docID_j), distThreshold);
					_distcache.set(Math.min(docID_i, docID_j), Math.max(docID_i, docID_j), distance);
				}
					
				neighbours.add(new DocSim(docID_j, distance));
				if(neighbours.size()>k){
					neighbours.pollLast();
					distThreshold = Math.min(neighbours.last().distance, distThreshold);
				}
			}

			nearest_neigbours.put(docID_i, new ArrayList<DocSim>(neighbours));
			
			if(progress++%(Math.max(totalsteps/10,1))==0)
				JatecsLogger.status().print("..."+(progress*100/totalsteps)+"%");
		}
		JatecsLogger.status().println("");
		
		return nearest_neigbours;
	}
	
	

	
	private static double euclideanSquaredDistanceThresholded(SparseVector v1, SparseVector v2, double threshold){				
		double dist = 0.0;
		Set<Integer> dims = SparseVector.getNonZeroDimensions(v1, v2);
	    for(int dim:dims){
			double diff = (v1.get(dim) - v2.get(dim));
			dist += (diff*diff);
			if(dist >= threshold)
				return Double.MAX_VALUE;
		}
		return dist;
	}
	
	

	@Override
	protected void positiveSampleDoc(int docID, GenericIndexBuilderRows indexbuilder){
		String oldName=_index.getDocumentDB().getDocumentName(docID);
		String docName=super.generateNewName(oldName, indexbuilder);
		
		//select a random neighbour
		List<SimilarDocument> neigbours=_nearest_neigbours.get(docID);
		if(neigbours!=null && !neigbours.isEmpty()){
			int randNeigID=neigbours.get(_rand.nextInt(neigbours.size())).docID;
			
			SparseVector docRep=_sampler.getDocumentWeights(docID);
			SparseVector neigbourRep=_sampler.getDocumentWeights(randNeigID);
			SparseVector difvec=SparseVector.minus(neigbourRep, docRep);
			difvec.times(_rand.nextDouble());
			SparseVector synthetic=SparseVector.sum(docRep, difvec);
			
			IShortIterator docCats=_index.getClassificationDB().getDocumentCategories(docID);
			indexbuilder.addDocumentRawWeights(docName, synthetic, docCats);
		}
	}
	
	@Override
	void negativeSampleDoc(int docID, GenericIndexBuilderRows indexbuilder) {
		simpleReplicateSampleDoc(docID, indexbuilder);		
	}
	
	protected void simpleReplicateSampleDoc(int docID, GenericIndexBuilderRows indexbuilder){
		String oldName=_index.getDocumentDB().getDocumentName(docID);
		String docName=super.generateNewName(oldName, indexbuilder);
		SparseVector docRep=_sampler.getDocumentWeights(docID);
		IShortIterator docCats=_index.getClassificationDB().getDocumentCategories(docID);
		indexbuilder.addDocumentRawWeights(docName, docRep, docCats);			
	}
	
	protected void clear(){
		this._nearest_neigbours.clear();
	}

}

class DocSim implements Comparable<DocSim>{
	public int docid;
	public double distance;
	
	public DocSim(int id, double dist){
		docid=id;
		distance=dist;
	}
	
	public String toString(){
		return ""+docid+":"+distance;
	}
	
	@Override
	public int compareTo(DocSim arg0) {
		if(this.docid==arg0.docid) return 0;
		return Double.compare(this.distance, arg0.distance);
	}		
}