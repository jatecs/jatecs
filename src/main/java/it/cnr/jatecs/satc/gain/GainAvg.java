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
import it.cnr.jatecs.satc.interfaces.IGain;


public class GainAvg implements IGain {

    private CTmeasure measure;

    public GainAvg(CTmeasure measure) {
        this.measure = measure;
    }

    public double FP(double tp, double tn, double fp, double fn) {
        double modifier = fp;
        if (fp > 0) {
            return (measure.get(tp, tn, fp - modifier, fn) - measure.get(tp, tn, fp, fn)) / modifier;
        } else {
            return 0;
        }
    }

    public double FN(double tp, double tn, double fp, double fn) {
        double modifier = fn;
        if (fn > 0) {
            return (measure.get(tp + modifier, tn, fp, fn - modifier) - measure.get(tp, tn, fp, fn)) / modifier;
        } else {
            return 0;
        }
    }

    public double FP(double[] table) {
        return FP(table[0], table[1], table[2], table[3]);
    }

    public double FN(double[] table) {
        return FN(table[0], table[1], table[2], table[3]);
    }

}
