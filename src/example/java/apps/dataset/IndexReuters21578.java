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
import it.cnr.jatecs.indexing.corpus.*;
import it.cnr.jatecs.indexing.corpus.Reuters21578.Reuters21578CorpusReader;
import it.cnr.jatecs.indexing.corpus.Reuters21578.Reuters21578SplitType;
import it.cnr.jatecs.indexing.module.FullIndexConstructor;
import it.cnr.jatecs.indexing.preprocessing.EnglishPorterStemming;
import it.cnr.jatecs.indexing.preprocessing.EnglishStopword;
import it.cnr.jatecs.indexing.weighting.IWeighting;
import it.cnr.jatecs.indexing.weighting.TfNormalizedIdf;
import it.cnr.jatecs.io.FileSystemStorageManager;

import java.io.IOException;

public class IndexReuters21578 {

	/**
	 * This application reads the Reuters-21578 document collection and creates an IIndex structure, according to the
	 * standard 'ModApté' split described in {@code http://www.daviddlewis.com/resources/testcollections/reuters21578/readme.txt}.
	 * */
    public static void main(String[] args) throws IOException {

        if (args.length != 3) {
            System.out.println("Usage: IndexReuters21578 <indexPath> <corpusPath> <categoryFile>");
            return;
        }

        String indexPath = args[0];
        String corpusPath = args[1];
        String categoriesFile = args[2];


        // DOMAIN
        TroveCategoryDBBuilder categoryDBBuilder = new TroveCategoryDBBuilder();
        FileCategoryReader categoriesReader = new FileCategoryReader(
                categoriesFile, categoryDBBuilder);
        ICategoryDB categoryDB = categoriesReader.getCategoryDB();

        // CORPUS READER
        Reuters21578CorpusReader corpusReader = new Reuters21578CorpusReader(
                categoryDB);

        corpusReader.excludeDocumentsWithoutValidCategories(false);
        corpusReader.setInputDir(corpusPath);
        corpusReader.setSplitType(Reuters21578SplitType.APTE);

        FeatureExtractor extractor = new BagOfWordsFeatureExtractor();

        extractor.disableEntitiesSubstitution();
        extractor.disableSpecialTermsSubstitution();
        extractor.disableSpellChecking();
        extractor.disableTFFeatures();
        extractor.enableStemming(new EnglishPorterStemming());
        extractor.enableStopwordRemoval(new EnglishStopword());

        FileSystemStorageManager storageManager = new FileSystemStorageManager(indexPath, false);
        storageManager.open();

        // TRAINING INDEX
        corpusReader.setDocumentSetType(SetType.TRAINING);
        IIndex training = indexTraining(categoryDB, corpusReader, extractor);

        TroveReadWriteHelper.writeIndex(storageManager, training, "Reuters21578-training", true);

        // TEST INDEX
        corpusReader.setDocumentSetType(SetType.TEST);
        IIndex test = indexTest(corpusReader, extractor, training);

        TroveReadWriteHelper.writeIndex(storageManager, test, "Reuters21578-test", true);
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
