package it.cnr.jatecs.representation.oversampling;

import it.cnr.jatecs.representation.vector.SparseVector;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

public class RandomIndexDictionary {
	private static Random rand=new Random();
	private HashMap<Integer, SparseVector> elementID_index;
	private int dimension;
	private int nonZeros;
	private boolean onlyPositiveDimensions=false;
	
	public RandomIndexDictionary(IIntIterator elementIDs, int dim, int nonZeros, boolean onlyPositive){
		this.dimension=dim;
		this.nonZeros=nonZeros;
		this.elementID_index=new HashMap<>();
		this.onlyPositiveDimensions=onlyPositive;
		
		//make sure each dimension is used at least once
		int firstDimension=0;
		
		elementIDs.begin();		
		while(elementIDs.hasNext()){
			int elID=elementIDs.next();
			
			HashSet<Integer> nonZeroDimensions=new HashSet<Integer>();
			nonZeroDimensions.add(firstDimension++%dim);
			while(nonZeroDimensions.size() < nonZeros)
				nonZeroDimensions.add(rand.nextInt(dim));

			SparseVector svec=new SparseVector(dim);
			for(int selDim:nonZeroDimensions)
				svec.set(selDim, decideNonZeroValue());
			
			svec.normalize();
			
			this.put(elID, svec);
		}
	}
	
	private int decideNonZeroValue(){
		if(onlyPositiveDimensions)
			return 1;
		else
			return rand.nextDouble() < 0.5 ? +1 : -1;			
	}
	
	public void put(int id, SparseVector index){
		elementID_index.put(id, index);
	}
	
	public SparseVector get(int id){
		return elementID_index.get(id);
	}
	
	public int getDimensions(){
		return this.dimension;
	}
	
	public int nonZeros(){
		return this.nonZeros;
	}
	
	public void setOnlyPositiveValues(){
		onlyPositiveDimensions=true;
	}
	
	public void setPositiveAndNegativeValues(){
		onlyPositiveDimensions=false;
	}
}
