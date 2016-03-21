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

import it.cnr.jatecs.classification.BaseLearner;
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.interfaces.ILearner;
import it.cnr.jatecs.classification.interfaces.ILearnerRuntimeCustomizer;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.util.HashMap;
import java.util.Vector;

/**
 * This learner can be used to learn in one shot a set of classifiers based on
 * different learners/training sets/parameters, and to combine them in a
 * committee based on a given policy.
 *
 * @author Tiziano Fagni
 */
public class CommitteeLearner extends BaseLearner {

    protected Vector<ILearner> _learners;
    protected Vector<IIndex> _indexes;

    public CommitteeLearner() {
        _learners = new Vector<ILearner>();
        _indexes = new Vector<IIndex>();
        setRuntimeCustomizer(new CommitteeLearnerCustomizer());
    }

    public void addLearner(int index, IIndex idx, ILearner learner) {
        _indexes.add(index, idx);
        _learners.add(index, learner);
    }

    public IClassifier build(IIndex notUsed) {
        CommitteeClassifier cl = new CommitteeClassifier();
        for (int i = 0; i < _learners.size(); i++) {
            ILearner l = _learners.get(i);
            IIndex idx = _indexes.get(i);

            IClassifier c = l.build(idx);
            cl._classifiers.add(c);
        }

        // Save mapping between categories and IDs.
        for (int i = 0; i < _indexes.size(); i++) {
            HashMap<String, Short> map = new HashMap<String, Short>();
            cl._mapping.add(map);
            IIndex idx = _indexes.get(i);
            IShortIterator cats = idx.getCategoryDB().getCategories();
            while (cats.hasNext()) {
                short catID = cats.next();
                String catName = idx.getCategoryDB().getCategoryName(catID);
                map.put(catName, catID);
            }
        }

        return cl;
    }

    @Override
    public void setRuntimeCustomizer(ILearnerRuntimeCustomizer customizer) {
        _customizer = customizer;
        CommitteeLearnerCustomizer cu = (CommitteeLearnerCustomizer) getRuntimeCustomizer();

        for (int i = 0; i < _learners.size(); i++) {
            ILearner cl = _learners.get(i);
            ILearnerRuntimeCustomizer cus = cu.getInternalRuntimeCustomizer(i);
            if (cus != null)
                cl.setRuntimeCustomizer(cus);
        }
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
