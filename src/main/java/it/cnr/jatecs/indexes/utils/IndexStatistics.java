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

package it.cnr.jatecs.indexes.utils;

import gnu.trove.TIntArrayList;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TShortArrayList;
import it.cnr.jatecs.indexes.DB.interfaces.*;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.Os;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.util.*;

public class IndexStatistics {

    /**
     * Print some statistics about the input index
     *
     * @param index
     * @param enableEquivalentFeaturesCheck
     * @param details
     */
    public static void printStatistics(IIndex index, boolean enableEquivalentFeaturesCheck,
                                       boolean details) {
        JatecsLogger.status().println("Statistics for: " + index.getName());

        int docsCount = index.getDocumentDB().getDocumentsCount();
        int featsCount = index.getFeatureDB().getFeaturesCount();
        int catsCount = index.getCategoryDB().getCategoriesCount();

        JatecsLogger.status().println("");
        JatecsLogger.status().println("Categories: " + catsCount);
        JatecsLogger.status().println("Documents: " + docsCount);
        JatecsLogger.status().println("Features: " + featsCount);

        // Print documents statistics.
        printDocumentsContentStatistics(index.getDocumentDB(),
                index.getContentDB(), details);

        JatecsLogger.status().println(")");

        // Print features statistics.
        printFeaturesContentStatistics(index.getFeatureDB(),
                index.getContentDB(), details);

        // Print categories statistics.
        printCategoriesClassificationStatistics(
                index.getCategoryDB(), index.getClassificationDB(), details);

        // Print documents classification statistics.
        printDocumentsClassificationStatistics(
                index.getDocumentDB(), index.getClassificationDB(), details);

        JatecsLogger.status().println(")");

        if (enableEquivalentFeaturesCheck) {
            // Print statistics about equivalent features.
            printEquivalentFeaturesStatistics(
                    index.getFeatureDB(), index.getContentDB(), details);
        }
    }

    /**
     * Print the statistics about the content of documents.
     *
     * @param docsDB      The input documents DB.
     * @param contentDB   The input content DB.
     * @param wantDetails True if must print more details about the statistics, false
     *                    otherwise.
     */
    public static void printDocumentsContentStatistics(IDocumentDB docsDB,
                                                       IContentDB contentDB, boolean wantDetails) {

        if (docsDB == null)
            throw new NullPointerException(
                    "The specified documents DB is 'null'");

        if (contentDB == null)
            throw new NullPointerException("The specified content DB is 'null'");

        double avgTF = 0.0;
        int maxTF = 2;
        TIntArrayList maxTFFeats = new TIntArrayList();
        TIntArrayList maxTFDocs = new TIntArrayList();

        double avgLength = 0.0;
        int TFden = 0;
        int minLength = Integer.MAX_VALUE;
        TIntArrayList minLengthDocs = new TIntArrayList();
        int maxLength = Integer.MIN_VALUE;
        TIntArrayList maxLengthDocs = new TIntArrayList();

        int docsCount = docsDB.getDocumentsCount();

        IIntIterator docIt = docsDB.getDocuments();
        while (docIt.hasNext()) {
            int doc = docIt.next();
            int dl = contentDB.getDocumentLength(doc);
            if (dl == minLength)
                minLengthDocs.add(doc);
            if (dl < minLength) {
                minLength = dl;
                minLengthDocs.clear();
                minLengthDocs.add(doc);
            }
            if (dl == maxLength)
                maxLengthDocs.add(doc);
            if (dl > maxLength) {
                maxLength = dl;
                maxLengthDocs.clear();
                maxLengthDocs.add(doc);
            }
            avgLength += dl;
            IIntIterator featIt = contentDB.getDocumentFeatures(doc);
            while (featIt.hasNext()) {
                int feat = featIt.next();
                int tf = contentDB.getDocumentFeatureFrequency(doc, feat);
                if (tf == maxTF) {
                    maxTFFeats.add(feat);
                    maxTFDocs.add(doc);
                }
                if (tf > maxTF) {
                    maxTF = tf;
                    maxTFFeats.clear();
                    maxTFFeats.add(feat);
                    maxTFDocs.clear();
                    maxTFDocs.add(doc);
                }
                avgTF += tf;
                ++TFden;
            }
        }

        avgLength /= docsCount;
        if (TFden != 0)
            avgTF /= TFden;
        else
            avgTF = 0;

        JatecsLogger.status().println("");
        JatecsLogger.status().println(
                "Average document length: " + Os.generateDouble(avgLength, 3));
        JatecsLogger.status().print(
                "Maximum document length: [" + maxLengthDocs.size() + "] "
                        + maxLength + " (");
        if (wantDetails) {
            for (int i = 0; i < maxLengthDocs.size(); ++i) {
                JatecsLogger.status().print(
                        docsDB.getDocumentName(maxLengthDocs.get(i)) + " ");
            }
        }
        JatecsLogger.status().println(")");
        JatecsLogger.status().print(
                "Minimum document length: [" + minLengthDocs.size() + "] "
                        + minLength + " (");
        if (wantDetails) {
            for (int i = 0; i < minLengthDocs.size(); ++i) {
                JatecsLogger.status().print(
                        docsDB.getDocumentName(minLengthDocs.get(i)) + " ");
            }
        }
        JatecsLogger.status().println(")");
        JatecsLogger.status().println("");
        JatecsLogger.status().println(
                "Average feature frequency: " + Os.generateDouble(avgTF, 3));
        JatecsLogger.status().print(
                "Maximum feature frequency (>1): [" + maxTFDocs.size() + "] "
                        + maxTF + " (");
        if (wantDetails) {
            for (int i = 0; i < maxTFDocs.size(); ++i) {
                JatecsLogger.status().print(
                        "("
                                + docsDB.getDocumentName(maxTFDocs.get(i))
                                + ","
                                + contentDB.getFeatureDB().getFeatureName(
                                maxTFFeats.get(i)) + ") ");
            }
        }
    }

