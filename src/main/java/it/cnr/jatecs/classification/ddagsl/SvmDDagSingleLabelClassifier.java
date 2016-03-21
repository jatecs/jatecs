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

package it.cnr.jatecs.classification.ddagsl;

import gnu.trove.TShortArrayList;
import it.cnr.jatecs.classification.BaseClassifier;
import it.cnr.jatecs.classification.ClassificationResult;
import it.cnr.jatecs.classification.ClassifierRange;
import it.cnr.jatecs.classification.ddagsl.SvmDDagSingleLabelLearner.WeightingType;
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.indexes.DB.interfaces.ICategoryDB;
import it.cnr.jatecs.indexes.DB.interfaces.IContentDB;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveCategoryDBBuilder;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveDependentIndexBuilder;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveDomainDB;
import it.cnr.jatecs.indexing.corpus.BagOfWordsFeatureExtractor;
import it.cnr.jatecs.indexing.corpus.IndexCorpusReader;
import it.cnr.jatecs.indexing.module.FullIndexConstructor;
import it.cnr.jatecs.indexing.weighting.BM25;
import it.cnr.jatecs.indexing.weighting.IWeighting;
import it.cnr.jatecs.indexing.weighting.TfNormalizedIdf;
import it.cnr.jatecs.utils.iterators.TShortArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.util.HashMap;

/**
 * DDAG-based single label classifier that uses SVMs.
 *
 * @author Tiziano Fagni
 */
public class SvmDDagSingleLabelClassifier extends BaseClassifier {

    private final HashMap<String, IIndex> localIndexes = new HashMap<String, IIndex>();
    private final HashMap<String, LocalClassifier> localClassifiers = new HashMap<String, LocalClassifier>();
    private ICategoryDB catsDB;

    public SvmDDagSingleLabelClassifier() {
        super();

    }

    public HashMap<String, LocalClassifier> getLocalClassifiers() {
        return localClassifiers;
    }

    public ICategoryDB getCatsDB() {
        return catsDB;
    }

    public ClassificationResult classify(IIndex testIndex, int docID) {

        ClassificationResult cr = new ClassificationResult();
        cr.documentID = docID;

        int numCats = testIndex.getCategoryDB().getCategoriesCount();

        ddagClassification(testIndex, docID, (short) 0, (short) (numCats - 1),
                cr);

        return cr;
    }

    private void ddagClassification(IIndex testIndex, int docID,
                                    short catPositive, short catNegative, ClassificationResult cr) {

        String key = LocalClassifier.buildKey(catPositive, catNegative);
        IIndex localIdx = localIndexes.get(key);
        if (localIdx == null) {
            localIdx = buildLocalIndex(catPositive, catNegative, testIndex);
            localIndexes.put(key, localIdx);
        }

        // Going through the dag.
        LocalClassifier lc = localClassifiers.get(key);
        ClassificationResult cres = lc.localClassifier
                .classify(localIdx, docID);
        if (cres.score.get(0) > 0) {
            // Positive classification.
            catNegative--;
        } else {
            // negative classification.
            catPositive++;
        }

        if (catPositive != catNegative) {
            ddagClassification(testIndex, docID, catPositive, catNegative, cr);
        } else {
            IShortIterator cats = testIndex.getCategoryDB().getCategories();
            while (cats.hasNext()) {
                short catID = cats.next();
                cr.categoryID.add(catID);
                if (catID == catPositive) {
                    cr.score.add(Math.abs(cres.score.get(0)));
                } else {
                    cr.score.add(-Double.MAX_VALUE);
                }
            }
        }

    }

