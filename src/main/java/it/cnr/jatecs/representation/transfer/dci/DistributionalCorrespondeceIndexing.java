/*
 * This file is part of JaTeCS.
 *
 * JaTeCS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JaTeCS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JaTeCS.  If not, see <http://www.gnu.org/licenses/>.
 *
 * The software has been mainly developed by (in alphabetical order):
 * - Andrea Esuli (andrea.esuli@isti.cnr.it)
 * - Tiziano Fagni (tiziano.fagni@isti.cnr.it)
 * - Alejandro Moreo Fernández (alejandro.moreo@isti.cnr.it)
 * Other past contributors were:
 * - Giacomo Berardi (giacomo.berardi@isti.cnr.it)
 */

package it.cnr.jatecs.representation.transfer.dci;

import it.cnr.jatecs.indexes.DB.generic.GenericIndex;
import it.cnr.jatecs.indexes.DB.interfaces.IFeatureDB;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveContentDBBuilder;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveDomainDB;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveFeatureDBBuilder;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveWeightingDBBuilder;
import it.cnr.jatecs.indexing.tsr.FeatureEntry;
import it.cnr.jatecs.indexing.tsr.RoundRobinTSR;
import it.cnr.jatecs.representation.vector.DenseVector;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


/**
 * Implementation of the Distributional Correspondence Algorithm described in:
 * <i>{@code Moreo Fernandez, A., Esuli, A., and Sebastiani, F. (2016). 
 * Distributional Correspondence Indexing for Cross-Lingual 
 * and Cross-Domain Sentiment Classification. 
 * Journal of Artificial Intelligence Research, 131-163.}</i>
 * */
public class DistributionalCorrespondeceIndexing {	

	//source training collection (labeled)
	private IIndex train_s;
	
	//source unlabeled collection
	private IDistributionalCorrespondenceFunction unlabel_s;
	
	//target unlabeled collection
	private IDistributionalCorrespondenceFunction unlabel_t;
	
	private DCIcustomizer _customizer;

	private IIndex projectedTrain;	
	
	
	private IFeatureDB lantentFeatures = null;
	private HashMap<Integer, DenseVector> sourceVectors = null;
	private HashMap<Integer, DenseVector> targetVectors = null;
	private List<Pivot> pivots;
	private ExecutorService executorService;
	private boolean _verbose=true;
	
	
	public DistributionalCorrespondeceIndexing(IIndex train_s, 
			IDistributionalCorrespondenceFunction unlabel_s, 
			IDistributionalCorrespondenceFunction unlabel_t, 
			DCIcustomizer customizer) {

		this.train_s=train_s;
		this.unlabel_s=unlabel_s;
		this.unlabel_t=unlabel_t;
		this._customizer=customizer;
		
		int sF = unlabel_s.getFeaturesCount();
		int tF = unlabel_t.getFeaturesCount();
		
		int npivots=_customizer._num_pivots;
		_customizer._num_pivots=Math.min(npivots, Math.min(sF, tF));		
	}

	public void compute() throws InterruptedException, ExecutionException {
		
		JatecsLogger.status().println("Computing Distributional Correspondence Indexing");	
		
		pivotSelection();
		
		computeFeatureProfiles();
		
		normalize();
		
		unification();
		
		createLatentFeatures();
		
		projectedTrain=project(train_s, sourceVectors, lantentFeatures, Pivot.fromSource(pivots));		

	}
	
	public void embeddings() throws InterruptedException, ExecutionException {
		
		pivotSelection();
		
		computeFeatureProfiles();
		
		normalize();
		
		unification();
	}
	
	private void pivotSelection(){
		List<FeatureEntry> Vp = sortFeaturesByInformativeness(maxCandidates());			
		if(_customizer._crosscorrespondence)
			Vp = reweightCrossInformative(Vp);
		if(_verbose)
			showPivots(Vp, _customizer._num_pivots);
		pivots = createPivots(Vp, _customizer._num_pivots);
	}
	
	private void computeFeatureProfiles() throws InterruptedException, ExecutionException{
		executorService = Executors.newFixedThreadPool(_customizer._nThreads);
		sourceVectors = getFeatureProfilesThread(unlabel_s, Pivot.fromSource(pivots));
		targetVectors = getFeatureProfilesThread(unlabel_t, Pivot.fromTarget(pivots));
		executorService.shutdown();
	}
	
