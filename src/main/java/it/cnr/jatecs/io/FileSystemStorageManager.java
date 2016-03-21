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
import java.util.ArrayList;
import java.util.List;

/**
 * Storage manager which stores all data on a specified directory on file
 * system.
 *
 * @author Tiziano Fagni
 */
public class FileSystemStorageManager implements IStorageManager {

    private final String baseDir;
    private boolean isOpen;
    private String _pathSeparator;

    /**
     * Create a new storage manager which store all its data in specified
     * "baseDir". If the specified base directory already exist and "overwrite"
     * is true then the directory will be recreated. If the directory already
     * exist and overwrite is false, the storage manager will use the old
     * available data. If the base directory does not exist, it will be created.
     *
     * @param baseDir   The base directory.
     * @param overwrite True to create an empty data directory, false otherwise.
     * @throws IllegalArgumentException Raised if the specified base directory is 'null' or empty
     *                                  string, or if the specified baseDir is a file.
     */
    public FileSystemStorageManager(String baseDir, boolean overwrite) {
        this(baseDir, overwrite, Os.pathSeparator());
    }

    public FileSystemStorageManager(String baseDir, boolean overwrite,
                                    String pathSeparator) {
        _pathSeparator = pathSeparator;
        if (baseDir == null || baseDir.isEmpty())
            throw new IllegalArgumentException(
                    "The specified base directory is invalid");

        File f = new File(baseDir);
        if (f.isFile())
            throw new IllegalArgumentException(
                    "The specified base directory is a regular file");

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

        isOpen = false;
    }

    @Override
    public String getPathSeparator() {
        return _pathSeparator;
    }

    @Override
    public void open() throws IOException {
        if (isOpen())
            throw new IllegalStateException(
                    "The storage manager is already open");

        isOpen = true;
    }

    @Override
    public void close() {
        if (!isOpen())
            throw new IllegalStateException("The storage manager is not open");

        isOpen = false;
    }

    @Override
    public boolean isOpen() {
        return isOpen;
    }

    @Override
    public boolean isResourceAvailable(String resourceName) {
        if (!isOpen())
            throw new IllegalStateException("The storage manager is not open");
        if (resourceName == null || resourceName.isEmpty())
            throw new IllegalArgumentException(
                    "The specified resource name is invalid");

        File f = new File(baseDir + Os.pathSeparator() + resourceName);
        if (f.exists() && f.isFile())
            return true;
        else
            return false;
    }

    @Override
    public List<String> getAvailableResourceNames() {
        if (!isOpen())
            throw new IllegalStateException("The storage manager is not open");

        ArrayList<String> ret = new ArrayList<String>(1000000);
        File f = new File(baseDir);
        File[] files = f.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile())
                ret.add(files[i].getName());
        }

        return ret;
    }

    @Override
    public InputStream getInputStreamForResource(String resourceName) {
        if (!isOpen())
            throw new IllegalStateException("The storage manager is not open");
        if (resourceName == null)
            throw new NullPointerException(
                    "The specified resource name is 'null'");
        if (!isResourceAvailable(resourceName))
            throw new IllegalArgumentException("The specified resource name <"
                    + resourceName + "> is unknown");

        try {
            BufferedInputStream is = new BufferedInputStream(
                    new FileInputStream(new File(baseDir + Os.pathSeparator()
                            + resourceName)));
            return is;
        } catch (Exception e) {
            throw new RuntimeException("Check the code. The resource <"
                    + resourceName
                    + "> should be available but, for some reason, it is not",
                    e);
        }
    }

    @Override
    public OutputStream getOutputStreamForResource(String resourceName) {
        if (!isOpen())
            throw new IllegalStateException("The storage manager is not open");
        if (resourceName == null || resourceName.isEmpty())
            throw new IllegalArgumentException(
                    "The specified resource name is invalid");
        try {
            File file = new File(baseDir + Os.pathSeparator()
                    + resourceName);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }

            String fname = baseDir + Os.pathSeparator()
                    + resourceName;
            new File(fname).getParentFile().mkdirs();
            BufferedOutputStream os = new BufferedOutputStream(
                    new FileOutputStream(file));
            return os;
        } catch (Exception e) {
            throw new RuntimeException("Creating or accessing the resource <"
                    + resourceName + "> on file system", e);
        }
    }

    @Override
    public void deleteResource(String resourceName) {
        if (!isOpen())
            throw new IllegalStateException("The storage manager is not open");
        if (resourceName == null)
            throw new NullPointerException(
                    "The specified resource name is 'null'");
        if (!isResourceAvailable(resourceName))
            throw new IllegalArgumentException("The specified resource name <"
                    + resourceName + "> is unknown");

        Os.delete(new File(baseDir + Os.pathSeparator() + resourceName));
    }

    @Override
    public void deleteAllResources() {
        if (!isOpen())
            throw new IllegalStateException("The storage manager is not open");

        // Delete and recreate base directory.
        File f = new File(baseDir);
        Os.deleteDirectory(f);
        f.mkdirs();
    }

    /**
     * Get the base directory on disk of this storage manager.
     *
     * @return The base directory on disk.
     */
    public String getBaseDir() {
        return baseDir;
    }

}