    /**
     * Print the statistics about features and content.
     *
     * @param featsDB     The features DB.
     * @param contentDB   The content DB.
     * @param wantDetails True if must print more details about the statistics, false
     *                    otherwise.
     */
    public static void printFeaturesContentStatistics(IFeatureDB featsDB,
                                                      IContentDB contentDB, boolean wantDetails) {
        if (featsDB == null)
            throw new NullPointerException(
                    "The specified features DB is 'null'");

        if (contentDB == null)
            throw new NullPointerException("The specified content DB is 'null'");

        int featsCount = featsDB.getFeaturesCount();
        TIntArrayList unusedFeats = new TIntArrayList();
        TIntArrayList minDFFeats = new TIntArrayList();
        TIntArrayList maxDFFeats = new TIntArrayList();

        IIntIterator featIt = featsDB.getFeatures();
        double avgDF = 0.0;
        int minDF = Integer.MAX_VALUE;
        int maxDF = Integer.MIN_VALUE;
        while (featIt.hasNext()) {
            int feat = featIt.next();
            int df = contentDB.getFeatureDocumentsCount(feat);
            if (df == minDF)
                minDFFeats.add(feat);
            if (df < minDF && df != 0) {
                minDF = df;
                minDFFeats.clear();
                minDFFeats.add(feat);
            }
            if (df == 0)
                unusedFeats.add(feat);
            if (df == maxDF)
                maxDFFeats.add(feat);
            if (df > maxDF) {
                maxDF = df;
                maxDFFeats.clear();
                maxDFFeats.add(feat);
            }
            avgDF += df;
        }

        if (featsCount != 0)
            avgDF /= featsCount;
        else
            avgDF = 0;

        JatecsLogger.status().println("");
        JatecsLogger.status().println(
                "Average document frequency: " + Os.generateDouble(avgDF, 3));
        JatecsLogger.status().print(
                "Maximum document frequency: [" + maxDFFeats.size() + "] "
                        + maxDF + " (");
        if (wantDetails) {
            for (int i = 0; i < maxDFFeats.size(); ++i) {
                JatecsLogger.status().print(
                        featsDB.getFeatureName(maxDFFeats.get(i)) + " ");
            }
        }
        JatecsLogger.status().println(")");
        JatecsLogger.status().print(
                "Minimum document frequency: [" + minDFFeats.size() + "] "
                        + minDF + " (");
        if (wantDetails) {
            for (int i = 0; i < minDFFeats.size(); ++i) {
                JatecsLogger.status().print(
                        featsDB.getFeatureName(minDFFeats.get(i)) + " ");
            }
        }
        JatecsLogger.status().println(")");
        if (unusedFeats.size() != 0) {
            JatecsLogger.status().println(
                    "Unused features: [" + unusedFeats.size() + "] (");
            if (wantDetails) {
                for (int i = 0; i < unusedFeats.size(); ++i) {
                    JatecsLogger.status().print(
                            featsDB.getFeatureName(unusedFeats.get(i)) + " ");
                }
            }
            JatecsLogger.status().println(")");
        } else
            JatecsLogger.status().println("Unused features: 0 ()");
    }

