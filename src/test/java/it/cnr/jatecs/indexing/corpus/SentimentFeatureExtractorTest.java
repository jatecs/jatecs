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

public class SentimentFeatureExtractorTest extends BaseFeatureExtractorTest  {

	@Override
	protected SentimentFeatureExtractor getFeatureExtractor() {
		SentimentFeatureExtractor feat1 = new SentimentFeatureExtractor();
		return feat1;
	}
	
	
	@Test
	public void testExtractFeaturesString() {
		SentimentFeatureExtractor ext = getFeatureExtractor();

		String testString = "The hotel is beautiful";
		List<String> feats = ext.extractFeatures(testString);
		
		assertTrue(feats.contains("beautiful hotel"));
		assertTrue(feats.contains("positive hotel"));
		assertTrue(feats.size()==2);

		testString = "The hotel is hardly beautiful";
		feats = ext.extractFeatures(testString);
		assertTrue(feats.contains("decrease positive hotel"));
		assertTrue(feats.contains("hardly beautiful hotel"));
		assertTrue(feats.contains("not hardly"));
		assertTrue(feats.contains("not beautiful"));
		assertTrue(feats.size()==4);
	}

}
