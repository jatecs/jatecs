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

import static org.junit.Assert.*;

import it.cnr.jatecs.indexing.preprocessing.EnglishPorterStemming;
import it.cnr.jatecs.indexing.preprocessing.EnglishStopword;

import java.util.List;

import org.junit.Test;

public class CharsNGramsFeatureExtractorTest extends FeatureExtractorTest {

	@Override
	protected CharsNGramFeatureExtractor getFeatureExtractor() {
		CharsNGramFeatureExtractor ext = new CharsNGramFeatureExtractor();
		
		ext.disableEntitiesSubstitution();
		ext.disableSpecialTermsSubstitution();
		ext.disableSpellChecking();
		ext.disableStemming();
		ext.disableStopwordRemoval();
		ext.disableTFFeatures();
		
		return ext;
	}
	
	@Test
	public void ngramsSizeTest() {
		CharsNGramFeatureExtractor ext = getFeatureExtractor();
		
		ext.setNGramSize(2);
		assertTrue(ext.getNGramSize() == 2);
		ext.setNGramSize(5);
		assertTrue(ext.getNGramSize() == 5);
	}
	
	
	@Test
	public void extractFeaturesTest() {
		CharsNGramFeatureExtractor ext = getFeatureExtractor();
		
		ext.setNGramSize(3);
		String text = "Oggi, che giorno!";
		List<String> features = ext.extractFeatures(text);
		assertTrue(features.size() == 13);
		assertTrue(features.get(0).equals("ogg"));
		assertTrue(features.get(1).equals("ggi"));
		assertTrue(features.get(2).equals("gi "));
		assertTrue(features.get(3).equals("i c"));
		assertTrue(features.get(4).equals(" ch"));
		assertTrue(features.get(5).equals("che"));
		assertTrue(features.get(6).equals("he "));
		assertTrue(features.get(7).equals("e g"));
		assertTrue(features.get(8).equals(" gi"));
		assertTrue(features.get(9).equals("gio"));
		assertTrue(features.get(10).equals("ior"));
		assertTrue(features.get(11).equals("orn"));
		assertTrue(features.get(12).equals("rno"));
		
		
		text = "Che Giorno!";
		ext.setNGramSize(5);
		features = ext.extractFeatures(text);
		assertTrue(features.size() == 6);
		assertTrue(features.get(0).equals("che g"));
		assertTrue(features.get(1).equals("he gi"));
		assertTrue(features.get(2).equals("e gio"));
		assertTrue(features.get(3).equals(" gior"));
		assertTrue(features.get(4).equals("giorn"));
		assertTrue(features.get(5).equals("iorno"));
		
	}
	
	
	
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
			fail("The stopword is not available");
		} catch (Exception e) {
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
			fail("The stemming is not available");
		} catch (Exception e) {
		}
		
	}
}
