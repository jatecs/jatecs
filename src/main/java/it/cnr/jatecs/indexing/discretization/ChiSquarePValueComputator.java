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

package it.cnr.jatecs.indexing.discretization;

/**
 * Based on code found on
 * http://www.codeproject.com/Articles/432194/How-to-Calculate
 * -the-Chi-Squared-P-Value
 *
 * @author Tiziano Fagni
 */
public class ChiSquarePValueComputator {

    /**
     * Compute the P-value that is the probability that the difference between the Observed and the Expected values
     * would happen purely by chance.
     *
     * @param Dof The degree of freedom.
     * @param Cv  The critical value.
     * @return The corresponding chi square table value.
     */
    public static double computePValue(int Dof, double Cv) {
        if (Cv < 0 || Dof < 1) {
            return 0.0;
        }
        double K = ((double) Dof) * 0.5;
        double X = Cv * 0.5;
        if (Dof == 2) {
            return Math.exp(-1.0 * X);
        }

        double PValue = igf(K, X);
        if (Double.isNaN(PValue) || Double.isInfinite(PValue) || PValue <= 1e-8) {
            return 1e-14;
        }

        PValue /= approx_gamma(K);
        // PValue /= tgamma(K);

        return (1.0 - PValue);
    }

    private static double igf(double S, double Z) {
        if (Z < 0.0) {
            return 0.0;
        }
        double Sc = (1.0 / S);
        Sc *= Math.pow(Z, S);
        Sc *= Math.exp(-Z);

        double Sum = 1.0;
        double Nom = 1.0;
        double Denom = 1.0;

        for (int I = 0; I < 200; I++) {
            Nom *= Z;
            S++;
            Denom *= S;
            Sum += (Nom / Denom);
        }

        return Sum * Sc;
    }


    private static double approx_gamma(double Z) {
        double RECIP_E = 0.36787944117144232159552377016147;  // RECIP_E = (E^-1) = (1.0 / E)
        double TWOPI = 6.283185307179586476925286766559;  // TWOPI = 2.0 * PI

        double D = 1.0 / (10.0 * Z);
        D = 1.0 / ((12 * Z) - D);
        D = (D + Z) * RECIP_E;
        D = Math.pow(D, Z);
        D *= Math.sqrt(TWOPI / Z);

        return D;
    }


    public static void main(String[] args) {
        double pvalue = computePValue(5, 11.70);
        System.out.println("PValue = " + pvalue);
    }
}
