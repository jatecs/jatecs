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

package apps.chunkClassification;

import it.cnr.jatecs.indexes.DB.interfaces.IDomainDB;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveDependentIndexBuilder;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveReadWriteHelper;
import it.cnr.jatecs.indexing.corpus.BagOfWordsFeatureExtractor;
import it.cnr.jatecs.indexing.corpus.CSV.CSVCorpusReader;
import it.cnr.jatecs.indexing.corpus.DocumentSplittingCorpusReader;
import it.cnr.jatecs.indexing.corpus.SetType;
import it.cnr.jatecs.indexing.module.FullIndexConstructor;
import it.cnr.jatecs.indexing.preprocessing.*;
import it.cnr.jatecs.io.FileSystemStorageManager;

import java.io.File;
import java.io.IOException;

/**
 * This app indexes a test index using a DocumentSplittingCorpusReader, i.e., splitting each document in a set of chunk-documents
 */
public class IndexDepSplittingCSV {

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err
                    .println("Usage: IndexDepCSV <mainIndexDirectory> <dataFile> [stemming language] [<noStopWords>]");
            System.err
                    .println("Stemming: choose between en, fr, it, deu, es. Write any other word as third argument to give the stopword file name as fourth argument whitout enabling stemming.");
            return;
        }

        String mainIndex = args[0];
        File mainFile = new File(mainIndex);
        String mainName = mainFile.getName();
        String mainPath = mainFile.getParent();

        String dataFilename = args[1];
        File dataFile = new File(dataFilename);
        String indexName = dataFile.getName();
        String dataPath = dataFile.getParent();

        String stemLanguage = null;
        if (args.length >= 3)
            stemLanguage = args[2];

        boolean enableStopwords = true;
        if (args.length == 4)
            enableStopwords = false;

        FileSystemStorageManager storageManager = new FileSystemStorageManager(
                mainPath, false);
        storageManager.open();

        IDomainDB domain = TroveReadWriteHelper.readDomain(storageManager,
                mainName);

        storageManager.close();

        CSVCorpusReader internalCorpusReader = new CSVCorpusReader(
                domain.getCategoryDB());

        internalCorpusReader.setFieldSeparator("\t");
        internalCorpusReader.setInputFile(dataFilename);
        internalCorpusReader.setDocumentSetType(SetType.TEST);

        DocumentSplittingCorpusReader corpusReader = new DocumentSplittingCorpusReader(
                internalCorpusReader);

        BagOfWordsFeatureExtractor extractor = new BagOfWordsFeatureExtractor();

        Stopword sw = null;
        if (stemLanguage != null) {
            if (stemLanguage.equals("en")) {
                extractor.enableStemming(new EnglishPorterStemming());
                sw = new EnglishStopword();
            } else if (stemLanguage.equals("fr")) {
                extractor.enableStemming(new FrenchStemmer());
                sw = new FrenchStopword();
            } else if (stemLanguage.equals("it")) {
                extractor.enableStemming(new ItalianStemmer());
                sw = new ItalianStopword();
            } else if (stemLanguage.equals("deu")) {
                extractor.enableStemming(new GermanStemmer());
                sw = new GermanStopword();
            } else if (stemLanguage.equals("es")) {
                extractor.enableStemming(new SpanishStemmer());
                sw = new SpanishStopword();
            } else
                stemLanguage = null;
        }

        if (enableStopwords) {
            extractor.enableStopwordRemoval(sw);
        }

        TroveDependentIndexBuilder testIndexBuilder = new TroveDependentIndexBuilder(
                domain);
        FullIndexConstructor testIndexConstructor = new FullIndexConstructor(
                corpusReader, testIndexBuilder);

        testIndexConstructor.setFeatureExtractor(extractor);

        testIndexConstructor.exec();

        IIndex test = testIndexConstructor.index();

        if (indexName.endsWith(".txt"))
            indexName = indexName.substring(0, indexName.length() - 4);

        indexName = indexName + "_" + mainName;

        if (stemLanguage != null)
            indexName += "_stem-" + stemLanguage;

        if (enableStopwords)
            indexName += "_sw-" + stemLanguage;

        indexName += ".split.idx";

        test.setName(indexName);

        storageManager = new FileSystemStorageManager(dataPath, false);
        storageManager.open();

        TroveReadWriteHelper.writeIndex(storageManager, test, indexName, true);

        storageManager.close();
    }
}
