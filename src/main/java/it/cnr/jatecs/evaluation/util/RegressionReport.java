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

package it.cnr.jatecs.evaluation.util;

import it.cnr.jatecs.evaluation.RegressionResult;
import it.cnr.jatecs.evaluation.RegressionResultSet;
import it.cnr.jatecs.utils.Os;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

public class RegressionReport {

    public static String printReport(RegressionResultSet tableSet) {

        StringBuilder stringBuilder = new StringBuilder();
        StringBuilder stringBuilder2 = new StringBuilder();
        double macromad = 0.0;
        double macromse = 0.0;

        String newLine = Os.newline();
        stringBuilder.append("Report from: " + tableSet.getName() + newLine);
        stringBuilder.append(newLine);
        stringBuilder.append("Number of categories: " + tableSet.getEvaluatedCategoriesCount() + newLine);
        stringBuilder.append(newLine);
        RegressionResult table = tableSet.getGlobalRegressionResult();
        stringBuilder.append("Global results (distance,count):" + newLine);
        for (int i = 0; i < table.binsCount(); ++i) {
            stringBuilder.append("( " + i + " , " + table.get(i) + " ) ");
        }
        stringBuilder.append(newLine);
        stringBuilder.append("(micro-average evaluation) MAD = " + table.meanAbsoluteDistace() + " MSE = " + table.meanSquaredError());
        stringBuilder.append(newLine);
        IShortIterator catIt = tableSet.getEvaluatedCategories();
        while (catIt.hasNext()) {
            short category = catIt.next();
            table = tableSet.getRegressionResult(category);
            stringBuilder2.append("Results for category "
                    + table.getName() + newLine);
            for (int i = 0; i < table.binsCount(); ++i) {
                stringBuilder2.append("( " + i + " , " + table.get(i) + " ) ");
            }
            stringBuilder2.append(newLine);
            macromad += table.meanAbsoluteDistace();
            macromse += table.meanSquaredError();
            stringBuilder2.append(" MAD = " + table.meanAbsoluteDistace() + " MSE = " + table.meanSquaredError());
            stringBuilder2.append(newLine);
        }
        macromad /= (double) tableSet.getEvaluatedCategoriesCount();
        macromse /= (double) tableSet.getEvaluatedCategoriesCount();
        stringBuilder.append("(macro-average evaluation) MAD = " + macromad + " MSE = " + macromse);
        stringBuilder.append(newLine);
        stringBuilder.append(newLine);
        return stringBuilder.toString() + stringBuilder2.toString();
    }
}
