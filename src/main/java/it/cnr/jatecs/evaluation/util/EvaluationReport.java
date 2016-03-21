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

import gnu.trove.TShortShortHashMap;
import it.cnr.jatecs.evaluation.*;
import it.cnr.jatecs.indexes.DB.interfaces.ICategoryDB;
import it.cnr.jatecs.utils.Os;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

public class EvaluationReport {

    public static String printReport(ContingencyTableSet tableSet) {

        StringBuilder stringBuilder = new StringBuilder();

        String newLine = Os.newline();
        stringBuilder.append("Report from: " + tableSet.getName() + newLine);
        stringBuilder.append(newLine);
        stringBuilder.append("Number of categories: " + tableSet.getEvaluatedCategoriesCount() + newLine);
        stringBuilder.append(newLine);
        ContingencyTable table = tableSet.getGlobalContingencyTable();
        stringBuilder.append("Global results (micro-averaged evaluation)" + newLine);
        stringBuilder.append("tp = " + table.tp()
                + "\ttn = " + table.tn()
                + "\tfp = " + table.fp()
                + "\tfn = " + table.fn() + newLine);

        String res = String.format("p = %.3f\tr = %.3f\tf1 = %.3f\ta = %.3f",
                table.precision(), table.recall(), table.f1(), table.accuracy());
        stringBuilder.append(res);
        stringBuilder.append(newLine);
        stringBuilder.append("Global results (macro-averaged evaluation)" + newLine);
        res = String.format("p = %.3f\tr = %.3f\tf1 = %.3f\ta = %.3f",
                tableSet.macroPrecision(), tableSet.macroRecall(),
                tableSet.macroF1(), tableSet.macroAccuracy());
        stringBuilder.append(res);
        stringBuilder.append(newLine);
        stringBuilder.append(newLine);
        IShortIterator catIt = tableSet.getEvaluatedCategories();
        while (catIt.hasNext()) {
            short category = catIt.next();
            table = tableSet.getCategoryContingencyTable(category);
            stringBuilder.append("Results for category "
                    + table.getName() + newLine);
            stringBuilder.append("tp = " + table.tp()
                    + "\ttn = " + table.tn()
                    + "\tfp = " + table.fp()
                    + "\tfn = " + table.fn());
            res = String.format("p = %.3f\tr = %.3f\tf1 = %.3f\ta = %.3f",
                    table.precision(), table.recall(), table.f1(), table.accuracy());
            stringBuilder.append(res);
            stringBuilder.append(newLine);
        }
        return stringBuilder.toString();
    }

    public static String printReport(ContingencyTableSet tableSet, ICategoryDB originalCatsDB) {

        StringBuilder stringBuilder = new StringBuilder();


        String newLine = Os.newline();
        stringBuilder.append("Report for: " + tableSet.getName() + newLine);
        stringBuilder.append(newLine);
        stringBuilder.append("Number of categories: " + tableSet.getEvaluatedCategoriesCount() + newLine);
        stringBuilder.append(newLine);
        ContingencyTable table = tableSet.getGlobalContingencyTable();

        stringBuilder.append("catId\tcatName\tdepth\ttype\tposCount\tTP\tTN\tFP\tFN\tprecision\trecall\tspecificity\troc\tf1\taccuracy\tpd\tkl");
        stringBuilder.append(newLine);

        String res = String.format("%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f",
                table.precision(), table.recall(), table.specificity(), table.roc(), table.f1(), table.accuracy(), table.pd(), table.kl());

        stringBuilder.append("-1\tmicro\t-1\tOverview\t" + (table.tp() + table.fn()) + "\t" + table.tp()
                + "\t" + table.tn()
                + "\t" + table.fp()
                + "\t" + table.fn()
                + "\t" + res);
        stringBuilder.append(newLine);

        res = String.format("%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f",
                tableSet.macroPrecision(), tableSet.macroRecall(), tableSet.macroSpecificity(), tableSet.macroRoc(),
                tableSet.macroF1(), tableSet.macroAccuracy(), tableSet.macroPd());

        stringBuilder.append("-1\tmacro\t-1\tOverview\t-1\t-1\t-1\t-1\t-1\t" + res);
        stringBuilder.append(newLine);

        IShortIterator catIt = tableSet.getEvaluatedCategories();
        TShortShortHashMap map = new TShortShortHashMap();
        while (catIt.hasNext()) {
            short catID = catIt.next();
            map.put(catID, catID);
        }

        printReportHierarchically(stringBuilder, Short.MIN_VALUE, tableSet, originalCatsDB, 0, map);

        return stringBuilder.toString();
    }

