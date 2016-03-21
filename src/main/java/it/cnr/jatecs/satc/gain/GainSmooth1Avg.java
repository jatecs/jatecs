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


public class GainSmooth1Avg extends GainAvg {

    public GainSmooth1Avg(CTmeasure measure) {
        super(measure);
        // TODO Auto-generated constructor stub
    }

    public double FP(double[] table) {
        smooth(table);
        return super.FP(table[0], table[1], table[2], table[3]);
    }

    public double FN(double[] table) {
        smooth(table);
        return super.FN(table[0], table[1], table[2], table[3]);
    }

    protected void smooth(double[] table) {
        double modifier = 0.0;
        for (int i = 0; i < table.length; i++) {
            if (table[i] < 1.0) {
                double tempMod = Math.ceil(1.0 - table[i]);
                if (tempMod > modifier) {
                    modifier = tempMod;
                }
            }
        }
        for (int i = 0; i < table.length; i++) {
            table[i] += modifier;
        }
    }

//	public double FP(double tp, double tn, double fp, double fn) {
//        tp = tp * scale;
//        tn = tn * scale;
//        fp = fp * scale;
//        fn = fn * scale;
//        double setSize = tp + tn + fp + fn;
//        double total = setSize + 4.0;
//        if (fp < 1.0 || tp < 1.0 || tn < 1.0 || fn < 1.0) {
//            return super.FP(setSize * ((tp + 1.0) / total), setSize * ((tn + 1.0) / total), setSize * ((fp + 1.0) / total), setSize * ((fn + 1.0) / total));
//        } else {
//            return super.FP(tp, tn, fp, fn);
//        }
//    }
//
//    public double FN(double tp, double tn, double fp, double fn) {
//        tp = tp * scale;
//        tn = tn * scale;
//        fp = fp * scale;
//        fn = fn * scale;
//        double setSize = tp + tn + fp + fn;
//        double total = setSize + 4.0;
//        if (fp < 1.0 || tp < 1.0 || tn < 1.0 || fn < 1.0) {
//            return super.FN(setSize * ((tp + 1.0) / total), setSize * ((tn + 1.0) / total), setSize * ((fp + 1.0) / total), setSize * ((fn + 1.0) / total));
//        } else {
//            return super.FN(tp, tn, fp, fn);
//        }
//    }

}