	private void unification(){
		if(_customizer._unification){
			List<Pivot> common = getCommonWords(pivots);
			unification(sourceVectors, targetVectors, pivots, unlabel_s, unlabel_t);
			unification(sourceVectors, targetVectors, common, unlabel_s, unlabel_t);
		}
	}
	
	private void normalize(){
		if(_customizer._rescale){
			JatecsLogger.status().print("\tRescaling feature-profiles...");
			pivotReescaleWeighted(sourceVectors, unlabel_s);
			pivotReescaleWeighted(targetVectors, unlabel_t);
		}
		
		JatecsLogger.status().print("\tNormalizing feature-profiles...");
		for(DenseVector vec : sourceVectors.values())
			vec.normalize();
		for(DenseVector vec : targetVectors.values())
			vec.normalize();
		JatecsLogger.status().println("[done]");		
	}	
	
	public HashMap<Integer, DenseVector> getSourceEmbeddings(){
		return sourceVectors;
	}
	public HashMap<Integer, DenseVector> getTargetEmbeddings(){
		return targetVectors;
	}

	private void pivotReescaleWeighted(HashMap<Integer, DenseVector> domainVectors, IDistributionalCorrespondenceFunction model) {
		int n_pivots=_customizer._num_pivots;		
		DenseVector average=new DenseVector(n_pivots);
		double n=0;
		for(int featID : domainVectors.keySet()){
			DenseVector vec = domainVectors.get(featID);
			int occurrencies=model.getFeatureDocumentsCount(featID);
			if(occurrencies>0){
				average.add(vec);
				n++;
			}
		}
		average.times(1.0/n);
		
		DenseVector st_dev=new DenseVector(n_pivots);
		for(int i = 0; i < n_pivots; i++){
			double mui = average.get(i);
			double squared_dif=0;
			for(int featID : domainVectors.keySet()){
				DenseVector vec = domainVectors.get(featID);			
				double xi = vec.get(i);				
				int occurrencies=model.getFeatureDocumentsCount(featID);
				if(occurrencies>0){
					squared_dif+=((xi-mui)*(xi-mui));
				}
			}
			st_dev.set(i, Math.sqrt(squared_dif/n));
		}
		
		for(DenseVector vec : domainVectors.values()){
			for(int i = 0; i < n_pivots; i++){
				double xi=vec.get(i);
				double mui=average.get(i);
				double td=st_dev.get(i);
				vec.set(i, (xi-mui)/td);
			}
		}
	}
	
	private int maxCandidates() {
		int maxCand = unlabel_s.getFeaturesCount();
		
		if(_customizer._oracle!=null){
			if(_customizer._oracle.isCreateOnDemand()){
				maxCand = Math.min(maxCand, _customizer._num_pivots*2);
			}
		}
		
		return maxCand;
	}

	private List<FeatureEntry> reweightCrossInformative(List<FeatureEntry> vp) {
		List<FeatureEntry> clean = new ArrayList<FeatureEntry>();
		for(int i = 0; i < vp.size(); i++){
			int sourceid = vp.get(i).featureID;
			String featureName = unlabel_s.getFeatureDB().getFeatureName(sourceid);	
			String targetName = translateFeature(featureName);
			int targetid = unlabel_t.getFeatureDB().getFeature(targetName);
			if(targetid!=-1){
				double featDistorsionSim = crossDistorsionSim(sourceid, targetid);				
				double crossScore = vp.get(i).score*featDistorsionSim;
				if(crossScore>0 && meaningfulTerm(featureName) && meaningfulTerm(targetName)){
					FeatureEntry fe = new FeatureEntry(sourceid, crossScore);
					clean.add(fe);
				}
			}
		}
		
		Collections.sort(clean);
		Collections.reverse(clean);		
				
		return clean;
	}
	
