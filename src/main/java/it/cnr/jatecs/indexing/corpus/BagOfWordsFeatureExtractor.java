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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * This class is a Bag-Of-Words feature extractor.
 *
 * @author Tiziano Fagni
 */
public class BagOfWordsFeatureExtractor extends FeatureExtractor {

    private Pattern pattern;

    private boolean skipNumbers;

    private List<String> toReplaceAll;

    public BagOfWordsFeatureExtractor() {
        pattern = Pattern
                .compile("([\\s]+)|(\\-(\\-)+)|([\\:\\.\\,\\;\"\\<\\>\\[\\]\\{\\}\\\\/'\\\\&\\#\\*\\(\\)\\=\\?\\^\\!\\|\\+\\-])");
        skipNumbers = true;
        toReplaceAll = null;
    }

    /**
     * Indicates if the parser is skipping or not numbers.
     *
     * @return True if the parser is skipping numbers, false otherwise.
     */
    public boolean isSkippingNumbers() {
        return skipNumbers;
    }

    /**
     * Set the possibility by the parser to skip numbers.
     *
     * @param skipNumbers True if the numbers must be skipped, false otherwise.
     */
    public void setSkipNumbers(boolean skipNumbers) {
        this.skipNumbers = skipNumbers;
    }

    @Override
    protected List<String> computeFeatures(String text, int numFeatures) {
        if (numFeatures < 1)
            numFeatures = Integer.MAX_VALUE;
        String[] feats = pattern.split(text);

        List<String> features = new ArrayList<String>();

        for (int i = 0; i < feats.length; i++) {
            // Skip empty string.
            if (!feats[i].isEmpty()) {

                // If this feature is a number skip it.
                boolean isNumber = false;
                try {
                    Long.valueOf(feats[i]);
                    isNumber = true;
                } catch (Exception e) {
                    isNumber = false;
                }

                if ((isNumber && !isSkippingNumbers()) || !isNumber) {
                    // Convert the string to lower case.
                    String curr = feats[i].toLowerCase();

                    if (isSkippingNumbers()) {
                        curr = curr.replaceAll("[0-9]+", "");
                    }

                    curr = performCircularReplacements(curr);

                    if (!curr.isEmpty())
                        features.add(curr);
                }

            }
        }

        if (features.size() > numFeatures)
            features = features.subList(0, numFeatures);
        return features;
    }

    public void addCircularRule(String regex) {
        if (this.toReplaceAll == null)
            this.toReplaceAll = new ArrayList<String>();
        this.toReplaceAll.add(regex);
    }

    private String performCircularReplacements(String feat) {
        String procfeat = feat;
        if (toReplaceAll != null) {
            String orig = null;
            do {
                orig = procfeat;
                for (String regex : toReplaceAll)
                    procfeat = procfeat.replaceAll(regex, "");
            } while (!orig.equals(procfeat));
        }
        return procfeat;
    }
}
