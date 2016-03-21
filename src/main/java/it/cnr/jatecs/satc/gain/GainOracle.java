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

package it.cnr.jatecs.satc.gain;

import it.cnr.jatecs.evaluation.measures.CTmeasure;

public class GainOracle extends Gain {

    public GainOracle(CTmeasure measure) {
        super(measure);
        // TODO Auto-generated constructor stub
    }

    public double FP(double[] table) {
        if (table[0] <= 0.0 && table[2] > 0.0) {
            return super.FP(table[0] + 1.0, table[1] + 1.0, table[2] + 1.0, table[3] + 1.0);
        } else {
            return super.FP(table[0], table[1], table[2], table[3]);
        }
    }

    public double FN(double[] table) {
        if (table[0] <= 0.0 && table[3] > 0.0) {
            return super.FN(table[0] + 1.0, table[1] + 1.0, table[2] + 1.0, table[3] + 1.0);
        } else {
            return super.FN(table[0], table[1], table[2], table[3]);
        }
    }

}

