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
import it.cnr.jatecs.indexes.DB.troveCompact.TroveDependentIndexBuilder;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveMainIndexBuilder;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveReadWriteHelper;
import it.cnr.jatecs.indexing.corpus.BagOfWordsFeatureExtractor;
import it.cnr.jatecs.indexing.corpus.CorpusReader;
import it.cnr.jatecs.indexing.corpus.FeatureExtractor;
import it.cnr.jatecs.indexing.corpus.RCV1.RCV1CategoryReader;
import it.cnr.jatecs.indexing.corpus.RCV1.RCV1CorpusReader;
import it.cnr.jatecs.indexing.corpus.SetType;
import it.cnr.jatecs.indexing.module.FullIndexConstructor;
import it.cnr.jatecs.indexing.preprocessing.EnglishPorterStemming;
import it.cnr.jatecs.indexing.preprocessing.EnglishStopword;
import it.cnr.jatecs.indexing.weighting.IWeighting;
import it.cnr.jatecs.indexing.weighting.TfNormalizedIdf;
import it.cnr.jatecs.io.FileSystemStorageManager;
import it.cnr.jatecs.utils.Os;

import java.io.IOException;

public class IndexRCV1 {

	/**
	 * This application reads the RCV1-v2 document collection, a more recent version of the precendet
	 * Reuters-21578 corpus, and creates an IIndex structure.
	 * See {@code Lewis, D. D., Yang, Y., Rose, T. G., & Li, F. (2004). Rcv1: A new 
	 * benchmark collection for text categorization research. The Journal of Machine 
	 * Learning Research, 5, 361-397.}.
	 * */
    public static void main(String[] args) throws IOException {

        if (args.length < 3 || args.length > 4) {
            System.out
                    .println("Usage: IndexRCV1 <indexPath> <corpusPath> <categoryFile> [splitCorpusPath]");
            return;
        }

        String indexPath = args[0];

        String fullCorpusPath = args[1];
        String categoriesFile = args[2];
        String splittedCorpusPath = null;
        if (args.length > 3) {
            splittedCorpusPath = args[3];
        }

        // DOMAIN
        RCV1CategoryReader categoriesReader = new RCV1CategoryReader(
                categoriesFile);
        ICategoryDB categoryDB = categoriesReader.getCategoryDB();

        // CORPUS READER
        RCV1CorpusReader corpusReader = new RCV1CorpusReader(categoryDB);

        corpusReader.excludeDocumentsWithoutValidCategories(false);
        corpusReader.setInputDir(fullCorpusPath);

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

        FileSystemStorageManager storageManager = new FileSystemStorageManager(
                indexPath, false);
        storageManager.open();
        TroveReadWriteHelper.writeIndex(storageManager, training,
                "RCV1-training", true);

        // TEST INDEX
        corpusReader = new RCV1CorpusReader(categoryDB);
        corpusReader.setDocumentSetType(SetType.TEST);
        if (splittedCorpusPath != null) {
            for (int i = 1; i < 13; ++i) {
                corpusReader.setInputDir(splittedCorpusPath + "/" + i);
                IIndex test = indexTest(corpusReader, extractor, training);
                TroveReadWriteHelper.writeIndex(storageManager, test,
                        "RCV1-test" + Os.pathSeparator() + i, true);
            }
        } else {
            corpusReader.setInputDir(fullCorpusPath);
            IIndex test = indexTest(corpusReader, extractor, training);
            TroveReadWriteHelper.writeIndex(storageManager, test, "RCV1-test",
                    true);
        }
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
