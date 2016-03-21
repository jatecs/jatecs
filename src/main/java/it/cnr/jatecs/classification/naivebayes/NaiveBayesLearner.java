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

package it.cnr.jatecs.classification.naivebayes;

import it.cnr.jatecs.classification.BaseLearner;
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.interfaces.ILearnerRuntimeCustomizer;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.Os;
import it.cnr.jatecs.utils.TextualProgressBar;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;
import it.cnr.jatecs.weighting.mmap.MemoryMappedWeighting3DBuilder;

import java.io.IOException;
import java.util.Vector;

/*
 * http://en.wikipedia.org/wiki/Naive_Bayesian_classification
 */
public class NaiveBayesLearner extends BaseLearner {

    public NaiveBayesLearner() {
        super();
        _customizer = new NaiveBayesLearnerCustomizer();
    }

    public IClassifier build(IIndex trainingIndex) {
        String path = Os.getTemporaryDirectory() + Os.pathSeparator()
                + "naivebayes" + Os.pathSeparator()
                + System.currentTimeMillis();
        String name = "naivebayes";

        NaiveBayesLearnerCustomizer customizer = (NaiveBayesLearnerCustomizer) _customizer;

        MemoryMappedWeighting3DBuilder weights = new MemoryMappedWeighting3DBuilder(
                trainingIndex.getFeatureDB().getFeaturesCount() + 1,
                trainingIndex.getCategoryDB().getCategoriesCount(), 1);
        try {
            weights.open(path, name, true);
        } catch (IOException e) {
            throw new RuntimeException("Error initializing the bayes learner",
                    e);
        }

        // File file = new File(path+Os.pathSeparator()+name);
        // file.deleteOnExit();
        // file = new File(path);
        // file.deleteOnExit();

        weights.setName(name);

        TextualProgressBar pb = new TextualProgressBar(
                "Computing a-priori probabilities");
        int catsCount = trainingIndex.getCategoryDB().getCategoriesCount();

        if (customizer._multinomial) {
            IShortIterator cats = trainingIndex.getCategoryDB().getCategories();
            int docsCount = trainingIndex.getDocumentDB().getDocumentsCount();
            while (cats.hasNext()) {
                short cat = cats.next();
                IIntIterator docIt = trainingIndex.getDocumentDB()
                        .getDocuments();
                int catLenght = 0;
                int notCatLenght = 0;
                while (docIt.hasNext()) {
                    int doc = docIt.next();
                    if (trainingIndex.getClassificationDB()
                            .hasDocumentCategory(doc, cat))
                        catLenght += trainingIndex.getDocumentLength(doc, cat);
                    else
                        notCatLenght += trainingIndex.getDocumentLength(doc,
                                cat);
                }
                int catFreq = trainingIndex.getClassificationDB()
                        .getCategoryDocumentsCount(cat);
                int notCatFreq = docsCount - catFreq;
                IIntIterator feats = trainingIndex.getDomainDB()
                        .getCategoryFeatures(cat); // local support
                while (feats.hasNext()) {
                    int featsCount = trainingIndex.getDomainDB()
                            .getCategoryFeaturesCount(cat); // local support
                    int feat = feats.next();
                    int featCatFreq = 0;
                    int featNotCatFreq = 0;
                    docIt = trainingIndex.getFeatureDocuments(feat, cat);
                    while (docIt.hasNext()) {
                        int doc = docIt.next();
                        if (trainingIndex.getClassificationDB()
                                .hasDocumentCategory(doc, cat))
                            featCatFreq += trainingIndex
                                    .getDocumentFeatureFrequency(doc, feat, cat);
                        else
                            featNotCatFreq += trainingIndex
                                    .getDocumentFeatureFrequency(doc, feat, cat);
                    }
                    double featCatP = (featCatFreq + customizer._smooth)
                            / (catLenght + featsCount * customizer._smooth);
                    double featNotCatP = (featNotCatFreq + customizer._smooth)
                            / (notCatLenght + featsCount * customizer._smooth);
                    double weight = featCatP / featNotCatP;
                    weights.setWeight(weight, feat, cat, 0);
                }
                double catP = (catFreq + 1.0) / (docsCount + 2.0);
                double notCatP = (notCatFreq + 1.0) / (docsCount + 2.0);
                double catWeight = catP / notCatP;
                weights.setWeight(catWeight, trainingIndex.getFeatureDB()
                        .getFeaturesCount(), cat, 0);
                pb.signal(((int) (cat * 100 / ((double) catsCount))));
            }
        } else { // multivariate
            IShortIterator cats = trainingIndex.getCategoryDB().getCategories();
            double docsCount = trainingIndex.getDocumentDB()
                    .getDocumentsCount() + 2 * customizer._smooth;
            while (cats.hasNext()) {
                short cat = cats.next();
                double catFreq = trainingIndex.getClassificationDB()
                        .getCategoryDocumentsCount(cat) + customizer._smooth;
                double notCatFreq = docsCount - catFreq;
                IIntIterator feats = trainingIndex.getDomainDB()
                        .getCategoryFeatures(cat); // local support
                while (feats.hasNext()) {
                    int feat = feats.next();
                    double featFreq = trainingIndex.getContentDB()
                            .getFeatureDocumentsCount(feat)
                            + 2
                            * customizer._smooth;
                    double featCatFreq = 0;
                    IIntIterator docIt = trainingIndex.getFeatureDocuments(
                            feat, cat);
                    while (docIt.hasNext()) {
                        if (trainingIndex.getClassificationDB()
                                .hasDocumentCategory(docIt.next(), cat))
                            ++featCatFreq;
                    }
                    featCatFreq += customizer._smooth;
                    double featNotCatFreq = featFreq - featCatFreq;
                    double featCatP = featCatFreq / ((double) catFreq);
                    double featNotCatP = featNotCatFreq / ((double) notCatFreq);
                    double weight = featCatP / featNotCatP;
                    weights.setWeight(weight, feat, cat, 0);
                }
                double catP = catFreq / ((double) docsCount);
                double notCatP = notCatFreq / ((double) docsCount);
                double catWeight = catP / notCatP;
                weights.setWeight(catWeight, trainingIndex.getFeatureDB()
                        .getFeaturesCount(), cat, 0);
                pb.signal(((int) (cat * 100 / ((double) catsCount))));
            }
        }
        pb.signal(100);

        try {
            weights.close();
        } catch (IOException e) {
            throw new RuntimeException("Error initializing the bayes learner "
                    + e.getMessage(), e);
        }
        NaiveBayesClassifier nbc = new NaiveBayesClassifier();
        try {
            nbc.read(path);
        } catch (IOException e) {
            throw new RuntimeException("Error initializing the bayes learner "
                    + e.getMessage(), e);
        }
        return nbc;
    }

