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

package it.cnr.jatecs.classification.cotraining;

import gnu.trove.TIntIntHashMap;
import it.cnr.jatecs.classification.ClassificationResult;
import it.cnr.jatecs.classification.ClassifierRange;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveMainIndexBuilder;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.util.ArrayList;

public class CoTrainer {
    public IIndex buildCotrainedIndex(IIndex originalTraining, IIndex test,
                                      ArrayList<CotrainingSetting> settings) {
        IShortIterator cats = test.getCategoryDB().getCategories();
        TIntIntHashMap docsToAdd = new TIntIntHashMap();

        for (int i = 0; i < settings.size(); i++) {
            CotrainingSetting setting = settings.get(i);
            cats.begin();
            while (cats.hasNext()) {
                short catID = cats.next();
                ClassificationResult[] res = setting.classifier.classify(test, catID);
                ClassifierRange crange = setting.classifier.getClassifierRange(catID);
                double threshold = setting.catsThreshold.get(catID);

                for (int j = 0; j < res.length; j++) {
                    ClassificationResult cr = res[j];
                    double val = cr.score.get(0) - crange.border;
                    if (val >= 0) {
                        double interval = crange.maximum - crange.border;
                        double curValue = val;
                        if (interval != 0)
                            val = curValue / interval;
                        else
                            val = 1;
                    } else {
                        double interval = crange.minimum - crange.border;
                        double curValue = val;
                        if (interval != 0)
                            val = -curValue / interval;
                        else
                            val = -1;

                        val = -val;
                    }

                    if (val >= threshold) {
                        if (!docsToAdd.containsKey(j))
                            docsToAdd.put(j, j);
                    }
                }
            }
        }

        return createEnrichedTraining(docsToAdd, test, originalTraining);
    }


    protected IIndex createEnrichedTraining(TIntIntHashMap map, IIndex testAllCats, IIndex training) {
        IIndex idx = training.cloneIndex();
        TroveMainIndexBuilder builder = new TroveMainIndexBuilder(idx);
        int numDocs = training.getDocumentDB().getDocumentsCount();

        int[] keys = map.keys();
        for (int i = 0; i < keys.length; i++) {
            int docID = keys[i];
            ArrayList<String> features = new ArrayList<String>(100);
            String docName = "" + (numDocs + (i + 1));
            IIntIterator feats = testAllCats.getContentDB().getDocumentFeatures(docID);
            while (feats.hasNext()) {
                int featID = feats.next();
                String featName = testAllCats.getFeatureDB().getFeatureName(featID);
                int count = testAllCats.getContentDB().getDocumentFeatureFrequency(docID, featID);
                for (int j = 0; j < count; j++)
                    features.add(featName);
            }

            ArrayList<String> categories = new ArrayList<String>();
            IShortIterator cats = testAllCats.getClassificationDB().getDocumentCategories(docID);
            while (cats.hasNext()) {
                short catID = cats.next();
                String catName = testAllCats.getCategoryDB().getCategoryName(catID);
                categories.add(catName);
            }

            builder.addDocument(docName, features.toArray(new String[0]), categories.toArray(new String[0]));
        }

        return idx;
    }
}
