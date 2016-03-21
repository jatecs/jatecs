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
 * Basic implementation of matrices
 * */
public interface IMatrix {
	
	/**
	 * Establishes two types of matrices, dense and sparse
	 * */
	public static enum MATRIX_MODE {DENSE_MATRIX, SPARSE_MATRIX};
	
	/**
	 * @return the number of rows of the matrix
	 * */
	public int getRowsDimension();
	
	/**
	 * @return the number of columns of the matrix
	 * */
	public int getColumnDimensions();
	
	/**
	 * Adds a given value to a position of the matrix especified by its
	 * row and column positions.
	 * @param x matrix row
	 * @param y matrix column
	 * @param v value to be added
	 * */
	public void set(int x, int y, double v);
	
	/**
	 * @param x matrix row
	 * @param y matrix column
	 * @return the value at row x and column y
	 * */
	public double get(int x, int y);
}
