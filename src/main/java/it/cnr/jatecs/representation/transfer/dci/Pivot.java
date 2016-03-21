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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A pivot is assumed to be a pair of features (as indicated by their ids) whose
 * role is similar in two or more domains. For example a word "excellent" might be a
 * good pivot for sentiment analysis in a book-review and a music-review domains, while
 * "predictable" would not be a good pivot for a book-review and an electronic-review
 * domains (as it has a negative connotation for a book, while it has a good connotation
 * when referring to an electronic device). 
 * */
public class Pivot implements Comparable<Pivot>{
	static DecimalFormat df = new DecimalFormat("##.##");
	public int sourceFeatId;
	public int targetFeatId;
	
	public Pivot(int s_featID, int t_featID) {
		sourceFeatId=s_featID;
		targetFeatId=t_featID;
	}
	
	public static List<Integer> fromSource(Collection<Pivot> pivots) {
		return from(pivots, true);
	}
	
	public static List<Integer> fromTarget(Collection<Pivot> pivots) {
		return from(pivots, false);
	}
	
	private static List<Integer> from(Collection<Pivot> pivots, boolean source) {
		ArrayList<Integer> from = new ArrayList<Integer>();
		for(Pivot p:pivots){
			from.add(source ? p.sourceFeatId : p.targetFeatId);
		}
		return from;
	}
	
	public String toString(IFeatureDB feats_s, IFeatureDB feats_t){
		String featsSname = feats_s.getFeatureName(this.sourceFeatId);
		String featsTname = feats_t.getFeatureName(this.targetFeatId);
		return "["+featsSname+"-"+featsTname+"]";
	}
	
	@Override
	public int compareTo(Pivot p) {
		int cmp = Integer.compare(this.sourceFeatId, p.sourceFeatId);
		if(cmp==0)
			cmp = Integer.compare(this.targetFeatId, p.targetFeatId);
		
		return cmp;
	}
}