    /**
     * Print statistics about categories and classification.
     *
     * @param catsDB           The categories DB.
     * @param classificationDB The classification DB.
     * @param wantDetails      True if must print more details about the statistics, false
     *                         otherwise.
     */
    public static void printCategoriesClassificationStatistics(
            ICategoryDB catsDB, IClassificationDB classificationDB,
            boolean wantDetails) {
        if (catsDB == null)
            throw new NullPointerException(
                    "The specified categories DB is 'null'");

        if (classificationDB == null)
            throw new NullPointerException(
                    "The specified classification DB is 'null'");

        int catsCount = catsDB.getCategoriesCount();
        double avgDocsPerCat = 0.0;
        int minDocsPerCat = Integer.MAX_VALUE;
        TShortArrayList minDocsPerCatCats = new TShortArrayList();
        int maxDocsPerCat = Integer.MIN_VALUE;
        TShortArrayList maxDocsPerCatCats = new TShortArrayList();

        IShortIterator catIt = catsDB.getCategories();
        while (catIt.hasNext()) {
            short cat = catIt.next();
            int catDocs = classificationDB.getCategoryDocumentsCount(cat);
            if (catDocs == minDocsPerCat)
                minDocsPerCatCats.add(cat);
            if (catDocs < minDocsPerCat) {
                minDocsPerCat = catDocs;
                minDocsPerCatCats.clear();
                minDocsPerCatCats.add(cat);
            }
            if (catDocs == maxDocsPerCat)
                maxDocsPerCatCats.add(cat);
            if (catDocs > maxDocsPerCat) {
                maxDocsPerCat = catDocs;
                maxDocsPerCatCats.clear();
                maxDocsPerCatCats.add(cat);
            }
            avgDocsPerCat += catDocs;
        }

        avgDocsPerCat /= catsCount;

        JatecsLogger.status().println("");
        JatecsLogger.status().println(
                "Average documents per categories: "
                        + Os.generateDouble(avgDocsPerCat, 3));
        JatecsLogger.status().print(
                "Maximum documents per categories: ["
                        + maxDocsPerCatCats.size() + "] " + maxDocsPerCat
                        + " (");
        if (wantDetails) {
            for (int i = 0; i < maxDocsPerCatCats.size(); ++i) {
                JatecsLogger.status().print(
                        catsDB.getCategoryName(maxDocsPerCatCats.get(i)) + " ");
            }
        }
        JatecsLogger.status().println(")");
        JatecsLogger.status().print(
                "Minimum documents per categories: ["
                        + minDocsPerCatCats.size() + "] " + minDocsPerCat
                        + " (");
        if (wantDetails) {
            for (int i = 0; i < minDocsPerCatCats.size(); ++i) {
                JatecsLogger.status().print(
                        catsDB.getCategoryName(minDocsPerCatCats.get(i)) + " ");
            }
        }
        JatecsLogger.status().println(")");

        JatecsLogger.status().println("");
        JatecsLogger.status().println("Category sizes");
        catIt.begin();
        while (catIt.hasNext()) {
            short category = catIt.next();
            JatecsLogger.status().println(
                    category
                            + "\t"
                            + catsDB.getCategoryName(category)
                            + "\t"
                            + classificationDB
                            .getCategoryDocumentsCount(category));
        }
    }

