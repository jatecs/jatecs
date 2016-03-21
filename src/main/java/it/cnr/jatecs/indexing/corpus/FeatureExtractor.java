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

/**
 *
 */
package it.cnr.jatecs.indexing.corpus;

import it.cnr.jatecs.indexing.preprocessing.HTMLEntities;
import it.cnr.jatecs.indexing.preprocessing.SpellCheck;
import it.cnr.jatecs.indexing.preprocessing.Stemming;
import it.cnr.jatecs.indexing.preprocessing.Stopword;
import it.cnr.jatecs.utils.JatecsLogger;

import java.util.*;


public abstract class FeatureExtractor implements IFeatureExtractor {

    /**
     * Indicate if stopword removal module.
     */
    protected Stopword _stopwordModule;


    /**
     * Indicate the stemming removal module.
     */
    protected Stemming _stemmingModule;


    /**
     * The module that treats the HTML/XML entities.
     */
    protected HTMLEntities _entities;


    /**
     * The dictionary containing special terms that must be substitute.
     */
    protected Hashtable<String, String> _specialTerms;

    /**
     * The module that performs spell checking.
     */
    protected SpellCheck _spellChecker;
    private boolean _addTFFeatures;

    /**
     * Construct a new FeatureExtractor with stopword, stemming and HTML/XML entities
     * substitution enabled.
     *
     * @throws Exception
     */
    public FeatureExtractor() {

        disableStopwordRemoval();
        disableStemming();
        disableEntitiesSubstitution();

        _specialTerms = null;
        _spellChecker = null;
        _addTFFeatures = false;
    }

    /**
     * Enable feature stopword removal by using the class specified by
     * "m".
     *
     * @param m The stopword module to use.
     */
    public void enableStopwordRemoval(Stopword m) {
        if (m == null)
            throw new NullPointerException("The specified stopword instance is 'null'");
        _stopwordModule = m;
        JatecsLogger.execution().info("Stopword removal enabled");
    }

    /**
     * Disable the feature stopword removal operation.
     */
    public void disableStopwordRemoval() {
        JatecsLogger.execution().info("Stopword removal disabled");
        _stopwordModule = null;
    }

    /**
     * Indicate if the stopword operation is or not enabled.
     *
     * @return True if the stopword is enabled, false otherwise.
     */
    public boolean isStopwordEnabled() {
        if (_stopwordModule == null)
            return false;
        else
            return true;
    }

    /**
     * Enabling the stemming operation on the features computed by this class.
     *
     * @param m The used stemming module.
     */
    public void enableStemming(Stemming m) {
        if (m == null)
            throw new NullPointerException("The specified stemming instance is 'null'");

        _stemmingModule = m;
        JatecsLogger.execution().info("Stemming enabled");
    }

    /**
     * Disable the stemming operation.
     */
    public void disableStemming() {
        _stemmingModule = null;
        JatecsLogger.execution().info("Stemming disabled");
    }

    /**
     * Indicate if the stemming operation is or not enabled.
     *
     * @return True if the stemming is enabled, false otherwise.
     */
    public boolean isStemmingEnabled() {
        if (_stemmingModule == null)
            return false;
        else
            return true;
    }

    /**
     * Enable the HTML/XML entities substitution operation.
     *
     * @throws JatecsException
     */
    public void enableEntitiesSubstitution() {
        _entities = new HTMLEntities();
        JatecsLogger.execution().info("HTML entities substitution enabled");
    }

    /**
     * Disable the HTML/XML entities substitution operation.
     */
    public void disableEntitiesSubstitution() {
        _entities = null;
        JatecsLogger.execution().info("HTML entities substitution disabled");
    }

    /**
     * Indicate if the entities substitution operation is or not enabled.
     *
     * @return True if the entities substitution is enabled, false otherwise.
     */
    public boolean isEntitiesSubstitutionEnabled() {
        if (_entities == null)
            return false;
        else
            return true;
    }

    /**
     * Enable special terms substituion with the specified dictionary.
     *
     * @param specialTerms The dictionary containing the terms that must be substituted.
     */
    public void enableSpecialTermsSubstitution(Hashtable<String, String> specialTerms) {
        if (specialTerms == null)
            throw new NullPointerException("The specifies special terms set is 'null'");

        _specialTerms = specialTerms;
        JatecsLogger.execution().info("special terms substitution enabled");
    }

