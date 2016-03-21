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

package it.cnr.jatecs.classification.bagging;

import gnu.trove.TIntArrayList;
import it.cnr.jatecs.classification.BaseLearner;
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.interfaces.ILearner;
import it.cnr.jatecs.classification.interfaces.ILearnerRuntimeCustomizer;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveMainIndexBuilder;
import it.cnr.jatecs.indexing.tsr.ITsr;
import it.cnr.jatecs.indexing.weighting.IWeighting;
import it.cnr.jatecs.utils.IOperationStatusListener;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.iterators.TIntArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.util.Random;
import java.util.Vector;

/**
 * This is a bagging learner. See http://link.springer.com/article/10.1007%2FBF00058655.
 *
 */
public class BaggingLearner extends BaseLearner {

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
    /**
     * The status listener
     */
    protected IOperationStatusListener _status;
    /**
     * The seed used for initialization of the random number generator
     */
    private long seed;

    public BaggingLearner(long seed, ILearner learner) {
        this(seed, learner, null);
    }

    public BaggingLearner(long seed, ILearner learner,
                          IOperationStatusListener status) {
        assert (learner != null);
        this.seed = seed;
        _customizer = new BaggingLearnerCustomizer();
        _learner = learner;
        _weighting = null;
        _tsr = null;
        _status = status;
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

        if (_status != null)
            _status.operationStatus(0.0);

        int bagCount = ((BaggingLearnerCustomizer) _customizer).getBagCount();

        BaggingClassifier classifier = new BaggingClassifier(bagCount);

        Random random = new Random(seed);

        int[] docs = new int[trainingIndex.getDocumentDB().getDocumentsCount()];

        for (int bagID = 0; bagID < bagCount; ++bagID) {

            for (int i = 0; i < docs.length; ++i)
                docs[i] = random.nextInt(docs.length);

            IIndex idx = trainingIndex.cloneIndex();

            int[] countDocs = new int[docs.length];
            for (int i = 0; i < docs.length; ++i)
                countDocs[docs[i]]++;

            TroveMainIndexBuilder build = new TroveMainIndexBuilder(idx);
            TIntArrayList missingDocs = new TIntArrayList();
            for (int i = 0; i < docs.length; ++i) {
                if (countDocs[i] == 0)
                    missingDocs.add(i);
                else if (countDocs[i] > 1) {
                    Vector<String> feats = new Vector<String>();
                    IIntIterator iter = idx.getContentDB().getDocumentFeatures(
                            i);
                    while (iter.hasNext()) {
                        int feat = iter.next();
                        int freq = idx.getContentDB()
                                .getDocumentFeatureFrequency(i, feat);
                        String featName = idx.getFeatureDB().getFeatureName(
                                feat);
                        for (int k = 0; k < freq; ++k)
                            feats.add(featName);
                    }

                    Vector<String> categoryNames = new Vector<String>();
                    IShortIterator cats = idx.getClassificationDB()
                            .getDocumentCategories(i);
                    while (cats.hasNext()) {
                        short cat = cats.next();
                        categoryNames.add(idx.getCategoryDB().getCategoryName(
                                cat));
                    }

                    String docName = idx.getDocumentDB().getDocumentName(i);
                    int toadd = countDocs[i] - 1;
                    for (int j = 0; j < toadd; ++j)
                        build.addDocument(docName,
                                feats.toArray(new String[0]),
                                categoryNames.toArray(new String[0]));
                }
            }

            idx.removeDocuments(new TIntArrayListIterator(missingDocs), false);

            if (_tsr != null) {
                JatecsLogger.status().println("Applying TSR to index.");
                _tsr.computeTSR(idx);
                JatecsLogger.status().println("Ok. TSR applied.");
            }

            if (_weighting != null) {
                JatecsLogger.status().println(
                        "Applying weighting to document features.");
                idx = _weighting.computeWeights(idx);
                JatecsLogger.status().println("Ok. Weighting operation done.");
            }

            JatecsLogger.status().println(
                    "The number of valid features is "
                            + idx.getFeatureDB().getFeaturesCount() + ".");

            if (((BaggingLearnerCustomizer) _customizer)
                    .getInternalCustomizer() != null)
                _learner.setRuntimeCustomizer(((BaggingLearnerCustomizer) _customizer)
                        .getInternalCustomizer());

            IClassifier c = _learner.build(idx);

            classifier._classifiers[bagID] = c;
        }

        if (_status != null)
            _status.operationStatus(100.0);
        return classifier;
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
}
