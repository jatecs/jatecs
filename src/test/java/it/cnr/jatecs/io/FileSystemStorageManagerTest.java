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

package it.cnr.jatecs.io;

import java.io.OutputStream;

import it.cnr.jatecs.utils.Os;

import org.junit.Test;

import static org.junit.Assert.*;

public class FileSystemStorageManagerTest extends BaseStorageManagerTest {

	@Override
	protected FileSystemStorageManager getStorageManager() {
		FileSystemStorageManager sm = new FileSystemStorageManager(Os.getTemporaryDirectory()+"fsTest", true);
		return sm;
	}
	
	@Override
	@Test
	public void getPathSeparatorTest() {
		// TODO Auto-generated method stub
		super.getPathSeparatorTest();
		
		assertTrue(storageManager.getPathSeparator().equals(Os.pathSeparator()));
	}
	
	
	
	@Test
	public void overwriteTest() {
		try {
			FileSystemStorageManager storageManager = getStorageManager();
			storageManager.open();
			OutputStream os = storageManager
					.getOutputStreamForResource("pippo");
			os.close();
			os = storageManager
					.getOutputStreamForResource("pluto");
			os.close();
			storageManager.close();
			FileSystemStorageManager sm = new FileSystemStorageManager(Os.getTemporaryDirectory()+"fsTest", false);
			sm.open();
			assertTrue(sm.getAvailableResourceNames().size() == 2);
			assertTrue(sm.isResourceAvailable("pippo"));
			assertTrue(sm.isResourceAvailable("pluto"));
			sm.close();
		} catch (Exception e) {
			fail();
		}
		
	}

}
