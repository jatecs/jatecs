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

package it.cnr.jatecs.classification.regression;

import gnu.trove.TIntArrayList;
import gnu.trove.TShortHashSet;
import it.cnr.jatecs.classification.BaseLearner;
import it.cnr.jatecs.classification.adaboost.AdaBoostRegressionLearner;
import it.cnr.jatecs.classification.adaboost.AdaBoostRegressionLearnerCustomizer;
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.interfaces.ILearner;
import it.cnr.jatecs.classification.interfaces.ILearnerRuntimeCustomizer;
import it.cnr.jatecs.indexes.DB.generic.GenericIndex;
import it.cnr.jatecs.indexes.DB.interfaces.ICategoryDB;
import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDB;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveCategoryDBBuilder;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveClassificationDBBuilder;
import it.cnr.jatecs.indexing.tsr.ITsr;
import it.cnr.jatecs.indexing.weighting.IWeighting;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.iterators.TIntArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.util.Vector;

public class TreeRegressLearner extends BaseLearner {

    private static String positiveCategoryName = "pos";
    protected IWeighting _weighting;
    protected ITsr _tsr;
    private HierarchyConstructionMethod _hierarchyConstructionMethod;
    private ILearner _learner;

    public TreeRegressLearner(ILearner learner) {
        _hierarchyConstructionMethod = HierarchyConstructionMethod.BALANCE_CATEGORIES;
        _learner = learner;
        _weighting = null;
        _tsr = null;
    }

    public static TreeNode buildCategoryBalancedHierarchy(IIndex trainingIndex) {
        int catCount = trainingIndex.getCategoryDB().getCategoriesCount();
        short[] cats = new short[catCount];
        IShortIterator catIt = trainingIndex.getCategoryDB().getCategories();
        int i = 0;
        while (catIt.hasNext()) {
            cats[i] = catIt.next();
            ++i;
        }
        return splitByCategories(cats);
    }

    private static TreeNode splitByCategories(short[] cats) {
        if (cats.length == 1)
            throw new RuntimeException("At least two categories are required");
        int halfCats = cats.length / 2;

        short[] negativeCats = new short[halfCats];
        for (int i = 0; i < halfCats; ++i)
            negativeCats[i] = cats[i];
        TreeNode negativeChild = null;
        if (negativeCats.length > 1)
            negativeChild = splitByCategories(negativeCats);

        short[] positiveCats = new short[cats.length - halfCats];
        for (int i = halfCats; i < cats.length; ++i)
            positiveCats[i - halfCats] = cats[i];
        TreeNode positiveChild = null;
        if (positiveCats.length > 1)
            positiveChild = splitByCategories(positiveCats);

        return new TreeNode(positiveCats, negativeCats, positiveChild,
                negativeChild);
    }

    public static TreeNode buildDocumentBalancedHierarchy(IIndex trainingIndex) {
        int catCount = trainingIndex.getCategoryDB().getCategoriesCount();
        short[] cats = new short[catCount];
        IShortIterator catIt = trainingIndex.getCategoryDB().getCategories();
        int i = 0;
        while (catIt.hasNext()) {
            cats[i] = catIt.next();
            ++i;
        }
        return splitByDocuments(trainingIndex, cats);
    }

    private static TreeNode splitByDocuments(IIndex index, short[] cats) {
        if (cats.length == 1)
            throw new RuntimeException("At least two categories are required");

        int half = 0;
        for (int i = 0; i < cats.length; ++i)
            half += index.getClassificationDB().getCategoryDocumentsCount(
                    cats[i]);
        half /= 2;

        int split = 0;
        int partialCount = index.getClassificationDB()
                .getCategoryDocumentsCount(cats[split]);
        int lastDiff = Math.abs(half - partialCount);
        while (true) {
            ++split;
            partialCount += index.getClassificationDB()
                    .getCategoryDocumentsCount(cats[split]);
            int nextDiff = Math.abs(half - partialCount);
            if (lastDiff < nextDiff)
                break;
            lastDiff = nextDiff;
        }

        short[] negativeCats = new short[split];
        for (int i = 0; i < split; ++i)
            negativeCats[i] = cats[i];
        TreeNode negativeChild = null;
        if (negativeCats.length > 1)
            negativeChild = splitByDocuments(index, negativeCats);

        short[] positiveCats = new short[cats.length - split];
        for (int i = split; i < cats.length; ++i)
            positiveCats[i - split] = cats[i];
        TreeNode positiveChild = null;
        if (positiveCats.length > 1)
            positiveChild = splitByDocuments(index, positiveCats);

        return new TreeNode(positiveCats, negativeCats, positiveChild,
                negativeChild);
    }

    public void setWeighting(IWeighting w) {
        _weighting = w;
    }

    public void setTSR(ITsr tsr) {
        _tsr = tsr;
    }

    public IClassifier build(IIndex trainingIndex) {

        JatecsLogger.status().println("Category order: ");
        IShortIterator catIt = trainingIndex.getCategoryDB().getCategories();
        while (catIt.hasNext()) {
            short category = catIt.next();
            JatecsLogger
                    .status()
                    .println(
                            trainingIndex.getCategoryDB().getCategoryName(
                                    category)
                                    + " ("
                                    + trainingIndex
                                    .getClassificationDB()
                                    .getCategoryDocumentsCount(category)
                                    + ") ");
        }
        JatecsLogger.status().println("");

        TreeNode root;
        if (_hierarchyConstructionMethod == HierarchyConstructionMethod.BALANCE_CATEGORIES) {
            root = buildCategoryBalancedHierarchy(trainingIndex);
        } else {
            root = buildDocumentBalancedHierarchy(trainingIndex);
        }

        learnClassifiers(trainingIndex, root);

        return new TreeRegressClassifier(root);
    }

