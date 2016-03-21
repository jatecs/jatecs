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

package it.cnr.jatecs.indexing.corpus.JRCAcquis;

import it.cnr.jatecs.indexes.DB.troveCompact.TroveCategoryDBBuilder;
import it.cnr.jatecs.indexes.utils.LanguageLabel;
import it.cnr.jatecs.indexing.corpus.CorpusDocument;
import it.cnr.jatecs.indexing.corpus.CorpusDocumentLang;
import it.cnr.jatecs.indexing.corpus.SetType;
import it.cnr.jatecs.utils.JatecsLogger;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JrcAcquisCorpusReaderCrossLingual extends JrcAcquisCorpusReader {

    public static int NumDocumentsSkiped = 0;
    public static int NumDocumentsReaded = 0;

    private int percentageTrainingData = 70;

    private HashSet<LanguageLabel> _languageFilter;
    private HashSet<LanguageLabel> _trainingLanguageFilter;
    private HashSet<LanguageLabel> _testingLanguageFilter;

	/*
     * public JrcAcquisCorpusReaderCrossLingual(TroveCategoriesDBBuilder
	 * builder) { super(builder); }
	 */

    public JrcAcquisCorpusReaderCrossLingual(TroveCategoryDBBuilder builder) {
        super(builder);
        _languageFilter = new HashSet<LanguageLabel>();
        _trainingLanguageFilter = new HashSet<LanguageLabel>();
        _testingLanguageFilter = new HashSet<LanguageLabel>();
    }

	/*
	 * public void randomPermutationFileOrder() { List<String> filesList =
	 * Arrays.asList(_files); Collections.shuffle(filesList); _files =
	 * filesList.toArray(new String[filesList.size()]); }
	 */

    private static int getYearFromPath(String path) {
        Pattern p = Pattern.compile("[/\\\\]([12][09][0-9]{2})[/\\\\]");
        Matcher m = p.matcher(path);
        if (m.find()) {
            String yearst = m.group(1);
            return Integer.parseInt(yearst);
        }
        return -1;
    }

    @Override
    public CorpusDocument next() {
        int trainDocs = numTrainingDocs();
        int numDocs = numDocs();
        if (getDocumentSetType() == SetType.TRAINING && _index >= trainDocs)
            return null;
        if (getDocumentSetType() == SetType.TEST && _index < trainDocs)
            begin();

        if (_index >= numDocs)
            return null;
        CorpusDocumentLang doclang = null;
        boolean done = false;
        while (!done && _index < numDocs) {

            String fname = _files[_index];
            LanguageLabel lang = LanguageLabel.valueOf(fname.substring(
                    fname.lastIndexOf('-') + 1).replaceAll("\\.xml", ""));

            if ((getDocumentSetType() == SetType.TRAINING && _trainingLanguageFilter
                    .contains(lang))
                    || (getDocumentSetType() == SetType.TEST && _testingLanguageFilter
                    .contains(lang))) {
                String xmlDoc;
                try {
                    xmlDoc = readFile(fname);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                _index++;

                CorpusDocument doc = generateDocument(xmlDoc);
                if (doc != null || _index >= _files.length) {
                    NumDocumentsReaded++;
                    if (!doc.categories().isEmpty()) {
                        doclang = new CorpusDocumentLang(doc.documentType(),
                                doc.name(), doc.content(), doc.categories(),
                                lang);

                        done = true;
                        continue;
                    } else {
                        NumDocumentsSkiped++;
                    }
                }
            } else
                _index++;
        }

        // System.out.println("" + (_index*100.0/numDocs));

        return doclang;
    }

    @Override
    public void begin() {
        if (getDocumentSetType() == SetType.TRAINING)
            _index = 0;
        else
            _index = numTrainingDocs();
    }

    private int numTrainingDocs() {
        return (int) Math.round(numDocs() * (percentageTrainingData / 100.0d));
    }

    private int numDocs() {
        return _files.length;
    }

    public void setPercentajeTrainingData(int percentageTrainingData) {
        if (percentageTrainingData < 1 || percentageTrainingData > 99)
            throw new IllegalArgumentException(
                    "The percentage of training data is not valid.");
        this.percentageTrainingData = percentageTrainingData;
    }

    public void filterByYear(int from, int to) {
        ArrayList<String> filesList = new ArrayList<String>(
                Arrays.asList(_files));
        boolean changes = false;
        for (int i = 0; i < filesList.size(); ) {
            int year = getYearFromPath(filesList.get(i));
            boolean remove = false;
            if ((from != -1 && year < from) || (to != -1 && year > to)) {
                remove = true;
                changes = true;
            }
            if (remove) {
                filesList.remove(i);
            } else {
                i++;
            }
        }
        if (changes) {
            _files = filesList.toArray(new String[filesList.size()]);
        }
    }

    public void filterIncompleteViews(int numLanguages) {
        JatecsLogger
                .execution()
                .println(
                        "Filtering out views with incomplete represetantions among languages");
        HashMap<String, ArrayList<String>> view_docpath = new HashMap<String, ArrayList<String>>();

        // groups all input paths by view name
        for (int i = 0; i < _files.length; i++) {
            String path_i = _files[i];
            File file = new File(path_i);
            String name = file.getName().replace(".xml", "");
            // the name of the view is the document name minus the posfix
            // e.g.:jrc31997D0182-es.xml -> jrc31997D0182
            String viewname = name.substring(0, name.lastIndexOf("-"));
            if (!view_docpath.containsKey(viewname)) {
                view_docpath.put(viewname, new ArrayList<String>());
            }
            view_docpath.get(viewname).add(path_i);
        }

        // keep all views with complete representations among languages (cluster
        // size == numLanguages)
        Iterator<String> views = view_docpath.keySet().iterator();
        ArrayList<String> filesList = new ArrayList<String>();
        while (views.hasNext()) {
            String view = views.next();
            if (view_docpath.get(view).size() == numLanguages) {
                filesList.addAll(view_docpath.get(view));
            }
        }

        JatecsLogger.execution()
                .println(
                        "Remaining documents " + filesList.size() + "/"
                                + _files.length);

        _files = filesList.toArray(new String[filesList.size()]);

    }

    public void addTrainingLanguage(LanguageLabel lang) {
        this._trainingLanguageFilter.add(lang);
        this._languageFilter.add(lang);
    }

    public void addTestLanguage(LanguageLabel lang) {
        this._testingLanguageFilter.add(lang);
        this._languageFilter.add(lang);

    }

    public HashSet<LanguageLabel> getLanguageFilter() {
        return _languageFilter;
    }

    public int numLanguagesAcepted() {
        return _languageFilter.size();
    }

}
