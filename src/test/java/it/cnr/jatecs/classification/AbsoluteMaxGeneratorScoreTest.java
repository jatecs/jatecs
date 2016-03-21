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

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class AbsoluteMaxGeneratorScoreTest extends BaseMaxScoreGeneratorTest {

	private ClassificationScoreDB classification;
	
	@Override
	protected AbsoluteMaxScoreGenerator getMaxScoreGenerator() {
		return new AbsoluteMaxScoreGenerator();
	}


	@Before
	public void setupClassificationResults() {
		ClassifierRange crange = new ClassifierRange();
		crange.border = 5;
		crange.maximum = 100;
		crange.minimum = -100;
		ClassificationScoreDB cl = new ClassificationScoreDB(3);
		cl.insertScore(0, (short) 0, 8, crange);
		cl.insertScore(1, (short) 0, 10, crange);
		cl.insertScore(2, (short) 0, 2.5, crange);
		cl.insertScore(2, (short) 1, 5.5, crange);
		cl.insertScore(1, (short) 1, -1, crange);
		classification = cl; 
	}
	
	
	@Test
	public void getScoreTest() {
		AbsoluteMaxScoreGenerator generator = getMaxScoreGenerator();
		assertTrue(generator.getMaximumPositiveScore(classification, (short)0) == 5.0);
		assertTrue(generator.getMaximumPositiveScore(classification, (short)0) == generator.getMaximumNegativeScore(classification, (short)0));
		assertTrue(generator.getMaximumPositiveScore(classification, (short)1) == 6);
		assertTrue(generator.getMaximumPositiveScore(classification, (short)1) == generator.getMaximumNegativeScore(classification, (short)1));
	}
	
}