    /**
     * Print statistics about document and classification.
     *
     * @param docsDB           The documents DB.
     * @param classificationDB The classification DB.
     * @param wantDetails      True if must print more details about the statistics, false
     *                         otherwise.
     */
    public static void printDocumentsClassificationStatistics(
            IDocumentDB docsDB, IClassificationDB classificationDB,
            boolean wantDetails) {
        if (docsDB == null)
            throw new NullPointerException(
                    "The specified documents DB is 'null'");

        if (classificationDB == null)
            throw new NullPointerException(
                    "The specified classification DB is 'null'");

        int docsCount = docsDB.getDocumentsCount();
        double avgCatsPerDoc = 0.0;
        int minCatsPerDoc = Integer.MAX_VALUE;
        TIntArrayList minCatsPerDocDocs = new TIntArrayList();
        int maxCatsPerDoc = Integer.MIN_VALUE;
        TIntArrayList maxCatsPerDocDocs = new TIntArrayList();
        IIntIterator docIt = docsDB.getDocuments();
        docIt.begin();
        while (docIt.hasNext()) {
            int doc = docIt.next();
            int docCats = classificationDB.getDocumentCategoriesCount(doc);
            if (docCats == minCatsPerDoc)
                minCatsPerDocDocs.add(doc);
            if (docCats < minCatsPerDoc) {
                minCatsPerDoc = docCats;
                minCatsPerDocDocs.clear();
                minCatsPerDocDocs.add(doc);
            }
            if (docCats == maxCatsPerDoc)
                maxCatsPerDocDocs.add(doc);
            if (docCats > maxCatsPerDoc) {
                maxCatsPerDoc = docCats;
                maxCatsPerDocDocs.clear();
                maxCatsPerDocDocs.add(doc);
            }
            avgCatsPerDoc += docCats;
        }

        avgCatsPerDoc /= docsCount;

        JatecsLogger.status().println("");
        JatecsLogger.status().println(
                "Average categories per documents: "
                        + Os.generateDouble(avgCatsPerDoc, 3));
        JatecsLogger.status().print(
                "Maximum categories per documents: ["
                        + maxCatsPerDocDocs.size() + "] " + maxCatsPerDoc
                        + " (");
        if (wantDetails) {
            for (int i = 0; i < maxCatsPerDocDocs.size(); ++i) {
                JatecsLogger.status().print(
                        docsDB.getDocumentName(maxCatsPerDocDocs.get(i)) + " ");
            }
        }
        JatecsLogger.status().println(")");
        JatecsLogger.status().print(
                "Minimum categories per documents: ["
                        + minCatsPerDocDocs.size() + "] " + minCatsPerDoc
                        + " (");
        if (wantDetails) {
            for (int i = 0; i < minCatsPerDocDocs.size(); ++i) {
                JatecsLogger.status().print(
                        docsDB.getDocumentName(minCatsPerDocDocs.get(i)) + " ");
            }
        }
    }

