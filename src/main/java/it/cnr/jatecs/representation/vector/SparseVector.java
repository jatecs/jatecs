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

package it.cnr.jatecs.representation.vector;

import java.util.HashSet;
import java.util.Set;

import gnu.trove.TIntDoubleHashMap;

/**
 * Implements a sparse version of the class vector, i.e., a vector in which 
 * the default value 0 is not explicitly stored, as it is supposed to be placed
 * in most vector entries. 
 * */
public class SparseVector extends AVector {

	private static final double inv_sqrt2 = 1.0 / Math.sqrt(2.0);

	private TIntDoubleHashMap _dim_value;
	private int _k;

	public SparseVector(int k) {
		_dim_value = new TIntDoubleHashMap();
		_k = k;
	}
	
	public SparseVector(SparseVector other) {
		_dim_value = new TIntDoubleHashMap(other.size());
		int[] dims=other._dim_value.keys();
		for(int dim:dims)
			_dim_value.put(dim, other._dim_value.get(dim));
		_k = other._k;
	}

	// an unitary axe with one 1 in pos1
	public static SparseVector getUnitaryAxe(int k, int pos1) {
		SparseVector axe = new SparseVector(k);
		axe.set(pos1, 1.0);
		return axe;
	}

	// an unitary axe with two positive dimensions
	public static SparseVector getUnitaryAxe(int k, int pos1, int pos2) {
		SparseVector index = new SparseVector(k);
		// normalized to sum 1
		index.set(pos1, inv_sqrt2);
		index.set(pos2, inv_sqrt2);
		return index;
	}

	@Override
	public double get(int p) {
		if (_dim_value.containsKey(p))
			return _dim_value.get(p);
		return 0;
	}

	@Override
	public int size() {
		return _k;
	}

	@Override
	public void times(double s) {
		int[] dims = _dim_value.keys();
		for (int dim : dims) {
			double old = _dim_value.get(dim);
			_dim_value.put(dim, old * s);
		}
	}

	@Override
	public void set(int pos, double val) {		
		if(pos>_k)
			throw new NullPointerException("out of range!");
		if(val!=0)
			_dim_value.put(pos, val);
	}
	
	public int[] getNonZeroDimensions(){
		return _dim_value.keys();
	}
	
	
	public int getNonZeroDimensionsCount(){
		return _dim_value.keys().length;
	}
	
	@Override
	public double euclideanNorm(){
		double norm = 0;
		int[] nonZeroDims = getNonZeroDimensions();
		for(int i:nonZeroDims){
			double r = this.get(i);
			norm += r*r;
		}
		return Math.sqrt(norm);
	}
	
	@Override
	public SparseVector clone(){
		return new SparseVector(this);
	}
	
	@Override
	public double dotProduct(IVector v) {
		double dot=0.0;
		int[] nonZeros = getNonZeroDimensions();

		for(int nonZero : nonZeros){
			dot += get(nonZero)*v.get(nonZero);
		}
		return dot;
	}
	
	public double quadraticEuclideanDistance(IVector v){
		if(!(v instanceof SparseVector))
			return super.quadraticEuclideanDistance(v);
		
		Set<Integer> nonZeroDims = SparseVector.getNonZeroDimensions(this, (SparseVector)v);
		
		double dist = 0.0;
	    for(int dim:nonZeroDims){
			double diff = (this.get(dim) - v.get(dim));
			dist += (diff*diff);
		}
		return dist;
	}
	
	public void add(IVector other){
		if(other instanceof SparseVector){
			Set<Integer> nonZeroDims=SparseVector.getNonZeroDimensions(this, (SparseVector)other);
			for(int nonZeroDim:nonZeroDims){
				this.add(nonZeroDim, other.get(nonZeroDim));
			}
		}
		else super.add(other);
	}
	
	public static Set<Integer> getNonZeroDimensions(SparseVector v1, SparseVector v2){
		HashSet<Integer> dims=new HashSet<>(v1.getNonZeroDimensionsCount()+v2.getNonZeroDimensionsCount());
		for(int dim:v1.getNonZeroDimensions()) dims.add(dim);
		for(int dim:v2.getNonZeroDimensions()) dims.add(dim);
		return dims;
	}

	public static SparseVector concatenate(SparseVector v, SparseVector w) {
		int vsize=v.size();
		int wsize=w.size();
		SparseVector concat=new SparseVector(vsize+wsize);
		for(int v_i:v.getNonZeroDimensions()){
			concat.set(v_i, v.get(v_i));
		}
		for(int w_i:w.getNonZeroDimensions()){
			concat.set(vsize+w_i, w.get(w_i));
		}
		return concat;
	}

	public static SparseVector times(SparseVector v, SparseVector w) {
		if(v.size()!=w.size()) throw new NullPointerException("vector multiplication: dimensions must agree");
		
		SparseVector res=new SparseVector(v.size());
		for(int dim:getNonZeroDimensions(v, w))
			res.set(dim, v.get(dim)*w.get(dim));
		
		return res;
	}

	public void add(SparseVector v, double s) {
		for(int dim:v.getNonZeroDimensions())
			this.add(dim, v.get(dim)*s);
	}

	public String toStringSparse() {
		StringBuilder st=new StringBuilder();
		boolean first=true;
		for(int dim:this.getNonZeroDimensions()){
			if(!first)
				st.append(" ");
			st.append(dim).append(":").append(this.get(dim));
			first=false;
		}
		return st.toString();
	}

	public static SparseVector minus(SparseVector v1, SparseVector v2) {
		SparseVector dif=new SparseVector(v1.size());
		for(int dim:SparseVector.getNonZeroDimensions(v1, v2))
			dif.set(dim, v1.get(dim)-v2.get(dim));
		
		return dif;
	}

	public static SparseVector sum(SparseVector v1, SparseVector v2) {
		SparseVector dif=new SparseVector(v1.size());
		for(int dim:SparseVector.getNonZeroDimensions(v1, v2))
			dif.set(dim, v1.get(dim)+v2.get(dim));
		
		return dif;
	}


}
