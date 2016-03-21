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

import java.util.List;
import java.util.Vector;
import java.util.regex.Pattern;

public class RichFeatureExtractor extends FeatureExtractor {


    private boolean bigrams = false;
    private boolean negprop = false;
    private boolean skipNums = false;

    public RichFeatureExtractor() throws Exception {
        super();
    }

    public void setBigrams(boolean enable) {
        bigrams = enable;
    }

    public void setNegationPropagation(boolean enable) {
        negprop = enable;
    }

    public void setSkipNumbers(boolean enable) {
        skipNums = enable;
    }

    protected List<String> computeFeatures(String text) {
        //this vector will hold all the features
        Vector<String> v = new Vector<String>();

        //for bigrams construction
        String prev = null;

        // Split input with the pattern
        Pattern sentenceBound = Pattern.compile("([\\.\\,\\;])");

        // Split features contained into a sentes
        Pattern featureBound = Pattern.compile("([\\s]+)|([\\:\\.\\,\\;\"\\<\\>\\[\\]\\{\\}\\\\/'\\-\\+\\&\\#\\*\\(\\)\\=\\?\\^\\!])");

        String[] sentences = sentenceBound.split(text);
        for (int s = 0; s < sentences.length; ++s) {

            boolean isNegated = false;
            String[] feats = featureBound.split(sentences[s]);
            for (int i = 0; i < feats.length; i++) {
                // Skip empty string.
                if (!feats[i].equals("")) {
                    // If this feature is a number skip it.
                    boolean skipFeat = false;
                    if (skipNums) {
                        try {
                            Long.valueOf(feats[i]);
                            skipFeat = true;
                        } catch (Exception e) {
                            skipFeat = false;
                        }
                    }

                    if (!skipFeat) {
                        // Convert the string to lower case.
                        String curr = feats[i].toLowerCase();

                        if (curr.equals("not") ||
                                curr.equals("t") ||
                                curr.equals("arent") ||
                                curr.equals("isnt") ||
                                curr.equals("havent") ||
                                curr.equals("hasnt") ||
                                curr.equals("no")
                                )
                            isNegated = true;

                        if (isNegated && negprop)
                            curr = "not_" + curr;
                        v.add(curr);
                        if (bigrams) {
                            if (prev != null)
                                v.add(prev + curr);
                            prev = curr;
                        }
                    }
                }
            }
        }

        return v;
    }

    @Override
    protected List<String> computeFeatures(String text, int numFeatures) {
        if (numFeatures < 0)
            numFeatures = Integer.MAX_VALUE;
        List<String> feats = computeFeatures(text);
        if (feats.size() > numFeatures)
            feats = feats.subList(0, numFeatures - 1);
        return feats;
    }

}
