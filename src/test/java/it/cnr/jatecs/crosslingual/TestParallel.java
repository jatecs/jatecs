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

package it.cnr.jatecs.crosslingual;

import static org.junit.Assert.*;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveParallelDB;

import org.junit.Before;
import org.junit.Test;

public class TestParallel {
	
	TroveParallelDB par;

	@Before
	public void init(){
		par = new TroveParallelDB(null);
	}
	
	@Test
	public void test1(){
		par.addParallelDocs(1, 2);
		assertTrue(par.areParallelDocuments(1, 2));
		assertTrue(!par.areParallelDocuments(10, 12));
		
		par.addParallelDocs(2, 3);
		assertTrue(par.areParallelDocuments(1, 3));
		
		par.addParallelDocs(10, 11);
		par.addParallelDocs(11, 12);
		par.addParallelDocs(12, 13);
		
		assertTrue(par.areParallelDocuments(10, 13));
		assertTrue(!par.areParallelDocuments(1, 10));
		
		par.addParallelDocs(1, 12);
		assertTrue(par.areParallelDocuments(1, 10));
		assertTrue(par.areParallelDocuments(2, 13));
		assertTrue(par.areParallelDocuments(3, 11));
		
		par.removeDocument(10);
		assertTrue(!par.hasParallelVersion(10));
		assertTrue(!par.areParallelDocuments(10, 11));
	}
}
