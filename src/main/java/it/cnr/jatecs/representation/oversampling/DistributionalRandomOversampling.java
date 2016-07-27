package it.cnr.jatecs.representation.oversampling;

import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveDomainDB;
import it.cnr.jatecs.indexes.utils.DocSet;
import it.cnr.jatecs.indexing.tsr.InformationGain;
import it.cnr.jatecs.representation.vector.DenseVector;
import it.cnr.jatecs.representation.vector.IVector;
import it.cnr.jatecs.representation.vector.SparseVector;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.Os;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DistributionalRandomOversampling {
	
	private IRandomDistributionModel _distmodel;
	
	private IIndex _weightedIndex=null;

	private DRIcustomizer _customizer;
	
	private final short _poscat=(short)0;
	
	private Random _rand;
	
	private ExecutorService _executor;
	
	public static int MAX_THREADS=1;
	
	public DistributionalRandomOversampling(IRandomDistributionModel distmodel, DRIcustomizer customizer){
		this._distmodel=distmodel;
		this._customizer=customizer;
		this._rand=new Random();
	}
	
	public IIndex compute(IIndex index, boolean test) throws IOException, InterruptedException, ExecutionException{		
		msg("Computing DRO " + (test?"test":"training"));
		this._executor = Executors.newFixedThreadPool(MAX_THREADS);
		TroveDomainDB domain=new TroveDomainDB(index.getCategoryDB(), _distmodel.getLatentFeatureSpace());
		GenericIndexBuilderRows proj=new GenericIndexBuilderRows(domain);
		
		HashMap<Integer,Integer> docReplicants=decideDocReplicants(index, test);
		double featSpaceEnhancementRatio=_distmodel.getLatentFeatureSpaceSize()*1.0/_distmodel.getFeatureSpaceSize();
		
		compute(index, featSpaceEnhancementRatio, proj, docReplicants, test);
		
		this._executor.shutdown();
		return proj.getIndex();
	}
	
	private void compute(IIndex index, double featSpaceRatio, GenericIndexBuilderRows proj, 
			HashMap<Integer,Integer> docReplicants, boolean test) throws IOException, InterruptedException, ExecutionException{
		
		DocSet positive=DocSet.genCatDocset(index, _poscat);
		DocSet negative=DocSet.minus(DocSet.genDocset(index.getDocumentDB()), positive);
	
		String modelfilepos=(test?"test":"train")+"pos.dat";
		String modelfileneg=(test?"test":"train")+"neg.dat";
		
		//tries to open a pre-calculated model, if exists
		if(openmodelset(modelfileneg, negative.size(), index, docReplicants, featSpaceRatio, proj) && 
				openmodelset(modelfilepos, positive.size(), index, docReplicants, featSpaceRatio, proj))			
			return;			
		
		//if doesnt exist, then calculates it		
		JatecsLogger.status().println("\t Scanning Negatives");
		computeset(negative, index, docReplicants, featSpaceRatio, proj, modelfileneg);
		
		JatecsLogger.status().println("\t Scanning Positives");
		computeset(positive, index, docReplicants, featSpaceRatio, proj, modelfilepos);

	}
	
	//if requested in the _customizer._loadSave..Path the model will be stored in disk
	private void computeset(DocSet docs, IIndex index, HashMap<Integer,Integer> docReplicants, double featSpaceEnhancementRatio, GenericIndexBuilderRows proj, String modelfile) throws IOException, InterruptedException, ExecutionException{	
		int nD=docs.size();
		int step=0;
		
		DataOutputStream fo=null;		
		if(_customizer._loadSaveProbmodelPath!=null){
			File dir=new File(_customizer._loadSaveProbmodelPath);
			if(!dir.exists()) dir.mkdirs();
			
			fo = new DataOutputStream(new FileOutputStream(_customizer._loadSaveProbmodelPath+Os.pathSeparator()+modelfile));
			fo.writeInt(nD);
			fo.writeInt(_distmodel.getLatentFeatureSpaceSize());
		}
		
		JatecsLogger.status().println("Creating threads...");
		Vector<Future<DocProbability>> futureDocProbs=new Vector<Future<DocProbability>>(docs.size());
		HashMap<Integer, Double> igcache = cacheFeatureTSRscore(_weightedIndex, _poscat);
		for(int docid:docs.asList()){		
			DocProbThread docprobThread=new DocProbThread(index, docid, _distmodel, 
					_customizer._useSoftmax, _poscat, igcache);			
			futureDocProbs.add(_executor.submit(docprobThread));
		}
		
		JatecsLogger.status().println("Getting results...");
		long inittime=System.currentTimeMillis();
		while(!futureDocProbs.isEmpty()){
			Future<DocProbability> result=futureDocProbs.remove(0);
			if(!result.isDone()){
				futureDocProbs.add(result);
				continue;
			}
			int docid=result.get().docID;
			CumumlativeProbFunction docFeatProbDist=result.get().prob;
			if(fo!=null) saveProbModel(docid, docFeatProbDist, fo);
			
			generateOversamples(index, docid, featSpaceEnhancementRatio, docReplicants.get(docid), docFeatProbDist, proj);
			//futureDocProbs.set(i, null);
			
			if(++step%(Math.max(nD/20, 1))==0){
				JatecsLogger.status().print("..."+step*100/nD+"% ("+((System.currentTimeMillis()-inittime)/1000)+"s)");
			}		
		}
		JatecsLogger.status().println("");
		
		if(fo!=null) fo.close();
	}
	
	private void saveProbModel(int docid,
			CumumlativeProbFunction docFeatProbDist, DataOutputStream fo) throws IOException {
		
		DenseVector massfunc = docFeatProbDist.getProbabilityMass();
		fo.writeInt(docid);
		for(int i = 0; i < massfunc.size(); i++){
			fo.writeDouble(massfunc.get(i));
		}		
	}

	private boolean openmodelset(String modelpath, int nD, 
			IIndex index, HashMap<Integer,Integer> docReplicants, double featSpaceEnhancementRatio, GenericIndexBuilderRows proj) throws IOException{				
		int step=0;
		ProbModelStoreManager probModelStoreManager = new ProbModelStoreManager(_customizer._loadSaveProbmodelPath);
		if(!probModelStoreManager.existFile(modelpath)) return false;
		probModelStoreManager.openModel(modelpath);
		JatecsLogger.status().println("Loading pre-calculated model from " + _customizer._loadSaveProbmodelPath+"/"+modelpath);
		while(probModelStoreManager.hasNext()){
			DocProbability docprob = probModelStoreManager.next();									
			generateOversamples(index, docprob.docID, featSpaceEnhancementRatio, docReplicants.get(docprob.docID), docprob.prob, proj);
			
			if(++step%(Math.max(nD/20, 1))==0){
				JatecsLogger.status().print("..."+step*100/nD+"%");
			}			
		}
		JatecsLogger.status().println("");
		probModelStoreManager.closeModel();
		return true;
	}
	
	private void generateOversamples(IIndex index, int docid, double featSpaceEnhancementRatio, int docRepresentations,
			CumumlativeProbFunction docFeatProbDist, GenericIndexBuilderRows proj){
		if(docRepresentations>0){
			String docName=index.getDocumentDB().getDocumentName(docid);
			IShortIterator docCats = index.getClassificationDB().getDocumentCategories(docid);
			int docLength=index.getContentDB().getDocumentLength(docid);
			int numLatentFeat=(int)(featSpaceEnhancementRatio*docLength);
			for(int i = 0; i < docRepresentations; i++){
				SparseVector docRep=docFeatProbDist.randomTrial(numLatentFeat);				
				proj.addDocumentRawFrequencies(docName+"_"+i, docRep, docCats);
			}
		}
	}
	
	//decide the number of times each document might be replicated
	private HashMap<Integer, Integer> decideDocReplicants(IIndex index, boolean test) {
		HashMap<Integer, Integer> docReplicants=new HashMap<>();
		
		if(test){
			IIntIterator docsit=index.getDocumentDB().getDocuments();
			while(docsit.hasNext())
				docReplicants.put(docsit.next(), _customizer._testReplicants);			
		}
		else{			
			int positiveDocuments=index.getClassificationDB().getCategoryDocumentsCount(_poscat);
			int negativeDocuments=index.getDocumentDB().getDocumentsCount()-positiveDocuments;
			
			IIntIterator docsit=index.getDocumentDB().getDocuments();			
			while(docsit.hasNext()){
				int docid=docsit.next();
				boolean positive=index.getClassificationDB().getDocumentCategoriesCount(docid)>0;
				double samplingRatio;
				int docRepresentations;
				if(positive){
					//oversampling
					samplingRatio=_customizer._trainReplicants*1.0/positiveDocuments;
					docRepresentations=decideReplicas(samplingRatio);
				}
				else{
					//undersampling
					if(_customizer._undersampling_ratio>0){
						double finalnegatives=_customizer._trainReplicants*_customizer._undersampling_ratio;
						samplingRatio = finalnegatives/negativeDocuments;
						docRepresentations=decideReplicas(samplingRatio);
					}
					else docRepresentations=1;
				}
				docReplicants.put(docid, docRepresentations);
			}
		}
		
		return docReplicants;
	}
	
	//returns a concrete number of replicas based on the sampling ratio.
	//for example, for 2.3 will return 2 (with probabilty 70%) or 3 (with probability 30%)
	private int decideReplicas(double samplingRatio){
		double parteEntera=Math.floor(samplingRatio);
		double resto=samplingRatio-parteEntera;
		int docRepresentations=(int)(parteEntera + (_rand.nextDouble()<resto? 1.0 : 0));
		return docRepresentations;
	}	

	private void msg(String msg){
		JatecsLogger.status().println(msg);
	}

	public void setSupervisedWeighting(IIndex train) {
		_weightedIndex=train;
	}
	
	private static HashMap<Integer,Double> cacheFeatureTSRscore(IIndex index, short poscat) {
		InformationGain ig=new InformationGain();
		HashMap<Integer,Double> cached=new HashMap<>();
		IIntIterator feats=index.getFeatureDB().getFeatures();
		while(feats.hasNext()){
			int featID = feats.next();
			double tsr=ig.compute(poscat, featID, index);
			cached.put(featID, tsr);
		}	
		return cached;
	}
}

