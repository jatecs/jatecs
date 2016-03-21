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

package it.cnr.jatecs.indexing.corpus.OHSUMED;

import it.cnr.jatecs.indexes.DB.interfaces.ICategoryDB;
import it.cnr.jatecs.indexes.DB.interfaces.ICategoryDBBuilder;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Stack;

public class OHSUMEDCategoryReader {

    public static ICategoryDB ReadOHSUMEDCategories(String rootName, String file, ICategoryDBBuilder categoryDBBuilder) {
        ArrayList<String> cats = new ArrayList<String>();
        HashMap<String, String> parents = new HashMap<String, String>();
        try {
            FileReader fr = new FileReader(file);
            BufferedReader reader = new BufferedReader(fr);

            String line = null;
            String rootCode = null;
            if (rootName != null) {
                while ((line = reader.readLine()) != null) {
                    String[] fields = line.split(";");
                    String name = fields[0];
                    if (name.equals(rootName)) {
                        rootCode = fields[1];
                        break;
                    }
                }
            }

            Stack<String> fatherNames = new Stack<String>();
            Stack<String> fatherCodes = new Stack<String>();
            while ((line = reader.readLine()) != null) {
                if (!line.contains(";"))
                    break;
                String[] fields = line.split(";");
                String name = fields[0];
                String code = fields[1];
                if (rootCode == null || !code.startsWith(rootCode))
                    break;
                String father = null;
                while (father == null) {
                    if (fatherCodes.size() > 0) {
                        if (code.startsWith(fatherCodes.peek())) {
                            father = fatherNames.peek();
                            fatherNames.push(name);
                            fatherCodes.push(code);
                            break;
                        } else {
                            fatherNames.pop();
                            fatherCodes.pop();
                        }
                    } else {
                        fatherNames.push(name);
                        fatherCodes.push(code);
                        break;
                    }
                }
                cats.add(name);
                if (father != null)
                    parents.put(name, father);
            }
            reader.close();
            fr.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        categoryDBBuilder.setCategories(cats.toArray(new String[0]));
        Iterator<Entry<String, String>> iterator = parents.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<String, String> entry = iterator.next();
            categoryDBBuilder.setParentCategory(entry.getKey(), entry.getValue());
        }

        return categoryDBBuilder.getCategoryDB();
    }


}
