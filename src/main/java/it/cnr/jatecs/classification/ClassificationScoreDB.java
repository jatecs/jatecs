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

package it.cnr.jatecs.classification;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

public class ClassificationScoreDB {

    private Vector<Hashtable<Short, ClassifierRangeWithScore>> _res;

    public ClassificationScoreDB(int documentCount) {
        _res = new Vector<Hashtable<Short, ClassifierRangeWithScore>>(documentCount);
        for (int i = 0; i < documentCount; i++) {
            _res.add(new Hashtable<Short, ClassifierRangeWithScore>());
        }
    }

    public static void write(String path, ClassificationScoreDB classification) throws IOException {
        File f = new File(path);
        f.getParentFile().mkdirs();
        FileWriter writer = new FileWriter(path);
        int numDoc = classification.getDocumentCount();
        writer.write(numDoc + "\n");
        for (int docID = 0; docID < numDoc; ++docID) {
            Set<Entry<Short, ClassifierRangeWithScore>> entries = classification.getDocumentScoresAsSet(docID);
            int entriesCount = entries.size();
            writer.write(entriesCount + "\n");
            Iterator<Entry<Short, ClassifierRangeWithScore>> iterator = entries.iterator();
            while (iterator.hasNext()) {
                Entry<Short, ClassifierRangeWithScore> next = iterator.next();
                writer.write(docID + " " + next.getKey() + " ");
                ClassifierRangeWithScore value = next.getValue();
                writer.write(value.score + " " + value.border + " " + value.minimum + " " + value.maximum + "\n");
            }
        }
        writer.close();
    }

    public static ClassificationScoreDB read(String path) throws IOException {
        ClassifierRange range = new ClassifierRange();
        BufferedReader file = new BufferedReader(new FileReader(path));
        int docCount = Integer.parseInt(file.readLine());
        ClassificationScoreDB classification = new ClassificationScoreDB(docCount);
        for (int i = 0; i < docCount; ++i) {
            int entriesCount = Integer.parseInt(file.readLine());
            for (int j = 0; j < entriesCount; ++j) {
                String line = file.readLine();
                Scanner scanner = new Scanner(line);
                scanner.useDelimiter(" ");
                int docID = Integer.parseInt(scanner.next());
                short cat = Short.parseShort(scanner.next());
                double score = Double.parseDouble(scanner.next());
                double border = Double.parseDouble(scanner.next());
                double minimum = Double.parseDouble(scanner.next());
                double maximum = Double.parseDouble(scanner.next());
                scanner.close();
                range.border = border;
                range.minimum = minimum;
                range.maximum = maximum;
                classification.insertScore(docID, cat, score, range);
            }
        }
        file.close();
        return classification;
    }

    public void insertScore(int docID, short catID, double score, ClassifierRange res) {
        Hashtable<Short, ClassifierRangeWithScore> r = _res.get(docID);
        ClassifierRangeWithScore sc = new ClassifierRangeWithScore();
        sc.border = res.border;
        sc.maximum = res.maximum;
        sc.minimum = res.minimum;
        sc.score = score;
        r.put(catID, sc);
    }

    public void removeScore(int docID, short catID) {
        Hashtable<Short, ClassifierRangeWithScore> r = _res.get(docID);
        r.remove(catID);
    }

    public Set<Entry<Short, ClassifierRangeWithScore>> getDocumentScoresAsSet(int docID) {
        Hashtable<Short, ClassifierRangeWithScore> r = _res.get(docID);
        return r.entrySet();
    }

    public Hashtable<Short, ClassifierRangeWithScore> getDocumentScoresAsHashtable(int docID) {
        Hashtable<Short, ClassifierRangeWithScore> r = _res.get(docID);
        return r;
    }

    public int getDocumentCount() {
        return _res.size();
    }
}
