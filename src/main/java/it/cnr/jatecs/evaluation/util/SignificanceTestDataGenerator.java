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

import gnu.trove.TShortHashSet;
import it.cnr.jatecs.evaluation.ContingencyTable;
import it.cnr.jatecs.evaluation.ContingencyTableSet;
import it.cnr.jatecs.evaluation.HierarchicalClassificationComparer;
import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDB;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

public class SignificanceTestDataGenerator {
    public static void generate_s_test(String filename, IClassificationDB gold, IClassificationDB system) {
        generate_s_test(filename, gold, system, gold.getCategoryDB().getCategories());
    }

    public static void generate_s_test(String filename, IClassificationDB gold, IClassificationDB system, IShortIterator validCategories) {
        try {
            File f = new File(filename);
            File parent = f.getParentFile();
            if (!parent.exists())
                parent.mkdirs();

            FileWriter outFile = new FileWriter(f);
            PrintWriter out = new PrintWriter(outFile);

            IIntIterator docs = gold.getDocumentDB().getDocuments();
            while (validCategories.hasNext()) {
                short catID = validCategories.next();
                docs.begin();
                while (docs.hasNext()) {
                    int docID = docs.next();
                    boolean resGold = gold.hasDocumentCategory(docID, catID);
                    boolean resSystem = system.hasDocumentCategory(docID, catID);
                    if (resGold == resSystem)
                        out.print("1\t");
                    else
                        out.print("0\t");
                }
                out.print("\n");
            }

            out.close();
        } catch (Exception e) {
            throw new RuntimeException("Bug in code", e);
        }
    }


    public static void generate_S_test(String filename, IClassificationDB gold, IClassificationDB system) {
        generate_S_test(filename, gold, system, gold.getCategoryDB().getCategories());
    }

    public static void generate_S_test(String filename, IClassificationDB gold, IClassificationDB system, IShortIterator validCategories) {
        try {
            File f = new File(filename);
            File parent = f.getParentFile();
            if (!parent.exists())
                parent.mkdirs();

            FileWriter outFile = new FileWriter(f);
            PrintWriter out = new PrintWriter(outFile);

            HierarchicalClassificationComparer comparer = new HierarchicalClassificationComparer(system, gold, validCategories);
            ContingencyTableSet tableSet = comparer.evaluate();

            IShortIterator availableCategoriesIterator = tableSet.getEvaluatedCategories();
            TShortHashSet availableCategories = new TShortHashSet();
            while (availableCategoriesIterator.hasNext())
                availableCategories.add(availableCategoriesIterator.next());

            validCategories.begin();
            while (validCategories.hasNext()) {
                short catID = validCategories.next();
                if (availableCategories.contains(catID)) {
                    ContingencyTable ct = tableSet.getCategoryContingencyTable(catID);
                    out.print(ct.f1() + "\n");
                }
            }

            out.close();
        } catch (Exception e) {
            throw new RuntimeException("Bug in code", e);
        }
    }


    public static void generate_p_test(String filename, IClassificationDB gold, IClassificationDB system) {
        generate_p_test(filename, gold, system, gold.getCategoryDB().getCategories());
    }

    public static void generate_p_test(String filename, IClassificationDB gold, IClassificationDB system, IShortIterator validCategories) {
        try {
            File f = new File(filename);
            File parent = f.getParentFile();
            if (!parent.exists())
                parent.mkdirs();

            FileWriter outFile = new FileWriter(f);
            PrintWriter out = new PrintWriter(outFile);

            HierarchicalClassificationComparer comparer = new HierarchicalClassificationComparer(system, gold, validCategories);
            ContingencyTableSet tableSet = comparer.evaluate();

            ContingencyTable global = tableSet.getGlobalContingencyTable();

            // First write macro accuracy data.
            int numTrialsAccuracy = tableSet.getEvaluatedCategoriesCount() * gold.getDocumentDB().getDocumentsCount();
            out.print(tableSet.macroAccuracy() + "\t" + numTrialsAccuracy + "\n");

            // Write macro error data.
            out.print(tableSet.macroError() + "\t" + numTrialsAccuracy + "\n");

            // Write macro precision data.
            out.print(tableSet.macroPrecision() + "\t" + (global.tp() + global.fp()) + "\n");

            // Write macro recall data.
            out.print(tableSet.macroRecall() + "\t" + global.tp() + global.fn() + "\n");

            // Write micro accuracy data.
            out.print(global.accuracy() + "\t" + numTrialsAccuracy + "\n");

            // Write micro error data.
            out.print(global.error() + "\t" + numTrialsAccuracy + "\n");

            // Write micro precision data.
            out.print(global.precision() + "\t" + (global.tp() + global.fp()) + "\n");

            // Write macro recall data.
            out.print(global.recall() + "\t" + global.tp() + global.fn() + "\n");

            out.close();
        } catch (Exception e) {
            throw new RuntimeException("Bug in code", e);
        }
    }
}
