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
import it.cnr.jatecs.classification.interfaces.IOptimizatorRuntimeCustomizer;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexing.tsr.ITsr;
import it.cnr.jatecs.indexing.weighting.IWeighting;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.Os;
import it.cnr.jatecs.utils.iterators.TIntArrayListIterator;
import it.cnr.jatecs.utils.iterators.TShortArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Vector;

public class TreeBoostCNLearner extends BaseLearner {

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

    protected IIndex _globalIndex;

    /**
     * The negatives chooser used.
     */
    protected INegativesChooser _negativesChooser;

    protected boolean _saveInternalCustomizer;
    protected TreeBoostLearnerCustomizer _lastSavedCustomizer;

    public TreeBoostCNLearner(ILearner learner) {
        assert (learner != null);
        _customizer = new TreeBoostLearnerCustomizer();
        _learner = learner;
        _weighting = null;
        _tsr = null;
        _negativesChooser = new SiblingNegativesChooser();
        _saveInternalCustomizer = false;
    }

    public void setNegativesChooser(INegativesChooser c) {
        _negativesChooser = c;
    }

    public ILearnerRuntimeCustomizer getRuntimeCustomizer() {
        return _customizer;
    }

    @Override
    public void setRuntimeCustomizer(
            Vector<ILearnerRuntimeCustomizer> customizers) {

    }

    public void setRuntimeCustomizer(TreeBoostLearnerCustomizer customizer) {
        _customizer = customizer;
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

        _globalIndex = trainingIndex;

        // Intialize negatives chooser.
        _negativesChooser.initialize(_globalIndex);

        _lastSavedCustomizer = new TreeBoostLearnerCustomizer();

        // Construct classifiers at root level.
        constructClassifiersAt(Short.MIN_VALUE, trainingIndex, c);

        // Release resources from negatives chooser.
        _negativesChooser.release();

        _globalIndex = null;

        return c;
    }

    protected void writeNegativesDebug(String catName, TIntArrayList docsList) {
        try {
            File f = new File(Os.getTemporaryDirectory() + Os.pathSeparator()
                    + "TreeBoostCN_"
                    + _negativesChooser.getClass().getSimpleName());
            f.mkdirs();

            String path = Os.getTemporaryDirectory() + Os.pathSeparator()
                    + "TreeBoostCN_"
                    + _negativesChooser.getClass().getSimpleName()
                    + Os.pathSeparator() + catName + ".txt";
            PrintWriter pw = new PrintWriter(new BufferedOutputStream(
                    new FileOutputStream(path)));
            TIntArrayListIterator docs = new TIntArrayListIterator(docsList);

            while (docs.hasNext()) {
                int docID = docs.next();
                pw.println("" + docID);
            }

            pw.close();
        } catch (Exception e) {
        }
    }

