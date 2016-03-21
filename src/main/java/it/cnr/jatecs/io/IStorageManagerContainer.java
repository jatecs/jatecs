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

import java.io.IOException;
import java.util.Iterator;

/**
 * A generic container of storage managers.
 *
 * @author Tiziano Fagni
 */
public interface IStorageManagerContainer {

    /**
     * Get the storage manager with the given ID. The caller must ensure that before using
     * the returned storage manager, its is correctly opened (see {@link IStorageManager#open()},
     * {@link IStorageManager#close()} and {@link IStorageManager#isOpen()} methods).
     *
     * @param storageManagerID The requested storage manager ID.
     * @return The requested storage manager.
     * @throws IllegalArgumentException Raised if the requested storage manager ID is 'null',
     *                                  empty string or not existent in this container.
     * @throws IOException              Raised if some I/O error occurs.
     */
    public IStorageManager getStorageManager(String storageManagerID) throws IOException;


    /**
     * Indicate if the requested storage manager ID is available on this container.
     *
     * @param storageManagerID The storage manager ID.
     * @return True if the requested storage manager ID is available, false otherwise.
     * @throws IllegalArgumentException Raised if the requested storage manager ID is 'null' or
     *                                  empty string.
     */
    public boolean isStorageManagerAvailable(String storageManagerID);


    /**
     * Delete the specified storage manager ID from this container.
     *
     * @param storageManagerID The storage manager ID.
     * @throws IllegalArgumentException Raised if the requested storage manager ID is 'null' or
     *                                  empty string or not existent in this container.
     * @throws IOException              Raised if some I/O error occurs.
     */
    public void deleteStorageManager(String storageManagerID) throws IOException;


    /**
     * Create a new storage manager with the given ID. If the storage manager already exists, it
     * will be overwritten.
     *
     * @param storageManagerID The storage manager ID
     * @return The created storage manager. The caller must ensure that before using
     * the returned storage manager, its is correctly opened (see {@link IStorageManager#open()},
     * {@link IStorageManager#close()} and {@link IStorageManager#isOpen()} methods).
     * @throws IllegalArgumentException Raised if the requested storage manager ID is 'null' or
     *                                  empty string.
     * @throws IOException              Raised if some I/O error occurs.
     */
    public IStorageManager createStorageManager(String storageManagerID) throws IOException;


    /**
     * Get the set of all available storage manager IDs stored in this container.
     *
     * @return The set of all available storage manager IDs.
     */
    public Iterator<String> getAvailableStorageManagerNames();


    /**
     * Delete all available storage managers stored on this container.
     */
    public void deleteAllStorageManagers();
}
