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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.cnr.jatecs.satc.evaluation;

import it.cnr.jatecs.evaluation.ContingencyTableSet;

/**
 * @author giacomo
 */
public class ExpectedErrorReduction {

    ContingencyTableSet[] evaluations;
    double docsCount;
    ContingencyTableSet testEvaluation;

    public ExpectedErrorReduction(ContingencyTableSet testEvaluation, ContingencyTableSet[] evaluations) {
        this.testEvaluation = testEvaluation;
        this.evaluations = evaluations;
        this.docsCount = evaluations.length;
    }

    protected double get(double k, boolean normalized, boolean isMacro) {
        double sum = 0.0;
        double p = 1.0 - (1.0 / (k * docsCount));
        double firstEval = 1.0 - ((isMacro) ? testEvaluation.macroF1() : testEvaluation.getGlobalContingencyTable().f1());
        double eval;
        double err;
        double P;
        for (int i = 1; i <= docsCount; i++) {
            eval = 1.0 - ((isMacro) ? evaluations[i - 1].macroF1() : evaluations[i - 1].getGlobalContingencyTable().f1());
            err = (firstEval - eval) / firstEval;
            P = (i == docsCount) ? Math.pow(p, i - 1.0) : (Math.pow(p, i - 1.0) * (1.0 - p));
            if (normalized) {
                sum += (err - (i / docsCount)) * P;
                //System.out.println(P);
            } else {
                sum += err * P;
            }
        }
        //System.out.println();
        return sum;
    }

    protected double getUniform(boolean normalized, boolean isMacro) {
        double sum = 0.0;
        double firstEval = 1.0 - ((isMacro) ? testEvaluation.macroF1() : testEvaluation.getGlobalContingencyTable().f1());
        double eval;
        double err;
        for (int i = 1; i <= docsCount; i++) {
            eval = 1.0 - ((isMacro) ? evaluations[i - 1].macroF1() : evaluations[i - 1].getGlobalContingencyTable().f1());
            err = (firstEval - eval) / firstEval;
            if (normalized) {
                sum += (err - (i / docsCount));
            } else {
                sum += err;
            }
        }
        return sum;
    }

    public double getMacro(double k, boolean normalized) {
        return get(k, normalized, true);
    }

    public double getMacro(double k) {
        return getMacro(k, true);
    }

    public double getMicro(double k, boolean normalized) {
        return get(k, normalized, false);
    }

    public double getMicro(double k) {
        return getMicro(k, true);
    }

    public double getMacroUniform(boolean normalized) {
        return getUniform(normalized, true);
    }

    public double getMicroUniform(boolean normalized) {
        return getUniform(normalized, false);
    }

    public double getMacroUniform() {
        return getMacroUniform(true);
    }

    public double getMicroUniform() {
        return getMicroUniform(true);
    }

}
