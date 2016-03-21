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

package it.cnr.jatecs.classification;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class TreeBoostMaxGeneratorTest extends DefaultMaxGeneratorScoreTest {

	@Override
	protected TreeBoostMaxScoreGenerator getMaxScoreGenerator() {
		return new TreeBoostMaxScoreGenerator();
	}
	
	@Override
	@Before
	public void setupClassificationResults() {
		ClassifierRange crange = new ClassifierRange();
		crange.border = 0;
		crange.maximum = Double.MAX_VALUE;
		crange.minimum = -Double.MIN_VALUE;
		ClassificationScoreDB cl = new ClassificationScoreDB(4);
		cl.insertScore(0, (short) 0, 10, crange);
		cl.insertScore(1, (short) 0, 8, crange);
		cl.insertScore(2, (short) 0, -2.5, crange);
		cl.insertScore(2, (short) 1, 0.5, crange);
		cl.insertScore(2, (short) 2, -3, crange);
		cl.insertScore(3, (short) 0, 5.5, crange);
		cl.insertScore(3, (short) 1, 7, crange);
		cl.insertScore(3, (short) 2, crange.minimum, crange);
		classification = cl;
		
	}
	
	
	@Override
	@Test
	public void getMaximumPositiveScoreTest() {
		TreeBoostMaxScoreGenerator generator = getMaxScoreGenerator();
		assertTrue(generator.getMaximumPositiveScore(classification, (short)0) == 10.0);
		assertTrue(generator.getMaximumPositiveScore(classification, (short)1) == 7);
		assertTrue(generator.getMaximumPositiveScore(classification, (short)2) == 0);
	}
	
	@Override
	@Test
	public void getMaximumNegativeScoreTest() {
		TreeBoostMaxScoreGenerator generator = getMaxScoreGenerator();
		assertTrue(generator.getMaximumNegativeScore(classification, (short)0) == 2.5);
		assertTrue(generator.getMaximumNegativeScore(classification, (short)1) == 0.0);
		assertTrue(generator.getMaximumNegativeScore(classification, (short)2) == 3);
	}
}
