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

import java.util.List;

import org.junit.Test;

public class BagOfWordsFeatureExtractorTest extends FeatureExtractorTest {

	@Override
	protected BagOfWordsFeatureExtractor getFeatureExtractor() {
		BagOfWordsFeatureExtractor ext = new BagOfWordsFeatureExtractor();
		
		ext.disableEntitiesSubstitution();
		ext.disableSpecialTermsSubstitution();
		ext.disableSpellChecking();
		ext.disableStemming();
		ext.disableStopwordRemoval();
		ext.disableTFFeatures();
		
		return ext;
	}
	
	@Test
	public void skipNumbersTest() {
		BagOfWordsFeatureExtractor ext = getFeatureExtractor();
		
		ext.setSkipNumbers(true);
		assertTrue(ext.isSkippingNumbers());
		
		ext.setSkipNumbers(false);
		assertTrue(!ext.isSkippingNumbers());
		
		String text = "12 posti per 44 candidati";
		List<String> features = ext.extractFeatures(text);
		assertTrue(features.get(0).equals("12"));
		assertTrue(features.get(3).equals("44"));
		
		ext.setSkipNumbers(true);
		features = ext.extractFeatures(text);
		assertTrue(features.get(0).equals("posti"));
		assertTrue(features.get(2).equals("candidati"));
	}
	
	
	@Test
	public void extractFeaturesTest() {
		BagOfWordsFeatureExtractor ext = getFeatureExtractor();
		
		ext.setSkipNumbers(true);
		String text = "Oggi, in data 07/11/2012, dichiaro che questo test, funziona|benissimo!!";
		List<String> features = ext.computeFeatures(text, 4);
		assertTrue(features.size() == 4);
		assertTrue(features.get(0).equals("oggi"));
		assertTrue(features.get(1).equals("in"));
		assertTrue(features.get(2).equals("data"));
		assertTrue(features.get(3).equals("dichiaro"));
		
		features = ext.extractFeatures(text);
		assertTrue(features.size() == 9);
		assertTrue(features.get(0).equals("oggi"));
		assertTrue(features.get(1).equals("in"));
		assertTrue(features.get(2).equals("data"));
		assertTrue(features.get(3).equals("dichiaro"));
		assertTrue(features.get(4).equals("che"));
		assertTrue(features.get(5).equals("questo"));
		assertTrue(features.get(6).equals("test"));
		assertTrue(features.get(7).equals("funziona"));
		assertTrue(features.get(8).equals("benissimo"));
		
		
		ext.setSkipNumbers(false);
		features = ext.computeFeatures(text, 5);
		assertTrue(features.size() == 5);
		assertTrue(features.get(0).equals("oggi"));
		assertTrue(features.get(1).equals("in"));
		assertTrue(features.get(2).equals("data"));
		assertTrue(features.get(3).equals("07"));
		assertTrue(features.get(4).equals("11"));
		
		
		features = ext.extractFeatures(text);
		assertTrue(features.size() == 12);
		assertTrue(features.get(0).equals("oggi"));
		assertTrue(features.get(1).equals("in"));
		assertTrue(features.get(2).equals("data"));
		assertTrue(features.get(3).equals("07"));
		assertTrue(features.get(4).equals("11"));
		assertTrue(features.get(5).equals("2012"));
		assertTrue(features.get(6).equals("dichiaro"));
		assertTrue(features.get(7).equals("che"));
		assertTrue(features.get(8).equals("questo"));
		assertTrue(features.get(9).equals("test"));
		assertTrue(features.get(10).equals("funziona"));
		assertTrue(features.get(11).equals("benissimo"));
	}
}