    private void learnClassifiers(IIndex index, TreeNode node) {

        JatecsLogger.status().print(
                "Learning a binary classifier for the split: [");
        for (int i = 0; i < node.getNegativeCategories().length; ++i) {
            JatecsLogger.status().print(
                    index.getCategoryDB().getCategoryName(
                            node.getNegativeCategories()[i])
                            + " ");
        }

        JatecsLogger.status().print("] vs [");

        for (int i = 0; i < node.getPositiveCategories().length; ++i) {
            JatecsLogger.status().print(
                    index.getCategoryDB().getCategoryName(
                            node.getPositiveCategories()[i])
                            + " ");
        }

        JatecsLogger.status().println("]");

        JatecsLogger.status().print("Splitting index...");
        TroveCategoryDBBuilder categoriesDBBuilder = new TroveCategoryDBBuilder();
        categoriesDBBuilder.addCategory(positiveCategoryName);

        ICategoryDB categoriesDB = categoriesDBBuilder.getCategoryDB();
        short positiveCategory = categoriesDB.getCategory(positiveCategoryName);

        TShortHashSet posCats = new TShortHashSet(node.getPositiveCategories());
        TShortHashSet negCats = new TShortHashSet(node.getNegativeCategories());

        IIndex nodeIndex = index.cloneIndex();

        TIntArrayList docsToRemove = new TIntArrayList();
        IIntIterator docIt = index.getDocumentDB().getDocuments();
        while (docIt.hasNext()) {
            int document = docIt.next();

            if (index.getClassificationDB()
                    .getDocumentCategoriesCount(document) != 1)
                throw new RuntimeException("Document "
                        + index.getDocumentDB().getDocumentName(document)
                        + " (" + document
                        + ") has not a single label attached.");
            IShortIterator catIt = index.getClassificationDB()
                    .getDocumentCategories(document);
            short category = catIt.next();

            if (!negCats.contains(category) && !posCats.contains(category))
                docsToRemove.add(document);
        }

        nodeIndex.removeDocuments(new TIntArrayListIterator(docsToRemove),
                false);

        TroveClassificationDBBuilder classificationDBBuilder = new TroveClassificationDBBuilder(
                nodeIndex.getDocumentDB(), categoriesDB);

        docIt = nodeIndex.getDocumentDB().getDocuments();
        while (docIt.hasNext()) {
            int document = docIt.next();

            if (nodeIndex.getClassificationDB().getDocumentCategoriesCount(
                    document) != 1)
                throw new RuntimeException("Document "
                        + nodeIndex.getDocumentDB().getDocumentName(document)
                        + " (" + document
                        + ") has not a single label attached.");
            IShortIterator catIt = nodeIndex.getClassificationDB()
                    .getDocumentCategories(document);
            short category = catIt.next();

            if (posCats.contains(category))
                classificationDBBuilder.setDocumentCategory(document,
                        positiveCategory);
        }
        IClassificationDB classificationDB = classificationDBBuilder
                .getClassificationDB();

        nodeIndex = new GenericIndex(nodeIndex.getFeatureDB(), nodeIndex
                .getDocumentDB(), categoriesDB, nodeIndex.getDomainDB(),
                nodeIndex.getContentDB(), nodeIndex.getWeightingDB(),
                classificationDB);

        JatecsLogger.status().println("done");

        System.out.println("The index contains: "
                + nodeIndex.getDocumentDB().getDocumentsCount() + " doc(s).");

        if (_tsr != null) {
            JatecsLogger.status().println("Applying TSR...");
            _tsr.computeTSR(nodeIndex);
            JatecsLogger.status().println("done");
        }

        if (_weighting != null) {
            JatecsLogger.status().println("Applying weighting...");
            nodeIndex = _weighting.computeWeights(nodeIndex);
            JatecsLogger.status().println("done");
        }

        if (_learner instanceof AdaBoostRegressionLearner) {
            AdaBoostRegressionLearnerCustomizer cust = (AdaBoostRegressionLearnerCustomizer) _learner
                    .getRuntimeCustomizer();
            cust.setOriginalIndex(index, node);
        }

        IClassifier classifier = _learner.build(nodeIndex);

        node.setClassifier(classifier);

        if (node.getNegativeCategories().length > 1)
            learnClassifiers(index, node.getNegativeChild());

        if (node.getPositiveCategories().length > 1)
            learnClassifiers(index, node.getPositiveChild());
    }

    public void setHierarchyConstructionMethod(
            HierarchyConstructionMethod method) {
        _hierarchyConstructionMethod = method;
    }

    @Override
    public ILearnerRuntimeCustomizer getRuntimeCustomizer(short catID) {
        return null;
    }

    @Override
    public IClassifier mergeClassifiers(Vector<IClassifier> classifiers) {
        return null;
    }

    @Override
    public void setRuntimeCustomizer(
            Vector<ILearnerRuntimeCustomizer> customizers) {

    }

    public enum HierarchyConstructionMethod {
        BALANCE_CATEGORIES, BALANCE_DOCUMENTS,
    }
}
