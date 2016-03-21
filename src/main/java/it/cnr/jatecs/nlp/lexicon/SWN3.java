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
import java.util.Set;
import java.util.Vector;

public class SWN3 {

    private static HashMap<String, String> _dict;

    public SWN3() {
        _dict = new HashMap<String, String>();
        HashMap<String, Vector<Double>> _temp = new HashMap<String, Vector<Double>>();
        try {
            BufferedReader csv = new BufferedReader(
                    new InputStreamReader(
                            SWN3.class
                                    .getResourceAsStream("/dict/SentiWordNet_3.0.0.txt"),
                            "UTF-8"));
            String line = "";
            while ((line = csv.readLine()) != null) {
                String[] data = line.split("\t");
                Double score = Double.parseDouble(data[2])
                        - Double.parseDouble(data[3]);
                String[] words = data[4].split(" ");
                for (String w : words) {
                    String[] w_n = w.split("#");
                    w_n[0] += "#" + data[0];
                    int index = Integer.parseInt(w_n[1]) - 1;
                    if (_temp.containsKey(w_n[0])) {
                        Vector<Double> v = _temp.get(w_n[0]);
                        if (index > v.size())
                            for (int i = v.size(); i < index; i++)
                                v.add(0.0);
                        v.add(index, score);
                        _temp.put(w_n[0], v);
                    } else {
                        Vector<Double> v = new Vector<Double>();
                        for (int i = 0; i < index; i++)
                            v.add(0.0);
                        v.add(index, score);
                        _temp.put(w_n[0], v);
                    }
                }
            }
            csv.close();

            Set<String> temp = _temp.keySet();
            for (Iterator<String> iterator = temp.iterator(); iterator
                    .hasNext(); ) {
                String word = (String) iterator.next();
                Vector<Double> v = _temp.get(word);
                double score = 0.0;
                double sum = 0.0;
                for (int i = 0; i < v.size(); i++)
                    score += ((double) 1 / (double) (i + 1)) * v.get(i);
                for (int i = 1; i <= v.size(); i++)
                    sum += (double) 1 / (double) i;
                score /= sum;
                String sent = "";
                if (score >= 0.75)
                    sent = "strong_positive";
                else if (score > 0.25 && score <= 0.5)
                    sent = "positive";
                else if (score > 0 && score >= 0.25)
                    sent = "weak_positive";
                else if (score < 0 && score >= -0.25)
                    sent = "weak_negative";
                else if (score < -0.25 && score >= -0.5)
                    sent = "negative";
                else if (score <= -0.75)
                    sent = "strong_negative";
                _dict.put(word, sent);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String get(String word, String pos) {
        if (pos.startsWith("NN"))
            pos = "n";
        else if (pos.startsWith("JJ"))
            pos = "a";
        else if (pos.startsWith("RB"))
            pos = "r";
        else if (pos.startsWith("V"))
            pos = "v";
        if (_dict.containsKey(word)) {
            String ret = _dict.get(word.toLowerCase() + "#" + pos);
            return (ret.isEmpty() ? null : ret);
        }
        return null;
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
                if (temp.length > 1) {
                    temp[0] = temp[0].toLowerCase();
                    String pos = "";
                    if (temp[1].startsWith("NN"))
                        pos = "n";
                    else if (temp[1].startsWith("JJ"))
                        pos = "a";
                    else if (temp[1].startsWith("RB"))
                        pos = "r";
                    else if (temp[1].startsWith("V"))
                        pos = "v";
                    if (_dict.containsKey(temp[0] + "#" + pos)) {
                        String patt = _dict.get(temp[0] + "#" + pos);
                        if (!patt.trim().isEmpty())
                            temp[0] = patt;
                    }
                }
                newpattern += temp[0] + " ";
            }
            ret.add(newpattern.trim());
        }
        return ret;
    }

}
