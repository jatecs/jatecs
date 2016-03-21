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
import java.util.HashMap;
import java.util.Iterator;

/**
 * A RAM container of RAM storage managers. The storage manager instances are
 * stored internally in an hash map. This implies that two subsequent calls of
 * {@link #getStorageManager(String)} with the same ID will return the same
 * storage manager instance.
 *
 * @author Tiziano Fagni
 */
public class RamStorageManagerContainer implements IStorageManagerContainer {

    private final HashMap<String, IStorageManager> storageManagers;

    public RamStorageManagerContainer() {
        this.storageManagers = new HashMap<String, IStorageManager>();
    }


    @Override
    public IStorageManager getStorageManager(String storageManagerID)
            throws IOException {
        if (storageManagerID == null)
            throw new IllegalArgumentException("The storage manager ID is 'null'");
        if (storageManagerID.isEmpty())
            throw new IllegalArgumentException("The storage manager ID is empty string");
        if (!storageManagers.containsKey(storageManagerID))
            throw new IllegalArgumentException("The storage manager ID is not available in this container: " + storageManagerID);

        return storageManagers.get(storageManagerID);
    }

    @Override
    public boolean isStorageManagerAvailable(String storageManagerID) {
        if (storageManagerID == null)
            throw new IllegalArgumentException("The storage manager ID is 'null'");
        if (storageManagerID.isEmpty())
            throw new IllegalArgumentException("The storage manager ID is empty string");

        return storageManagers.containsKey(storageManagerID);
    }

    @Override
    public void deleteStorageManager(String storageManagerID)
            throws IOException {
        if (storageManagerID == null)
            throw new IllegalArgumentException("The storage manager ID is 'null'");
        if (storageManagerID.isEmpty())
            throw new IllegalArgumentException("The storage manager ID is empty string");
        if (!storageManagers.containsKey(storageManagerID))
            throw new IllegalArgumentException("The storage manager ID is not available in this container: " + storageManagerID);

        storageManagers.remove(storageManagerID);
    }

    @Override
    public IStorageManager createStorageManager(String storageManagerID)
            throws IOException {
        if (storageManagerID == null)
            throw new IllegalArgumentException("The storage manager ID is 'null'");
        if (storageManagerID.isEmpty())
            throw new IllegalArgumentException("The storage manager ID is empty string");

        if (storageManagers.containsKey(storageManagerID))
            storageManagers.remove(storageManagerID);
        RamStorageManager storageManager = new RamStorageManager();
        storageManagers.put(storageManagerID, storageManager);
        return storageManager;
    }

    @Override
    public Iterator<String> getAvailableStorageManagerNames() {
        return storageManagers.keySet().iterator();
    }

    @Override
    public void deleteAllStorageManagers() {
        storageManagers.clear();
    }

}
