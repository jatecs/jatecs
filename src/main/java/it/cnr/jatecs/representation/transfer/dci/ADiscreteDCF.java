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

import it.cnr.jatecs.indexes.DB.interfaces.IContentDB;
import it.cnr.jatecs.indexes.DB.interfaces.IFeatureDB;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;


import it.cnr.jatecs.indexes.utils.DocSet;
import it.cnr.jatecs.representation.vector.DenseVector;

import java.util.Hashtable;
import java.util.List;

public abstract class ADiscreteDCF implements IDistributionalCorrespondenceFunction{
	protected Hashtable<Integer, DocSet> _featDocSet;
	protected int _nD;
	protected int _nF;
	protected IFeatureDB _featuresDB;
	protected Hashtable<Integer, Hashtable<Integer, Double>> _cache;
	protected IContentDB _content;

	public ADiscreteDCF(IIndex index) {
		_nD = index.getDocumentDB().getDocumentsCount();
		_nF = index.getFeatureDB().getFeaturesCount();
		
		_content=index.getContentDB();
		_featuresDB=index.getFeatureDB();		
				
		_featDocSet = new Hashtable<Integer, DocSet>(_nF);
		_cache=new Hashtable<Integer, Hashtable<Integer, Double>>();
	}
	
	@Override
	public IFeatureDB getFeatureDB() {
		return _featuresDB;
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
		return _content.getFeatureDocumentsCount(featID);
	}

	@Override
	public double getFeatProportion(int featID) {
		int indocs = getFeatureDocumentsCount(featID);
		return indocs*1.0/_nD;
	}
	
	private boolean inCache(int feat1, int feat2){
		if(_cache.containsKey(feat1)){
			return _cache.get(feat1).containsKey(feat2);
		}
		return false;
	}
	
	private double getFromCache(int feat1, int feat2){
		return _cache.get(feat1).get(feat2);
	}
	
	protected void setInCache(int feat1, int feat2, double val){
		if(!_cache.containsKey(feat1))
			_cache.put(feat1, new Hashtable<Integer,Double>());
		_cache.get(feat1).put(feat2, val);
	}
	
	private DocSet getPivotDocSet(int pivotID){
		if(!_featDocSet.containsKey(pivotID))
			_featDocSet.put(pivotID, DocSet.genFeatDocset(_content, pivotID));
		return _featDocSet.get(pivotID); 
	}
	
	public DenseVector distributionalCorrespondenceFunction(int featID, List<Integer> pivotsID){
		DocSet featDist = null;
		DenseVector profile = new DenseVector(pivotsID.size());
		int profDim=0;
		for(int pivotID : pivotsID){
			double distCorrVal=0.0;
			if(inCache(featID, pivotID))
				distCorrVal=getFromCache(featID, pivotID);
			else{
				if(featDist==null){
					//if featID is a pivot
					if(_featDocSet.containsKey(featID))
						featDist=_featDocSet.get(featID);
					else
						featDist=DocSet.genFeatDocset(_content, featID);
				}
				DocSet pivotDist = getPivotDocSet(pivotID);
				distCorrVal=distributionalCorrespondence(featDist, pivotDist);
				setInCache(featID, pivotID, distCorrVal);
			}
			profile.set(profDim++, distCorrVal);
		}

		return profile;
	}
	
	protected abstract double distributionalCorrespondence(DocSet featDist, DocSet pivotDist);
}
