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

package it.cnr.jatecs.indexing.tsr;

public class FeatureEntry implements Comparable<FeatureEntry>
{
	/**
	 * The feature ID.
	 */
	public int featureID;
	
	/**
	 * The feature TSR score for a category.
	 */
	public double score;
	
	public FeatureEntry(int featID, double score){
		this.featureID=featID;
		this.score=score;
	}
	public FeatureEntry(){
	}

	@Override
	public boolean equals(Object obj) {
		
		if (!(obj instanceof FeatureEntry))
			return false;
		FeatureEntry fe = (FeatureEntry) obj;
		
		return (compareTo(fe) == 0);
		
	}

	public int compareTo(FeatureEntry fe){
		int cmp=Double.compare(this.score, fe.score);
		if(cmp==0)
			cmp=Integer.compare(this.featureID, fe.featureID);
		return cmp;
	}
		
}
