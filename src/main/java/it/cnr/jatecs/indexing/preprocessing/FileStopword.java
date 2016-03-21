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

package it.cnr.jatecs.indexing.preprocessing;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Vector;

public class FileStopword implements Stopword {

    /**
     * The dictionary of stopwords.
     */
    private LinkedHashMap<String, String> _stopwords;

    /**
     * The path of file that contains the stopwords to use.
     */
    private String _stopwordsFilename;

    /**
     * Stop word removal module
     *
     * @param filename The path of file that contains the stopwords to use
     * @throws IOException
     */
    public FileStopword(String filename) throws IOException {
        super();

        setStopwordsFile(filename);
    }

    public List<String> applyStopwords(List<String> text) {

        Vector<String> good = new Vector<String>();

        for (int i = 0; i < text.size(); i++) {
            String feature = text.get(i);
            if (!_stopwords.containsKey(feature)) {
                // This feature is good. Save it.
                good.add(feature);
            }
        }

        String[] toReturn = new String[good.size()];
        toReturn = good.toArray(toReturn);

        return good;
    }

    /**
     * @see it.cnr.jatecs.indexing.phase.JatecsModule#initData()
     */
    protected void initData() throws IOException {
        // Construct a new empty dictionary.
        _stopwords = new LinkedHashMap<String, String>(500);

        BufferedReader fr = null;

        // Read the stopwords from a file.
        fr = new BufferedReader(new InputStreamReader(new FileInputStream(
                _stopwordsFilename), "ISO-8859-1"));

        BufferedReader reader = new BufferedReader(fr);

        String line = reader.readLine();
        while (line != null) {
            line = line.trim();
            // Assume a "row" in the file IS a "stopword".
            if (!_stopwords.containsKey(line)) {
                _stopwords.put(line, line);
            }

            // Read next line.
            line = reader.readLine();
        }

        // Close the file reader.
        reader.close();
    }

    /**
     * Set the filename of the file containing the stopwords to use.
     *
     * @param filename The path to file containing the stopwords.
     */
    public void setStopwordsFile(String filename) throws IOException {
        _stopwordsFilename = filename;
        initData();
    }

    /**
     * Get the path of file containing the stopwords used in this module.
     *
     * @return The path of file containing stopwords.
     */
    public String stopwordsFile() {
        return _stopwordsFilename;
    }
}