    /**
     * Compute and print statistics about equivalent features in documents
     * content.
     *
     * @param featsDB     The features DB.
     * @param contentDB   The content DB.
     * @param wantDetails True if must print more details about the statistics, false
     *                    otherwise.
     */
    public static void printEquivalentFeaturesStatistics(IFeatureDB featsDB,
                                                         IContentDB contentDB, boolean wantDetails) {

        if (featsDB == null)
            throw new NullPointerException(
                    "The specified features DB is 'null'");

        if (contentDB == null)
            throw new NullPointerException("The specified content DB is 'null'");

        IIntIterator featIt = featsDB.getFeatures();
        TIntIntHashMap equivalentFeats = new TIntIntHashMap();
        Vector<TIntArrayList> equivalentFeatsGroups = new Vector<TIntArrayList>();
        featIt.begin();
        while (featIt.hasNext()) {
            int feat = featIt.next();
            if (!equivalentFeats.containsKey(feat)) {
                IIntIterator featDocsIt = contentDB.getFeatureDocuments(feat);
                IIntIterator featIt2 = featsDB.getFeatures();
                while (featIt2.hasNext()) {
                    int feat2 = featIt2.next();
                    if (feat2 == feat)
                        break;
                }
                while (featIt2.hasNext()) {
                    int feat2 = featIt2.next();
                    IIntIterator feat2DocsIt = contentDB
                            .getFeatureDocuments(feat2);
                    featDocsIt.begin();
                    boolean ok = true;
                    while (ok) {
                        if (featDocsIt.hasNext()) {
                            if (feat2DocsIt.hasNext()) {
                                int doc = featDocsIt.next();
                                int doc2 = feat2DocsIt.next();
                                if (doc != doc2)
                                    ok = false;
                            } else
                                ok = false;
                        } else {
                            if (feat2DocsIt.hasNext())
                                ok = false;
                            else
                                break;
                        }
                    }
                    if (ok) {
                        int groupPos;
                        if (!equivalentFeats.containsKey(feat)) {
                            TIntArrayList group = new TIntArrayList();
                            group.add(feat);
                            groupPos = equivalentFeatsGroups.size();
                            equivalentFeatsGroups.add(group);
                            equivalentFeats.put(feat, groupPos);
                        } else
                            groupPos = equivalentFeats.get(feat);
                        equivalentFeats.put(feat2, groupPos);
                        TIntArrayList group = equivalentFeatsGroups
                                .get(groupPos);
                        group.add(feat2);
                    }
                }
            }
        }
        int totalFeats = 0;
        int maxFeatsPerGroup = Integer.MIN_VALUE;
        TIntArrayList maxFeatsPerGroupGroups = new TIntArrayList();

        for (int i = 0; i < equivalentFeatsGroups.size(); ++i) {
            int gsize = equivalentFeatsGroups.get(i).size();
            if (gsize == maxFeatsPerGroup)
                maxFeatsPerGroupGroups.add(i);
            if (gsize > maxFeatsPerGroup) {
                maxFeatsPerGroup = gsize;
                maxFeatsPerGroupGroups.clear();
                maxFeatsPerGroupGroups.add(i);
            }
            totalFeats += gsize;
        }
        double avgFeatsPerGroup = totalFeats
                / (double) equivalentFeatsGroups.size();
        JatecsLogger.status().println("");
        JatecsLogger.status().println(
                "Number of equivalent features: " + totalFeats);
        JatecsLogger.status().println(
                "Number of equivalent features groups: "
                        + equivalentFeatsGroups.size());
        JatecsLogger.status().println(
                "Average features per group: "
                        + Os.generateDouble(avgFeatsPerGroup, 3));
        JatecsLogger.status().print(
                "Maximum features per group: [" + maxFeatsPerGroupGroups.size()
                        + "] " + maxFeatsPerGroup + " (");
        if (wantDetails) {
            for (int i = 0; i < maxFeatsPerGroupGroups.size(); ++i) {
                TIntArrayList group = equivalentFeatsGroups
                        .get(maxFeatsPerGroupGroups.get(i));
                JatecsLogger.status().print(" (");
                for (int j = 0; j < group.size(); ++j) {
                    JatecsLogger.status().print(
                            featsDB.getFeatureName(group.get(j)) + " ");
                }
                JatecsLogger.status().print(")");
            }
        }
        JatecsLogger.status().println(")");
    }

    /**
     * Print statistics about categories.
     *
     * @param catsDB      The categories DB.
     * @param wantDetails True if must print more details about the statistics, false
     *                    otherwise.
     */
    public static void printCategoriesStatistics(ICategoryDB catsDB,
                                                 boolean wantDetails) {
        if (catsDB == null)
            throw new NullPointerException(
                    "The specified categories DB is 'null'");

        JatecsLogger.status().println(
                "Categories DB size: " + catsDB.getCategoriesCount());
        IShortIterator itCats = catsDB.getRootCategories();
        int rootsCount = 0;
        while (itCats.hasNext()) {
            itCats.next();
            rootsCount++;
        }
        JatecsLogger.status().println("Categories roots size: " + rootsCount);

        double avgChildren = 0;
        double avgMinHeight = 1;
        double avgMaxHeight = 1;
        itCats.begin();
        while (itCats.hasNext()) {
            short rootID = itCats.next();
            avgMaxHeight += computeMaxHeight(catsDB, rootID, 1);
            avgMinHeight += computeMinHeight(catsDB, rootID, 1);
            avgChildren += computeAvgChildren(catsDB, rootID, 0, 1);
        }

        avgChildren /= rootsCount;
        avgMinHeight /= rootsCount;
        avgMaxHeight /= rootsCount;

        JatecsLogger.status().println(
                "Categories average number of children: " + avgChildren);
        JatecsLogger.status().println(
                "Categories average max tree height: " + avgMaxHeight);
        JatecsLogger.status().println(
                "Categories average min tree height: " + avgMinHeight);

        if (wantDetails) {
            JatecsLogger.status().println("Print trees of categories");
            JatecsLogger.status().println("-------------------------");
            JatecsLogger.status().println("");
            itCats.begin();
            while (itCats.hasNext()) {
                short rootID = itCats.next();
                printCategoryTree(catsDB, rootID, 0);
                JatecsLogger.status().print("\n");
            }
        }
    }

