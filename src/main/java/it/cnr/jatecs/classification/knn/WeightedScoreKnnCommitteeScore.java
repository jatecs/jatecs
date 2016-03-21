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

package it.cnr.jatecs.classification.knn;

import gnu.trove.TShortDoubleHashMap;
import it.cnr.jatecs.classification.ClassificationResult;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;

import java.util.Vector;

public class WeightedScoreKnnCommitteeScore implements IKnnCommitteeScorer {


    public WeightedScoreKnnCommitteeScore() {
    }


    public ClassificationResult computeScore(KnnCommitteeClassifier cl,
                                             Vector<ClassificationResult> results, IIndex testIndex, int docID) {
        assert (results.size() > 0);

        ClassificationResult cr = new ClassificationResult();
        cr.documentID = docID;


        for (int i = 0; i < results.get(0).categoryID.size(); i++) {
            cr.categoryID.add(results.get(0).categoryID.get(i));
            cr.score.add(0);
        }

        TShortDoubleHashMap mapNomalization = new TShortDoubleHashMap();
        for (int i = 0; i < results.get(0).categoryID.size(); i++) {
            short catID = results.get(0).categoryID.get(i);
            double norm = 0;
            for (int j = 0; j < results.size(); j++) {
                KnnClassifierCustomizer cust = (KnnClassifierCustomizer) cl._classifiers.get(j).getRuntimeCustomizer();
                norm += cust.getEfficacy(catID);
            }

            mapNomalization.put(catID, norm);
        }


        for (int i = 0; i < results.size(); i++) {
            ClassificationResult res = results.get(i);
            KnnClassifierCustomizer cust = (KnnClassifierCustomizer) cl._classifiers.get(i).getRuntimeCustomizer();

            for (int j = 0; j < res.score.size(); j++) {
                short catID = res.categoryID.get(j);
                double val = 0;
                val = res.score.get(j) - cust.getClassifierRange(catID).border;
                if (val >= 0) {
                    double interval = cust.getClassifierRange(catID).maximum - cust.getClassifierRange(catID).border;
                    double curValue = val;
                    if (interval != 0)
                        val = curValue / interval;
                    else
                        val = 1;
                } else {
                    double interval = cust.getClassifierRange(catID).minimum - cust.getClassifierRange(catID).border;
                    double curValue = val;
                    if (interval != 0)
                        val = -curValue / interval;
                    else
                        val = -1;
                }

                double score = cr.score.get(j) + (cust.getEfficacy(catID) * val / mapNomalization.get(catID));
                cr.score.set(j, score);
            }

        }

        // DEBUG
        for (int i = 0; i < cr.score.size(); i++) {
            double score = cr.score.get(i);
            if (score < -1 || score > 1)
                System.out.println("MMM...score: " + score);
        }

        return cr;
    }

}