    public void constructClassifiersAt(short catID, IIndex training,
                                       TreeBoostClassifier classifiers) {
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

        Vector<IClassifier> cl = new Vector<IClassifier>();
        Vector<ILearnerRuntimeCustomizer> clLRC = new Vector<ILearnerRuntimeCustomizer>();

        childs.begin();
        while (childs.hasNext()) {
            short categoryID = childs.next();

            // Create a temporary index containing only the valid categories.
            JatecsLogger.status().print(
                    "Creating a temporary index for category <"
                            + training.getCategoryDB().getCategoryName(
                            categoryID) + ">...");
            IIndex idx = training.cloneIndex();
            TShortArrayList toRemove = new TShortArrayList();
            for (short i = 0; i < training.getCategoryDB().getCategoriesCount(); i++) {
                if (i == categoryID) {
                    continue;
                }

                toRemove.add(i);
            }

            idx.removeCategories(new TShortArrayListIterator(toRemove));

            // Select negatives documents for current category.
            TIntArrayList docsList = new TIntArrayList();
            TIntArrayListIterator docs = _negativesChooser
                    .selectNegatives(training.getCategoryDB().getCategoryName(
                            categoryID));
            while (docs.hasNext()) {
                int docID = docs.next();
                docsList.add(docID);
            }

            int docNegatives = docsList.size();

            IIntIterator docsPositive = training.getClassificationDB()
                    .getCategoryDocuments(categoryID);
            while (docsPositive.hasNext()) {
                int documentID = docsPositive.next();
                assert (!docsList.contains(documentID));
                docsList.add(documentID);
            }

            // DEBUG
            // docsList.sort();
            // writeNegativesDebug(training.getCategoriesDB().getCategoryName(categoryID),
            // docsList);

            int docPositives = docsList.size() - docNegatives;

            // Remove all unwanted documents from index.
            TIntArrayList docsToRemove = new TIntArrayList();
            docsList.sort();
            System.out.println("Idx size: "
                    + idx.getDocumentDB().getDocumentsCount());
            for (int i = 0; i < idx.getDocumentDB().getDocumentsCount(); i++) {
                if (docsList.contains(i))
                    continue;

                docsToRemove.add(i);
            }

            docsToRemove.sort();
            idx.removeDocuments(new TIntArrayListIterator(docsToRemove), false);
            JatecsLogger.status().println(
                    "done. The index now contains " + docPositives
                            + " document(s) positive and " + docNegatives
                            + " document(s) negative.");

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

            if (((TreeBoostLearnerCustomizer) _customizer)
                    .getInternalCustomizer(catID, categoryID) != null)
                _learner.setRuntimeCustomizer(((TreeBoostLearnerCustomizer) _customizer)
                        .getInternalCustomizer(catID, categoryID));

            // Construct the classifier for this level.
            IClassifier c = _learner.build(idx);

            // Save classifier.
            cl.add(c);
            clLRC.add(_learner.getRuntimeCustomizer());

            if (_learner.getRuntimeCustomizer() instanceof IOptimizatorRuntimeCustomizer) {
                IOptimizatorRuntimeCustomizer cu = (IOptimizatorRuntimeCustomizer) _learner
                        .getRuntimeCustomizer();
                _lastSavedCustomizer.setInternalCustomizer(catID, categoryID,
                        cu.getOptimizedLearnerRuntimeCustomizer());
            } else
                _lastSavedCustomizer.setInternalCustomizer(catID, categoryID,
                        _learner.getRuntimeCustomizer());
        }

        // Merge all classifiers into one classfier.
        IClassifier resCl = _learner.mergeClassifiers(cl);
        // _learner.setRuntimeCustomizer(clLRC);

        for (int k = 0; k < cl.size(); k++)
            cl.get(k).destroy();

        // Save data at this level.
        short levelCatID = -1;
        if (catID != Short.MIN_VALUE) {
            short globalCatID = catID;
            classifiers._map.put(globalCatID, resCl);
            levelCatID = globalCatID;
        } else {
            classifiers._map.put(catID, resCl);
            levelCatID = catID;
        }

        // Init iterator.
        childs.begin();

        short pos = 0;
        while (childs.hasNext()) {
            short curCatID = childs.next();

            // Get current global catID and add reference to classifier object.
            short globalCatID = curCatID;
            TreeBoostClassifierAddress addr = new TreeBoostClassifierAddress();
            addr.level = levelCatID;
            addr.categoryID = pos++;

            classifiers._mapCatLevel.put(globalCatID, addr);

            IShortIterator ch = training.getCategoryDB().getChildCategories(
                    curCatID);
            if (!ch.hasNext())
                continue;

            // Recursion over current category.
            constructClassifiersAt(curCatID, training, classifiers);
        }
    }

    protected IShortIterator getAllChildsCategoriesFor(IIndex idx, short catID) {
        TShortArrayList childs = new TShortArrayList();

        IShortIterator curChilds = null;
        if (catID == Short.MIN_VALUE)
            curChilds = idx.getCategoryDB().getRootCategories();
        else
            curChilds = idx.getCategoryDB().getChildCategories(catID);

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
        if (!childs.contains(catID) && catID != Short.MIN_VALUE)
            childs.add(catID);

        childs.sort();
        return new TShortArrayListIterator(childs);
    }

    public TreeBoostLearnerCustomizer getLastGeneratedLearnerCustomizer() {
        return _lastSavedCustomizer;
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
