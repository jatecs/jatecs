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
package it.cnr.jatecs.indexing.corpus.RCV2;

import it.cnr.jatecs.indexes.DB.interfaces.ICategoryDB;
import it.cnr.jatecs.indexes.utils.LanguageLabel;
import it.cnr.jatecs.indexing.corpus.CorpusDocument;
import it.cnr.jatecs.indexing.corpus.CorpusDocumentLang;
import it.cnr.jatecs.indexing.corpus.CorpusReader;
import it.cnr.jatecs.indexing.corpus.RCV1.CategoryType;
import it.cnr.jatecs.indexing.corpus.SetType;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.Os;

import java.io.*;
import java.util.*;

public class RCV2CorpusReader extends CorpusReader {

    /**
     * The files contained in the input directory.
     */
    private ArrayList<String> _files;

    /**
     * The reader used to iterate thorough the input files.
     */
    private BufferedReader _reader;

    /**
     * The ID for the file that is currently read.
     */
    private int _idFile;

    /**
     * The categories type
     */
    private CategoryType _catType;

    private boolean _shuffleFileList = false;

    /**
     * The filename which define the valid categories.
     */
    private String _validCategoriesFilename;

    private String _lastDocPathLoaded;

    private int _documentsCount;
    private int _trainingDocuments;
    private double _trainPercentaje = 0.7;
    private int _maxDocByLanguage = -1;

    /*
     * Indicate if we must exclude document with no valid categories.
     */
    private boolean _excludeDocWithoutCat;


    /*
     * List of accepted languages. Documents belonging to different languages
     * will be skiped.
     */
    private HashSet<LanguageLabel> _acceptedLanguagesFilter;
    private HashSet<LanguageLabel> _acceptedLanguagesFilterTraining;
    private HashSet<LanguageLabel> _acceptedLanguagesFilterTesting;

    /*
     * Indicates whether the documents should be evenly distributed among
     * training and testing for each language. For example, if there are 100
     * docs in English and 50 in Spanish and _trainPercentaje=0.8, then there
     * should be 80(en)+40(es) in training and 20(en)+10(es) in test.
     */
    @SuppressWarnings("unused")
    private boolean _evenlyLanguageDistribution = true;

    /*
     * Exclude all non-xml files, and files with languages labels different from
     * those in _acceptedLanguagesFilter
     */
    private RCV2FileNameFilter _fileNameFilter;

	/*
	 * public boolean isRandomizeFileOrder() { return _randomizeFileOrder; }
	 * 
	 * public void setRandomizeFileOrder(boolean _randomizeFileOrder) {
	 * this._randomizeFileOrder = _randomizeFileOrder; }
	 */

    /**
     * The XML parser used.
     */
    // private RCV1XmlParser _parser;
    private RCV2FastParser _parser;

    public RCV2CorpusReader(ICategoryDB catsDB) {
        super(catsDB);

        setName("RCV2");

        String description = "The class read the RCV2 Multilingual corpus collection.";

        setDescription(description);

        // Reset the input reuters directory.
        // _inputDir = "";
        _files = new ArrayList<String>();

        _catType = CategoryType.TOPICS;

        _parser = new RCV2FastParser(this);

        _excludeDocWithoutCat = false;

        // _randomizeFileOrder = false;

        _acceptedLanguagesFilter = new HashSet<LanguageLabel>();
        _acceptedLanguagesFilterTraining = new HashSet<LanguageLabel>();
        _acceptedLanguagesFilterTesting = new HashSet<LanguageLabel>();

        _fileNameFilter = new RCV2FileNameFilter(_acceptedLanguagesFilter);

        _lastDocPathLoaded = null;

        _maxDocByLanguage = -1;

        _evenlyLanguageDistribution = true;// false;

    }

