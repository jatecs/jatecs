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

package it.cnr.jatecs.nlp.utils;

import java.util.Vector;

public class WordNGrams {

    public static Vector<String> extractNGrams(String text, int n) {
        Vector<String> ngrams = new Vector<String>();

        String[] sentences = text.replaceAll(",|:", "").split("\\.|;|\\?|!");
        if (sentences.length == 0)
            sentences = new String[]{text};
        for (String sentence : sentences) {
            String[] words = sentence.split("\\s+");
            for (int i = n - 1; i < words.length; i++) {
                String temp = "";
                for (int j = n - 1; j >= 0; j--)
                    temp += " " + words[i - j];
                ngrams.add(temp.trim());
            }
        }

        return ngrams;
    }
}
