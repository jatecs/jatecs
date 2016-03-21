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

package it.cnr.jatecs.classification.treeboost;

import gnu.trove.TIntArrayList;
import gnu.trove.TShortArrayList;
import it.cnr.jatecs.classification.BaseLearner;
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.interfaces.ILearner;
import it.cnr.jatecs.classification.interfaces.ILearnerRuntimeCustomizer;
import it.cnr.jatecs.indexes.DB.interfaces.ICategoryDB;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexing.tsr.ITsr;
import it.cnr.jatecs.indexing.weighting.IWeighting;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.Os;
import it.cnr.jatecs.utils.iterators.TIntArrayListIterator;
import it.cnr.jatecs.utils.iterators.TShortArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.util.Vector;

public class TreeBoostCMCLearner extends BaseLearner {

    /**
     * The learner object which construct classifiers.
     */
    protected ILearner _learner;

    /**
     * The weighting module.
     */
    protected IWeighting _weighting;

    /**
     * The tsr module.
     */
    protected ITsr _tsr;

    public TreeBoostCMCLearner(ILearner learner) {
        assert (learner != null);
        _customizer = new TreeBoostLearnerCustomizer(null);
        _learner = learner;
        _weighting = null;
        _tsr = null;
    }

    @Override
    public TreeBoostLearnerCustomizer getRuntimeCustomizer() {
        return (TreeBoostLearnerCustomizer) _customizer;
    }

    @Override
    public void setRuntimeCustomizer(
            Vector<ILearnerRuntimeCustomizer> customizers) {
    }

    /**
     * Set the weighting module to use to compute the features weight inside
     * documents.
     *
     * @param w The weighting module.
     */
    public void setWeighting(IWeighting w) {
        _weighting = w;
    }

    /**
     * Set the TSR module to use to reduce the feature space of documents.
     *
     * @param tsr The TSR module.
     */
    public void setTSR(ITsr tsr) {
        _tsr = tsr;
    }

    public IClassifier build(IIndex trainingIndex) {
        TreeBoostClassifier c = new TreeBoostClassifier();

        // Construct classifiers at root level.
        constructClassifiersAt(Short.MIN_VALUE, trainingIndex.getCategoryDB(),
                trainingIndex, c);

        return c;
    }

    protected Vector<String> selectAncestors(short catID, IIndex training,
                                             TShortArrayList toRemove) {

        Vector<String> ancestors = new Vector<String>();
        boolean selected = false;
        short curCatID = catID;
        while (!selected) {
            toRemove.sort();
            IShortIterator parents = training.getCategoryDB()
                    .getParentCategories(curCatID);
            short parentID = parents.next();
            IShortIterator siblings = training.getCategoryDB()
                    .getSiblingCategories(parentID);
            while (siblings.hasNext()) {
                selected = true;
                short cat = siblings.next();
                int pos = toRemove.binarySearch(cat);
                toRemove.remove(pos);
                toRemove.sort();

                ancestors.add(training.getCategoryDB().getCategoryName(cat));
            }

            curCatID = parentID;
        }

        return ancestors;
    }