class NumCatDocs implements Comparable<NumCatDocs>{
	short catid;
	int ndocs;
	public NumCatDocs(short catid, int ndocs){
		this.catid=catid;
		this.ndocs=ndocs;
	}
	@Override
	public int compareTo(NumCatDocs other) {
		int cmp=Integer.compare(this.ndocs, other.ndocs);
		if(cmp==0)
			cmp=Short.compare(this.catid, other.catid);
		return cmp;
	}
}

class DocProbability{	
	int docID;
	CumumlativeProbFunction prob;
	public DocProbability(int docid, CumumlativeProbFunction p) {
		this.docID=docid;
		this.prob=p;
	}
}

class ProbModelStoreManager{
	String _path;
	private DataInputStream br;
	int ndocs=-1;
	int docread=-1;
	int latent=-1;
	
	public ProbModelStoreManager(String path) {
		_path = path;
	}
	
	public boolean existFile(String name){
		if(this._path!=null){
			File f=new File(this._path+Os.pathSeparator()+name);
			return f.exists() && f.isFile();
		}
		return false;
	}
	
	public void openModel(String name) throws IOException{	
		br = new DataInputStream(new FileInputStream(this._path+Os.pathSeparator()+name));
		this.ndocs = br.readInt();
		this.latent= br.readInt();
		this.docread=0;
	}
	
