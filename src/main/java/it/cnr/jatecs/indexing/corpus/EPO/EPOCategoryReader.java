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

package it.cnr.jatecs.indexing.corpus.EPO;

import it.cnr.jatecs.indexes.DB.interfaces.ICategoryDB;
import it.cnr.jatecs.indexes.DB.interfaces.ICategoryDBBuilder;
import it.cnr.jatecs.indexing.corpus.ICategoryReader;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.Pair;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

public class EPOCategoryReader implements ICategoryReader {

    /**
     * The string representing the null parent.
     */
    public static final String NULL_PARENT = "-1";

    /**
     * The name of the file containing the categories structure.
     */
    private String _filename;
    private ICategoryDBBuilder _categoriesDBBuilder;

    /**
     * Construct a new reader for categories contained in the file specified by
     * "categoriesFile".
     * <p>
     * The syntax of the of the input file accepted by this reader is the
     * following: - a line with first character equals to '#' is a comment and
     * then skipped by the parser; - a line with first character different from
     * '#' or not empty line is a valid line and must follow this syntax. Each
     * field in the line is separated by one TAB character and order of fields
     * have the following meaning: 1) The category ID. 2) The human-friendly
     * category name. 3) The parent category ID or -1 if tha category has no
     * parent.
     *
     * @param categoriesFile The file containing the categories to read.
     */
    public EPOCategoryReader(String categoriesFile,
                             ICategoryDBBuilder categoriesDBBuilder) {
        assert (categoriesFile != null && categoriesDBBuilder != null);
        _filename = categoriesFile;
        _categoriesDBBuilder = categoriesDBBuilder;
    }

    /**
     * Get the name of the file containing the definition of valid categories.
     *
     * @return The filename containing valid categories.
     */
    public String categoriesFilename() {
        return _filename;
    }

    public ICategoryDBBuilder getCategoryDBBuilder() {
        return _categoriesDBBuilder;
    }

    /**
     * @see it.cnr.jatecs.indexing.corpus.ICategoryReader#getCategoryDB()
     */
    public ICategoryDB getCategoryDB() throws Exception {
        _categoriesDBBuilder.getCategoryDB().removeCategories(
                _categoriesDBBuilder.getCategoryDB().getCategories());

        FileReader fr = null;
        try {
            fr = new FileReader(_filename);
        } catch (Exception e) {
            throw new Exception("Error opening the categories file <"
                    + _filename + ">: " + e.getMessage());
        }

        BufferedReader reader = new BufferedReader(fr);
        Hashtable<String, Pair<String, String>> categories = new Hashtable<String, Pair<String, String>>();

        try {
            String line = reader.readLine();
            while (line != null) {
                if (line.startsWith("#")) {
                    // This is a comment line.
                    line = reader.readLine();
                    continue;
                }

                try {
                    String[] fields = line.split("[\\s]+");
                    if (fields.length == 0) {
                        line = reader.readLine();
                        continue;
                    }

                    assert (fields.length == 3);

                    // Assume a "row" in the file IS a category description.
                    if (!categories.containsKey(fields[0])) {
                        String catName = fields[0];
                        String parentName = (fields[2].equals(NULL_PARENT) ? ""
                                : fields[2]);
                        Pair<String, String> p = new Pair<String, String>(
                                fields[1], parentName);
                        categories.put(catName, p);
                    }

                    line = reader.readLine();
                } catch (Exception e) {
                    String msg = "Error decoding line <" + line + ">: skip it.";
                    JatecsLogger.status().println(msg);
                    line = reader.readLine();
                    continue;
                }
            }

            String[] cats = new String[categories.size()];
            Iterator<Pair<String, String>> it = categories.values().iterator();
            int i = 0;
            while (it.hasNext()) {
                Pair<String, String> p = it.next();
                cats[i++] = p.getFirst();
            }

            // Add categories to the builder
            _categoriesDBBuilder.setCategories(cats);

            // Set parent categories
            Enumeration<String> en = categories.keys();
            while (en.hasMoreElements()) {
                String cat = en.nextElement();
                String parent = categories.get(cat).getSecond();
                if (!parent.equals("")) {
                    try {
                        _categoriesDBBuilder.setParentCategory(
                                categories.get(cat).getFirst(),
                                categories.get(parent).getFirst());
                    } catch (Exception e) {
                        reader.close();
                        throw new RuntimeException(
                                "Error setting parent category", e);
                    }
                }
            }

            // Close the file reader.
            reader.close();
            JatecsLogger.status().println(
                    _categoriesDBBuilder.getCategoryDB().getCategoriesCount()
                            + " categories read");
            return _categoriesDBBuilder.getCategoryDB();
        } catch (Exception e) {
            throw new Exception("Error reading the categories file "
                    + _filename + ": " + e.getMessage());
        }
    }
}
