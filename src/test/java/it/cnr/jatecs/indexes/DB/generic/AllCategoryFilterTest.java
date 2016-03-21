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

import it.cnr.jatecs.indexes.DB.interfaces.ICategoryDB;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveCategoryDBBuilder;

import org.junit.Test;

import static org.junit.Assert.*;

public class AllCategoryFilterTest extends BaseCategoryFilterTest {

	@Override
	protected AllCategoriesFilter getCategoryFilter() {
		return new AllCategoriesFilter();
	}
	
	@Test
	public void setCategorySetTest() {
		AllCategoriesFilter filter = getCategoryFilter();
		try {
			filter.setCategorySetType(null);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof NullPointerException);
		}
		
		filter.setCategorySetType(CategorySetType.ALL_CATEGORIES);
		assertTrue(filter.getCategorySetType() == CategorySetType.ALL_CATEGORIES);
		filter.setCategorySetType(CategorySetType.INTERNAL_CATEGORIES);
		assertTrue(filter.getCategorySetType() == CategorySetType.INTERNAL_CATEGORIES);
		filter.setCategorySetType(CategorySetType.LEAFS_CATEGORIES);
		assertTrue(filter.getCategorySetType() == CategorySetType.LEAFS_CATEGORIES);
	}

	
	
	@Test
	public void isAvailableAllCategoriesTest() {
		AllCategoriesFilter filter = getCategoryFilter();
		
		TroveCategoryDBBuilder builder = new TroveCategoryDBBuilder();
		builder.addCategory("cat1");
		builder.addCategory("cat2");
		builder.addCategory("cat3");
		builder.addCategory("cat4");
		builder.addCategory("cat5");
		builder.addCategory("cat6");
		builder.addCategory("cat7");
		builder.setParentCategory("cat4", "cat1");
		builder.setParentCategory("cat5", "cat1");
		builder.setParentCategory("cat6", "cat2");
		builder.setParentCategory("cat7", "cat6");
		ICategoryDB catsDB = builder.getCategoryDB();
		
		filter.setCategorySetType(CategorySetType.ALL_CATEGORIES);
		assertTrue(filter.isAvailable(catsDB, catsDB.getCategory("cat1")));
		assertTrue(filter.isAvailable(catsDB, catsDB.getCategory("cat2")));
		assertTrue(filter.isAvailable(catsDB, catsDB.getCategory("cat3")));
		assertTrue(filter.isAvailable(catsDB, catsDB.getCategory("cat4")));
		assertTrue(filter.isAvailable(catsDB, catsDB.getCategory("cat5")));
		
		filter.setCategorySetType(CategorySetType.INTERNAL_CATEGORIES);
		assertTrue(filter.isAvailable(catsDB, catsDB.getCategory("cat1")));
		assertTrue(filter.isAvailable(catsDB, catsDB.getCategory("cat2")));
		assertFalse(filter.isAvailable(catsDB, catsDB.getCategory("cat3")));
		assertFalse(filter.isAvailable(catsDB, catsDB.getCategory("cat4")));
		assertFalse(filter.isAvailable(catsDB, catsDB.getCategory("cat5")));
		assertTrue(filter.isAvailable(catsDB, catsDB.getCategory("cat6")));
		
		filter.setCategorySetType(CategorySetType.LEAFS_CATEGORIES);
		assertFalse(filter.isAvailable(catsDB, catsDB.getCategory("cat1")));
		assertFalse(filter.isAvailable(catsDB, catsDB.getCategory("cat2")));
		assertTrue(filter.isAvailable(catsDB, catsDB.getCategory("cat3")));
		assertTrue(filter.isAvailable(catsDB, catsDB.getCategory("cat4")));
		assertTrue(filter.isAvailable(catsDB, catsDB.getCategory("cat5")));
		assertTrue(filter.isAvailable(catsDB, catsDB.getCategory("cat7")));
	}
}
