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

package it.cnr.jatecs.classification;

import java.util.Hashtable;


/**
 * This is a max score generator which computes the max score by taking
 * the absolute greatest value both from positive and negative classifications. As
 * a result, the generator computes the same value both for {@link #getMaximumPositiveScore(ClassificationScoreDB, short)} and
 * {@link #getMaximumNegativeScore(ClassificationScoreDB, short)} methods.
 *
 * @author Tiziano Fagni
 */
public class AbsoluteMaxScoreGenerator implements IMaxScoreGenerator {

    @Override
    public double getMaximumPositiveScore(ClassificationScoreDB cl, short catID) {

        if (cl == null)
            throw new NullPointerException("The specified classification results instance is 'null'");

        boolean atLeastOneCategory = false;
        int numDocs = cl.getDocumentCount();
        double maxValue = 0;
        for (int docID = 0; docID < numDocs; docID++) {
            Hashtable<Short, ClassifierRangeWithScore> res = cl.getDocumentScoresAsHashtable(docID);
            ClassifierRangeWithScore clRes = res.get(catID);
            if (clRes == null)
                continue;
            atLeastOneCategory = true;
            if (clRes.score >= clRes.border) {
                double val = clRes.score - clRes.border;
                maxValue = Math.max(val, maxValue);
            } else {
                double val = clRes.score - clRes.border;
                maxValue = Math.max(-val, maxValue);
            }
        }

        if ((!atLeastOneCategory))
            throw new IllegalArgumentException("The specified category <" + catID + "> can not be found!");

        return maxValue;
    }

    @Override
    public double getMaximumNegativeScore(ClassificationScoreDB cl, short catID) {
        return getMaximumPositiveScore(cl, catID);
    }

}
