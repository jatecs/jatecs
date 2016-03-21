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
import it.cnr.jatecs.indexes.DB.interfaces.IDocumentLanguageDB;
import it.cnr.jatecs.indexes.DB.interfaces.ILanguageDB;
import it.cnr.jatecs.indexes.utils.LanguageLabel;
import it.cnr.jatecs.io.IStorageManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

/**
 * @autor Alejandro Moreo
 * <p>
 * Indexes each document with its language label. Each document could
 * belong to various languages.
 */
public class TroveDocumentLanguageDB implements IDocumentLanguageDB {

    private IDocumentDB documentsDB;
    private ILanguageDB languagesDB;

    private HashMap<Integer, HashSet<LanguageLabel>> docIdLang;
    private HashMap<LanguageLabel, HashSet<Integer>> langDocID;

    public TroveDocumentLanguageDB(IDocumentDB documentsDB,
                                   ILanguageDB languagesDB) {
        this.documentsDB = documentsDB;
        this.languagesDB = languagesDB;
        docIdLang = new HashMap<Integer, HashSet<LanguageLabel>>();
        langDocID = new HashMap<LanguageLabel, HashSet<Integer>>();
    }

    public TroveDocumentLanguageDB() {
        this.documentsDB = null;
        this.languagesDB = new TroveLanguagesDB();
        docIdLang = new HashMap<Integer, HashSet<LanguageLabel>>();
        langDocID = new HashMap<LanguageLabel, HashSet<Integer>>();
    }

    public static void write(IStorageManager storageManager,
                             IDocumentLanguageDB documentLanguageDB, String mapname)
            throws IOException {
        StringBuilder st = new StringBuilder();
        Iterator<Integer> docs = documentLanguageDB.getDocumentsWithLangLabel();
        while (docs.hasNext()) {
            int nextdoc = docs.next();
            Iterator<LanguageLabel> it = documentLanguageDB
                    .getDocumentLanguages(nextdoc).iterator();
            st.append(nextdoc);
            while (it.hasNext())
                st.append("\t").append(it.next().toString());
            st.append("\n");
        }

        OutputStream out = storageManager.getOutputStreamForResource(mapname);
        out.write(st.toString().getBytes());
        out.close();
    }

    public static TroveDocumentLanguageDB read(IStorageManager storageManager,
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

        TroveLanguagesDB languagesDB = new TroveLanguagesDB();
        TroveDocumentLanguageDB map = new TroveDocumentLanguageDB(documentsDB,
                languagesDB);
        String[] lines = st.toString().split("\\n");
        for (int i = 0; i < lines.length; i++) {
            String[] parts = lines[i].split("\\t");
            int doc = Integer.parseInt(parts[0]);
            for (int j = 1; j < parts.length; j++) {
                LanguageLabel lang = LanguageLabel.valueOf(parts[j]);
                map.indexDocLang(doc, lang);
                languagesDB.addLanguage(lang);
            }
        }

        return map;

    }

    public void indexDocLang(int doc, LanguageLabel lang) {
        if (!docIdLang.containsKey(doc))
            docIdLang.put(doc, new HashSet<LanguageLabel>());
        docIdLang.get(doc).add(lang);
        if (!langDocID.containsKey(lang))
            langDocID.put(lang, new HashSet<Integer>());
        langDocID.get(lang).add(doc);
        languagesDB.addLanguage(lang);
    }

    public int getLanguagesCount() {
        return langDocID.keySet().size();
    }

    public LanguageLabel getDocumentLanguage(int doc) {
        return docIdLang.get(doc).iterator().next();
    }

    public HashSet<LanguageLabel> getDocumentLanguages(int doc) {
        return docIdLang.get(doc);
    }

    public boolean isMonolingualDocument(int doc) {
        HashSet<LanguageLabel> langs = getDocumentLanguages(doc);
        return langs != null && langs.size() == 1;
    }

    public boolean isMultilingualDocument(int doc) {
        HashSet<LanguageLabel> langs = getDocumentLanguages(doc);
        return langs != null && langs.size() > 1;
    }

    public HashSet<Integer> getDocumentsInLanguage(LanguageLabel lang) {
        return langDocID.get(lang);
    }

    public boolean isDocumentLabeled(int doc) {
        return docIdLang.containsKey(doc);
    }

    public Iterator<Integer> getDocumentsWithLangLabel() {
        return this.docIdLang.keySet().iterator();
    }

    @Override
    public boolean removeDocument(int doc) {
        if (docIdLang.containsKey(doc)) {
            this.docIdLang.remove(doc);

            Iterator<LanguageLabel> langs = langDocID.keySet().iterator();
            while (langs.hasNext()) {
                LanguageLabel next = langs.next();
                langDocID.get(next).remove(doc);
            }

            return true;
        } else
            return false;
    }

    public void setLanguagesDB(ILanguageDB languagesDB) {
        this.languagesDB = languagesDB;
    }

    @Override
    public IDocumentDB getDocumentDB() {
        return this.documentsDB;
    }

    public void setDocumentDB(IDocumentDB documentDB) {
        this.documentsDB = documentDB;
    }

    @Override
    public ILanguageDB getLanguageDB() {
        return this.languagesDB;
    }

    @Override
    public IDocumentLanguageDB cloneDB(IDocumentDB docs, ILanguageDB langs) {
        TroveDocumentLanguageDB clon = new TroveDocumentLanguageDB(docs, langs);
        clon.docIdLang.putAll(this.docIdLang);
        clon.langDocID.putAll(this.langDocID);
        return clon;
    }

}