	private void showPivots(List<FeatureEntry> p, int max){
		DecimalFormat format=new DecimalFormat("#.#######");
		JatecsLogger.status().println("#\t[term]\tscore\t[distSim:ps,pt]\n---------------------------------------------");
		for(int i = 0; i < p.size() && i < max; i++){
			FeatureEntry fe = p.get(i);
			int sourceid = fe.featureID;
			String featureName = unlabel_s.getFeatureDB().getFeatureName(sourceid);	
			String targetName = translateFeature(featureName);
			int targetid = unlabel_t.getFeatureDB().getFeature(targetName);
			boolean sameWord=featureName.equals(targetName);
			
			if(targetid!=-1){
				double featDist = crossDistorsionSim(sourceid, targetid);
				double hits_s = unlabel_s.getFeatProportion(sourceid);
				double hits_t = unlabel_t.getFeatProportion(targetid);
				JatecsLogger.status().println(i+"\t["+featureName+(sameWord?"":", " + targetName)+"]\t" + format.format(fe.score) + "\t["+format.format(featDist)+": "+format.format(hits_s)+"|"+format.format(hits_t)+"]");
			}
		}
	}

	private void unification(
			HashMap<Integer, DenseVector> sourceVectors,
			HashMap<Integer, DenseVector> targetVectors,
			List<Pivot> pivots, IDistributionalCorrespondenceFunction smodel, IDistributionalCorrespondenceFunction tmodel) {
		
		int n_pivots=_customizer._num_pivots;
		for(Pivot p : pivots){
			DenseVector sourceVec = sourceVectors.get(p.sourceFeatId);
			DenseVector targetVec = targetVectors.get(p.targetFeatId);
			if(sourceVec==null || targetVec==null)
				continue;
			
			DenseVector average = new DenseVector(n_pivots);
			average.add(sourceVec);
			average.add(targetVec);
			average.normalize();
			sourceVectors.put(p.sourceFeatId, average);
			targetVectors.put(p.targetFeatId, average);
		}
	}

	private List<Pivot> getCommonWords(List<Pivot> pivots) {
		HashSet<Integer> fromSourcePivot = new HashSet<Integer>(Pivot.fromSource(pivots));
		HashSet<Integer> fromTargetPivot = new HashSet<Integer>(Pivot.fromTarget(pivots));
		int added=0;
		
		ArrayList<Pivot> common = new ArrayList<Pivot>();
		
		int phi=_customizer._phi;
		IIntIterator featSourceIt = unlabel_s.getFeatureDB().getFeatures();		
		while(featSourceIt.hasNext()){
			int featSid=featSourceIt.next();
			String featname_s = unlabel_s.getFeatureDB().getFeatureName(featSid);
			int featTid = unlabel_t.getFeatureDB().getFeature(featname_s);
				
			if(featTid!=-1 && 
					!fromSourcePivot.contains(featSid) && 
					!fromTargetPivot.contains(featTid) &&
					meaningfulTerm(featname_s)){
				
				int source_freq = unlabel_s.getFeatureDocumentsCount(featSid);
				int target_freq = unlabel_t.getFeatureDocumentsCount(featTid);
				
				if(source_freq > phi && target_freq > phi){
					common.add(new Pivot(featSid, featTid));
					added++;
				}
			}
		}
		JatecsLogger.status().println("Common words found " + added + " from " + unlabel_s.getFeaturesCount()+"(S), " + unlabel_t.getFeaturesCount()+"(T)");
		
		return common;
	}

