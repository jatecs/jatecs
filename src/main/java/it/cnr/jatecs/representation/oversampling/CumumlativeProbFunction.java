package it.cnr.jatecs.representation.oversampling;

import it.cnr.jatecs.indexing.tsr.FeatureEntry;
import it.cnr.jatecs.representation.vector.DenseVector;
import it.cnr.jatecs.representation.vector.SparseVector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class CumumlativeProbFunction {
	
	private DenseVector probMass;
	private double[] probAccum;
	private static Random rand=new Random();

	public static CumumlativeProbFunction fromDiscreteMassFunction(DenseVector featProbVec) {
		int events=featProbVec.size();
		CumumlativeProbFunction P=new CumumlativeProbFunction();
		P.probMass=featProbVec;
		P.probAccum=new double[events];
		double accumProb=0;
		for(int i = 0; i < events; i++){
			accumProb+=featProbVec.get(i);
			P.probAccum[i]=accumProb;
		}	
		for(int i = 0; i < events; i++){			
			P.probAccum[i]/=accumProb;
		}
		return P;
	}
	
	private int events(){
		return probAccum.length;
	}
	
	public DenseVector getProbabilityMass(){
		return this.probMass;
	}
	
	//returns the most likely vector with n non-zeros
	public SparseVector mostLikelyRep(int n){
		List<FeatureEntry> dim_prob=new ArrayList<FeatureEntry>();
		dim_prob.add(new FeatureEntry(0,probAccum[0]));
		for(int i = 1; i < probAccum.length; i++){
			double prob_i=probAccum[i]-probAccum[i-1];
			dim_prob.add(new FeatureEntry(i, prob_i));
		}
		Collections.sort(dim_prob);
		
		SparseVector most=new SparseVector(this.events());
		int added=0;
		while(added < n){
			int best_dim=dim_prob.remove(dim_prob.size()-1).featureID;
			most.set(best_dim, 1);
			added++;
		}
		
		return most;
	}
	
	//returns a vector with n non zero values distributed according to this cumulative probability function
	public SparseVector randomTrial(int n){
		SparseVector dotdoc=new SparseVector(events());
		for(int i = 0; i < n; i++){			
			int selFeat=randomTrial();			
			dotdoc.add(selFeat, 1);
		}
		
		return dotdoc;
	}
	
	//returns a random position according to the cumulative probability function
	public int randomTrial(){
		double trial=rand.nextDouble();
		return sortSearch(this, trial, 0, events());
	}
	
	//returns the first feat with accum probability greater than trial in the sublist probVector.sub(low, high) [inclusive, exclusive]
	private static int sortSearch(CumumlativeProbFunction probVector, double trial, int low, int high){
		if(high-low <=20){
			int selFeat=low;
			while(probVector.probAccum[selFeat] < trial) selFeat++;
			return selFeat;
		}
		else{
			int midpoint=low+(high-low)/2;
			double p_mid=probVector.probAccum[midpoint];
			
			//stop condition
			if(p_mid>=trial && (midpoint==0 || probVector.probAccum[midpoint-1]<trial))
				return midpoint;
			
			if(p_mid>=trial)
				high=midpoint;
			else
				low=midpoint;
			return sortSearch(probVector, trial, low, high);
		}	
	}
}
