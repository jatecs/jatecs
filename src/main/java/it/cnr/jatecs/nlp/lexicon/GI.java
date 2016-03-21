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

package it.cnr.jatecs.nlp.lexicon;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

public class GI {
    private HashMap<String, String[]> _csv;
    private HashMap<String, Integer> _map;
    private boolean _useEnrichedData = true;

    public GI() {

        _csv = new HashMap<String, String[]>();
        _map = new HashMap<String, Integer>();
        try {
            BufferedReader csv = new BufferedReader(
                    new InputStreamReader(
                            GI.class.getResourceAsStream("/dict/inquireraugmented.csv"),
                            "UTF-8"));
            String line = "";
            int i = 0;
            while ((line = csv.readLine()) != null) {
                String[] data = line.split(";");
                if (i == 0) {
                    for (int j = 0; j < data.length; j++)
                        _map.put(data[j], j);
                } else {
                    if (i != 1) {
                        String pos = "";
                        if (!data[_map.get("Positiv")].trim().isEmpty()
                                || !data[_map.get("Pstv")].trim().isEmpty()
                                || !data[_map.get("PosAff")].trim().isEmpty())
                            pos = "positive";
                        String neg = "";
                        if (!data[_map.get("Negativ")].trim().isEmpty()
                                || !data[_map.get("Ngtv")].trim().isEmpty()
                                || !data[_map.get("NegAff")].trim().isEmpty())
                            neg = "negative";
                        String[] temp = null;
                        if (_useEnrichedData)
                            temp = new String[]{pos, neg,
                                    data[_map.get("Hostile")],
                                    data[_map.get("Strong")],
                                    data[_map.get("Weak")],
                                    data[_map.get("Pleasur")],
                                    data[_map.get("EMOT")],
                                    data[_map.get("Virtue")],
                                    data[_map.get("Increas")]};
                        else
                            temp = new String[]{pos, neg};
                        if (data[0].indexOf('#') != -1) {
                            String[] temp1 = data[0].split("#");
                            if (temp1[1].equalsIgnoreCase("1"))
                                _csv.put(temp1[0].toLowerCase(), temp);
                        } else
                            _csv.put(data[0].toLowerCase(), temp);
                    }
                    i++;
                }
            }

            csv.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String implode(String[] ary, String delim) {
        String out = "";
        for (int i = 0; i < ary.length; i++) {
            if (ary[i].trim().isEmpty())
                continue;
            out += ary[i].trim();
            if (i != ary.length - 1) {
                out += delim;
            }
        }
        return out.trim();
    }

    public static void main(String[] args) {

    }

    public Vector<String> extract(Vector<String> patterns) {
        Vector<String> ret = new Vector<String>();
        for (Iterator<String> iterator = patterns.iterator(); iterator
                .hasNext(); ) {
            String pattern = (String) iterator.next();
            String newpattern = "";
            String[] words = pattern.trim().split(" ");
            for (int i = 0; i < words.length; i++) {
                String[] temp = words[i].split("#");
                temp[0] = temp[0].toLowerCase();
                if (_csv.containsKey(temp[0])) {
                    String patt = implode(_csv.get(temp[0]), " ");
                    if (!patt.trim().isEmpty())
                        temp[0] = patt;
                }
                newpattern += temp[0] + " ";
            }
            ret.add(newpattern.trim());
        }
        return ret;
    }

    public String get(String word, String pos) {
        String[] temp = _csv.get(word.toLowerCase());
        String t = "";
        if (temp != null)
            t = implode(temp, " ").trim();

        return t.isEmpty() ? null : t;
    }

    public void disableEnrichedData() {
        _useEnrichedData = false;
    }
}
