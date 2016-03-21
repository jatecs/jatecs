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


import org.junit.Test;

import static org.junit.Assert.*;

public abstract class BaseMaxScoreGeneratorTest {

	/**
	 * Get a new max score generator instance to test.
	 * 
	 * @return A new max score generator instance to test.
	 */
	protected abstract IMaxScoreGenerator getMaxScoreGenerator();

	@Test
	public void getMaximumPositiveScoreExceptionsTest() {
		IMaxScoreGenerator generator = getMaxScoreGenerator();

		try {
			generator.getMaximumPositiveScore(null, (short) 1);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof NullPointerException);
		}
		
		try {
			ClassifierRange crange = new ClassifierRange();
			crange.border = 5;
			crange.maximum = 100;
			crange.minimum = -100;
			ClassificationScoreDB cl = new ClassificationScoreDB(5);
			cl.insertScore(0, (short) 0, 10, crange);
			generator.getMaximumPositiveScore(cl, (short) 1);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}
		
		try {
			ClassifierRange crange = new ClassifierRange();
			crange.border = 5;
			crange.maximum = 100;
			crange.minimum = -100;
			ClassificationScoreDB cl = new ClassificationScoreDB(5);
			cl.insertScore(0, (short) 0, 10, crange);
			generator.getMaximumPositiveScore(cl, (short) 0);
		} catch (Exception e) {
			fail();
		}
	}

	
	@Test
	public void getMaximumNegativeScoreExceptionsTest() {
		IMaxScoreGenerator generator = getMaxScoreGenerator();

		try {
			generator.getMaximumNegativeScore(null, (short) 1);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof NullPointerException);
		}
		
		try {
			ClassifierRange crange = new ClassifierRange();
			crange.border = 5;
			crange.maximum = 100;
			crange.minimum = -100;
			ClassificationScoreDB cl = new ClassificationScoreDB(5);
			cl.insertScore(0, (short) 0, 10, crange);
			generator.getMaximumNegativeScore(cl, (short) 1);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}
		
		try {
			ClassifierRange crange = new ClassifierRange();
			crange.border = 5;
			crange.maximum = 100;
			crange.minimum = -100;
			ClassificationScoreDB cl = new ClassificationScoreDB(5);
			cl.insertScore(0, (short) 0, 10, crange);
			generator.getMaximumNegativeScore(cl, (short) 0);
		} catch (Exception e) {
			fail();
		}
	}
}
