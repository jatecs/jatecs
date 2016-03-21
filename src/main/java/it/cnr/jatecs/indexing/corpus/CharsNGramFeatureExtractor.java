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

import it.cnr.jatecs.indexing.preprocessing.Stemming;
import it.cnr.jatecs.indexing.preprocessing.Stopword;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is a character NGrams features extractor.
 *
 * @author Tiziano Fagni
 */
public class CharsNGramFeatureExtractor extends FeatureExtractor {

    private int nGramSize;

    /**
     * Build a new instance with the ngram size set to 3.
     */
    public CharsNGramFeatureExtractor() {
        nGramSize = 3;
        disableStemming();
        disableStopwordRemoval();
    }

    public int getNGramSize() {
        return nGramSize;
    }

    /**
     * Set the ngrams size.
     *
     * @param nGramsSize The ngrams size (a value greater than 1).
     */
    public void setNGramSize(int nGramsSize) {
        if (nGramsSize <= 1)
            throw new IllegalArgumentException(
                    "The specified ngrams size is invalid: " + nGramsSize);
        this.nGramSize = nGramsSize;
    }

    /**
     * The stemming has non-sense in this extractor so it is always disabled.
     */
    @Override
    public void enableStemming(Stemming m) {
        throw new IllegalArgumentException(
                "This feature has non-sense in this feature extractor");
    }

    /**
     * The stopwords removal has non-sense in this extractor so it is always
     * disabled.
     */
    @Override
    public void enableStopwordRemoval(Stopword m) {
        throw new IllegalArgumentException(
                "This feature has non-sense in this feature extractor");
    }

    @Override
    protected List<String> computeFeatures(String text, int numFeatures) {
        if (numFeatures < 1)
            numFeatures = Integer.MAX_VALUE;

        List<String> features = new ArrayList<String>();

        // Extract all words trimming all spaces/new lines/etc.
        // String[] words = text.split("\\s+");
        String[] words = text
                .split("([\\s]+)|([\\:\\.\\,\\;\"\\<\\>\\[\\]\\{\\}\\\\/'\\\\&\\#\\*\\(\\)\\=\\?\\^\\!\\|])");

        // Build a new content string with all spaces trimmed.
        String content = buildTextToBeAnalyzed(words);

        // Get the ngrams features.
        addNGrams(features, content);

        if (features.size() > numFeatures)
            features = features.subList(0, numFeatures);
        return features;
    }

    private void addNGrams(List<String> features, String text) {
        int shiftsCount = text.length() - getNGramSize();
        if (shiftsCount > 0) {
            for (int i = 0; i <= shiftsCount; ++i)
                features.add(text.substring(i, i + getNGramSize())
                        .toLowerCase());
        } else {
            features.add(text);
        }
    }

    private String buildTextToBeAnalyzed(String[] words) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (words[i].isEmpty())
                continue;

            if (i == 0)
                sb.append(words[i]);
            else
                sb.append(" " + words[i]);
        }

        return sb.toString();
    }

}
