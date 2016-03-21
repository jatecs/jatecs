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

import it.cnr.jatecs.nlp.lexicon.GI;
import it.cnr.jatecs.nlp.patterns.Parser;
import it.cnr.jatecs.nlp.utils.WordNGrams;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

public class NLPFeatureExtractor extends BagOfWordsFeatureExtractor {

    private GI gi = new GI();

    private boolean _extractWord2Grams = true;
    private boolean _extractWord3Grams = true;
    private boolean _extractPatterns = true;
    private boolean _extractNegation = true;

    public void disableWord2Grams() {
        _extractWord2Grams = false;
    }

    public void disableWord3Grams() {
        _extractWord3Grams = false;
    }

    public void disablePatterns() {
        _extractPatterns = false;
    }

    public void disableNegation() {
        _extractNegation = false;
    }

    protected List<String> computeFeatures(String text) {
        return computeFeatures(text, -1);
    }

    @Override
    protected List<String> computeFeatures(String text, int numFeatures) {
        if (numFeatures < 0)
            numFeatures = Integer.MAX_VALUE;

        Vector<String> features = new Vector<String>();

        if (_extractWord2Grams) {
            Vector<String> ngrams = WordNGrams.extractNGrams(text, 2);
            String word1 = null;
            if (ngrams.size() > 0)
                word1 = gi.get(ngrams.get(0).split("\\s+")[0], "");
            for (Iterator<String> iterator = ngrams.iterator(); iterator.hasNext(); ) {
                String ngram = (String) iterator.next();
                String[] temp = ngram.trim().split("\\s+");
                if (temp.length < 2)
                    continue;
                String word2 = gi.get(temp[1], "");
                if (word1 != null || word2 != null) {
                    String bigram = (word1 != null ? word1 : temp[0]) + " " + (word2 != null ? word2 : temp[1]);
                    features.add(bigram);
                }
                word1 = word2;
            }
        }

        if (_extractWord3Grams) {
            features.addAll(WordNGrams.extractNGrams(text, 3));
        }

        if (_extractPatterns) {
            Parser parser = new Parser();
            features.addAll(parser.extract(text));
        }

        if (_extractNegation) {
            String[] tempText = text.split("\\.|;|!|\\?|,|\\(");
            for (String tText : tempText) {
                tText = tText.replace(",", " , ").replace("  ", " ");
                int notIdx = -1;
                for (int i = 0; i < Parser.negatives.length && notIdx == -1; i++)
                    notIdx = tText.indexOf(Parser.negatives[i]);
                if (notIdx > -1) {
                    tText = tText.substring(notIdx);
                    Vector<String> ngrams = new Vector<String>();
                    ngrams.addAll(WordNGrams.extractNGrams(tText, 1));
                    for (Iterator<String> iterator = ngrams.iterator(); iterator
                            .hasNext(); ) {
                        String ngram = (String) iterator.next();
                        features.add("not " + ngram);
                    }
                }
            }
        }
        //A questo punto richiamo la superclasse per fargli estrarre il resto delle feature
        features.addAll(super.computeFeatures(text, numFeatures));

        if (features.size() > numFeatures)
            features = (Vector<String>) features.subList(0, numFeatures - 1);
        return features;
    }
}
