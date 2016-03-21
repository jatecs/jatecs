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

package it.cnr.jatecs.indexing.corpus.RCV1;

import it.cnr.jatecs.indexes.DB.interfaces.ICategoryDB;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveCategoryDBBuilder;
import it.cnr.jatecs.indexing.corpus.ICategoryReader;
import it.cnr.jatecs.indexing.corpus.ValidCategory;
import it.cnr.jatecs.utils.JatecsLogger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;

public class RCV1CategoryReader implements ICategoryReader {

    /**
     * The string representing the null parent.
     */
    public static final String NULL_PARENT = "Root";

    /**
     * The name of the file containing the categories structure.
     */
    private String _filename;

    /**
     * Construct a new reader for categories contained in the file specified by
     * "categoriesFile". It accepts the format as specified in Appendix 2 of
     * site http://www.ai.mit.edu/projects/jmlr/papers/volume5/lewis04a/
     * lyrl2004_rcv1v2_README.htm .
     *
     * @param categoriesFile The file containing the categories to read.
     */
    public RCV1CategoryReader(String categoriesFile) {
        assert (categoriesFile != null);
        _filename = categoriesFile;
    }

    /**
     * Get the name of the file containing the definition of valid categories.
     *
     * @return The filename containing valid categories.
     */
    public String getCategoriesFilename() {
        return _filename;
    }

    /**
     * Set the name of file containing the list of valid categories.
     *
     * @param fname The file contatining the list of valid categories.
     */
    public void setCategoriesFilename(String fname) {
        assert (fname != null);
        _filename = fname;
    }

    /*
     * (non-Javadoc)
     *
     * @see it.cnr.jatecs.indexing.corpus.CategoriesReader#getCategories()
     */
    public ICategoryDB getCategoryDB() {
        FileReader fr = null;

        // Read the stopwords from a file.
        try {
            fr = new FileReader(_filename);
        } catch (Exception e) {
            String msg = "Error opening the categories file <" + _filename
                    + ">";
            throw new RuntimeException(msg, e);
        }

        BufferedReader reader = new BufferedReader(fr);

        Hashtable<String, ValidCategory> validCategories = new Hashtable<String, ValidCategory>();

        String line;
        try {
            line = reader.readLine();
        } catch (IOException e2) {
            try {
                reader.close();
            } catch (IOException e) {
                throw new RuntimeException(e2);
            }
            throw new RuntimeException(e2);
        }
        while (line != null) {
            if (line.startsWith("#")) {
                // This is a comment line.

                // Read next line.
                try {
                    line = reader.readLine();
                } catch (IOException e2) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e2);
                    }
                    throw new RuntimeException(e2);
                }

                continue;
            }

            try {
                String[] fields = decodeLine(line);
                if (fields.length == 0) {
                    // Read next line.
                    line = reader.readLine();
                    continue;
                }

                assert (fields.length == 3);

                // Assume a "row" in the file IS a category description.
                if (!validCategories.containsKey(fields[1])) {
                    ValidCategory cat = new ValidCategory();
                    cat.parent = (fields[0].equals(NULL_PARENT) ? null
                            : fields[0]);
                    cat.name = fields[1];

                    validCategories.put(cat.name, cat);
                }

                // Read next line.
                line = reader.readLine();
            } catch (Exception e) {
                String msg = "Error decoding line <" + line + ">: skip it.";
                JatecsLogger.execution().warning(msg);
                try {
                    line = reader.readLine();
                } catch (IOException e1) {
                    try {
                        reader.close();
                    } catch (IOException e2) {
                        throw new RuntimeException(e1);
                    }
                    throw new RuntimeException(e1);
                }
                continue;
            }
        }

        Hashtable<String, ValidCategory> catsToAdd = new Hashtable<String, ValidCategory>();

        // Validate the readed categories.
        Iterator<String> it = validCategories.keySet().iterator();
        while (it.hasNext()) {
            String catID = it.next();
            ValidCategory vc = validCategories.get(catID);
            if (!(vc.parent == null)) {
                if (!validCategories.containsKey(vc.parent)) {
                    ValidCategory valcat = new ValidCategory();
                    valcat.name = vc.parent;
                    valcat.parent = null;

                    catsToAdd.put(valcat.name, valcat);
                }
            }
        }

        // Add the remaining categories.
        it = catsToAdd.keySet().iterator();
        while (it.hasNext()) {
            String catID = it.next();
            ValidCategory vc = catsToAdd.get(catID);
            validCategories.put(catID, vc);
        }

        // Close the file reader.
        try {
            reader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        TroveCategoryDBBuilder builder = new TroveCategoryDBBuilder();
        String[] cats = new String[validCategories.size()];
        Iterator<ValidCategory> itCats = validCategories.values().iterator();
        int count = 0;
        while (itCats.hasNext()) {
            ValidCategory vc = itCats.next();
            cats[count] = vc.name;
            count++;
        }
        builder.setCategories(cats);

        itCats = validCategories.values().iterator();
        while (itCats.hasNext()) {
            ValidCategory vc = itCats.next();
            if (vc.parent == null)
                continue;

            builder.setParentCategory(vc.name, vc.parent);
        }

        return builder.getCategoryDB();
    }

    private String[] decodeLine(String line) {
        String prefix1 = "parent:";
        String prefix2 = "child:";
        String prefix3 = "child-description:";

        // Get parent.
        int startPos = line.indexOf(prefix1);
        if (startPos == 1)
            return new String[0];
        startPos += prefix1.length();

        int endPos = line.indexOf(prefix2);
        assert (endPos != -1);

        String[] toReturn = new String[3];
        String parent = new String(line.substring(startPos, endPos).trim());
        toReturn[0] = parent;

        // Get child.
        startPos = endPos + prefix2.length();
        endPos = line.indexOf(prefix3);
        assert (endPos != -1);

        String child = new String(line.substring(startPos, endPos).trim());
        if (child.equals(NULL_PARENT))
            return new String[0];

        toReturn[1] = child;

        // Get description
        startPos = endPos + prefix3.length();
        String descr = new String(line.substring(startPos).trim());
        toReturn[2] = descr;

        return toReturn;
    }

}
