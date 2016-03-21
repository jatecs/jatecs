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

public class ComponentWeightedInformationGain extends ATsrFunction {
	
	private static final double log_2 = Math.log(2);
	
	private double _tcWeight;
	private double _tncWeight;
	private double _ntcWeight;
	private double _ntncWeight;

	public ComponentWeightedInformationGain(double tcWeight, double tncWeight,
			double ntcWeight, double ntncWeight) {
		_tcWeight = tcWeight;
		_tncWeight = tncWeight;
		_ntcWeight = ntcWeight;
		_ntncWeight = ntncWeight;
	}
	
	@Override
	protected double compute(TsrFunctionHelper c) {		
		return ComponentWeightedInformationGain.compute(c,_tcWeight,_tncWeight,_ntcWeight,_ntncWeight);
	}
	
	public static double compute(TsrFunctionHelper c, double tcWeight, double tncWeight, double ntcWeight, double ntncWeight) {		
		if (c.Pc() == 0 || c.PnotC() == 0 || c.Pt() == 0 || c.PnotT() == 0)
			return 0;
		
		double factTC	= tcWeight * factor(c.Ptc(), c.Pt(), c.Pc());
		double factNTC	= ntcWeight * factor(c.PnotTc(), c.PnotT(), c.Pc());
		double factTNC	= tncWeight * factor(c.PtnotC(), c.Pt(), c.PnotC());
		double factNTNC	= ntncWeight * factor(c.PnotTnotC(), c.PnotT(), c.PnotC());
		
		double ig = factTC + factNTC + factTNC + factNTNC;
		
		return ig;
	} 
	
	private static double factor(double Ptc, double Pt, double Pc){
		if(Ptc>0 && Pt>0 && Pc>0)
			return Ptc*(Math.log( Ptc / (Pt * Pc) ) / log_2);
		
		return 0;
	}

}
