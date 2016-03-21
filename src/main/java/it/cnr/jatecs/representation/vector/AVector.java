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

public abstract class AVector implements IVector{
	
	public void normalize(){
		double norm = euclideanNorm();
		if(norm!=0 && norm!=1.0){
			this.times(1.0/norm);
		}
	}
	
	public double max(){
		double max = 0;
		if(this.size()>0){
			int dim = this.size();
			for(int i = 0; i < dim; i++){
				max = Math.max(max, get(i));
			}	
		}
		return max;
	}
	
	public abstract double dotProduct(IVector v);
	
	public double cosineSimilarity(IVector v) {
		double dotProduct = dotProduct(v);
	    double eucledianNorm = euclideanNorm() * v.euclideanNorm();
	    if(eucledianNorm != 0){
	    	return dotProduct / eucledianNorm;
	    }
	    else
	    	return 0.0;
	}
	
	public void print(){
		StringBuilder st = new StringBuilder();
		for(int i = 0; i < this.size(); i++){
			st.append(get(i)).append(i<this.size()-1?", ":"");
		}
		System.out.println(st.toString());
	}
	
	public void add(double s){
		for(int i = 0; i < size(); i++){
			add(i,s);
		}
	}
	
	public void add(int pos, double s){
		double old=get(pos);
		set(pos, old+s);
	}	
	
	@Override
	public abstract AVector clone();
	
	@Override
	public String toString(){
		StringBuilder st = new StringBuilder("(");
		
		int n=this.size(); 
		for(int i = 0; i < n; i++){
			st.append(this.get(i)+(i<n-1? ", " : ")"));			
		}
		
		return st.toString();
	}

	public double euclideanDistance(IVector v){
		return Math.sqrt(quadraticEuclideanDistance(v));
	}
	
	public double quadraticEuclideanDistance(IVector v){
		double dist = 0.0;
		int dim=v.size();
	    for(int i = 0; i < dim; i++){
			double diff = (this.get(i) - v.get(i));
			dist += (diff*diff);
		}
		return dist;
	}
	
	public void add(IVector other){
		for(int i = 0; i < this.size(); i++){
			this.add(i, other.get(i));
		}
	}
	
	public void add(IVector vect, double scalar) {
		int n=vect.size();
		if(n!=this.size())
			throw new NullPointerException("null pointer, dimensions must agree!");
		for(int i = 0; i < n; i++)
			this.add(i, vect.get(i)*scalar);
	}
	
	public void add(double[] vect, double scalar) {
		int n=vect.length;
		if(n!=this.size())
			throw new NullPointerException("null pointer, dimensions must agree!");
		for(int i = 0; i < n; i++)
			this.add(i, vect[i]*scalar);
	}
}
