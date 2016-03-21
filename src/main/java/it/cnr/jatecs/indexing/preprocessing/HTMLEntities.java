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

/**
 *
 */
package it.cnr.jatecs.indexing.preprocessing;

import java.io.*;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author fagni
 */
public class HTMLEntities {

    private Hashtable<String, Entity> _entities;
    private Pattern _pattern;

    public HTMLEntities() {
        _entities = new Hashtable<String, Entity>();

        // Load default entities file from jar resource.
        InputStream is = HTMLEntities.class.getResourceAsStream("/configuration/html-entities.txt");
        assert (is != null);
        setEntityFile(is);
        try {
            is.close();
        } catch (IOException e) {
            throw new RuntimeException("Closing jar resource", e);
        }

        _pattern = Pattern.compile("\\&[^\\s]+;");
    }

    public void setEntityFile(InputStream is) {

        _entities.clear();
        try {
            // Read the entities from a file.
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            String line = reader.readLine();
            while (line != null) {

                if (line.equals("")) {
                    line = reader.readLine();
                    continue;
                }

                String[] tokens = line.split("\t");
                for (int i = 0; i < tokens.length; i = i + 2) {
                    Entity entity = new Entity();
                    entity.entity = tokens[i];
                    entity.symbol = tokens[i + 1];
                    _entities.put(entity.entity, entity);
                }

                // Read the next line.
                line = reader.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setEntityFile(String filename) {

        BufferedInputStream bis = null;
        try {
            bis = new BufferedInputStream(new FileInputStream(filename));
            setEntityFile(bis);
        } catch (FileNotFoundException e1) {
            throw new RuntimeException(e1);
        } finally {
            if (bis != null)
                try {
                    bis.close();
                } catch (IOException e) {
                    throw new RuntimeException("Closing entity file", e);
                }
        }

    }

    public String replaceEntities(String text) {
        String toReturn = null;

        Matcher m = _pattern.matcher(text);

        StringBuffer sb = new StringBuffer();

        while (m.find()) {
            String matched = m.group();

            if (!_entities.containsKey(matched))
                continue;

            Entity entity = _entities.get(matched);
            m.appendReplacement(sb, entity.symbol);
        }

        m.appendTail(sb);
        toReturn = sb.toString();

        return toReturn;
    }

    class Entity {
        String entity;
        String symbol;
    }

}
