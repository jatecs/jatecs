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

import gnu.trove.TShortArrayList;
import it.cnr.jatecs.classification.BaseClassifier;
import it.cnr.jatecs.classification.ClassificationResult;
import it.cnr.jatecs.classification.ClassifierRange;
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.interfaces.IClassifierRuntimeCustomizer;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.iterators.TShortArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

import java.util.HashMap;
import java.util.Vector;

/**
 * This classifier combines the decision of a set of classifiers following a
 * given policy.
 *
 * @author Tiziano Fagni
 */
public class CommitteeClassifier extends BaseClassifier {

    protected Vector<IClassifier> _classifiers;
    protected Vector<HashMap<String, Short>> _mapping;

    protected ICommitteePolicy _policy;

    protected IIndex _testIndex;
    protected Vector<IIndex> _tests;

    public CommitteeClassifier() {
        _classifiers = new Vector<IClassifier>();
        _policy = new MajorityVotePolicy();
        _mapping = new Vector<HashMap<String, Short>>();
        setRuntimeCustomizer(new CommitteeClassifierCustomizer());
        _testIndex = null;
        _tests = new Vector<IIndex>();
    }

    protected void initTestIndexes(IIndex idx) {
        IShortIterator cats = idx.getCategoryDB().getCategories();
        _tests.clear();
        for (int i = 0; i < _mapping.size(); i++) {
            IIndex idxAdHoc = idx.cloneIndex();
            HashMap<String, Short> map = _mapping.get(i);
            TShortArrayList toRemove = new TShortArrayList();
            cats.begin();
            while (cats.hasNext()) {
                short catID = cats.next();
                String catName = idx.getCategoryDB().getCategoryName(catID);
                if (!map.containsKey(catName))
                    toRemove.add(catID);
            }
            idxAdHoc.removeCategories(new TShortArrayListIterator(toRemove));
            _tests.add(idxAdHoc);
        }

        _testIndex = idx;
    }

    public ClassificationResult classify(IIndex testIndex, int docID) {
        Vector<ClassificationResult> results = new Vector<ClassificationResult>();
        if (_testIndex != testIndex) {
            initTestIndexes(testIndex);
        }

        for (int i = 0; i < _classifiers.size(); i++) {
            IClassifier cl = _classifiers.get(i);
            ClassificationResult res = cl.classify(_tests.get(i), docID);
            results.add(res);
        }

        ClassificationResult res = _policy.computeScores(this, results,
                _mapping, testIndex);
        return res;
    }

    public ClassifierRange getClassifierRange(short catID) {
        ClassifierRange r = new ClassifierRange();
        r.border = 0;
        r.maximum = 1;
        r.minimum = -1;

        return r;
    }

    @Override
    public int getCategoryCount() {
        if (_classifiers.size() == 0)
            return 0;
        else
            return _classifiers.get(0).getCategoryCount();
    }

    @Override
    public IShortIterator getCategories() {
        if (_classifiers.size() == 0)
            return new TShortArrayListIterator(new TShortArrayList());
        else
            return _classifiers.get(0).getCategories();
    }

    @Override
    public void setRuntimeCustomizer(IClassifierRuntimeCustomizer customizer) {
        _customizer = (CommitteeClassifierCustomizer) customizer;
        CommitteeClassifierCustomizer c = (CommitteeClassifierCustomizer) customizer;
        for (int i = 0; i < _classifiers.size(); i++) {
            IClassifier cl = _classifiers.get(i);
            IClassifierRuntimeCustomizer cu = c.getInternalRuntimeCustomizer(i);
            if (cu != null)
                cl.setRuntimeCustomizer(cu);
        }
    }
}