	private IIndex project(IIndex index,
			HashMap<Integer, DenseVector> featVecs, 
			IFeatureDB latentFeatures,
			List<Integer> pivotsIDs) {
		
		JatecsLogger.status().println("Projecting index into latent space");
		
		HashMap<Integer, DenseVector> documentVectors = getDocumentVectors(index, featVecs);
		
		int n_pivots=_customizer._num_pivots;
		TroveDomainDB domain = new TroveDomainDB(index.getCategoryDB(), latentFeatures);
		TroveContentDBBuilder latentContent = new TroveContentDBBuilder(index.getDocumentDB(), latentFeatures);
		TroveWeightingDBBuilder latentWeight = new TroveWeightingDBBuilder(latentContent.getContentDB());
		
		IIntIterator docit = index.getDocumentDB().getDocuments();
		while(docit.hasNext()){
			int docid = docit.next();			
			DenseVector docVec = documentVectors.get(docid);
			
			//rewrite semantic vectors of documents into index
			IIntIterator latentFeatsIt=latentFeatures.getFeatures();
			for(int i = 0; i < n_pivots; i++){
				int latentid =latentFeatsIt.next();
				latentContent.setDocumentFeatureFrequency(docid, latentid, 1);
				latentWeight.setDocumentFeatureWeight(docid, latentid, docVec.get(i));
			}	
		}
		
		GenericIndex projection = new GenericIndex(latentFeatures,
				index.getDocumentDB(),
				index.getCategoryDB(),
				domain,
				latentContent.getContentDB(),
				latentWeight.getWeightingDB(),
				index.getClassificationDB());
		
		return projection;
	}
	
	
	private HashMap<Integer, DenseVector> getDocumentVectors(IIndex index, HashMap<Integer, DenseVector> featVectors){
		JatecsLogger.status().println("\tGetting Document Vectors");
		int nD = index.getDocumentDB().getDocumentsCount();
		HashMap<Integer, DenseVector> docVecs = new HashMap<Integer, DenseVector>(nD);

		int n_pivots=_customizer._num_pivots;
		IIntIterator docit = index.getDocumentDB().getDocuments();
		while(docit.hasNext()){
			int docid=docit.next();
			
			DenseVector docVec = new DenseVector(n_pivots);			
			IIntIterator featit = index.getContentDB().getDocumentFeatures(docid);
			while(featit.hasNext()){
				int featid = featit.next();
				String featname = index.getFeatureDB().getFeatureName(featid);
				
				if(meaningfulTerm(featname) && featVectors.containsKey(featid)){
					DenseVector featVec = featVectors.get(featid);
					double weight = index.getWeightingDB().getDocumentFeatureWeight(docid, featid);
					docVec.add(DenseVector.times(featVec, weight));
				}
			}		
			docVec.normalize();
			
			docVecs.put(docid, docVec);
		}
		
		return docVecs;
	}

	private void createLatentFeatures(){
		TroveFeatureDBBuilder featDB = new TroveFeatureDBBuilder();
		int nF = _customizer._num_pivots;
		for(int i = 0; i < nF; i++){
			featDB.addFeature("f"+i);
		}
		
		lantentFeatures = featDB.getFeatureDB();
	}
	
	private HashMap<Integer, DenseVector> getFeatureProfilesThread(IDistributionalCorrespondenceFunction model, List<Integer> pivotlist) throws InterruptedException, ExecutionException {
		JatecsLogger.status().println("Getting Feature profiles");

		int nF = model.getFeaturesCount();
		HashMap<Integer, DenseVector> featProfiles = new HashMap<Integer, DenseVector>(nF);
		
		int progress = 0;
		int skippedFeats=0;
		IIntIterator allfeats = model.getFeatureDB().getFeatures();		
		JatecsLogger.status().println("Start!");		
		
		JatecsLogger.status().print("\tcomplete:");
		
		List<Future<WordProfile>> futureProfiles=new ArrayList<Future<WordProfile>>();
		
		while(allfeats.hasNext()){
			int featID = allfeats.next();
			
			int inDocs = model.getFeatureDocumentsCount(featID);
			if(inDocs>0){	
				RunnableDCFunction dcfunctionRun = new RunnableDCFunction(model, featID, pivotlist);
				futureProfiles.add((Future<WordProfile>) executorService.submit(dcfunctionRun));
			}
			else{
				skippedFeats++;
			}		
		}
		int totalSteps=(nF-skippedFeats);
		for (Future<WordProfile> futureProfile:futureProfiles){
			WordProfile profile = futureProfile.get();
			featProfiles.put(profile.featID, profile.embedding);
				
			if(progress++%(totalSteps/10)==0){
				JatecsLogger.status().print(" "+(progress*100/totalSteps)+"%");
			}			
		}
		JatecsLogger.status().println("\n\tSkippedFeats " + skippedFeats);
		
		return featProfiles;
	}
	
	
	private boolean meaningfulTerm(String featname){		
		if(!_customizer._cleanfeats)
			return true;
		
		boolean isMark = featname.matches("['\"\\(\\)?!¡¿\\.,;\\-0-9]+");
		if(isMark)
			return false;
		
		int length=featname.length();
		if(length < 3){
			boolean isOccidental = featname.matches("[a-z]{1,2}");
			if(isOccidental)
				return false;
		}
		
		return true;
	}

