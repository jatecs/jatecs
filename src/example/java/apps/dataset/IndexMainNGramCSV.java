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

import it.cnr.jatecs.indexes.DB.interfaces.ICategoryDB;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveCategoryDBBuilder;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveMainIndexBuilder;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveReadWriteHelper;
import it.cnr.jatecs.indexing.corpus.CSV.CSVCorpusReader;
import it.cnr.jatecs.indexing.corpus.CharsNGramFeatureExtractor;
import it.cnr.jatecs.indexing.corpus.FileCategoryReader;
import it.cnr.jatecs.indexing.corpus.SetType;
import it.cnr.jatecs.indexing.module.FullIndexConstructor;
import it.cnr.jatecs.indexing.preprocessing.*;
import it.cnr.jatecs.indexing.tsr.GlobalThresholdTSR;
import it.cnr.jatecs.indexing.tsr.MinimumDF;
import it.cnr.jatecs.indexing.weighting.IWeighting;
import it.cnr.jatecs.indexing.weighting.TfNormalizedIdf;
import it.cnr.jatecs.io.FileSystemStorageManager;

import java.io.File;
import java.io.IOException;

public class IndexMainNGramCSV {

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err
                    .println("Usage: IndexMainCSV <dataFile> <categoryFile> [stemming language] [<noStopWords>]");
            System.err
                    .println("Stemming: choose between en, fr, it, deu, es. Write any other word as third argument to give the stopword file name as fourth argument whitout enabling stemming.");
            return;
        }

        String dataFile = args[0];
        File file = new File(dataFile);
        String indexName = file.getName();
        String dataPath = file.getParent();

        String categoriesFile = args[1];
        File catFile = new File(categoriesFile);
        String catName = catFile.getName();

        String stemLanguage = null;
        if (args.length >= 3)
            stemLanguage = args[2];

        boolean enableStopwords = true;
        if (args.length == 4)
            enableStopwords = false;

        TroveCategoryDBBuilder categoryDBBuilder = new TroveCategoryDBBuilder();
        FileCategoryReader categoriesReader = new FileCategoryReader(
                categoriesFile, categoryDBBuilder);
        ICategoryDB categoryDB = categoriesReader.getCategoryDB();

        CSVCorpusReader corpusReader = new CSVCorpusReader(categoryDB);

        corpusReader.setFieldSeparator("\t");

        CharsNGramFeatureExtractor extractor = new CharsNGramFeatureExtractor();

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

        extractor.setNGramSize(4);

        if (enableStopwords) {
            extractor.enableStopwordRemoval(sw);
        }

        corpusReader.setInputFile(dataFile);

        corpusReader.setDocumentSetType(SetType.TRAINING);

        TroveMainIndexBuilder mainIndexBuilder = new TroveMainIndexBuilder(
                categoryDB);
        FullIndexConstructor mainIndexConstructor = new FullIndexConstructor(
                corpusReader, mainIndexBuilder);

        mainIndexConstructor.setFeatureExtractor(extractor);
        mainIndexConstructor.exec();

        IIndex index = mainIndexConstructor.index();

        GlobalThresholdTSR tsrFunc = new GlobalThresholdTSR(0.5, new MinimumDF(
                3));
        tsrFunc.computeTSR(index);

        IWeighting weightingFunction = new TfNormalizedIdf(index);

        index = weightingFunction.computeWeights(index);

        if (catName.endsWith(".txt"))
            catName = catName.substring(0, catName.length() - 4);

        if (indexName.endsWith(".txt"))
            indexName = indexName.substring(0, indexName.length() - 4);

        indexName = indexName + "_" + catName;

        if (stemLanguage != null)
            indexName += "_stem-" + stemLanguage;

        if (enableStopwords)
            indexName += "_sw-" + stemLanguage;

        indexName += ".idx";

        index.setName(indexName);

        FileSystemStorageManager storageManager = new FileSystemStorageManager(
                dataPath, false);
        storageManager.open();
        TroveReadWriteHelper.writeIndex(storageManager, index, indexName, true);
        storageManager.close();
    }
}
