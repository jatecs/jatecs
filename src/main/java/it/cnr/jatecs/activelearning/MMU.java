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

import it.cnr.jatecs.classification.ClassificationScoreDB;
import it.cnr.jatecs.classification.ClassifierRangeWithScore;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Max-Margin prediction Uncertainty sampling strategy proposed in
 * "X. Li and Y. Guo. Active learning with multi-label svm classification. In IJCAI, 2013"
 *
 * @author giacomo
 */
public class MMU extends ALpoolRank {

    public MMU(ClassificationScoreDB confidenceUnlabelled, IIndex trainingSet) {
        super(confidenceUnlabelled, trainingSet);

        for (int docId = 0; docId < unlabelledSize; docId++) {

            Set<Entry<Short, ClassifierRangeWithScore>> entries = confidenceUnlabelled
                    .getDocumentScoresAsSet(docId);
            Iterator<Entry<Short, ClassifierRangeWithScore>> iterator = entries
                    .iterator();
            double minPosConf = Double.MAX_VALUE;
            double minNegConf = Double.MAX_VALUE;
            while (iterator.hasNext()) {
                Entry<Short, ClassifierRangeWithScore> next = iterator.next();
                ClassifierRangeWithScore value = next.getValue();
                double score = value.score - value.border;
                if (score > 0) {
                    score = Math.abs(score);
                    if (score < minPosConf) {
                        minPosConf = score;
                    }
                } else {
                    score = Math.abs(score);
                    if (score < minNegConf) {
                        minNegConf = score;
                    }
                }
            }
            double value = 0.0;
            if (minPosConf == Double.MAX_VALUE) {
                value = 1.0 / minNegConf;
            } else if (minNegConf == Double.MAX_VALUE) {
                value = 1.0 / minPosConf;
            } else {
                value = 1.0 / (minPosConf + minNegConf);
            }
            rankingMap.put(docId, value);
            updateMax(docId, value);
        }
    }

}
