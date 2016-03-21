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

import java.util.Hashtable;

import it.cnr.jatecs.indexing.preprocessing.EnglishPorterStemming;
import it.cnr.jatecs.indexing.preprocessing.EnglishStopword;

import org.junit.Test;
import static org.junit.Assert.*;

public abstract class FeatureExtractorTest extends BaseFeatureExtractorTest {

	@Override
	protected abstract FeatureExtractor getFeatureExtractor();

	@Test
	public void stopwordTest() {
		FeatureExtractor ext = getFeatureExtractor();

		ext.disableStopwordRemoval();
		assertTrue("The stopword is enabled", !ext.isStopwordEnabled());
		
		try {
			ext.enableStopwordRemoval(null);
			fail();
		} catch (Exception e) {
		}
		
		
		try {
			ext.enableStopwordRemoval(new EnglishStopword());
			assertTrue("The stopword is not enabled", ext.isStopwordEnabled());
		} catch (Exception e) {
			fail();
		}
		
	}
	
	
	@Test
	public void stemmingTest() {
		FeatureExtractor ext = getFeatureExtractor();

		ext.disableStemming();
		assertTrue("The stemming is enabled", !ext.isStemmingEnabled());
		
		try {
			ext.enableStemming(null);
			fail();
		} catch (Exception e) {
		}
		
		
		try {
			ext.enableStemming(new EnglishPorterStemming());
			assertTrue("The stemming is not enabled", ext.isStemmingEnabled());
		} catch (Exception e) {
			fail();
		}
		
	}
	
	
	@Test
	public void entitiesSubsititutionTest() {
		FeatureExtractor ext = getFeatureExtractor();

		ext.disableEntitiesSubstitution();
		assertTrue("The entities substitution is enabled", !ext.isEntitiesSubstitutionEnabled());
		
		try {
			ext.enableEntitiesSubstitution();
			assertTrue("The entities substitution is not enabled", ext.isEntitiesSubstitutionEnabled());
		} catch (Exception e) {
			fail();
		}
		
	}
	
	
	@Test
	public void specialTermsTest() {
		FeatureExtractor ext = getFeatureExtractor();

		ext.disableSpecialTermsSubstitution();
		assertTrue("The special terms is enabled", !ext.isSpecialTermsSubstitutionEnabled());
		
		try {
			ext.enableSpecialTermsSubstitution(null);
			fail();
		} catch (Exception e) {
		}
		
		
		try {
			Hashtable<String, String> dict = new Hashtable<String, String>();
			dict.put("pippo", "pippo");
			ext.enableSpecialTermsSubstitution(dict);
			assertTrue("The special terms is not enabled", ext.isSpecialTermsSubstitutionEnabled());
		} catch (Exception e) {
			fail();
		}
		
	}
	
	
	@Test
	public void spellCheckingTest() {
		FeatureExtractor ext = getFeatureExtractor();

		ext.disableSpellChecking();
		assertTrue("The spell checking is enabled", !ext.isSpellCheckingEnabled());
		
		try {
			ext.enableSpellChecking(null);
			fail();
		} catch (Exception e) {
		}
		
	}
	
	
	@Test
	public void tfFeaturesTest() {
		FeatureExtractor ext = getFeatureExtractor();

		ext.disableTFFeatures();
		assertTrue("The TF feature is enabled", !ext.isTFFeaturesEnabled());
		
		try {
			ext.enableTFFeatures();
			assertTrue("The TF feature is not enabled", ext.isTFFeaturesEnabled());
		} catch (Exception e) {
			fail();
		}
		
	}
}