    private IIndex buildLocalIndex(short catIDPositive, short catIDNegative,
                                   IIndex originalTestIdx) {
        String key = LocalClassifier.buildKey(catIDPositive, catIDNegative);
        LocalClassifier lc = localClassifiers.get(key);

        TroveCategoryDBBuilder catsDBBuilder = new TroveCategoryDBBuilder();
        String catName = originalTestIdx.getCategoryDB().getCategoryName(
                catIDPositive);
        catsDBBuilder.addCategory(catName);
        String catName2 = originalTestIdx.getCategoryDB().getCategoryName(
                catIDNegative);
        catsDBBuilder.addCategory(catName2);

        IndexCorpusReader corpusReader = new IndexCorpusReader(originalTestIdx,
                catsDBBuilder.getCategoryDB());

        BagOfWordsFeatureExtractor extractor = new BagOfWordsFeatureExtractor();

        extractor.disableEntitiesSubstitution();
        extractor.disableSpecialTermsSubstitution();
        extractor.disableSpellChecking();
        extractor.disableTFFeatures();
        extractor.disableStemming();
        extractor.disableStopwordRemoval();
        extractor.setSkipNumbers(false);

        // Build initial index.
        TroveDomainDB domainDB = new TroveDomainDB(
                catsDBBuilder.getCategoryDB(),
                lc.localContentDB.getFeatureDB());
        TroveDependentIndexBuilder testIndexBuilder = new TroveDependentIndexBuilder(
                domainDB);
        FullIndexConstructor testIndexConstructor = new FullIndexConstructor(
                corpusReader, testIndexBuilder);
        testIndexConstructor.setFeatureExtractor(extractor);
        testIndexConstructor.exec();
        IIndex index = testIndexConstructor.index();

        printDocStats(index, 0);

        // // Remove unwanted documents.
        // TIntArrayList docsToRemove = new TIntArrayList();
        // IIntIterator docs = index.getDocumentsDB().getDocuments();
        // while (docs.hasNext()) {
        // int docID = docs.next();
        // IShortIterator curCats = index.getClassificationDB()
        // .getDocumentCategories(docID);
        // if (!curCats.hasNext())
        // docsToRemove.add(docID);
        // }
        //
        // docsToRemove.sort();
        // index.removeDocuments(new TIntArrayListIterator(docsToRemove),
        // false);
        //
        //
        // Remove 2nd category to make an index for a binary classifier.
        TShortArrayList toRemove = new TShortArrayList();
        String catNameToRemove = catName2;
        toRemove.add(index.getCategoryDB().getCategory(catNameToRemove));
        index.removeCategories(new TShortArrayListIterator(toRemove));

        // Compute weighting
        IWeighting weighting = null;
        if (lc.weightingType == WeightingType.TF_IDF) {
            weighting = new TfNormalizedIdf(lc.localContentDB);
        } else if (lc.weightingType == WeightingType.BM25) {
            weighting = new BM25(lc.localContentDB);
        }

        index = weighting.computeWeights(index);

        System.out.println("The local index contains "
                + index.getDocumentDB().getDocumentsCount() + " docs, "
                + index.getCategoryDB().getCategoriesCount() + " cats and "
                + index.getFeatureDB().getFeaturesCount() + " feats.");

        return index;
    }

    private void printDocStats(IIndex index, int docID) {

    }

    public ClassifierRange getClassifierRange(short catID) {
        ClassifierRange cr = new ClassifierRange();
        cr.border = 0;
        cr.maximum = Double.MAX_VALUE;
        cr.minimum = -Double.MAX_VALUE;

        return cr;
    }

    public void addLocalBinaryClassifier(short catIDPositive,
                                         short catIDNegative, IClassifier binaryClassifier,
                                         IContentDB trainingContentDB, WeightingType weightingType) {
        if (catIDPositive == -1)
            throw new RuntimeException("The cat ID positive must be >= 0");
        if (catIDNegative == -1)
            throw new RuntimeException("The cat ID negative must be >= 0");
        if (catIDPositive == catIDNegative)
            throw new RuntimeException(
                    "The cat ID positive is equal to cat ID negative");
        if (binaryClassifier == null)
            throw new NullPointerException("The binary classifier is 'null'");

        LocalClassifier lc = new LocalClassifier();
        lc.catIDPositive = catIDPositive;
        lc.catIDNegative = catIDNegative;
        lc.localClassifier = binaryClassifier;
        lc.localContentDB = trainingContentDB;
        lc.weightingType = weightingType;
        String key = LocalClassifier.buildKey(catIDPositive, catIDNegative);

        localClassifiers.put(key, lc);
    }

    public void setCategoriesDB(ICategoryDB catsDB) {
        if (catsDB == null)
            throw new NullPointerException("The cats DB is 'null'");

        this.catsDB = catsDB.cloneDB();
    }

    @Override
    public int getCategoryCount() {
        return catsDB.getCategoriesCount();
    }

    @Override
    public IShortIterator getCategories() {
        return catsDB.getCategories();
    }

    @Override
    public void destroy() {

    }

    static class LocalClassifier {
        short catIDPositive;
        short catIDNegative;
        IClassifier localClassifier;
        IContentDB localContentDB;
        WeightingType weightingType;

        public static String buildKey(short catIDPositive, short catIDNegative) {
            return catIDPositive + "_" + catIDNegative;
        }

        @Override
        public int hashCode() {
            return buildKey(catIDPositive, catIDNegative).hashCode();
        }
    }

}