    public static String readRawDocument(String filepath, String stopToken) {
        BufferedReader bufferReader = null;
        try {
            bufferReader = new BufferedReader(new FileReader(filepath));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return readRawDocument(bufferReader, stopToken);
    }

    private static String readRawDocument(BufferedReader bufferReader,
                                          String stopToken) {
        boolean docRead = false;

        StringBuilder docBuilder = new StringBuilder(4096);
        String doc = null;

        while (!docRead) {
            String line;
            try {
                line = bufferReader.readLine();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            if (line == null) {
                doc = null;
                return doc;
            } else if (line.contains(stopToken)) {
                docRead = true;
            }

            docBuilder.append(line).append(Os.newline());
        }

        doc = docBuilder.toString();
        return doc;
    }

    public void setShuffleFileList(boolean shuffle) {
        _shuffleFileList = shuffle;
    }

    public void setTrainingPercentaje(double perc) {
        _trainPercentaje = perc;
    }

	/*
	 * public void setEvenDistributionByLang(boolean even) {
	 * _evenlyLanguageDistribution = even; }/*
	 */

    public void excludeDocumentsWithoutValidCategories(boolean exclude) {
        _excludeDocWithoutCat = exclude;
    }

    public void setMaxDocumentsPerLanguage(int maxDocs) {
        _maxDocByLanguage = maxDocs;
    }

    // Implementing Fisher–Yates shuffle
	/*
	 * static void shuffleArray(String[] ar) { Random rnd = new Random(); for
	 * (int i = ar.length - 1; i > 0; i--) { int index = rnd.nextInt(i + 1); //
	 * Simple swap String a = ar[index]; ar[index] = ar[i]; ar[i] = a; } }
	 */

    /**
     * Get the filename of the file defining the categories set used.
     *
     * @return The filename.
     */
    public String validCategoriesFilename() {
        return _validCategoriesFilename;
    }

    /**
     * @see it.cnr.jatecs.indexing.corpus.CorpusReader#begin()
     */
    @Override
    public void begin() {
        if (_acceptedLanguagesFilter.isEmpty()) {
            JatecsLogger.execution().warning(
                    "No languages added to language filter. Exit!");
            System.exit(0);
        }

        if (getDocumentSetType() == SetType.TEST) {
            _idFile = _trainingDocuments;
            // return;
        } else if (getDocumentSetType() == SetType.TRAINING) {
            _idFile = 0;
        }

        String filename = /* _inputDir + Os.pathSeparator() + */_files
                .get(_idFile);

        try {
            _reader = new BufferedReader(new FileReader(filename));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void close() {
        try {
            _reader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setInputDir(String inputDir) {
        setInputDir(inputDir, null);
    }

    public void setInputDir(String inputDirRCV2, String inputDirRCV1) {
        JatecsLogger.execution().println(
                "Selecting files with allowed languages...");

        File dirRCV2 = new File(inputDirRCV2);
        String[] filesRCV2 = dirRCV2.list(_fileNameFilter);
        for (String file : filesRCV2) {
            _files.add(inputDirRCV2 + Os.pathSeparator() + file);
        }

        if (inputDirRCV1 != null) {
            File dirRCV1 = new File(inputDirRCV1);
            String[] filesRCV1 = dirRCV1.list(_fileNameFilter);
            for (String file : filesRCV1) {
                _files.add(inputDirRCV1 + Os.pathSeparator() + file);
            }
        }

        _fileNameFilter.getInfo();

        assert (_files != null);
        assert (_files.size() > 0);

        _files = distributeEvently(_fileNameFilter.getPathsByLanguage());

    }

    private ArrayList<String> distributeEvently(
            HashMap<LanguageLabel, ArrayList<String>> pathsByLanguage) {
        ArrayList<String> trainingFiles = new ArrayList<String>();
        ArrayList<String> testFiles = new ArrayList<String>();
        for (LanguageLabel lang : _acceptedLanguagesFilter) {
            List<String> langFiles = pathsByLanguage.get(lang);
            if (langFiles != null && langFiles.size() > 0) {
                if (_maxDocByLanguage != -1
                        && langFiles.size() > _maxDocByLanguage) {
                    langFiles = langFiles.subList(0, _maxDocByLanguage);
                }
                int toTrainSet = (int) (langFiles.size() * _trainPercentaje);

                if (_shuffleFileList)
                    shuffle(langFiles);

                if (_acceptedLanguagesFilterTraining.contains(lang))
                    trainingFiles.addAll(langFiles.subList(0, toTrainSet));
                if (_acceptedLanguagesFilterTesting.contains(lang))
                    testFiles.addAll(langFiles.subList(toTrainSet,
                            langFiles.size()));
            } else {
                JatecsLogger.execution().warning(
                        "No documents found for language " + lang.toString()
                                + "\n");
            }
        }

        ArrayList<String> paths = new ArrayList<String>(trainingFiles.size()
                + testFiles.size());
        paths.addAll(trainingFiles);
        paths.addAll(testFiles);
        _documentsCount = paths.size();
        _trainingDocuments = trainingFiles.size();
        return paths;
    }

    private void shuffle(List<String> langFiles) {
        Random rnd = new Random();
        for (int i = langFiles.size() - 1; i > 0; i--) {
            int index = rnd.nextInt(i + 1);
            Collections.swap(langFiles, i, index);
        }
    }

    /**
     * @see it.cnr.jatecs.indexing.corpus.CorpusReader#next()
     */
    @Override
    public CorpusDocument next() {
        if (!hasNext()) {
            return null;
        }

        CorpusDocumentLang doc = null;

        while (doc == null && hasNext()) {
            String xmlDoc = loadNext();

            // Process the raw document and possibly obtain a valid
            // CorpusDocumentLang object.
            doc = processXmlDocument(xmlDoc);

            if (doc != null) {
                if (_excludeDocWithoutCat) {
                    if (doc.categories().size() == 0) {
                        // Skip document.
                        doc = null;
                        continue;
                    }
                }
            }
        }

        return doc;
    }

    private boolean hasNext() {
        if (_idFile >= _documentsCount)
            return false;
        else {
            if (getDocumentSetType() == SetType.TRAINING
                    && _idFile >= _trainingDocuments)
                return false;
            if (getDocumentSetType() == SetType.TEST
                    && _idFile < _trainingDocuments)
                return false;
            return true;
        }
    }

    private String loadNext() {
        _lastDocPathLoaded = /* _inputDir + Os.pathSeparator() + */_files
                .get(_idFile);
        try {
            _reader = new BufferedReader(new FileReader(_lastDocPathLoaded));
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }

        String xmlDoc = readRawDocument();
        _idFile++;

        return xmlDoc;
    }

    protected String readRawDocument() {
        return readRawDocument(_reader, "</newsitem>");
    }

    protected CorpusDocumentLang processXmlDocument(String xmlDoc) {
        CorpusDocumentLang doc = null;

        _parser.setXMLDocument(xmlDoc);
        doc = _parser.parse();

        if (doc == null) {
            JatecsLogger.execution().warning(
                    "...Unable to parse document. Bad file format encountered in "
                            + _lastDocPathLoaded + "\n");
        }

        return doc;
    }

    public CategoryType categoriesType() {
        return _catType;
    }

    public void setCategoriesType(CategoryType catType) {
        _catType = catType;
    }

    public void addTrainingLanguage(LanguageLabel lang) {
        _acceptedLanguagesFilterTraining.add(lang);
        _acceptedLanguagesFilter.add(lang);
    }

    public void addTestLanguage(LanguageLabel lang) {
        _acceptedLanguagesFilterTesting.add(lang);
        _acceptedLanguagesFilter.add(lang);
    }

    public HashSet<LanguageLabel> getAcceptedLanguagesList() {
        return _acceptedLanguagesFilter;
    }
}

class RCV2FileNameFilter implements FilenameFilter {

    private int filesCount = 0;
    private int filesSkiped = 0;
    private HashSet<LanguageLabel> _acceptedLanguagesFilter;
    private HashMap<LanguageLabel, ArrayList<String>> _langFileMap;

    public RCV2FileNameFilter(HashSet<LanguageLabel> acceptedLanguagesFilter) {
        _acceptedLanguagesFilter = acceptedLanguagesFilter;
        _langFileMap = new HashMap<LanguageLabel, ArrayList<String>>();
    }

    public void getInfo() {
        JatecsLogger.execution().println(
                "[Done] Skiped " + filesSkiped + "/" + filesCount);
        Iterator<LanguageLabel> it = _langFileMap.keySet().iterator();
        while (it.hasNext()) {
            LanguageLabel lab = it.next();
            System.out.println(lab.toString() + ": "
                    + _langFileMap.get(lab).size());
        }
    }

    public boolean accept(File dir, String name) {
        boolean accept = false;

        if (filesCount % 10000 == 0)
            JatecsLogger.execution().println("" + filesCount);
        else if (filesCount % 100 == 0)
            JatecsLogger.execution().print(".");

        filesCount++;

        // if (name.matches("[0-9]+\\.xml")) {
        if (name.endsWith(".xml")) {
            String filepath = dir.getAbsolutePath() + Os.pathSeparator() + name;
            // System.out.println(filepath);
            LanguageLabel langLabel = RCV2FastParser.showLangLabel(filepath);
            if (langLabel == null || langLabel == LanguageLabel.unknown) {
                JatecsLogger.execution().warning(
                        "Unknow language label found in file " + filepath);
            } else {
                accept = _acceptedLanguagesFilter.contains(langLabel);
                if (!_langFileMap.containsKey(langLabel)) {
                    _langFileMap.put(langLabel, new ArrayList<String>());
                }
                _langFileMap.get(langLabel).add(filepath);
            }
        }

        if (!accept)
            filesSkiped++;

        return accept;
    }

    public HashMap<LanguageLabel, ArrayList<String>> getPathsByLanguage() {
        return _langFileMap;
    }

}