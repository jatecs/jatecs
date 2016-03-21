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


import it.cnr.jatecs.indexes.DB.interfaces.IFeatureDB;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.representation.vector.DenseVector;
import it.cnr.jatecs.representation.vector.IVector;
import it.cnr.jatecs.representation.vector.SparseVector;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;

import java.util.Hashtable;
import java.util.List;

public abstract class AVectorDCF implements IDistributionalCorrespondenceFunction{
	
	protected Hashtable<Integer, IVector> _featVecSet;
	protected int _nD;
	protected int _nF;
	protected Hashtable<Integer, Hashtable<Integer, Double>> _cache;
	private boolean _normalize;
	protected IIndex _index;
	private Hashtable<Integer,Integer> _docIDdimMap;
	private boolean _useWeight=true;
	private boolean _center=false;
	
	public AVectorDCF(IIndex index, boolean normalize, boolean center){
		_nD = index.getDocumentDB().getDocumentsCount();
		_nF = index.getFeatureDB().getFeaturesCount();
		_index = index;
		_normalize = normalize;
		_featVecSet = new Hashtable<Integer, IVector>();
		_cache=new Hashtable<Integer, Hashtable<Integer, Double>>();	
		_docIDdimMap=new Hashtable<>();
		_center=center;
		IIntIterator docsit = index.getDocumentDB().getDocuments();
		int dimpos=0;
		while(docsit.hasNext()){
			int docid = docsit.next();
			_docIDdimMap.put(docid, dimpos++);
		}
	}

	@Override
	public IFeatureDB getFeatureDB() {
		return _index.getFeatureDB();
	}

	@Override
	public int getFeaturesCount() {
		return _nF;
	}

	@Override
	public int getDocumentsCount() {
		return _nD;
	}

	@Override
	public int getFeatureDocumentsCount(int featID) {
		return _index.getContentDB().getFeatureDocumentsCount(featID);
	}

	@Override
	public double getFeatProportion(int featID) {
		int count  = getFeatureDocumentsCount(featID);
		return count * 1.0 / _nD;
	}
	
	protected boolean inCache(int feat1, int feat2){
		if(_cache.containsKey(feat1)){
			return _cache.get(feat1).containsKey(feat2);
		}
		return false;
	}
	
	protected double getFromCache(int feat1, int feat2){
		return _cache.get(feat1).get(feat2);
	}
	
	protected void setInCache(int feat1, int feat2, double val){
		if(!_cache.containsKey(feat1))
			_cache.put(feat1, new Hashtable<Integer,Double>());
		_cache.get(feat1).put(feat2, val);
	}
	
	protected IVector computeDistVector(int featID){
		double load = getFeatProportion(featID)*1.0/_nD;
		IVector featVec = load < 0.5 ? new SparseVector(_nD) : new DenseVector(_nD); 

		IIntIterator docit = _index.getContentDB().getFeatureDocuments(featID);
		
		while(docit.hasNext()){
			int docID = docit.next();
			double weight = _index.getWeightingDB().getDocumentFeatureWeight(docID, featID);
			int count = _index.getContentDB().getDocumentFeatureFrequency(docID, featID);
			if(count!=0){
				int dimpos = _docIDdimMap.get(docID);
				featVec.set(dimpos, _useWeight? weight : 1);
			}
		}
		
		if(_normalize){
			featVec.normalize();
		}
		return featVec;
	}
	
	protected IVector getPivotVector(int pivotID) {
		if(!_featVecSet.containsKey(pivotID)){
			_featVecSet.put(pivotID, computeDistVector(pivotID));
		}
		return _featVecSet.get(pivotID);
	}
	
	protected IVector getFeatVector(int featID){
		//if featID is a pivot
		if(_featVecSet.containsKey(featID))
			return _featVecSet.get(featID);
		else
			return computeDistVector(featID);
	}
	
	public DenseVector distributionalCorrespondenceFunction(int featID, List<Integer> pivotsID){
		IVector featDist = getFeatVector(featID);
		DenseVector profile = new DenseVector(pivotsID.size());
		int profDim=0;
		for(int pivotID : pivotsID){
			double distCorrVal=0.0;
			if(inCache(featID, pivotID))
				distCorrVal=getFromCache(featID, pivotID);
			else{
				IVector pivotDist = getPivotVector(pivotID);
				distCorrVal=distributionalCorrespondence(featDist, pivotDist);
				if(_center)
					distCorrVal-=distributionalRandomCorrespondence(featID, pivotID);
				setInCache(featID, pivotID, distCorrVal);
			}
			profile.set(profDim++, distCorrVal);
		}

		return profile;
	}
	
	protected void useWeights(boolean use){
		this._useWeight=use;
	}

	protected abstract double distributionalCorrespondence(IVector featDist, IVector pivotDist);
	protected abstract double distributionalRandomCorrespondence(int feat1, int feat2);

}