    protected static void printReportHierarchically(StringBuilder stringBuilder, short catID, ContingencyTableSet tableSet, ICategoryDB catsDB, int level, TShortShortHashMap map) {
        String newLine = Os.newline();
        if (map.containsKey(catID)) {
            short category = catID;
            String type;
            if (catsDB.hasChildCategories(category))
                type = "Internal";
            else
                type = "Leaf";
            ContingencyTable table = tableSet.getCategoryContingencyTable(category);

            String res = String.format("%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f",
                    table.precision(), table.recall(), table.specificity(), table.roc(), table.f1(), table.accuracy(), table.pd(), table.kl());

            stringBuilder.append(category + "\t" + table.getName() + "\t" + level + "\t" + type + "\t" + (table.tp() + table.fn()) + "\t" + table.tp()
                    + "\t" + table.tn()
                    + "\t" + table.fp()
                    + "\t" + table.fn()
                    + "\t" + res);
            stringBuilder.append(newLine);
        } else {

            String catName = "";
            if (catID == Short.MIN_VALUE)
                catName = "HIERARCHY_ROOT";
            else
                catName = catsDB.getCategoryName(catID);

            stringBuilder.append("-1\t" + catName + "\t-1\tNotRelevant\t-1\t-1\t-1\t-1\t-1\t-1\t-1\t-1\t-1\t-1\t-1\t-1\t-1");
            stringBuilder.append(newLine);
        }

        IShortIterator catIt = null;
        if (catID == Short.MIN_VALUE)
            catIt = catsDB.getRootCategories();
        else
            catIt = catsDB.getChildCategories(catID);

        while (catIt.hasNext()) {
            short category = catIt.next();

            printReportHierarchically(stringBuilder, category, tableSet, catsDB, level + 1, map);
        }
    }


    public static String printReport(ConfusionMatrix cm, ICategoryDB catsDB) {
        StringBuilder sb = new StringBuilder();

        sb.append("*** Accuracy: " + cm.getAccuracy() + "\n\n");
        int numCats = catsDB.getCategoriesCount();
        for (short i = 0; i < numCats; i++) {
            sb.append("Acc (" + cm.getAccuracy(i) + ") ");
            sb.append("--> Category " + catsDB.getCategoryName(i) + ": ");
            for (short j = 0; j < numCats; j++) {
                String catName = catsDB.getCategoryName(j);
                int numErrors = cm.getError(i, j);
                sb.append(catName + "(" + numErrors + ") ");
            }
            sb.append("\n\n");
        }

        return sb.toString();
    }


    public static String printReport(MRRCategorySet cset, ICategoryDB catsDB, int maxRank) {
        StringBuilder sb = new StringBuilder();
        sb.append("Micro MRR: " + Os.generateDoubleString(cset.getGlobalMRR().getMRRValue(), 3) + "\n");
        sb.append("Macro MRR: " + Os.generateDoubleString(cset.getMacroMRRValue(), 3) + "\n");
        sb.append("CatName");
        for (int i = 0; i <= maxRank; i++) {
            if (i != maxRank)
                sb.append("\t" + (i + 1) + "-rank");
            else
                sb.append("\tOut_top_ranks");
        }
        sb.append("\tMRR\n");
        IShortIterator cats = cset.getEvaluatedCategories();
        while (cats.hasNext()) {
            short catID = cats.next();
            String catName = getAncestorsName(catsDB, catID, null);
            MRRCategory c = cset.getMRR(catID);
            sb.append(catName);
            for (int i = 0; i <= maxRank; i++) {
                sb.append("\t" + c.getRank(i));
            }
            sb.append("\t" + Os.generateDoubleString(c.getMRRValue(), 3) + "\n");
        }
        return sb.toString();
    }

    private static String getAncestorsName(ICategoryDB catsDB, short catID, String suffixName) {
        String catName = catsDB.getCategoryName(catID);
        IShortIterator parents = catsDB.getParentCategories(catID);
        String currentName = catName;
        if (suffixName != null)
            currentName += "->" + suffixName;

        if (!parents.hasNext())
            return currentName;
        else
            return getAncestorsName(catsDB, parents.next(), currentName);
    }
}
