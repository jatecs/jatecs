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

package it.cnr.jatecs.classification.treeboost;

import gnu.trove.TIntArrayList;
import gnu.trove.TShortArrayList;
import gnu.trove.TShortIntHashMap;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexing.similarity.EuclideanDistance;
import it.cnr.jatecs.indexing.similarity.ISimilarityFunction;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.LRUMap;
import it.cnr.jatecs.utils.Os;
import it.cnr.jatecs.utils.iterators.TIntArrayListIterator;
import it.cnr.jatecs.utils.iterators.TShortArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;
import it.cnr.jatecs.weighting.mmap.MemoryMappedWeighting3DBuilder;

import java.io.*;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

public class BestKNNNegativesChooser implements INegativesChooser {

    protected IIndex _index;
    protected ISimilarityFunction _similarity;
    protected Vector<TreeSet<Item>> _best;
    protected TShortIntHashMap _numNegatives;
    protected int _numNearestPositive;
    protected MemoryMappedWeighting3DBuilder _distances;
    protected int _cacheSize;
    /**
     * The directory where to store the informaiobn about best positives for a
     * given category a a given negative document ID.
     */
    protected String _storingDir;
    /**
     * The maximum number of nearest positives to consider for each category and
     * negative document ID.
     */
    protected int _maxNumNearest;
    protected int _numGrouped;

    public BestKNNNegativesChooser() {
        _similarity = new EuclideanDistance();
        _best = null;
        _numNearestPositive = 5;
        _distances = null;
        _cacheSize = 500000;
        _storingDir = "KNNNegativesChooser";
        _maxNumNearest = 20;
        _numGrouped = 10;
    }

    public void setNumGroupedCategories(int numGrouped) {
        if (numGrouped < 1)
            numGrouped = 1;
        _numGrouped = numGrouped;
    }

    public void setStoringDir(String storingDir) {
        _storingDir = storingDir;
    }

    public void setNumNearestPositive(int numNearestPositive) {
        _numNearestPositive = numNearestPositive;
    }

    public void setMaxNumNearestPositive(int maxNum) {
        _maxNumNearest = maxNum;
    }

    public void initialize(IIndex index) {

        _index = index;

        JatecsLogger.status().println(
                "Starting KNN analyzer for negatives selection policy");
        JatecsLogger.status().println(
                "The index contains "
                        + _index.getDocumentDB().getDocumentsCount()
                        + " document(s), "
                        + _index.getFeatureDB().getFeaturesCount()
                        + " feature(s) and "
                        + _index.getCategoryDB().getCategoriesCount()
                        + " category(ies).");

        // Next compute the number of negatives that must be choosed for each
        // category (based on sibling nodes).
        _numNegatives = new TShortIntHashMap(_index.getCategoryDB()
                .getCategoriesCount());
        IShortIterator catsIt = _index.getCategoryDB().getCategories();
        while (catsIt.hasNext()) {
            short catID = catsIt.next();
            int numNeg = 0;

            IShortIterator parents = _index.getCategoryDB()
                    .getParentCategories(catID);
            if (parents.hasNext()) {
                while (parents.hasNext()) {
                    short parentID = parents.next();
                    numNeg += _index.getClassificationDB()
                            .getCategoryDocumentsCount(parentID);
                }
            } else {
                numNeg = _index.getDocumentDB().getDocumentsCount();
            }

            // Now consider positive documents for this category.
            numNeg = numNeg
                    - _index.getClassificationDB().getCategoryDocumentsCount(
                    catID);

            _numNegatives.put(catID, numNeg);
        }

        // Compute distance between positives and negatives in all categories.
        computeDistances(_numNegatives);
    }

    public void release() {
        _best = null;
    }

    protected void writeNearestPositives(File dir, short catID,
                                         TreeSet<Item>[][] struct, int maxNumNegatives) throws IOException {
        File f = new File(dir.getAbsolutePath() + Os.pathSeparator());
        if (!f.exists())
            f.mkdirs();
        f = new File(f.getAbsolutePath() + Os.pathSeparator() + catID + ".dat");

        DataOutputStream os = new DataOutputStream(new BufferedOutputStream(
                new FileOutputStream(f)));

        // Number of documents for each category.
        os.writeInt(struct[0].length);

        for (int i = 0; i < struct[catID].length; i++) {
            TreeSet<Item> nearestPositive = (TreeSet<Item>) struct[catID][i];
            if (nearestPositive.size() == 0) {
                os.writeInt(-1);
                continue;
            } else
                // Write ID of valid document.
                os.writeInt(i);

            // Number of negatives stored.
            os.writeInt(nearestPositive.size());

            Iterator<Item> it = nearestPositive.iterator();
            while (it.hasNext()) {
                Item item = it.next();
                os.writeInt(item.docID);
                os.writeDouble(item.score);
            }
        }

        os.close();
    }