    public void constructClassifiersAt(short catID, ICategoryDB catsDB,
                                       IIndex training, TreeBoostClassifier classifiers) {
        IShortIterator childs = null;
        if (catID == Short.MIN_VALUE)
            childs = training.getCategoryDB().getRootCategories();
        else
            childs = training.getCategoryDB().getChildCategories(catID);

        if (!childs.hasNext())
            // Nothing to do.
            return;

        JatecsLogger.status().print(
                "---> Start analyzing the hierarchy level owned by category <"
                        + (catID == Short.MIN_VALUE ? "RootCategory" : training
                        .getCategoryDB().getCategoryName(catID)) + ">."
                        + Os.newline());
        // JatecsLogger.status().println("---> Start analyzing the hierarchy level owned by category <"+(catID
        // ==
        // Short.MIN_VALUE?"RootCategory":training.getCategoriesDB().getCategoryName(catID))+">.");

        // Create a temporary index containing only the valid categories.
        IIndex idx = training.cloneIndex();
        TShortArrayList toRemove = new TShortArrayList();
        short nextCatID = childs.next();
        for (short i = 0; i < training.getCategoryDB().getCategoriesCount(); i++) {
            if (i == nextCatID) {
                if (childs.hasNext())
                    nextCatID = childs.next();
                continue;
            }

            toRemove.add(i);
        }

        Vector<String> ancestors = new Vector<String>();
        if (toRemove.size() == (training.getCategoryDB().getCategoriesCount() - 1)) {

            ancestors = selectAncestors(nextCatID, training, toRemove);
        }

        idx.removeCategories(new TShortArrayListIterator(toRemove));

        // Use only positives of the selected categories.
        TIntArrayList docsRemoved = new TIntArrayList();
        IIntIterator docs = idx.getDocumentDB().getDocuments();
        while (docs.hasNext()) {
            int docID = docs.next();
            if (idx.getClassificationDB().getDocumentCategoriesCount(docID) == 0)
                docsRemoved.add(docID);
        }

        idx.removeDocuments(new TIntArrayListIterator(docsRemoved), false);

        toRemove = new TShortArrayList();
        for (int i = 0; i < ancestors.size(); i++) {
            short cat = idx.getCategoryDB().getCategory(ancestors.get(i));
            toRemove.add(cat);
        }
        idx.removeCategories(new TShortArrayListIterator(toRemove));

        JatecsLogger.status().print(
                "Creating a temporary index containing "
                        + idx.getDocumentDB().getDocumentsCount()
                        + " documents...");
        JatecsLogger.status().println(
                "done. The categories valid are "
                        + idx.getCategoryDB().getCategoriesCount() + ".");

        if (_tsr != null) {
            JatecsLogger.status().println("Now apply TSR to index.");
            _tsr.computeTSR(idx);
            JatecsLogger.status().println("Ok. TSR applied.");
        }

        if (_weighting != null) {
            JatecsLogger.status().println(
                    "Now apply weighting to document features.");
            idx = _weighting.computeWeights(idx);
            JatecsLogger.status().println("Ok. Weighting operation done.");
        }

        JatecsLogger.status().println(
                "The number of valid features is "
                        + idx.getFeatureDB().getFeaturesCount() + ".");

        if (getRuntimeCustomizer() != null) {
            if (getRuntimeCustomizer().getInternalCustomizer(
                    TreeBoostLearnerCustomizer.ALL_LEVELS,
                    TreeBoostLearnerCustomizer.ALL_CATEGORIES) != null)
                _learner.setRuntimeCustomizer(getRuntimeCustomizer()
                        .getInternalCustomizer(
                                TreeBoostLearnerCustomizer.ALL_LEVELS,
                                TreeBoostLearnerCustomizer.ALL_CATEGORIES));
        }

        // Construct the classifier for this level.
        IClassifier c = _learner.build(idx);

        // Save data at this level.
        short levelCatID = -1;
        if (catID != Short.MIN_VALUE) {
            String catName = training.getCategoryDB().getCategoryName(catID);
            short globalCatID = catsDB.getCategory(catName);
            classifiers._map.put(globalCatID, c);
            levelCatID = globalCatID;
        } else {
            classifiers._map.put(catID, c);
            levelCatID = catID;
        }

        // Init iterator.
        childs.begin();

        while (childs.hasNext()) {
            short curCatID = childs.next();

            // Get current global catID and add reference to classifier object.
            String catName = training.getCategoryDB().getCategoryName(curCatID);
            short globalCatID = catsDB.getCategory(catName);
            TreeBoostClassifierAddress addr = new TreeBoostClassifierAddress();
            addr.level = levelCatID;
            addr.categoryID = idx.getCategoryDB().getCategory(catName);

            classifiers._mapCatLevel.put(globalCatID, addr);

            IShortIterator ch = training.getCategoryDB().getChildCategories(
                    curCatID);
            if (!ch.hasNext())
                continue;

            JatecsLogger
                    .status()
                    .println(
                            "The child category <"
                                    + training.getCategoryDB().getCategoryName(
                                    curCatID)
                                    + "> is the owner of a subtree of categories. Analyze it.");

            // Recursion over current category.
            constructClassifiersAt(curCatID, catsDB, training, classifiers);
        }
    }

    protected IIndex selectPositives(short catID, IIndex training) {
        // First create a new index.
        IIndex idx = training.cloneIndex();

        IShortIterator childCats = getAllChildsCategoriesFor(idx, catID);
        short nextCatID = Short.MIN_VALUE;
        if (childCats.hasNext())
            nextCatID = childCats.next();

        // Remove unwanted categories.
        TShortArrayList toRemove = new TShortArrayList();
        for (short i = 0; i < training.getCategoryDB().getCategoriesCount(); i++) {
            if (i == nextCatID) {
                if (childCats.hasNext())
                    nextCatID = childCats.next();
                continue;
            }

            toRemove.add(i);
        }
        toRemove.sort();
        idx.removeCategories(new TShortArrayListIterator(toRemove));

        // Remove unwanted documents.
        TIntArrayList docsToRemove = new TIntArrayList();
        IIntIterator docs = idx.getDocumentDB().getDocuments();
        while (docs.hasNext()) {
            int docID = docs.next();
            IShortIterator curCats = idx.getClassificationDB()
                    .getDocumentCategories(docID);
            if (!curCats.hasNext())
                docsToRemove.add(docID);
        }

        docsToRemove.sort();
        idx.removeDocuments(new TIntArrayListIterator(docsToRemove), false);

        return idx;
    }

    protected IShortIterator getAllChildsCategoriesFor(IIndex idx, short catID) {
        TShortArrayList childs = new TShortArrayList();

        IShortIterator curChilds = idx.getCategoryDB()
                .getChildCategories(catID);
        while (curChilds.hasNext()) {
            short curCatID = curChilds.next();
            IShortIterator c = getAllChildsCategoriesFor(idx, curCatID);
            // Merge the results with current list.
            while (c.hasNext()) {
                short id = c.next();
                if (!childs.contains(id))
                    childs.add(id);
            }
        }

        // Add this category.
        if (!childs.contains(catID))
            childs.add(catID);

        childs.sort();
        return new TShortArrayListIterator(childs);
    }

    @Override
    public ILearnerRuntimeCustomizer getRuntimeCustomizer(short catID) {
        return null;
    }

    @Override
    public IClassifier mergeClassifiers(Vector<IClassifier> classifiers) {
        return null;
    }
}