    @Override
    public IClassifier mergeClassifiers(Vector<IClassifier> classifiers) {
        String path = Os.getTemporaryDirectory() + Os.pathSeparator()
                + "naivebayes" + Os.pathSeparator()
                + System.currentTimeMillis();
        String name = "naivebayes";
        NaiveBayesClassifier cl = (NaiveBayesClassifier) classifiers.get(0);
        int numFeatures = cl._weights.getFirstDimensionSize();
        int numCategories = classifiers.size();
        MemoryMappedWeighting3DBuilder weights = new MemoryMappedWeighting3DBuilder(
                numFeatures, numCategories, 1);
        try {
            weights.open(path, name, true);
        } catch (IOException e) {
            throw new RuntimeException("Error initializing the bayes learner",
                    e);
        }

        for (short catID = 0; catID < classifiers.size(); catID++) {
            cl = (NaiveBayesClassifier) classifiers.get(catID);

            for (int featID = 0; featID < numFeatures; featID++) {
                weights.setWeight(cl._weights.getWeight(featID, 0, 0), featID,
                        catID, 0);
            }
        }

        try {
            weights.close();
        } catch (IOException e) {
            throw new RuntimeException("Error initializing the bayes learner "
                    + e.getMessage(), e);
        }
        NaiveBayesClassifier nbc = new NaiveBayesClassifier();
        try {
            nbc.read(path);
        } catch (IOException e) {
            throw new RuntimeException("Error initializing the bayes learner "
                    + e.getMessage(), e);
        }

        return nbc;
    }

    @Override
    public ILearnerRuntimeCustomizer getRuntimeCustomizer(short catID) {
        return null;
    }

    @Override
    public void setRuntimeCustomizer(
            Vector<ILearnerRuntimeCustomizer> customizers) {

    }

}
