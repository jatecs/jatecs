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

package apps.dataset;

import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveDependentIndexBuilder;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveReadWriteHelper;
import it.cnr.jatecs.indexing.corpus.BagOfWordsFeatureExtractor;
import it.cnr.jatecs.indexing.corpus.CSV.CSVCorpusReader;
import it.cnr.jatecs.indexing.corpus.SetType;
import it.cnr.jatecs.indexing.module.FullIndexConstructor;
import it.cnr.jatecs.indexing.preprocessing.*;
import it.cnr.jatecs.indexing.weighting.IWeighting;
import it.cnr.jatecs.indexing.weighting.TfNormalizedIdf;
import it.cnr.jatecs.io.FileSystemStorageManager;

import java.io.File;
import java.io.IOException;

public class IndexDepCSV {

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err
                    .println("Usage: IndexDepCSV <mainIndexDirectory> <dataFile>");
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
        if (mainIndex.indexOf("_stem-") >= 0) {
            stemLanguage = mainIndex.substring(mainIndex.indexOf("_stem-") + 6,
                    mainIndex.indexOf("_stem-") + 8);
            System.err.println("Stemming language: " + stemLanguage);
        }

        boolean enableStopwords;
        if (mainIndex.contains("_sw-")) {
            enableStopwords = true;
            System.err.println("Stopwords will be removed");
        } else {
            enableStopwords = false;
            System.err.println("Stopword will be not removed.");
        }

        FileSystemStorageManager storageManager = new FileSystemStorageManager(
                mainPath, false);
        storageManager.open();
        IIndex training = TroveReadWriteHelper.readIndex(storageManager,
                mainName);
        storageManager.close();

        CSVCorpusReader corpusReader = new CSVCorpusReader(
                training.getCategoryDB());

        corpusReader.setFieldSeparator("\t");
        corpusReader.setInputFile(dataFilename);
        corpusReader.setDocumentSetType(SetType.TEST);

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
                training.getDomainDB());
        FullIndexConstructor testIndexConstructor = new FullIndexConstructor(
                corpusReader, testIndexBuilder);

        testIndexConstructor.setFeatureExtractor(extractor);

        testIndexConstructor.exec();

        IIndex test = testIndexConstructor.index();

        IWeighting weightingFunction = new TfNormalizedIdf(training);

        test = weightingFunction.computeWeights(test);

        if (indexName.endsWith(".txt"))
            indexName = indexName.substring(0, indexName.length() - 4);

        indexName = indexName + "_" + mainName;

        indexName += ".idx";

        test.setName(indexName);

        storageManager = new FileSystemStorageManager(dataPath, false);
        storageManager.open();
        TroveReadWriteHelper.writeIndex(storageManager, test, indexName, true);
        storageManager.close();
    }
}
