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

/**
 * A feature extractor which uses internally a set of specified features
 * extractors to get all the features from a given text.
 *
 * @author Tiziano Fagni
 */
public class SetFeatureExtractor implements IFeatureExtractor {

    private final ArrayList<IFeatureExtractor> extractors;

    public SetFeatureExtractor() {
        extractors = new ArrayList<IFeatureExtractor>();
    }

    /**
     * Get the set of feature extractors used by this extractor. The set
     * returned can be used directly to add or remove extractors.
     *
     * @return The set of feature extractors used by this extractor.
     */
    public ArrayList<IFeatureExtractor> getExtractors() {
        return extractors;
    }

    @Override
    public List<String> extractFeatures(String text) {
        return extractFeatures(text, -1);
    }

    /**
     * The max number of features returned is considered by using the order of
     * declared extractors.
     */
    @Override
    public List<String> extractFeatures(String text, int maxNumberOfFeatures) {
        if (text == null)
            throw new NullPointerException("The specified text is 'null'");

        int numFeatures = Integer.MAX_VALUE;
        if (maxNumberOfFeatures >= 1)
            numFeatures = maxNumberOfFeatures;

        ArrayList<String> features = new ArrayList<String>();
        for (IFeatureExtractor extractor : extractors) {
            List<String> computedFeatures = extractor.extractFeatures(text);
            int usedFeatures = Math.min(computedFeatures.size(), numFeatures);
            if (usedFeatures == numFeatures) {
                for (int j = 0; j < numFeatures; j++)
                    features.add(computedFeatures.get(j));
            } else {
                features.addAll(computedFeatures);
            }
            numFeatures -= usedFeatures;
            if (numFeatures <= 0)
                break;
        }
        return features;
    }

}
