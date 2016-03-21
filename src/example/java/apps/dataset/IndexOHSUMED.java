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
import it.cnr.jatecs.indexes.DB.troveCompact.TroveDependentIndexBuilder;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveMainIndexBuilder;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveReadWriteHelper;
import it.cnr.jatecs.indexing.corpus.BagOfWordsFeatureExtractor;
import it.cnr.jatecs.indexing.corpus.CorpusReader;
import it.cnr.jatecs.indexing.corpus.FeatureExtractor;
import it.cnr.jatecs.indexing.corpus.OHSUMED.OHSUMEDCategoryReader;
import it.cnr.jatecs.indexing.corpus.OHSUMED.OHSUMEDCorpusReader;
import it.cnr.jatecs.indexing.corpus.SetType;
import it.cnr.jatecs.indexing.module.FullIndexConstructor;
import it.cnr.jatecs.indexing.module.UnusedCategoriesRemover;
import it.cnr.jatecs.indexing.preprocessing.EnglishPorterStemming;
import it.cnr.jatecs.indexing.preprocessing.EnglishStopword;
import it.cnr.jatecs.indexing.weighting.IWeighting;
import it.cnr.jatecs.indexing.weighting.TfNormalizedIdf;
import it.cnr.jatecs.io.FileSystemStorageManager;

import java.io.IOException;

public class IndexOHSUMED {

    public static void main(String[] args) throws IOException {

        if (args.length < 4) {
            System.out
                    .println("Usage: IndexOHSUMED <indexPath> <corpusPath> <categoryFile> <rootCategory, eg. \"Heart Diseases\"> [-x] [-q]");
            return;
        }

        String indexPath = args[0];
        String corpusPath = args[1];
        String categoriesFile = args[2];
        String rootCategory = args[3];
        boolean excludeDocsWithoutCats = false;
        boolean quantificationSplit = false;

        if (args.length >= 5) {
            if (args[4].compareTo("-x") == 0) {
                excludeDocsWithoutCats = true;
            }
        }
        if (args.length >= 6) {
            if (args[5].compareTo("-x") == 0) {
                excludeDocsWithoutCats = true;
            }
        }
        if (args.length >= 5) {
            if (args[4].compareTo("-q") == 0) {
                quantificationSplit = true;
            }
        }
        if (args.length >= 6) {
            if (args[5].compareTo("-q") == 0) {
                quantificationSplit = true;
            }
        }

        // DOMAIN
        if (quantificationSplit) {
            OHSUMEDCorpusReader.trainFiles = new String[]{"ohsumed.87"};
            OHSUMEDCorpusReader.testFiles = new String[]{"ohsumed.88",
                    "ohsumed.89", "ohsumed.90", "ohsumed.91"};
        }

        TroveCategoryDBBuilder categoryDBBuilder = new TroveCategoryDBBuilder();
        ICategoryDB categoryDB = OHSUMEDCategoryReader.ReadOHSUMEDCategories(
                rootCategory, categoriesFile, categoryDBBuilder);

        // CORPUS READER
        OHSUMEDCorpusReader corpusReader = new OHSUMEDCorpusReader(categoryDB,
                corpusPath);

        corpusReader
                .excludeDocumentsWithoutValidCategories(excludeDocsWithoutCats);

        FeatureExtractor extractor = new BagOfWordsFeatureExtractor();

        extractor.disableEntitiesSubstitution();
        extractor.disableSpecialTermsSubstitution();
        extractor.disableSpellChecking();
        extractor.disableTFFeatures();
        extractor.enableStemming(new EnglishPorterStemming());
        extractor.enableStopwordRemoval(new EnglishStopword());

        // TRAINING INDEX
        corpusReader.setDocumentSetType(SetType.TRAINING);
        IIndex training = indexTraining(categoryDB, corpusReader, extractor);

        UnusedCategoriesRemover remover = new UnusedCategoriesRemover(training);
        remover.exec();
        training = ((UnusedCategoriesRemover) remover).getProcessedIndex();

        String indexName = "OH";

        if (excludeDocsWithoutCats)
            indexName += "-S";

        indexName += "_" + categoriesFile + "_" + rootCategory;

        FileSystemStorageManager storageManager = new FileSystemStorageManager(
                indexPath, false);
        storageManager.open();
        TroveReadWriteHelper.writeIndex(storageManager, training, indexName
                + "_train", true);

        // Update the filtered categories in the corpusReader
        corpusReader.setCategoryDB(training.getDomainDB().getCategoryDB());

        // TEST INDEX

        corpusReader.setDocumentSetType(SetType.TEST);
        IIndex test = indexTest(corpusReader, extractor, training);

        TroveReadWriteHelper.writeIndex(storageManager, test, indexName
                + "_test", true);
        storageManager.close();

    }

    private static IIndex indexTraining(ICategoryDB categoryDB,
                                        CorpusReader corpusReader, FeatureExtractor extractor) {
        TroveMainIndexBuilder trainingIndexBuilder = new TroveMainIndexBuilder(
                categoryDB);
        FullIndexConstructor traningIndexConstructor = new FullIndexConstructor(
                corpusReader, trainingIndexBuilder);

        traningIndexConstructor.setFeatureExtractor(extractor);

        traningIndexConstructor.exec();

        IIndex index = traningIndexConstructor.index();

        IWeighting weighting = new TfNormalizedIdf(index);

        index = weighting.computeWeights(index);

        return index;
    }

    private static IIndex indexTest(CorpusReader corpusReader,
                                    FeatureExtractor extractor, IIndex training) {
        TroveDependentIndexBuilder testIndexBuilder = new TroveDependentIndexBuilder(
                training.getDomainDB());
        FullIndexConstructor testIndexConstructor = new FullIndexConstructor(
                corpusReader, testIndexBuilder);

        testIndexConstructor.setFeatureExtractor(extractor);

        testIndexConstructor.exec();

        IIndex index = testIndexConstructor.index();

        IWeighting weighting = new TfNormalizedIdf(training);

        index = weighting.computeWeights(index);

        return index;
    }
}