    protected static void printCategoryTree(ICategoryDB catsDB, short catID,
                                            int level) {
        String catName = catsDB.getCategoryName(catID);
        for (int i = 0; i < level; i++)
            JatecsLogger.status().print("\t");
        JatecsLogger.status().println("" + catName + "(id:" + catID + ")");
        IShortIterator itCats = catsDB.getChildCategories(catID);
        while (itCats.hasNext()) {
            short child = itCats.next();
            printCategoryTree(catsDB, child, level + 1);
        }
    }

    protected static int computeMaxHeight(ICategoryDB catsDB, short catID,
                                          int currentLevel) {
        IShortIterator children = catsDB.getChildCategories(catID);
        int level = currentLevel;
        while (children.hasNext()) {
            short childID = children.next();
            int childLevel = computeMaxHeight(catsDB, childID, currentLevel + 1);
            if (childLevel > level)
                level = childLevel;
        }

        return level;
    }

    protected static int computeMinHeight(ICategoryDB catsDB, short catID,
                                          int currentLevel) {
        IShortIterator children = catsDB.getChildCategories(catID);
        int level = currentLevel;
        if (children.hasNext()) {
            level = Integer.MAX_VALUE;
            while (children.hasNext()) {
                short childID = children.next();
                int childLevel = computeMinHeight(catsDB, childID,
                        currentLevel + 1);
                if (childLevel < level)
                    level = childLevel;
            }
        }

        return level;
    }

    protected static double computeAvgChildren(ICategoryDB catsDB, short catID,
                                               double avg, int n) {
        IShortIterator children = catsDB.getChildCategories(catID);
        if (children.hasNext()) {
            avg = avg + ((catsDB.getChildCategoriesCount(catID) - avg) / n);
            while (children.hasNext()) {
                short childID = children.next();
                avg = computeAvgChildren(catsDB, childID, avg, n + 1);

            }
        }
        return avg;
    }

    /**
     * Prints some statistics on a parallel index.
     *
     * @param index
     */
    public static void printParallelIndexInfo(IParallelMultilingualIndex index) {
        showMainIndexInfo(index);
        showDocumentsByLanguage(index);
        showCategoriesIndexInfo(index);
        showParallelInfo(index);
    }

    private static void showParallelInfo(IParallelMultilingualIndex index) {
        int numLanguages = index.getLanguageDB().getLanguagesCount();
        int numFullViews = 0;
        ArrayList<ArrayList<Integer>> clusters = index.getParallelDB()
                .getViewsClusters();
        for (int i = 0; i < clusters.size(); i++) {
            if (clusters.get(i).size() == numLanguages)
                numFullViews++;
        }
        System.out.println("Num full views: " + numFullViews);
    }

    /**
     * Prints some statistics on a cross language index.
     *
     * @param index
     */
    public static void printCrossLanguageIndexInfo(IMultilingualIndex index) {
        showMainIndexInfo(index);
        showDocumentsByLanguage(index);
        showCategoriesIndexInfo(index);
        showFeatureCLInfo(index);
    }

    /**
     * Prints some statistics on a index.
     *
     * @param index
     */
    public static void printIndexInfo(IIndex index) {
        showMainIndexInfo(index);
        showCategoriesIndexInfo(index);
    }

    private static void showMainIndexInfo(IIndex index) {
        System.out.println("Cross Lingual Index: " + index.getName());
        System.out
                .println("Docs: " + index.getDocumentDB().getDocumentsCount());
        System.out.println("Feats: " + index.getFeatureDB().getFeaturesCount());
        System.out.println("Cats: "
                + index.getCategoryDB().getCategoriesCount());
    }

