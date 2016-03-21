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

public abstract class BaseFeatureExtractorTest {

	/**
	 * Get the new instance of the feature extractor to test.
	 * 
	 * @return New instance of the feature extractor to test.
	 */
	protected abstract IFeatureExtractor getFeatureExtractor();
	
	
	@Test
	public void extractFeaturesTextNullTest() {
		IFeatureExtractor ext = getFeatureExtractor();
		
		try {
			ext.extractFeatures(null);
			fail("An assertion shoud be raised about the 'null' specified text");
		} catch(Exception e) {
		}
		
		try {
			ext.extractFeatures(null, 2);
			fail("An assertion shoud be raised about the 'null' specified text");
		} catch(Exception e) {
		}
		
		try {
			List<String> features = ext.extractFeatures("Ciao sono Tiziano!", 2);
			assertTrue("The set of features is 'null'", features != null );
		} catch(Exception e) {
			fail("An assertion shoud not be raised!");
		}
	}
	
	
	@Test
	public void extractFeaturesMaxNumberTest() {
		String text = "Ciao mi chiamo Tiziano!";
		IFeatureExtractor ext = getFeatureExtractor();
		List<String> features = ext.extractFeatures(text,2);
		assertTrue("The number of extracted features is not correct", features.size() <= 2);
		features = ext.extractFeatures(text,3);
		assertTrue("The number of extracted features is not correct", features.size() <= 3);
		int maxNumberFeatures = ext.extractFeatures(text).size();
		features = ext.extractFeatures(text,0);
		assertTrue("The number of extracted features is not correct", features.size() == maxNumberFeatures);
		features = ext.extractFeatures(text,-1);
		assertTrue("The number of extracted features is not correct", features.size() == maxNumberFeatures);
		features = ext.extractFeatures(text,-10);
		assertTrue("The number of extracted features is not correct", features.size() == maxNumberFeatures);
	}
}
