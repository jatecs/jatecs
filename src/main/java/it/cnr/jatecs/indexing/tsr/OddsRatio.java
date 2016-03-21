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

public class OddsRatio  extends ATsrFunction {

	@Override
	protected double compute(TsrFunctionHelper c) {
        if (c.Pc() == 0 || c.PnotC() == 0) 
            return 0;        

        double P_t_cond_c = c.Ptc() / c.Pc();
        double P_t_cond_notc = c.PtnotC()/ c.PnotC();
        
        double num = P_t_cond_c * (1-P_t_cond_notc);
        double den = (1 - P_t_cond_c) * P_t_cond_notc;
        
        if (den!=0) 
        	return num / den;
        
        return 0;
    }
	
}