    private static void showCategoriesIndexInfo(IIndex index) {
        IShortIterator cats = index.getCategoryDB().getCategories();
        System.out.println("Categories");
        while (cats.hasNext()) {
            short catID = cats.next();
            System.out.println("\t"
                    + index.getCategoryDB().getCategoryName(catID)
                    + "\t"
                    + index.getClassificationDB().getCategoryDocumentsCount(
                    catID));
        }
    }

    private static void showDocumentsByLanguage(IMultilingualIndex index) {
        Iterator<LanguageLabel> it = index.getLanguageDB().getLanguages();
        while (it.hasNext()) {
            LanguageLabel lang = it.next();
            System.out.println("\t"
                    + lang.toString()
                    + ": "
                    + index.getDocumentLanguageDB()
                    .getDocumentsInLanguage(lang).size());
        }
    }

    private static void showFeatureCLInfo(IMultilingualIndex index) {
        int nL = index.getLanguageDB().getLanguagesCount();
        HashMap<LanguageLabel, Integer> langIndex = new HashMap<>();
        HashMap<Integer, LanguageLabel> indexLang = new HashMap<>();

        Iterator<LanguageLabel> langs = index.getLanguageDB().getLanguages();
        int lind = 0;
        while (langs.hasNext()) {
            LanguageLabel lang = langs.next();
            langIndex.put(lang, lind);
            indexLang.put(lind, lang);
            lind++;
        }

        langs = index.getLanguageDB().getLanguages();
        HashMap<LanguageLabel, HashSet<Integer>> featsByLang = new HashMap<>();
        while (langs.hasNext()) {
            LanguageLabel lang = langs.next();
            featsByLang.put(lang, new HashSet<Integer>());
            HashSet<Integer> docsLang = index.getDocumentLanguageDB()
                    .getDocumentsInLanguage(lang);
            for (int docID : docsLang) {
                IIntIterator feats = index.getContentDB().getDocumentFeatures(
                        docID);
                while (feats.hasNext()) {
                    featsByLang.get(lang).add(feats.next());
                }
            }
        }

        int[][] sharedFeats = new int[nL][nL];
        for (LanguageLabel l1 : langIndex.keySet()) {
            int l1ind = langIndex.get(l1);
            for (LanguageLabel l2 : langIndex.keySet()) {
                int l2ind = langIndex.get(l2);
                HashSet<Integer> intersection = new HashSet<>(
                        featsByLang.get(l1));
                intersection.retainAll(featsByLang.get(l2));
                sharedFeats[l1ind][l2ind] = intersection.size();
                sharedFeats[l2ind][l1ind] = intersection.size();
            }
        }

        for (int i = 0; i < nL; i++) {
            System.out.print((i == 0 ? "\t" : "") + indexLang.get(i)
                    + (i < nL - 1 ? "\t" : "\n"));
        }
        for (int i = 0; i < nL; i++) {
            System.out.print(indexLang.get(i) + "\t");
            for (int j = 0; j < nL; j++) {
                System.out.print(sharedFeats[i][j] + (j < nL - 1 ? "\t" : ""));
            }
            System.out.println();
        }

        int[] sharedLangCount = new int[nL];
        Arrays.fill(sharedLangCount, 0);
        IIntIterator feats = index.getFeatureDB().getFeatures();
        while (feats.hasNext()) {
            int featid = feats.next();
            HashSet<LanguageLabel> inLangs = new HashSet<>();
            IIntIterator docs = index.getContentDB()
                    .getFeatureDocuments(featid);
            while (docs.hasNext()) {
                int docID = docs.next();
                inLangs.add(index.getDocumentLanguageDB().getDocumentLanguage(
                        docID));
            }
            int inLangsSize = inLangs.size();

            if (inLangsSize > 0)
                sharedLangCount[inLangsSize - 1]++;

        }

        System.out.println();
        for (int i = 0; i < nL; i++) {
            System.out.println("Feats in " + (i + 1) + " langs:\t"
                    + sharedLangCount[i]);
        }

    }
}