    /**
     * Disable the special terms substitution.
     */
    public void disableSpecialTermsSubstitution() {
        _specialTerms = null;
        JatecsLogger.execution().info("special terms substitution disabled");
    }

    /**
     * Indicate if the special terms substitution operation is or not enabled.
     *
     * @return True if the special terms  substitution is enabled, false otherwise.
     */
    public boolean isSpecialTermsSubstitutionEnabled() {
        if (_specialTerms == null)
            return false;
        else
            return true;
    }

    /**
     * Enables spell checking.
     *
     * @param spellChecker the spell checker object
     */
    public void enableSpellChecking(SpellCheck spellChecker) {
        if (spellChecker == null)
            throw new NullPointerException("The specified spell checker is 'null'");

        _spellChecker = spellChecker;
        JatecsLogger.execution().info("spell checking enabled");
    }

    public void disableSpellChecking() {
        _spellChecker = null;
        JatecsLogger.execution().info("spell checking disabled");
    }

    public boolean isSpellCheckingEnabled() {
        if (_spellChecker == null)
            return false;
        else
            return true;
    }

    public void enableTFFeatures() {
        _addTFFeatures = true;
    }

    public void disableTFFeatures() {
        _addTFFeatures = false;
    }

    public boolean isTFFeaturesEnabled() {
        return _addTFFeatures;
    }

    @Override
    public List<String> extractFeatures(String text, int maxNumberInitialFeatures) {
        if (text == null)
            throw new NullPointerException("The specified text is 'null'");

        if (isEntitiesSubstitutionEnabled())
            text = _entities.replaceEntities(text);

        if (isSpellCheckingEnabled())
            text = _spellChecker.spellCorrect(text);

        List<String> features = computeFeatures(text, maxNumberInitialFeatures);
        if (features == null)
            features = new Vector<String>();

        if (isSpecialTermsSubstitutionEnabled()) {
            ArrayList<String> specialFeaturesList = new ArrayList<String>();
            for (String f : features) {
                String[] stFeat = f.split("\\s+");
                boolean addFeat = false;
                String newFeat = "";
                for (String sf : stFeat) {
                    if (_specialTerms.containsKey(sf)) {
                        addFeat = true;
                        newFeat += _specialTerms.get(sf) + " ";
                    } else
                        newFeat += sf + " ";
                }
                if (addFeat)
                    specialFeaturesList.add(newFeat.trim());
            }
            features.addAll(specialFeaturesList);
        }

        if (isStopwordEnabled())
            features = _stopwordModule.applyStopwords(features);


        if (isStemmingEnabled())
            features = _stemmingModule.applyStemming(features);

        if (_addTFFeatures && features.size() > 0) {
            Collections.sort(features);
            if (features.size() == 1)
                features.set(0, features.get(0) + "_1");
            else {
                int count = 1;
                for (int i = 1; i < features.size(); ++i) {
                    if (features.get(i).equals(features.get(i - 1))) {
                        features.set(i - 1, features.get(i - 1) + "_" + count);
                        ++count;
                    } else {
                        features.set(i - 1, features.get(i - 1) + "_" + count);
                        count = 1;
                    }
                }
                features.set(features.size() - 1, features.get(features.size() - 1) + "_" + count);
            }
        }

        return features;
    }


    @Override
    public List<String> extractFeatures(String text) {
        return computeFeatures(text, -1);
    }


    /**
     * Extract the features from the specified text.
     *
     * @param text The text to be analyzed.
     * @return The set of features found in the text.
     */
    protected List<String> computeFeatures(String text) {
        return computeFeatures(text, -1);
    }


    /**
     * Extract the features from the specified text and returning at
     * maximum the first maxNumFeatures features.
     *
     * @param text        The text to be analyzed.
     * @param numFeatures The maximum number of features to return or -1 if you want
     *                    to extract all features.
     * @return The set of features found in the text.
     */
    protected abstract List<String> computeFeatures(String text, int numFeatures);

}
