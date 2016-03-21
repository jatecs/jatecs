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

/**
 * From:
 * Quan, X., Wenyin, L., & Qiu, B. (2011). Term weighting schemes for question 
 * categorization. Pattern Analysis and Machine Intelligence, IEEE Transactions 
 * on, 33(5), 1009-1021.
 * */
public class VRF extends ATsrFunction{

	@Override
	protected double compute(TsrFunctionHelper c) {		
		//ATTENTION, there is a mismatch between the original reference notation and ours.
		//they have interchanged fp and fn
		double num = Math.log(c.TP() + 1.0);
		double den = Math.log(c.FP() + 1.0);
		
		double vrf = 0;
		
		if(den!=0)
			vrf = num/den;
		
		return vrf;
	}

}