	public void closeModel() throws IOException{
		br.close();
		this.ndocs = -1;
		this.latent= -1;
		this.docread=-1;
	}
	
	public DocProbability next() throws IOException{
		int docid=br.readInt();
		DenseVector probs = new DenseVector(latent);
		for(int i = 0; i < latent; i++)
			probs.set(i, br.readDouble());
		
		DocProbability docprob=new DocProbability(docid, CumumlativeProbFunction.fromDiscreteMassFunction(probs));
		docread++;
		return docprob;
	}
	
	public boolean hasNext() throws IOException{
		return br.available()>0 && docread<ndocs;
	}
}

class DocProbThread implements Callable<DocProbability> {
    private IIndex index;
    private int docID;
    private IRandomDistributionModel distModel;
    private boolean useSoftmax;
    private short _poscat;
	private HashMap<Integer,Double> _igcache;
    
    public DocProbThread(IIndex index, int docID, IRandomDistributionModel distModel, 
    		boolean useSoftmax, short _poscat, HashMap<Integer,Double> igcache){
    	this.index=index;
    	this.docID=docID;
    	this.distModel=distModel;
    	this.useSoftmax=useSoftmax;
    	this._poscat=_poscat;
    	this._igcache=igcache;
    }

	@Override
	public DocProbability call() throws Exception {
		//System.out.println("Ejecutandome!");
		int latentSpace=distModel.getLatentFeatureSpaceSize();
		
		//relative probability of each latent feature
		DenseVector featProbVec = new DenseVector(latentSpace);
		
		IIntIterator docFeats=index.getContentDB().getDocumentFeatures(docID);		
		while(docFeats.hasNext()){
			int featid=docFeats.next();
			//usar el tfidf es solo una prueba, los experimentos están hechos con weight=1
			double weight=index.getWeightingDB().getDocumentFeatureWeight(docID, featid);
			//double weight=1;
			double importance=_igcache.get(featid) * weight;
			if(importance>0){
				IVector featVec = distModel.getFeatureVector(featid);		
				featProbVec.add(featVec, importance);
			}
		}
		
		//get probability distribution with softmax
		if(useSoftmax)
			featProbVec=softmax(featProbVec);
		
		CumumlativeProbFunction absoluteProbs=CumumlativeProbFunction.fromDiscreteMassFunction(featProbVec);
		
		return new DocProbability(docID, absoluteProbs);	
	}
	
	private DenseVector softmax(DenseVector featProbVec) {
		int n_events=featProbVec.size();
		DenseVector softm=new DenseVector(n_events);
		double denom=0;
		for(int i = 0; i < n_events; i++){
			double z_i=Math.exp(featProbVec.get(i));
			softm.set(i, z_i);
			denom+=z_i;
		}
		for(int i = 0; i < n_events; i++)
			softm.set(i, softm.get(i)/denom);
		return softm;
	}
}