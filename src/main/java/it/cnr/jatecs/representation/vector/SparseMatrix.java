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

import java.util.HashMap;
import java.util.Set;

/**
 * Implements a sparse version of the class matrix, i.e., a matrix in which 
 * the default value 0 is not explicitly stored, as it is supposed to be placed
 * in most matrix entries. 
 * */
public class SparseMatrix implements IMatrix{
	private int _rows;
	private int _cols;
	private HashMap<XY,Double> m;
	
	public SparseMatrix(int rows, int cols){
		_rows=rows;
		_cols=cols;
		m=new HashMap<XY, Double>();
	}
	
	public int getRowsDimension(){
		return _rows;
	}
	
	public int getColumnDimensions(){
		return _cols;
	}
	
	public void set(int x, int y, double v){
		if(x>=_rows||y>=_cols)
			throw new NullPointerException("Out of range!");
		if(v!=0.0)
			m.put(new XY(x, y), v);
	}
	
	public double get(int x, int y){
		if(x>=_rows||y>=_cols)
			throw new NullPointerException("Out of range!");
		
		XY xy=new XY(x,y);
		if(m.containsKey(xy))
			return m.get(xy);
		else
			return 0;
	}

	public void clear() {
		this.m.clear();
	}
	
	public Set<XY> getNonZeroPositions(){
		return this.m.keySet();
	}
	
	public class XY{
		public int x;
		public int y;
		
		public XY(int x, int y){
			this.x=x;
			this.y=y;
		}
		
		public boolean equals(Object o) {
		      return ((XY)o).x == this.x && ((XY)o).y == this.y;
		}

		public int hashCode() { 
			return (""+x+"_"+y).hashCode(); 
		}	
	}
	
}


