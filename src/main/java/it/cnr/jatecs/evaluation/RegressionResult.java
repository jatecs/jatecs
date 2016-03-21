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

public class RegressionResult implements INamed, INameable {

    private String _name;
    private int[] _bins;

    public RegressionResult(String name, int categoriesCount) {
        _name = name;
        _bins = new int[categoriesCount];
        reset();
    }

    public void add(int distance) {
        ++_bins[distance];
    }

    public int get(int distance) {
        return _bins[distance];
    }

    public void set(int distance, int count) {
        _bins[distance] = count;
    }

    public void reset() {
        for (int i = 0; i < _bins.length; ++i)
            _bins[i] = 0;
    }

    public int binsCount() {
        return _bins.length;
    }

    public String getName() {
        return _name;
    }

    public void setName(String name) {
        _name = name;
    }

    public double meanAbsoluteDistace() {
        double sum = 0.0;
        int count = _bins[0];
        for (int i = 1; i < _bins.length; ++i) {
            sum += i * _bins[i];
            count += _bins[i];
        }
        return sum / count;
    }

    public double meanSquaredError() {
        double sum = 0.0;
        int count = _bins[0];
        for (int i = 1; i < _bins.length; ++i) {
            sum += i * i * _bins[i];
            count += _bins[i];
        }
        return sum / count;
    }
}
