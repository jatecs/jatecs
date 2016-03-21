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

public class GainRatio extends ATsrFunction {
	private final static double log_2 = Math.log(2);
	
	@Override
	protected double compute(TsrFunctionHelper c) {
		InformationGain ig=new InformationGain();
		double iGain = ig.compute(c);

		if (c.Pc() == 0 || c.PnotC()==0)
			return 0;
		
		double denominator = -((c.Pc() * (Math.log(c.Pc()) / log_2)) + (c.PnotC() * (Math.log(c.PnotC() ) / log_2)));
		
		return iGain / denominator;
	}

}
