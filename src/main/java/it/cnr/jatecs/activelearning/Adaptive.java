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

package it.cnr.jatecs.activelearning;

import gnu.trove.TDoubleArrayList;
import gnu.trove.TIntArrayList;
import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TShortHashSet;
import it.cnr.jatecs.classification.ClassificationScoreDB;
import it.cnr.jatecs.classification.ClassifierRangeWithScore;
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.interfaces.IClassifierRuntimeCustomizer;
import it.cnr.jatecs.classification.interfaces.ILearner;
import it.cnr.jatecs.classification.module.Classifier;
import it.cnr.jatecs.indexes.DB.generic.GenericIndex;
import it.cnr.jatecs.indexes.DB.interfaces.*;
import it.cnr.jatecs.indexing.weighting.IWeighting;
import it.cnr.jatecs.indexing.weighting.TfNormalizedIdf;
import it.cnr.jatecs.utils.Pair;
import it.cnr.jatecs.utils.Ranker;
import it.cnr.jatecs.utils.iterators.TIntArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Adaptive active learning method that uses MMU and LCI. Proposed in
 * "X. Li and Y. Guo. Active learning with multi-label svm classification. In IJCAI, 2013"
 *
 * @author giacomo
 */
public class Adaptive extends ALpoolRank {

    public Adaptive(ClassificationScoreDB confidenceUnlabelled,
                    IIndex trainingSet, IClassificationDB classificationUnlabelled,
                    IIndex testSet, IIndex originalTrainingSet, int numOfSamples,
                    double[] weightsList, ILearner learner,
                    IClassifierRuntimeCustomizer classifierCustomizer) {
        super(confidenceUnlabelled, trainingSet);

        MMU mmu = new MMU(confidenceUnlabelled, trainingSet);
        LCI lci = new LCI(confidenceUnlabelled, trainingSet,
                classificationUnlabelled);
        TIntDoubleHashMap mmuRank = mmu.getRanking();
        TIntDoubleHashMap lciRank = lci.getRanking();

        ArrayList<TIntArrayList> ranks = new ArrayList<TIntArrayList>();
        ArrayList<TIntDoubleHashMap> rankMaps = new ArrayList<TIntDoubleHashMap>();

        for (int i = 0; i < weightsList.length; i++) {
            int[] docIds = mmuRank.keys();
            TIntDoubleHashMap currRank = new TIntDoubleHashMap();
            for (int docId = 0; docId < docIds.length; docId++) {
                currRank.put(
                        docId,
                        Math.pow(mmuRank.get(docId), weightsList[i])
                                * Math.pow(lciRank.get(docId),
                                1.0 - weightsList[i]));
            }
            Ranker r = new Ranker();
            ranks.add(r.get(currRank));
            rankMaps.add(currRank);
        }

        Pair<IIndex, IIndex> labelUnlabel = null;
        int bestWeightsIndex = -1;
        double mixSamplesValue = Double.MAX_VALUE;

        for (int i = 0; i < weightsList.length; i++) {
            TIntArrayList samples = ranks.get(i).subList(0, numOfSamples);
            IIndex tempTrainSet = trainingSet.cloneIndex();
            IDocumentDB docDB = classificationUnlabelled.getDocumentDB()
                    .cloneDB();
            ICategoryDB catDB = classificationUnlabelled.getCategoryDB()
                    .cloneDB();
            IFeatureDB featDB = testSet.getFeatureDB().cloneDB();
            IContentDB contentDB = testSet.getContentDB()
                    .cloneDB(docDB, featDB);
            IIndex tempTestSet = new GenericIndex(featDB, docDB, catDB,
                    testSet.getDomainDB(), contentDB, testSet.getWeightingDB()
                    .cloneDB(contentDB),
                    classificationUnlabelled.cloneDB(catDB, docDB));
            if (!samples.isEmpty()) {
                try {
                    labelUnlabel = moveDocuments(tempTrainSet, tempTestSet,
                            samples, originalTrainingSet);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(-1);
                }
                tempTrainSet = labelUnlabel.getFirst();
                tempTestSet = labelUnlabel.getSecond();
            }
            IClassifier cl = learner.build(tempTrainSet);
            cl.setRuntimeCustomizer(classifierCustomizer);
            Classifier classifier = new Classifier(tempTestSet, cl, true);
            classifier.exec();
            IClassificationDB unlabelClassification = classifier
                    .getClassificationDB();
            ClassificationScoreDB unlabelConfidences = classifier
                    .getConfidences();

            double sum = 0;

            for (int docId = 0; docId < unlabelConfidences.getDocumentCount(); docId++) {

                TShortHashSet trueCats = new TShortHashSet();
                IShortIterator catIt = unlabelClassification
                        .getDocumentCategories(docId);
                while (catIt.hasNext()) {
                    trueCats.add(catIt.next());
                }

                Set<Entry<Short, ClassifierRangeWithScore>> entries = unlabelConfidences
                        .getDocumentScoresAsSet(docId);
                Iterator<Entry<Short, ClassifierRangeWithScore>> iterator = entries
                        .iterator();

                TDoubleArrayList positives = new TDoubleArrayList();
                TDoubleArrayList negatives = new TDoubleArrayList();

                while (iterator.hasNext()) {
                    Entry<Short, ClassifierRangeWithScore> next = iterator
                            .next();
                    ClassifierRangeWithScore value = next.getValue();
                    short catId = next.getKey();
                    double score = value.score - value.border;

                    if (trueCats.contains(catId)) {
                        positives.add(Math.max(0, 1 - score));
                    } else {
                        negatives.add(Math.max(0, 1 + score));
                    }
                }

                sum += (positives.isEmpty() ? 0 : positives.max())
                        + (negatives.isEmpty() ? 0 : negatives.max());
            }

            if (sum < mixSamplesValue) {
                bestWeightsIndex = i;
                mixSamplesValue = sum;
            }
        }

        rankingMap = rankMaps.get(bestWeightsIndex);
        int bestDocId = ranks.get(bestWeightsIndex).get(0);
        updateMax(bestDocId, rankingMap.get(bestDocId));

    }

