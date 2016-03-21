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

package it.cnr.jatecs.utils;

import java.io.File;
import java.net.JarURLConnection;
import java.net.URL;

public class FileUtils {

    /**
     * Resolve the specified resource name with a filename which can be used
     * with {@link File} class.
     *
     * @param resourceName The resource name to be resolved.
     * @return The corresponding filename.
     */
    public static String resolveFilenameFromResource(String resourceName) {
        try {
            URL url = FileUtils.class.getResource(resourceName);
            String fileName;
            if (url.getProtocol().equals("file")) {
                fileName = url.toURI().getPath();
            } else if (url.getProtocol().equals("jar")) {
                JarURLConnection jarUrl = (JarURLConnection) url
                        .openConnection();
                fileName = jarUrl.toString();
                fileName = fileName.substring(fileName.indexOf("jar:"));
            } else {
                throw new IllegalArgumentException("Not a file");
            }

            return fileName;
        } catch (Exception e) {
            return null;
        }
    }
}
