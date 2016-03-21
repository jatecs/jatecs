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

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * A storage manager which stores all its data in RAM memory.
 *
 * @author Tiziano Fagni
 */
public class RamStorageManager implements IStorageManager {

    private final HashMap<String, ResourceData> resources;
    private boolean isOpen;

    public RamStorageManager() {
        resources = new HashMap<String, RamStorageManager.ResourceData>();
    }

    @Override
    public String getPathSeparator() {
        return "_";
    }

    @Override
    public void open() throws IOException {
        if (isOpen())
            throw new IllegalStateException(
                    "The storage manager is already open");
        this.isOpen = true;
    }

    @Override
    public void close() {
        if (!isOpen())
            throw new IllegalStateException("The storage manager is not open");
        this.isOpen = false;
    }

    @Override
    public boolean isOpen() {
        return this.isOpen;
    }

    @Override
    public boolean isResourceAvailable(String resourceName) {
        if (!isOpen())
            throw new IllegalStateException("The storage manager is not open");
        if (resourceName == null || resourceName.isEmpty())
            throw new IllegalArgumentException("The specified resource name is invalid");
        return resources.containsKey(resourceName);
    }

    @Override
    public List<String> getAvailableResourceNames() {
        if (!isOpen())
            throw new IllegalStateException("The storage manager is not open");

        Iterator<String> it = resources.keySet().iterator();
        ArrayList<String> ret = new ArrayList<String>();
        while (it.hasNext()) {
            ret.add(it.next());
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
            throw new IllegalArgumentException("The resource name <"
                    + resourceName
                    + "> is not available on this storage manager");
        ByteArrayInputStream is = new ByteArrayInputStream(
                resources.get(resourceName).data.toByteArray());
        return is;
    }

    @Override
    public OutputStream getOutputStreamForResource(String resourceName) {
        if (!isOpen())
            throw new IllegalStateException("The storage manager is not open");
        if (resourceName == null || resourceName.isEmpty())
            throw new IllegalArgumentException(
                    "The specified resource name is 'null'");

        ResourceData rd = resources.get(resourceName);
        if (rd == null) {
            rd = new ResourceData();
            rd.data = new ByteArrayOutputStream();
            resources.put(resourceName, rd);
        } else {
            rd.data.reset();
        }
        return rd.data;
    }

    @Override
    public void deleteResource(String resourceName) {
        if (!isOpen())
            throw new IllegalStateException("The storage manager is not open");
        if (resourceName == null)
            throw new NullPointerException(
                    "The specified resource name is 'null'");
        if (!isResourceAvailable(resourceName))
            throw new IllegalArgumentException("The resource name <"
                    + resourceName
                    + "> is not available on this storage manager");

        resources.remove(resourceName);
    }

    @Override
    public void deleteAllResources() {
        if (!isOpen())
            throw new IllegalStateException("The storage manager is not open");

        resources.clear();
    }

    private static class ResourceData {

        /**
         * The data used by this resource.
         */
        ByteArrayOutputStream data;
    }

}
