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

/**
 * A basic vector operations manager.
 * */
public interface IVector {
	/**
	 * @returns the max value in the vector
	 * */
	public double max();
	
	/**
	 * @param pos the position being inspected
	 * @returns the vaue at a certain position
	 * */
	public double get(int pos);
	
	/**
	 * Adds a value to a given position of the vector
	 * @param pos the position in which the value is to be added
	 * @param val the value to add
	 * */
	public void set(int pos, double val);
	
	/**
	 * @returns the size of the vector, i.e., the number of dimensions
	 * */
	public int size();
	
	/**
	 * Multiplies the vector by a scalar s, so that each value vi of the vector 
	 * is modified as vi=vi*s
	 * @param s the scalar to which the vector is to be multiplied
	 * */
	public void times(double s);
	
	/**
	 * Adds a value s to all values vi of the vector, so that the each value is vi=vi+s
	 * @param s the value to be added
	 * */
	public void add(double s);
	
	/**
	 * Adds a value s to one specific value vi of the vector, so that the value in the 
	 * specified position ends up with vi=vi+s
	 * @param pos the position to be modified
	 * @param s the value to be added
	 * */
	public void add(int pos, double s);
	
	/**
	 * Normalizes a vector (l2-normalization)
	 * */
	public void normalize();
	
	/**
	 * @return a copy of the vector
	 * */
	public IVector clone();
	
	/**
	 * Computes the dot product between this vector and another one given as argument
	 * @param v a vector
	 * @return the dot product between this vector and v
	 * */
	public double dotProduct(IVector v);
	
	/**
	 * @return the Euclidean norm of this vector
	 * */
	public double euclideanNorm();
	
	/**
	 * Computes the cosine similarity between this vector and another one given as argument
	 * @param v a vector
	 * @return the cosine similarity between this vector and v
	 * */
	public double cosineSimilarity(IVector v);
	
	/**
	 * Computes the Euclidean distance between this vector and another one given as argument
	 * @param v a vector
	 * @return the Euclidean distance between this vector and v
	 * */
	public double euclideanDistance(IVector v);
	
	/**
	 * Computes the cuadratic Euclidean distance between this vector and another one given as argument
	 * @param v a vector
	 * @return the cuadratic Euclidean distance between this vector and v
	 * */
	public double quadraticEuclideanDistance(IVector v);
	
	/**
	 * Adds (element-wise) a given vector to this one 
	 * @param featVec a vector of the same dimension to this one
	 * */
	public void add(IVector featVec);
	
	/**
	 * Adds (element-wise) a given vector to this one, and multiplies (also element-wise) all
	 * values by a scalar
	 * 
	 * @param featVec a vector of the same dimension to this one
	 * @param scalar the scalar to which the elements are to be multiplied
	 * */

	public void add(IVector vect, double scalar);
}