    protected void readNearestPositives(File dir, short catID,
                                        TreeSet<Item>[][] struct) throws IOException {
        File f = new File(dir.getAbsolutePath() + Os.pathSeparator() + catID
                + ".dat");

        DataInputStream is = new DataInputStream(new BufferedInputStream(
                new FileInputStream(f)));

        // Read number of documents stored.
        int numDocuments = is.readInt();

        for (int i = 0; i < numDocuments; i++) {
            struct[catID][i] = new TreeSet<Item>();

            int id = is.readInt();
            if (id == -1)
                continue;

            // Read number of best positives for each negative.
            int numPositives = is.readInt();

            TreeSet<Item> ts = struct[catID][i];
            int numItems = numPositives;
            while (numItems > 0) {
                int docID = is.readInt();
                double distance = is.readDouble();

                Item item = new Item();
                item.docID = docID;
                item.score = distance;
                ts.add(item);

                numItems--;
            }
        }

        is.close();
    }

    protected void computeDistances(int docID1, int docID2, IIndex index,
                                    LRUMap<Double> cache, TShortIntHashMap numNegatives,
                                    TreeSet<Item>[][] struct, IShortIterator cats) {
        String key = "" + docID1 + " " + docID2;

        while (cats.hasNext()) {
            short catID = cats.next();

            boolean doc1B = _index.getClassificationDB().hasDocumentCategory(
                    docID1, catID);
            boolean doc2B = _index.getClassificationDB().hasDocumentCategory(
                    docID2, catID);

            if (doc1B == doc2B)
                continue;

            double distance = 0;
            if (cache.containsKey(key))
                distance = cache.get(key);
            else {
                distance = _similarity.compute(docID1, docID2, _index);
                cache.put(key, distance);
            }

            int posDocID = (doc1B == true) ? docID1 : docID2;
            int negDocID = (doc1B == true) ? docID2 : docID1;

            Item item = new Item();
            item.docID = posDocID;
            item.score = distance;

            struct[catID][negDocID].add(item);
            if (struct[catID][negDocID].size() > _maxNumNearest)
                struct[catID][negDocID].remove(struct[catID][negDocID].last());
        }
    }

    protected void computeAllNegatives(IIndex _index, LRUMap<Double> cache,
                                       TShortIntHashMap numNegatives, TreeSet<Item>[][] struct) {
        IIntIterator it1 = _index.getDocumentDB().getDocuments();
        IIntIterator it2 = _index.getDocumentDB().getDocuments();

        Vector<IShortIterator> groups = new Vector<IShortIterator>();

        IShortIterator cats = _index.getCategoryDB().getCategories();
        TShortArrayList l = new TShortArrayList();
        groups.add(new TShortArrayListIterator(l));
        int cont = 1;
        while (cats.hasNext()) {
            short cat = cats.next();
            l.add(cat);
            if ((cont % _numGrouped) == 0) {
                l = new TShortArrayList();
                groups.add(new TShortArrayListIterator(l));
            }

            cont++;
        }

        for (int i = 0; i < groups.size(); i++) {
            IShortIterator categories = groups.get(i);
            categories.begin();
            try {
                // DEBUG
                System.out.println("Leggo nella directory: " + _storingDir);

                while (categories.hasNext()) {
                    short catID = categories.next();
                    readNearestPositives(new File(_storingDir), catID, struct);
                }
            } catch (Exception e) {
                // DEBUG
                System.out.println("Entro nella eccezione...");

                categories.begin();
                int count = 0;
                it1.begin();
                while (it1.hasNext()) {
                    int docID1 = it1.next();

                    it2.begin();
                    while (it2.hasNext()) {
                        int docID2 = it2.next();
                        if (docID2 > docID1)
                            break;

                        categories.begin();
                        computeDistances(docID1, docID2, _index, cache,
                                numNegatives, struct, categories);
                    }

                    if ((count % 25) != 0)
                        JatecsLogger.status().print(".");
                    else
                        JatecsLogger.status().print("" + count);

                    count++;
                    if ((count % 50) == 0)
                        JatecsLogger.status().println("");

                    // DEBUG
                    // if (count == 50)
                    // break;
                }

                JatecsLogger.status()
                        .print("Writing best negatives to disk...");
                categories.begin();
                try {
                    writeNegativesToDisk(struct, categories);
                } catch (IOException e1) {
                    throw new RuntimeException(e1);
                }
                JatecsLogger.status().println("done.");
            }

            // For each category select the best negatives.
            categories.begin();
            computeBestNegatives(numNegatives, struct, categories);
        }
    }

