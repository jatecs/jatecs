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

import gnu.trove.TShortArrayList;
import it.cnr.jatecs.utils.Os;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.io.*;

public class ContingencyTableDataManager {

    /**
     * Writes the given contingency table on specified filename.
     *
     * @param filename The file where the CT data will be written.
     * @param ct       The contingency table to write.
     * @throws IOException
     */
    public static void writeContingencyTable(String filename,
                                             ContingencyTable ct) throws IOException {
        File f = new File(filename);
        String path = f.getParent();
        File fparent = new File(path);
        fparent.mkdirs();

        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(f)));
        bw.write(ct.getName() + "\n");
        bw.write(ct._fn + "\n");
        bw.write(ct._fp + "\n");
        bw.write(ct._tn + "\n");
        bw.write(ct._tp + "\n");

        bw.close();
    }

    /**
     * Reads the given filename and returns the corresponding contingency table
     * object.
     *
     * @param filename The file to read.
     * @return The corresponding contingency table.
     * @throws IOException
     */
    public static ContingencyTable readContingencyTable(String filename)
            throws IOException {

        File f = new File(filename);
        BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(f)));

        ContingencyTable ct = new ContingencyTable(br.readLine());
        ct._fn = Integer.parseInt(br.readLine());
        ct._fp = Integer.parseInt(br.readLine());
        ct._tn = Integer.parseInt(br.readLine());
        ct._tp = Integer.parseInt(br.readLine());

        br.close();
        return ct;

    }

    /**
     * Writes all the contingency tables coming from specified "comparer"
     * object.
     *
     * @param outputDir The output directory where the data will be written.
     * @param comparer  The input comparer object.
     * @throws IOException
     */
    public static void writeContingencyTableSet(String outputDir,
                                                ContingencyTableSet tableSet) throws IOException {
        File f = new File(outputDir);
        f.mkdirs();

        String categories = outputDir + Os.pathSeparator() + "categories.db";
        BufferedWriter os = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(categories)));
        IShortIterator cats = tableSet.getEvaluatedCategories();

        os.write(tableSet.getName() + "\n");

        // First write which are the valid categories.
        os.write(tableSet.getEvaluatedCategoriesCount() + "\n");
        while (cats.hasNext()) {
            short catID = cats.next();
            os.write(catID + "\n");
        }
        os.close();

        // Write the contingency table for each category.
        cats.begin();
        while (cats.hasNext()) {
            short catID = cats.next();
            String fname = outputDir + Os.pathSeparator() + catID + ".txt";
            writeContingencyTable(fname, tableSet
                    .getCategoryContingencyTable(catID));
        }

        // Write the global contingency table.
        String fname = outputDir + Os.pathSeparator() + "global.txt";
        writeContingencyTable(fname, tableSet.getGlobalContingencyTable());
    }

    public static ContingencyTableSet readContingencyTableSet(String inputDir)
            throws IOException {
        File f = new File(inputDir + Os.pathSeparator() + "categories.db");
        BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(f)));

        ContingencyTableSet tableSet = new ContingencyTableSet(br.readLine());

        TShortArrayList cats = new TShortArrayList();
        int catCount = Integer.parseInt(br.readLine());
        for (int i = 0; i < catCount; ++i) {
            cats.add(Short.parseShort(br.readLine()));
        }
        br.close();
        for (int i = 0; i < catCount; ++i) {
            short cat = cats.getQuick(i);
            String fname = inputDir + Os.pathSeparator() + cat + ".txt";
            ContingencyTable table = readContingencyTable(fname);
            tableSet.addContingenyTable(cat, table);
        }

        return tableSet;
    }
}
