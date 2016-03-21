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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * This is a generic storage manager to store/retrieve data resources.
 *
 * @author Tiziano Fagni
 */
public interface IStorageManager {

    /**
     * Get a path separator optimized for the specific implementation of the
     * storage manager. Useful in resource name construction to better organize
     * resources inside storage manager.
     *
     * @return A path separator optimized for the specific implementation of the
     * storage manager.
     */
    public String getPathSeparator();

    /**
     * Open the storage manager.
     *
     * @throws IOException           Raised if the storage manager, for some reason, can not be
     *                               open.
     * @throws IllegalStateException Raised if the storage manager is already open.
     */
    public void open() throws IOException;

    /**
     * Close the storage manager to eventually deallocates any resources.
     *
     * @throws IllegalStateException Raised if the storage manager is not open.
     */
    public void close();

    /**
     * Indicate if the storage manager is or not open.
     *
     * @return True if the storage manager is open, false otherwise.
     */
    public boolean isOpen();

    /**
     * Indicate if the specified resource name is available in the storage
     * manager.
     *
     * @param resourceName The resource name to check.
     * @return True if the resource name is available, false otherwise.
     * @throws IllegalArgumentException Raised if the resource name is 'null' or empty.
     * @throws IllegalStateException    Raised if the storage manager is not open.
     */
    public boolean isResourceAvailable(String resourceName);

    /**
     * Get the list of available resource names declared in this storage
     * manager.
     *
     * @return The list of available resource names declared in this storage
     * manager.
     * @throws IllegalStateException Raised if the storage manager is not open.
     */
    public List<String> getAvailableResourceNames();

    /**
     * Get an input stream for the specified named resource. It is
     * responsibility of the caller to close the returned input stream when he
     * has finished using it.
     *
     * @param resourceName The named resource to access.
     * @return The input stream for the specified resource.
     * @throws NullPointerException     Raised if the specified resource name is 'null'.
     * @throws IllegalArgumentException Raised if the specified resource name is unknown.
     * @throws IllegalStateException    Raised if the storage manager is not open.
     */
    public InputStream getInputStreamForResource(String resourceName);

    /**
     * Get an output stream for the specified named resource. It is
     * responsibility of the caller to close the returned output stream when he
     * has finished using it. If the resource with the specified name does not
     * exist, it will be created. If the requested resource already exists, it
     * will be overwritten.
     *
     * @param resourceName The named resource to access.
     * @return The output stream for the specified resource.
     * @throws IllegalArgumentException Raised if the specified resource name is 'null' or empty.
     * @throws IllegalStateException    Raised if the storage manager is not open.
     */
    public OutputStream getOutputStreamForResource(String resourceName);

    /**
     * Delete the specified named resource from this storage manager.
     *
     * @param resourceName The resource to delete.
     * @throws IllegalStateException    Raised if the storage manager is not open.
     * @throws NullPointerException     Raised if the specified resource is 'null'.
     * @throws IllegalArgumentException Raised if the specified resource does not exists.
     */
    public void deleteResource(String resourceName);

    /**
     * Delete all declared resources from this storage manager.
     *
     * @throws IllegalStateException Raised if the storage manager is not open.
     */
    public void deleteAllResources();

}
