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

package it.cnr.jatecs.indexing.corpus;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Hashtable;

public class SpecialTermLoader {

    private Hashtable<String, String> _specialTerms;

    public SpecialTermLoader() {
        super();
        _specialTerms = new Hashtable<String, String>();
    }

    public void loadSpecialTerms(String sourceFile) {
        FileReader fr;
        try {
            fr = new FileReader(sourceFile);
        } catch (FileNotFoundException e) {
            fr = null;
        }
        if (fr == null)
            return;
        BufferedReader br = new BufferedReader(fr);
        String line;
        try {
            line = br.readLine();
        } catch (IOException e) {
            line = null;
        }
        while (line != null) {
            String[] terms = line.split(" ");
            if (terms.length > 1)
                _specialTerms.put(terms[0], terms[1]);
            try {
                line = br.readLine();
            } catch (IOException e) {
                line = null;
            }
        }
    }

    public void clearSpecialTerms() {
        _specialTerms.clear();
    }

    public Hashtable<String, String> getSpecialTerms() {
        return _specialTerms;
    }

}
