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

package it.cnr.jatecs.indexes.DB.troveCompact;

import it.cnr.jatecs.indexes.DB.interfaces.IDocumentDB;
import it.cnr.jatecs.indexes.DB.interfaces.IParallelDB;
import it.cnr.jatecs.io.IStorageManager;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

public class TroveParallelDB implements IParallelDB {

    private IDocumentDB _documentsDB;
    private TroveParallelCorpusType _CorpusType;
    private HashMap<Integer, HashSet<Integer>> _parallel;

    public TroveParallelDB(IDocumentDB documentsDB) {
        _documentsDB = documentsDB;
        _parallel = new HashMap<Integer, HashSet<Integer>>();
    }

    public static void write(IParallelDB parallelDB,
                             IStorageManager storageManager, String mapname) throws IOException {
        StringBuilder st = new StringBuilder();
        st.append(parallelDB.getParallelCorpusType().toString()).append("\n");
        Iterator<Integer> docs = parallelDB.getAllParallelDocuments();
        while (docs.hasNext()) {
            int nextdoc = docs.next();
            Iterator<Integer> it = parallelDB.getParallelDocuments(nextdoc)
                    .iterator();
            st.append(nextdoc);
            while (it.hasNext())
                st.append("\t").append(it.next().toString());
            st.append("\n");
        }

        OutputStream out = storageManager.getOutputStreamForResource(mapname);
        out.write(st.toString().getBytes());
        out.close();
    }

    public static TroveParallelDB read(IStorageManager storageManager,
                                       String mapname, IDocumentDB documentsDB) throws IOException {
        InputStream in = storageManager.getInputStreamForResource(mapname);
        byte[] buffer = new byte[1024];
        StringBuilder st = new StringBuilder();
        int bread = -1;
        do {
            bread = in.read(buffer);
            if (bread > 0) {
                st.append(new String(buffer, 0, bread));
            }
        } while (bread > 0);

        in.close();

        TroveParallelDB parallelDB = new TroveParallelDB(documentsDB);
        String[] lines = st.toString().split("\\n");
        TroveParallelCorpusType corpusType = TroveParallelCorpusType
                .valueOf(lines[0].trim());
        parallelDB.setParallelCorpusType(corpusType);
        for (int i = 1; i < lines.length; i++) {
            String[] parts = lines[i].split("\\t");
            int doc = Integer.parseInt(parts[0]);
            for (int j = 1; j < parts.length; j++) {
                int parDoc = Integer.parseInt(parts[j]);
                parallelDB.addParallelDocs(doc, parDoc);
            }
        }

        return parallelDB;
    }

    @Override
    public IDocumentDB getDocumentDB() {
        return _documentsDB;
    }

    @Override
    public boolean areParallelDocuments(int doc1, int doc2) {
        return hasParallelVersion(doc1) && _parallel.get(doc1).contains(doc2);
    }

    @Override
    public List<Integer> getParallelDocuments(int doc) {
        ArrayList<Integer> all = new ArrayList<Integer>();
        if (hasParallelVersion(doc))
            all.addAll(_parallel.get(doc));
        return all;
    }

    @Override
    public boolean hasParallelVersion(int doc) {
        return _parallel.containsKey(doc) && !_parallel.get(doc).isEmpty();
    }

    public void addParallelDocs(int doc1, int doc2) {
        if (!_parallel.containsKey(doc1) && !_parallel.containsKey(doc2)) {
            HashSet<Integer> docsParallels = new HashSet<Integer>();
            addAndAttach(docsParallels, doc1);
            addAndAttach(docsParallels, doc2);
        } else if (!_parallel.containsKey(doc1) && _parallel.containsKey(doc2)) {
            addAndAttach(_parallel.get(doc2), doc1);
        } else if (_parallel.containsKey(doc1) && !_parallel.containsKey(doc2)) {
            addAndAttach(_parallel.get(doc1), doc2);
        } else if (_parallel.containsKey(doc1) && _parallel.containsKey(doc2)) {
            HashSet<Integer> docsParallels = new HashSet<Integer>();
            docsParallels.addAll(_parallel.get(doc1));
            docsParallels.addAll(_parallel.get(doc2));
            for (Integer updatedoc : docsParallels) {
                _parallel.put(updatedoc, docsParallels);
            }
        }
    }

    private void addAndAttach(HashSet<Integer> set, int el) {
        set.add(el);
        _parallel.put(el, set);
    }

    @Override
    public void removeDocument(int doc) {
        if (hasParallelVersion(doc)) {
            // remove the docid from all parallel documents
            _parallel.get(doc).remove(doc);

            // remove the entry for doc
            _parallel.remove(doc);
        }
    }

    @Override
    public Iterator<Integer> getAllParallelDocuments() {
        return _parallel.keySet().iterator();
    }

    @Override
    public TroveParallelCorpusType getParallelCorpusType() {
        return _CorpusType;
    }

    @Override
    public void setParallelCorpusType(TroveParallelCorpusType corpusType) {
        _CorpusType = corpusType;
    }

    @Override
    public ArrayList<ArrayList<Integer>> getViewsClusters() {
        ArrayList<ArrayList<Integer>> clusters = new ArrayList<ArrayList<Integer>>();

        IIntIterator docs = this.getDocumentDB().getDocuments();
        HashSet<Integer> docsAdded = new HashSet<Integer>();
        while (docs.hasNext()) {
            int doc = docs.next();
            if (!docsAdded.contains(doc)) {
                ArrayList<Integer> cluster = new ArrayList<Integer>();
                if (hasParallelVersion(doc)) {
                    cluster.addAll(getParallelDocuments(doc));
                } else {
                    cluster.add(doc);
                }
                clusters.add(cluster);
                docsAdded.addAll(cluster);
            }
        }

        return clusters;
    }

}
