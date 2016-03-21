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

import org.junit.Test;

import static org.junit.Assert.*;

public class SetFeatureExtractorTest extends BaseFeatureExtractorTest {

	@Override
	protected SetFeatureExtractor getFeatureExtractor() {
		SetFeatureExtractor ext = new SetFeatureExtractor();
		BagOfWordsFeatureExtractor feat1 = new BagOfWordsFeatureExtractor();
		feat1.disableEntitiesSubstitution();
		feat1.disableSpecialTermsSubstitution();
		feat1.disableSpellChecking();
		feat1.disableStemming();
		feat1.disableStopwordRemoval();
		feat1.disableTFFeatures();
		ext.getExtractors().add(feat1);
		return ext;
	}

	
	@Test
	public void getExtractorsTest() {
		SetFeatureExtractor ext = getFeatureExtractor();
		assertTrue(ext.getExtractors() != null);
	}
	
	
	@Test
	public void extractFeaturesTest() {
		SetFeatureExtractor ext = getFeatureExtractor();
		ext.getExtractors().clear();
		
		BagOfWordsFeatureExtractor feat1 = new BagOfWordsFeatureExtractor();
		feat1.disableEntitiesSubstitution();
		feat1.disableSpecialTermsSubstitution();
		feat1.disableSpellChecking();
		feat1.disableStemming();
		feat1.disableStopwordRemoval();
		feat1.disableTFFeatures();
		ext.getExtractors().add(feat1);
		
		String text = "Hello world!";
		List<String> features = ext.extractFeatures(text);
		assertTrue(features.size() == 2);
		
		features = ext.extractFeatures(text, 1);
		assertTrue(features.size() == 1);
		
		CharsNGramFeatureExtractor feat2 = new CharsNGramFeatureExtractor();
		feat2.disableEntitiesSubstitution();
		feat2.disableSpecialTermsSubstitution();
		feat2.disableSpellChecking();
		feat2.disableStemming();
		feat2.disableStopwordRemoval();
		feat2.disableTFFeatures();
		feat2.setNGramSize(5);
		ext.getExtractors().add(feat2);
		
		features = ext.extractFeatures(text);
		assertTrue(features.size() == 9);
		
		features = ext.extractFeatures(text, 4);
		assertTrue(features.size() == 4);
		assertTrue(features.get(2).equals("hello"));
		assertTrue(features.get(3).equals("ello "));
	}
}
