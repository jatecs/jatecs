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

/**
 * A generic feature extractor.
 *
 * @author Tiziano Fagni
 */
public interface IFeatureExtractor {

    /**
     * Extract the features from the given text.
     *
     * @param text The text to be analyzed.
     * @return The set of extracted features.
     * @throws NullPointerException Raised if the specified text is 'null'.
     */
    public List<String> extractFeatures(String text);


    /**
     * Extract the specified max number of features from the given text.
     *
     * @param text                The text to be analyzed.
     * @param maxNumberOfFeatures The max number of features to return or a value less than 1 if
     *                            you want to extract all features.
     * @return The set of extracted features.
     * @throws NullPointerException Raised if the specified text is 'null'.
     */
    public List<String> extractFeatures(String text, int maxNumberOfFeatures);

}
