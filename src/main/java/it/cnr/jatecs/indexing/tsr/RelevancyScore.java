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

public class RelevancyScore extends ATsrFunction {
	
	private final static double log_2 = Math.log(2);
	private final static double d = Double.MIN_VALUE;

    @Override
    protected double compute(TsrFunctionHelper c) {
        if (c.PtnotC() == 0 || c.Ptc() == 0 || c.PnotTc() == 0 || c.PnotTnotC() == 0 || c.Pc() == 0) {
            return 0;
        }

        double P_t_cond_c = c.Ptc() / c.Pc();
        double P_nott_cond_notc = c.PnotTnotC() / c.PnotC();       
        
        return Math.log((P_t_cond_c + d)/(P_nott_cond_notc + d)) / log_2;
    }

}
