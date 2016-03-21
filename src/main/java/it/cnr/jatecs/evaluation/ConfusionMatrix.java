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

public class ConfusionMatrix {
    private int[][] _matrix;

    public ConfusionMatrix(int numCategories) {
        _matrix = new int[numCategories][numCategories];
    }

    public int getError(short trueCategory, short falseCategory) {
        return _matrix[trueCategory][falseCategory];
    }


    public void setError(short trueCategory, short falseCategory, int numErrors) {
        _matrix[trueCategory][falseCategory] = numErrors;
    }


    public int getCorrects(short category) {
        return _matrix[category][category];
    }

    public int getNumCategories() {
        return _matrix.length;
    }


    public double getAccuracy() {
        int numCorrects = 0;
        int numClassifications = 0;

        for (int i = 0; i < _matrix.length; i++) {
            for (int j = 0; j < _matrix[0].length; j++) {
                if (i == j)
                    numCorrects += _matrix[i][i];

                numClassifications += _matrix[i][j];
            }
        }

        if (numClassifications == 0)
            return 0;

        double accuracy = ((double) numCorrects) / ((double) numClassifications);
        return accuracy;
    }


    public double getAccuracy(short catID) {
        int numCorrects = 0;
        int numClassifications = 0;

        for (int j = 0; j < _matrix[catID].length; j++) {
            if (catID == j)
                numCorrects += _matrix[catID][j];

            numClassifications += _matrix[catID][j];
        }

        if (numClassifications == 0)
            return 0;

        double accuracy = ((double) numCorrects) / ((double) numClassifications);
        return accuracy;
    }
}
