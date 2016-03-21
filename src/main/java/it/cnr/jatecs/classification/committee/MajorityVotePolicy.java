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

package it.cnr.jatecs.classification.committee;

import it.cnr.jatecs.classification.ClassificationResult;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.util.HashMap;
import java.util.Vector;

public class MajorityVotePolicy implements ICommitteePolicy {


    public ClassificationResult computeScores(CommitteeClassifier c,
                                              Vector<ClassificationResult> results,
                                              Vector<HashMap<String, Short>> mapping, IIndex index) {
        ClassificationResult res = new ClassificationResult();
        res.documentID = results.get(0).documentID;

        Vector<HashMap<Short, Integer>> reverseMaps = new Vector<HashMap<Short, Integer>>();

        IShortIterator cats = index.getCategoryDB().getCategories();
        while (cats.hasNext()) {
            short cat = cats.next();
            String catName = index.getCategoryDB().getCategoryName(cat);

            int pos = 0;
            int neg = 0;
            for (int i = 0; i < results.size(); i++) {
                ClassificationResult r = results.get(i);

                HashMap<Short, Integer> reverseMap = null;
                if (reverseMaps.size() <= i) {
                    // Do it only one time.
                    reverseMap = new HashMap<Short, Integer>();
                    reverseMaps.add(reverseMap);
                    for (int z = 0; z < r.categoryID.size(); z++)
                        reverseMap.put(r.categoryID.get(z), z);
                } else
                    reverseMap = reverseMaps.get(i);

                HashMap<String, Short> map = mapping.get(i);
                short catID = map.containsKey(catName) ? map.get(catName) : -1;
                if (catID != -1) {
                    Integer position = reverseMap.get(catID);
                    if (position != null) {
                        double score = r.score.get(position);
                        if (score >= c._classifiers.get(i).getClassifierRange(catID).border)
                            pos++;
                        else

                            neg++;
                    } else
                        neg++;

                } else {
                    neg++;
                }
            }

            boolean positive = (pos >= neg) ? true : false;
            res.categoryID.add(cat);
            res.score.add(positive ? 1.0 : -1.0);
        }

        return res;
    }

}
