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

package it.cnr.jatecs.indexes.DB.generic;

import it.cnr.jatecs.indexes.DB.interfaces.ICategoryFilter;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveCategoryDBBuilder;

import org.junit.Test;

import static org.junit.Assert.*;

public abstract class BaseCategoryFilterTest {

	/**
	 * Get new instance of the category filter to test.
	 * 
	 * @return New instance of the category filter to test.
	 */
	protected abstract ICategoryFilter getCategoryFilter();
	
	
	@Test
	public void isAvailableIllegalArgumentsTest() {
		ICategoryFilter filter = getCategoryFilter();
		try {
			filter.isAvailable(null, (short) 0);
			fail();
		} catch(Exception e) {
			assertTrue(e instanceof NullPointerException);
		}
		
		try {
			TroveCategoryDBBuilder builder = new TroveCategoryDBBuilder();
			filter.isAvailable(builder.getCategoryDB(), (short) 0);
			fail();
		} catch(Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}
	}
}
