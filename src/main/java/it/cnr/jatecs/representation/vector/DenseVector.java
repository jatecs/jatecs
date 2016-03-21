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

import java.util.Arrays;

/**
 * Implements a dense version of the class vector, i.e., a vector where all
 * values are allocated in memory.
 * */
public class DenseVector extends AVector{
	private double[] rand;
	
	public DenseVector(int k) {
		rand = new double[k];
		Arrays.fill(rand, 0);
	}
	
	public DenseVector(DenseVector other) {
		rand = Arrays.copyOf(other.rand, other.size());	
	}
	
	public double get(int p) {
		return rand[p];
	}
	
	public int size(){
		return this.rand.length;
	}
	
	public void times(double s){
		for(int i = 0; i < size(); i++)
			rand[i]=rand[i]*s;
	}
	

	@Override
	public void set(int pos, double val) {
		rand[pos]=val;		
	}
	
	@Override
	public DenseVector clone(){
		return new DenseVector(this);
	}

	
	public static DenseVector times(DenseVector vec, double w) {

		DenseVector res = new DenseVector(vec.size());
		for(int i = 0; i < vec.size(); i++){
			res.set(i, vec.get(i)*w);
		}
		return res;
		
	}

	public void add(DenseVector vec) {
		for(int i = 0; i < this.rand.length; i++){
			this.rand[i]+=vec.rand[i];
		}
	}
	
	public static DenseVector minus(DenseVector a, DenseVector b) {
		DenseVector dif = new DenseVector(a.size());
		for(int i = 0; i < dif.size(); i++){
			dif.set(i, a.get(i)-b.get(i));
		}
		return dif;
	}
	
	public static DenseVector plus(DenseVector a, DenseVector b) {
		DenseVector dif = new DenseVector(a.size());
		for(int i = 0; i < dif.size(); i++){
			dif.set(i, a.get(i)+b.get(i));
		}
		return dif;
	}

	
	@Override
	public double dotProduct(IVector v) {
		double dot=0.0;
		int size=this.rand.length;
		for(int i = 0; i < size; i++){
			dot += (rand[i]*v.get(i));
		}
		return dot;
	}
	
	@Override
	public double euclideanNorm(){
		double norm = 0;
		int dim = this.size();
		for(int i = 0; i < dim; i++){
			double r = this.rand[i];
			norm += r*r;
		}
		return Math.sqrt(norm);
	}
	
	public void add(SparseVector vect, double scalar) {
		int n=vect.size();
		if(n!=this.size())
			throw new NullPointerException("null pointer, dimensions must agree!");
		for(int i : vect.getNonZeroDimensions())
			this.rand[i]+=(vect.get(i)*scalar);
	}

}
