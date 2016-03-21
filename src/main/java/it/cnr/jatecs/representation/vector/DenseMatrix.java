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
 * Implements a dense version of the class matrix, i.e., a matrix where all
 * values are allocated in memory.
 * */
public class DenseMatrix implements IMatrix{

	private double[][] m;
	
	public DenseMatrix(int rows, int cols){
		m = new double[rows][cols];
		for(int i = 0; i < rows; i++)
			for(int j = 0; j < cols; j++)
				m[i][j]=0.0;
	}
	
	@Override
	public int getRowsDimension() {
		return m.length;
	}

	@Override
	public int getColumnDimensions() {
		if(m.length>0)
			return m[0].length;
		return 0;
	}

	@Override
	public void set(int x, int y, double v) {
		m[x][y]=v;
	}

	@Override
	public double get(int x, int y) {
		return m[x][y];
	}

}
