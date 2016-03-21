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

/**
 * @author Stefano Baccianella
 *         <p>
 *         Transforms patterns extracted from reviews to sentiment pattern
 *         derived by Shlomo-Argamon lexicon
 */
public class ShlomoArgamonExtractor {
    private HashMap<String, String[]> _csv;

    public ShlomoArgamonExtractor() {
        _csv = new HashMap<String, String[]>();
        try {
            BufferedReader csv = new BufferedReader(
                    new InputStreamReader(ShlomoArgamonExtractor.class
                            .getResourceAsStream("/dict/argamon.csv"), "UTF-8"));
            String line = "";
            while ((line = csv.readLine()) != null) {
                String[] data = line.split(",");
                _csv.put(data[0] + "#" + data[1], new String[]{data[2],
                        data[3]});
            }
            csv.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        ShlomoArgamonExtractor s = new ShlomoArgamonExtractor();
        Vector<String> t = new Vector<String>();
        t.add("not#RB very#RB good#JJ work#NN");
        Vector<String> a = s.extract(t);
        System.out.println(a.get(0));
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
                // newpattern += retrieveInformation(temp[0].trim(),
                // temp[1].trim()) + " ";
                if (temp.length == 2)
                    newpattern += retrieveInformationCSV(temp[0].trim(),
                            temp[1].trim()) + " ";
            }
            ret.add(newpattern.trim());
        }
        return ret;
    }

    public String get(String word, String pos) {
        String force = retrieveInformationCSV(word.toLowerCase(), pos);
        return (!force.equals(word) ? force : null);
    }

    private String retrieveInformationCSV(String word, String pos) {
        word = word.replace("'", "");
        pos = pos.replace("'", "");
        String[] sentiment_force = null;
        if (_csv.containsKey(word + '#' + pos))
            sentiment_force = _csv.get(word + '#' + pos);
        else if (_csv.containsKey(word + '#'))
            sentiment_force = _csv.get(word + '#');
        if (sentiment_force != null)
            if (sentiment_force[0].length() != 0)
                return sentiment_force[0];
            else if (sentiment_force[1].length() != 0)
                return sentiment_force[1];
        return word;
    }

}