	private List<Pivot> createPivots(List<FeatureEntry> Vp, int max) {
		ArrayList<Pivot> pivots = new ArrayList<>();
		
		int phi=_customizer._phi;
		int discarded=0;
		int skippedCandidates=0;
		for(int i = 0; i < Vp.size() && pivots.size() < max; i++){
			int s_featID=Vp.get(i).featureID;
			String source_feat = train_s.getFeatureDB().getFeatureName(s_featID);
			int occurrences_s = unlabel_s.getFeatureDocumentsCount(s_featID);
			
			if(meaningfulTerm(source_feat) && occurrences_s > phi){	
				String target_feat = translateFeature(source_feat);
				int t_featID = unlabel_t.getFeatureDB().getFeature(target_feat);
				if(t_featID != -1){
					int occurrences_t = unlabel_t.getFeatureDocumentsCount(t_featID);
					
					if(meaningfulTerm(target_feat) && occurrences_t > phi){						
						Pivot candidate = new Pivot(s_featID, t_featID);
						pivots.add(candidate);
					}
				}
				else discarded++;
			}
			else
				discarded++;
		}
		
		JatecsLogger.status().println("#Pivots obtained = " + pivots.size());
		JatecsLogger.status().println("#Discarded pivots " + discarded);
		JatecsLogger.status().println("#Skipped candidates " + skippedCandidates);
		
		_customizer._num_pivots = pivots.size();
			
		return pivots;
	}
	
	private String translateFeature(String source_feat) {
		//cross_lingual
		if(_customizer._oracle!=null){
			if(_customizer._oracle.canTranslate(source_feat)){
				return _customizer._oracle.translate(source_feat).toLowerCase();
			}
			else{
				//no translation available
				return null;
			}
		}
			
		//monolingual
		return source_feat;
	}

	private List<FeatureEntry> sortFeaturesByInformativeness(int n) {
		IIndex clone = this.train_s.cloneIndex();
		
		RoundRobinTSR rrTSR = new RoundRobinTSR(_customizer._tsrFunction);
		rrTSR.setNumberOfBestFeatures(n);
		
		List<FeatureEntry> featSelectedList=new ArrayList<FeatureEntry>(rrTSR.selectBestFeatures(clone));
		Collections.sort(featSelectedList);
		Collections.reverse(featSelectedList);
		
		return featSelectedList;	
	}

	public IIndex getLatentTrainIndex() {
		JatecsLogger.status().println("Returning projected Train");
		return projectedTrain;
	}
	
	public IIndex projectSourceIndex(IIndex source_index){	
		IIndex projected=project(source_index, sourceVectors, lantentFeatures, Pivot.fromSource(pivots));		
		return projected;
	}
	
	public IIndex projectTargetIndex(IIndex target_index){	
		JatecsLogger.status().println("Returning projected Test");
		IIndex projected=project(target_index, targetVectors, lantentFeatures, Pivot.fromTarget(pivots));		
		return projected;
	}

	private double crossDistorsionSim(int s_featID, int t_featID){
		int phi=_customizer._phi;		
		if(unlabel_s.getFeatureDocumentsCount(s_featID) < phi || 
				unlabel_t.getFeatureDocumentsCount(t_featID) < phi)
			return 0;
		
		double freqS = unlabel_s.getFeatProportion(s_featID);
		double freqT = unlabel_t.getFeatProportion(t_featID);
		
		return Math.min(freqS, freqT) / Math.max(freqS, freqT);
	}
	
	public List<Pivot> getPivots() {
		return this.pivots;
	}
}

class RunnableDCFunction implements Callable<WordProfile>{
	IDistributionalCorrespondenceFunction model;
	int featID;
	List<Integer> pivotIDs;
	
	public RunnableDCFunction(IDistributionalCorrespondenceFunction model, int featID, List<Integer> pivotIDs){
		this.model=model;
		this.featID=featID;
		this.pivotIDs=pivotIDs;
	}

	@Override
	public WordProfile call() throws Exception {
		DenseVector embedding = model.distributionalCorrespondenceFunction(featID, pivotIDs);
		return new WordProfile(featID, embedding);
	}
}

class WordProfile{
	int featID;
	DenseVector embedding;
	public WordProfile(int featID, DenseVector embedding){
		this.featID=featID;
		this.embedding=embedding;
	}
}
