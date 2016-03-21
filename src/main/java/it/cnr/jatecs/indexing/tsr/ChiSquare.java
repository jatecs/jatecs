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

public class ChiSquare extends ATsrFunction {

	@Override
	protected double compute(TsrFunctionHelper c) {
		int numDocuments = c.total();		
		
		assert(c.Pt() != 0);
		assert(c.Pc() != 0);
		assert(c.PnotT() != 0);
		assert(c.PnotC() != 0);
		
		double numerator = (c.Ptc() * c.PnotTnotC()) - (c.PtnotC() * c.PnotTc());
		numerator = numerator * numerator * numDocuments; 
		
		double denominator = c.Pt() * c.PnotT() * c.Pc() * c.PnotC();		
		assert(denominator != 0);
		
		return numerator / denominator;	
	}
	

}
