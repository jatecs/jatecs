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


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.Test;
import static org.junit.Assert.*;

public abstract class BaseStorageManagerTest {

	protected IStorageManager storageManager;

	/**
	 * Get a new instance of storage manager to test.
	 * 
	 * @return The storage manager to test.
	 */
	protected abstract IStorageManager getStorageManager();

	@Test
	public void getPathSeparatorTest() {
		storageManager = getStorageManager();

		assertTrue(storageManager.getPathSeparator() != null);
	}

	@Test
	public void openCloseTest() {
		storageManager = getStorageManager();
		assertTrue(!storageManager.isOpen());
		try {
			storageManager.open();
			assertTrue(storageManager.isOpen());
		} catch (Exception e) {
			fail();
		}

		try {
			storageManager.open();
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalStateException);
		}

		storageManager.close();
		assertTrue(!storageManager.isOpen());

		try {
			storageManager.close();
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalStateException);
		}
	}

	@Test
	public void resourceAvailableTest() {
		storageManager = getStorageManager();
		try {
			storageManager.isResourceAvailable("pippo");
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalStateException);
		}

		try {
			storageManager.open();
		} catch (Exception e) {
			fail();
		}

		try {
			storageManager.isResourceAvailable(null);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		try {
			storageManager.isResourceAvailable("");
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		assertTrue(!storageManager.isResourceAvailable("pippo"));
		try {
			OutputStream os = storageManager
					.getOutputStreamForResource("pippo");
			os.close();
		} catch (Exception e) {
			fail();
		}
		assertTrue(storageManager.isResourceAvailable("pippo"));
		storageManager.deleteResource("pippo");
		assertTrue(!storageManager.isResourceAvailable("pippo"));

		storageManager.close();
	}

	@Test
	public void getAvailableResourceNamesTest() {
		storageManager = getStorageManager();
		try {
			storageManager.getAvailableResourceNames();
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalStateException);
		}

		try {
			storageManager.open();
		} catch (Exception e) {
			fail();
		}

		try {
			assertTrue(storageManager.getAvailableResourceNames() != null);
			assertTrue(storageManager.getAvailableResourceNames().size() == 0);
			OutputStream os = storageManager
					.getOutputStreamForResource("pippo");
			os.close();
			assertTrue(storageManager.getAvailableResourceNames().size() == 1);
			os = storageManager.getOutputStreamForResource("pluto");
			os.close();
			assertTrue(storageManager.getAvailableResourceNames().size() == 2);
			storageManager.deleteResource("pippo");
			storageManager.deleteResource("pluto");
			assertTrue(storageManager.getAvailableResourceNames().size() == 0);

			storageManager.close();
		} catch (Exception e) {
			fail();
		}
	}

	@Test
	public void getInputStreamForResourceTest() {
		storageManager = getStorageManager();
		try {
			storageManager.getInputStreamForResource("pippo");
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalStateException);
		}

		try {
			storageManager.open();
			assertTrue(storageManager.getAvailableResourceNames().size() == 0);
		} catch (Exception e) {
			fail();
		}

		try {
			OutputStream os = storageManager.getOutputStreamForResource("pippo");
			os.close();
			InputStream is = storageManager.getInputStreamForResource("pippo");
			assertTrue(is.read() == -1);
			is.close();
		} catch (Exception e) {
			fail();
		}

		try {
			OutputStream os = storageManager
					.getOutputStreamForResource("pippo");
			DataOutputStream dos = new DataOutputStream(os);
			dos.writeUTF("Hello!");
			dos.writeInt(100);
			os.close();
		} catch (Exception e) {
			fail();
		}

		try {
			InputStream is = storageManager.getInputStreamForResource("pippo");
			DataInputStream dis = new DataInputStream(is);
			String text = dis.readUTF();
			assertTrue(text.equals("Hello!"));
			int val = dis.readInt();
			assertTrue(val == 100);
			is.close();
		} catch (Exception e) {
			fail();
		}

		storageManager.deleteResource("pippo");
		try {
			storageManager.getInputStreamForResource("pippo");
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}
	}

	@Test
	public void getOuputStreamForResourceTest() {
		storageManager = getStorageManager();

		try {
			storageManager.open();
			assertTrue(storageManager.getAvailableResourceNames().size() == 0);
		} catch (Exception e) {
			fail();
		}

		try {
			storageManager.getOutputStreamForResource(null);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		try {
			storageManager.getOutputStreamForResource("");
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		try {
			OutputStream os = storageManager
					.getOutputStreamForResource("pippo");
			assertTrue(os != null);
			DataOutputStream dos = new DataOutputStream(os);
			dos.writeUTF("Hello!");
			dos.close();
			InputStream is = storageManager.getInputStreamForResource("pippo");
			DataInputStream dis = new DataInputStream(is);
			assertTrue(dis.readUTF().equals("Hello!"));
			dis.close();
			os = storageManager
					.getOutputStreamForResource("pippo");
			assertTrue(os != null);
			dos = new DataOutputStream(os);
			dos.writeUTF("world");
			dos.close();
			is = storageManager.getInputStreamForResource("pippo");
			dis = new DataInputStream(is);
			assertTrue(dis.readUTF().equals("world"));
			dis.close();
		} catch (Exception e) {
			fail();
		}
		
		
	}

	@Test
	public void deleteResourceTest() {
		storageManager = getStorageManager();

		try {
			storageManager.deleteResource("pippo");
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalStateException);
		}

		try {
			storageManager.open();
			assertTrue(storageManager.getAvailableResourceNames().size() == 0);
		} catch (Exception e) {
			fail();
		}

		try {
			storageManager.deleteResource(null);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof NullPointerException);
		}

		try {
			storageManager.deleteResource("pippo");
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		try {
			OutputStream os = storageManager.getOutputStreamForResource("pippo");
			os.close();
			storageManager.deleteResource("pippo");
		} catch (Exception e) {
			fail();
		}
	}

	@Test
	public void deleteAllResourcesTest() {
		storageManager = getStorageManager();

		try {
			storageManager.deleteAllResources();
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof IllegalStateException);
		}
		
		try {
			storageManager.open();
			assertTrue(storageManager.getAvailableResourceNames().size() == 0);
		} catch (Exception e) {
			fail();
		}
		
		try {
			OutputStream os = storageManager.getOutputStreamForResource("pippo");
			os.close();
			os = storageManager.getOutputStreamForResource("pluto");
			os.close();
			assertTrue(storageManager.getAvailableResourceNames().size() == 2);
		} catch (Exception e) {
			fail();
		}
		storageManager.deleteAllResources();
		assertTrue(storageManager.getAvailableResourceNames().size() == 0);
	}
}
