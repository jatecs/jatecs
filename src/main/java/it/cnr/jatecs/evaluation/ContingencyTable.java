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

package it.cnr.jatecs.evaluation;

import it.cnr.jatecs.utils.interfaces.INameable;
import it.cnr.jatecs.utils.interfaces.INamed;

public class ContingencyTable implements INamed, INameable {

    protected int _tp;
    protected int _tn;
    protected int _fp;
    protected int _fn;
    private String _name;

    public ContingencyTable(String name) {
        this();
        _name = name;
    }

    public ContingencyTable() {
        super();
        _tp = 0;
        _tn = 0;
        _fp = 0;
        _fn = 0;
        _name = "generic";
    }

    public void addTP() {
        ++_tp;
    }

    public void addTN() {
        ++_tn;
    }

    public void addFP() {
        ++_fp;
    }

    public void addFN() {
        ++_fn;
    }

    public int tp() {
        return _tp;
    }

    public int tn() {
        return _tn;
    }

    public int fp() {
        return _fp;
    }

    public int fn() {
        return _fn;
    }

    public void setFP(int c) {
        _fp = c;
    }

    public void setFN(int c) {
        _fn = c;
    }

    public void setTP(int c) {
        _tp = c;
    }

    public void setTN(int c) {
        _tn = c;
    }

    public void reset() {
        _tp = 0;
        _tn = 0;
        _fp = 0;
        _fn = 0;
    }

    public int total() {
        return _fn + _fp + _tn + _tp;
    }

    public double precision() {
        double den = _tp + _fp;
        if (den != 0)
            return _tp / den;
        else
            return 1.0;
    }

    public double recall() {
        double den = _tp + _fn;
        if (den != 0)
            return _tp / den;
        else
            return 1.0;
    }

    public double f(double beta) {
        double beta2 = beta * beta;
        double den = (beta2 + 1.0) * _tp + _fp + beta2 * _fn;
        if (den != 0)
            return (beta2 + 1.0) * _tp / den;
        else
            return 1.0;
    }

    public double f1Pos() {
        return f1();
    }

    public double f1Neg() {
        double den = (2.0 * _tn) + _fp + _fn;
        if (den != 0)
            return 2.0 * _tn / den;
        else
            return 1.0;
    }

    public double f1() {
        return f(1.0);
    }

    public double accuracy() {
        double den = _tp + _tn + _fp + _fn;
        if (den != 0)
            return (_tp + _tn) / den;
        else
            return 1.0;
    }

    public double error() {
        return 1.0 - accuracy();
    }

    public double pd() {
        double den = _tp + _tn + _fp + _fn;
        if (den == 0)
            return 0.0;
        return Math.abs((_fp - _fn) / den);
    }

    public double relativePd() {
        double den = _tp + _tn + _fp + _fn;
        if (den == 0)
            return 0;

        double pd = pd();
        double pos = (_tp + _fn) / den;

        if (pos == 0) {
            if (pd == 0)
                return 0;
            else
                return 1.0;
        } else
            return Math.min(pd / pos, 1.0);
    }

    /**
     * @return the kullback liebler divergence between the true coding and the
     * predicted one, -1 if kl is undefined.
     */
    public double kl() {
        try {
            double den = _tp + _tn + _fp + _fn;

            double predt = (_tp + _fp) / den;
            double truet = (_tp + _fn) / den;
            double predn = (_tn + _fn) / den;
            double truen = (_tn + _fp) / den;

            return truet * Math.log(truet / predt) / Math.log(2.0) + truen
                    * Math.log(truen / predn) / Math.log(2.0);
        } catch (Exception e) {
            return -1;
        }
    }

    public String getName() {
        return _name;
    }

    public void setName(String name) {
        _name = name;
    }

    public double sensitivity() {
        return recall();
    }

    public double tpr() {
        return recall();
    }

    public double specificity() {
        double den = tn() + fp();
        if (den != 0)
            return tn() / den;
        else
            return 0;
    }

    public double fpr() {
        return 1.0 - specificity();
    }

    public double roc() {
        return (specificity() + recall()) / 2;
    }

    public ContingencyTable cloneTable() {
        ContingencyTable ct = new ContingencyTable();
        ct._fn = _fn;
        ct._fp = _fp;
        ct._tn = _tn;
        ct._tp = _tp;
        ct._name = _name;
        return ct;
    }
}