    protected void writeDebugInfo(TreeSet<Item>[][] struct,
                                  IShortIterator categories) {
        try {
            while (categories.hasNext()) {
                short catID = categories.next();
                String catName = _index.getCategoryDB().getCategoryName(catID);
                for (int i = 0; i < struct[0].length; i++) {
                    TreeSet<Item> set = struct[catID][i];
                    if (set.size() == 0) {
                        assert (_index.getClassificationDB()
                                .hasDocumentCategory(i, catID));
                        continue;
                    }

                    File f = new File(Os.getTemporaryDirectory()
                            + Os.pathSeparator() + "KNN" + Os.pathSeparator()
                            + catName);
                    f.mkdirs();

                    String path = Os.getTemporaryDirectory()
                            + Os.pathSeparator() + "KNN" + Os.pathSeparator()
                            + catName + Os.pathSeparator() + i;
                    PrintWriter pw = new PrintWriter(new BufferedOutputStream(
                            new FileOutputStream(path)));

                    Iterator<Item> it = set.iterator();
                    while (it.hasNext()) {
                        Item item = it.next();
                        String line = item.score + "\t" + item.docID;
                        pw.println(line);
                    }
                    pw.close();
                }
            }
        } catch (Exception e) {

        }
    }

    protected void computeDistances(TShortIntHashMap numNegatives) {
        _best = new Vector<TreeSet<Item>>();
        for (short i = 0; i < _index.getCategoryDB().getCategoriesCount(); i++)
            _best.add(null);

        LRUMap<Double> cache = new LRUMap<Double>(_cacheSize);

        @SuppressWarnings("unchecked")
        TreeSet<Item>[][] struct = new TreeSet[_index.getCategoryDB()
                .getCategoriesCount()][_index.getDocumentDB()
                .getDocumentsCount()];
        for (short c = 0; c < _index.getCategoryDB().getCategoriesCount(); c++) {
            for (int d = 0; d < _index.getDocumentDB().getDocumentsCount(); d++) {
                struct[c][d] = new TreeSet<Item>();
            }
        }

        // Fill struct with all distances between documents.
        computeAllNegatives(_index, cache, numNegatives, struct);
    }

    protected void writeNegativesToDisk(TreeSet<Item>[][] struct,
                                        IShortIterator cats) throws IOException {
        while (cats.hasNext()) {
            short catID = cats.next();
            writeNearestPositives(new File(_storingDir), catID, struct,
                    _maxNumNearest);
        }

    }

    protected TreeSet<Item>[][] readNegativesFromDisk() throws IOException {
        @SuppressWarnings("unchecked")
        TreeSet<Item>[][] struct = new TreeSet[_index.getCategoryDB()
                .getCategoriesCount()][_index.getDocumentDB()
                .getDocumentsCount()];

        for (short catID = 0; catID < _index.getCategoryDB()
                .getCategoriesCount(); catID++) {

            readNearestPositives(new File(_storingDir), catID, struct);

        }

        return struct;
    }

    protected void computeBestNegatives(TShortIntHashMap numNegatives,
                                        TreeSet<Item>[][] struct, IShortIterator cats) {
        while (cats.hasNext()) {
            short catID = cats.next();
            TreeSet<Item> ts = new TreeSet<Item>();

            for (int docID = 0; docID < struct[0].length; docID++) {
                TreeSet<Item> set = (TreeSet<Item>) struct[catID][docID];
                if (set.size() == 0)
                    continue;

                int numItems = _numNearestPositive;
                double denominator = (set.size() > numItems) ? numItems : set
                        .size();

                double score = 0;
                while (set.size() > 0 && numItems > 0) {
                    Item it = set.first();
                    assert (_index.getClassificationDB().hasDocumentCategory(
                            it.docID, catID));
                    set.remove(set.first());
                    score += it.score;
                    numItems--;
                }

                // Trash all items.
                struct[catID][docID] = null;

                Item item = new Item();
                item.docID = docID;
                item.score = score / denominator;

                ts.add(item);
                if (ts.size() > numNegatives.get(catID))
                    ts.remove(ts.last());
            }

            _best.set(catID, ts);
        }
    }

    public TIntArrayListIterator selectNegatives(String category) {

        short catID = _index.getCategoryDB().getCategory(category);

        TreeSet<Item> best = _best.get(catID);
        assert (best != null);

        TIntArrayList neg = new TIntArrayList();
        Iterator<Item> it = best.iterator();
        while (it.hasNext()) {
            Item docS = it.next();
            assert (!neg.contains(docS.docID));
            neg.add(docS.docID);
        }

        neg.sort();

        return new TIntArrayListIterator(neg);
    }

    protected class Item implements Comparable<Item> {
        int docID;
        double score;

        public int compareTo(Item o) {
            if (score < o.score)
                return -1;
            else if (score > o.score)
                return 1;
            else {
                if (docID < o.docID)
                    return -1;
                else if (docID == o.docID)
                    return 0;
                else
                    return 1;
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Item))
                return false;

            Item i = (Item) obj;
            return (compareTo(i) == 0);
        }

    }

}
