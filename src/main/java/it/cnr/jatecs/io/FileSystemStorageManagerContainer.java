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

import it.cnr.jatecs.utils.Os;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;

/**
 * A file system container of storage managers. The storage manager instances
 * (each corresponding to a specific subfolder of base dir on disk) are stored
 * internally in an hash map. This implies that two subsequent calls of
 * {@link #getStorageManager(String)} with the same ID will return the same
 * storage manager instance.
 *
 * @author Tiziano Fagni
 */
public class FileSystemStorageManagerContainer implements
        IStorageManagerContainer {

    private final static String STORAGE_MANAGER_FLAG_FILE = ".storage_manager_";
    private final String baseDir;
    private final HashMap<String, FileSystemStorageManager> storageManagers;

    /**
     * Create new instance of container with specified base directory. If overwrite is
     * true, the container will be reset to an empty container.
     *
     * @param baseDir   The base directory.
     * @param overwrite True if the container must be reset to an empty container,
     *                  false otherwise..
     */
    public FileSystemStorageManagerContainer(String baseDir, boolean overwrite) {
        if (baseDir == null)
            throw new IllegalArgumentException("The base directory is 'null'");
        if (baseDir.isEmpty())
            throw new IllegalArgumentException(
                    "The base directory is empty string");

        File f = new File(baseDir);
        if (f.isFile())
            throw new IllegalArgumentException(
                    "The specified base directory is a regular file");

        this.storageManagers = new HashMap<String, FileSystemStorageManager>(1000000);
        this.baseDir = baseDir;
        if (f.exists()) {
            if (overwrite) {
                // Delete and recreate base directory.
                Os.deleteDirectory(f);
                f.mkdirs();
            }
        } else {
            f.mkdirs();
        }

        // Load all available storage managers.
        loadAvailableStorageManagers(f);
    }

    private void loadAvailableStorageManagers(File baseDir) {
        // Load all available storage managers.
        File[] files = baseDir.listFiles();
        if (files == null)
            return;
        for (int i = 0; i < files.length; i++) {
            File fsm = new File(files[i] + Os.pathSeparator()
                    + STORAGE_MANAGER_FLAG_FILE);
            if (fsm.exists()) {
                // Load ID of storage manager.
                try {
                    DataInputStream is = null;
                    try {
                        is = new DataInputStream(
                                new FileInputStream(fsm));
                        String storageManagerID = is.readUTF();

                        FileSystemStorageManager storageManager = new FileSystemStorageManager(
                                files[i].getAbsolutePath() + Os.pathSeparator() + "data", false);
                        storageManagers.put(storageManagerID, storageManager);
                    } finally {
                        if (is != null)
                            is.close();
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Bug in code", e);
                }

            }
        }
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

        FileSystemStorageManager storageManager = storageManagers.get(storageManagerID);
        storageManagers.remove(storageManagerID);
        Os.deleteDirectory(new File(storageManager.getBaseDir()));
    }

    @Override
    public IStorageManager createStorageManager(String storageManagerID)
            throws IOException {
        if (storageManagerID == null)
            throw new IllegalArgumentException("The storage manager ID is 'null'");
        if (storageManagerID.isEmpty())
            throw new IllegalArgumentException("The storage manager ID is empty string");

        if (storageManagers.containsKey(storageManagerID))
            deleteStorageManager(storageManagerID);
        try {
            File fsm = new File(baseDir + Os.pathSeparator() + storageManagerID + Os.pathSeparator() + STORAGE_MANAGER_FLAG_FILE);
            fsm.getParentFile().mkdirs();
            DataOutputStream os = null;
            try {
                os = new DataOutputStream(
                        new FileOutputStream(fsm));
                os.writeUTF(storageManagerID);

                fsm = new File(baseDir + Os.pathSeparator() + storageManagerID + Os.pathSeparator() + "data");
                FileSystemStorageManager storageManager = new FileSystemStorageManager(fsm.toString(), true);
                storageManagers.put(storageManagerID, storageManager);
            } finally {
                if (os != null)
                    os.close();
            }
        } catch (Exception e) {
            throw new RuntimeException("Bug in code", e);
        }

        return storageManagers.get(storageManagerID);
    }

    @Override
    public Iterator<String> getAvailableStorageManagerNames() {
        return storageManagers.keySet().iterator();
    }

    @Override
    public void deleteAllStorageManagers() {
        storageManagers.clear();
    }

    /**
     * Get the base directory used by this container.
     *
     * @return The base directory used by this container.
     */
    public String getBaseDir() {
        return baseDir;
    }

}
