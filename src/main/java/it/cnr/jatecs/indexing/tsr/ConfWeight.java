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

import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.distribution.TDistribution;



/** From article:
 * Soucy, P., & Mineau, G. W. (2005, July). Beyond TFIDF weighting for 
 * text categorization in the vector space model. 
 * In IJCAI (Vol. 5, pp. 1130-1135).
 * 
 * This article proposes a Supervised Weighting function that is later combined with
 * a global policy following: (max(compute(c_i, f_j, I)))^2 
 * */
public class ConfWeight extends ATsrFunction{
	
	private final static double log2 = Math.log(2.0);
	
	@Override
	protected double compute(TsrFunctionHelper c) {
		int C = c.C();
		int notC = c.total()-C;
		int tp = c.TP();
		int fp = c.FP();
		
		ConfidenceInterval p_pos = p(tp, C);
		ConfidenceInterval p_neg = p(fp, notC);
		
		double MinPos = p_pos.range_min;
		double MaxNeg = p_neg.range_max;
		
		double MinPosRelFreq = (MinPos / (MinPos+MaxNeg));
		
		double str_tplus = strength(MinPosRelFreq, MinPos, MaxNeg);		
		
		return str_tplus;
	}
	
	private double strength(double minPosRelFreq, double minPos, double maxNeg) {
		if(minPos > maxNeg){
			return Math.log(2*minPosRelFreq)/log2;
		}
		else{
			return 0.0;
		}
	}

	//Wilson proportion estimate
	private ConfidenceInterval p(int xt, int n){
		//this Z value is the result of phi.inverseCumulativeProbability(0.5+0.95/2.0);
		//on a normally distributed prob. mass function N(0,1)
		double Z2=3.84;
		RealDistribution phi = null;
		if(n<30){
			//the # degrees of freedom of a t-student is n-1
			//in the article is not specified what happens if n=1
			//I will here assume it becomes 1
			int degrees_freedom=Math.max(n-1, 1);
			phi=new TDistribution(degrees_freedom);
			
			//Z for confindence interval of 0.95
			double Z = phi.inverseCumulativeProbability(0.5+0.95/2.0);
			Z2 = Z*Z;
		}
		
		double p = (xt + 0.5*Z2)/(n + Z2);
		
		double amplitude = 0.5*Z2*Math.sqrt((p*(1.0-p))/(n+Z2));
		
		ConfidenceInterval interval = new ConfidenceInterval();
		interval.center=p;
		interval.amplitude=amplitude;
		interval.range_min=p-amplitude;
		interval.range_max=p+amplitude;
		
		return interval;
	}
	
	class ConfidenceInterval{
		double center;
		double amplitude;
		double range_min;
		double range_max;
	}

	
}