    public static void reWeight(IIndex index, IIndex dfIndex) {
        IWeighting weightingFunction = new TfNormalizedIdf(dfIndex);
        index = weightingFunction.computeWeights(index);
    }

    public static Pair<IIndex, IIndex> moveDocuments(IIndex currTrainSet,
                                                     IIndex currUnlabelSet, TIntArrayList docIdsToMove,
                                                     IIndex originalTrainSet) throws Exception {

        if (currTrainSet.getDomainDB().hasLocalRepresentation()) {
            throw new Exception(
                    "The training set has a local representation (per category) of the features");
        }

        // remove documents to be moved to the training set
        docIdsToMove.sort();
        currUnlabelSet.removeDocuments(new TIntArrayListIterator(docIdsToMove),
                false);

        // get the names of unlabelled documents
        IDocumentDB docDB = currUnlabelSet.getDocumentDB();
        HashSet<String> unlabelDocNames = new HashSet<String>(
                docDB.getDocumentsCount());
        IIntIterator docIt = docDB.getDocuments();
        while (docIt.hasNext()) {
            int docId = docIt.next();
            unlabelDocNames.add(docDB.getDocumentName(docId));
        }

        // take the original training set and remove the unlabelled documents
        IIndex tempOriginalTrainSet = originalTrainSet.cloneIndex();
        docDB = originalTrainSet.getDocumentDB();
        docIt = docDB.getDocuments();
        TIntArrayList docsToRemove = new TIntArrayList();
        while (docIt.hasNext()) {
            int docId = docIt.next();
            String docName = docDB.getDocumentName(docId);
            if (unlabelDocNames.contains(docName)) {
                docsToRemove.add(docId);
            }
        }
        docsToRemove.sort();
        tempOriginalTrainSet.removeDocuments(new TIntArrayListIterator(
                docsToRemove), false);
        currTrainSet = tempOriginalTrainSet;

        // re-weight the two indexes
        reWeight(currTrainSet, currTrainSet);
        reWeight(currUnlabelSet, currTrainSet);

        return new Pair<IIndex, IIndex>(currTrainSet, currUnlabelSet);
    }

}
