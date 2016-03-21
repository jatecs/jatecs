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

import gnu.trove.TShortArrayList;
import gnu.trove.TShortObjectHashMap;
import gnu.trove.TShortObjectIterator;
import it.cnr.jatecs.classification.BaseClassifier;
import it.cnr.jatecs.classification.ClassificationResult;
import it.cnr.jatecs.classification.ClassifierRange;
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.classification.interfaces.IClassifierRuntimeCustomizer;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.DoubleMappingShortObject;
import it.cnr.jatecs.utils.iterators.TShortArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

public class SingleLabelTreeBoostClassifier extends BaseClassifier {

    protected TShortObjectHashMap<IClassifier> _map;
    protected DoubleMappingShortObject<TreeBoostClassifierAddress> _mapCatLevel;
    protected IClassifier _classifier;

    public SingleLabelTreeBoostClassifier() {
        _map = new TShortObjectHashMap<IClassifier>();
        _mapCatLevel = new DoubleMappingShortObject<TreeBoostClassifierAddress>();
        _classifier = null;
    }

    public IClassifier internalClassifier() {
        return _classifier;
    }

    public TreeBoostClassifierCustomizer getRuntimeCustomizer() {
        return (TreeBoostClassifierCustomizer) _customizer;
    }

    public void setRuntimeCustomizer(IClassifierRuntimeCustomizer customizer) {
        super.setRuntimeCustomizer(customizer);

        if (getRuntimeCustomizer() != null) {
            // Set the customizer for all base classifier loaded.
            TShortObjectIterator<IClassifier> it = _map.iterator();
            while (it.hasNext()) {
                it.advance();
                IClassifier c = (IClassifier) it.value();

                // Set the wanted customizer.
                c.setRuntimeCustomizer(getRuntimeCustomizer()
                        .getInternalCustomizer());
            }
        }
    }

    public ClassificationResult classify(IIndex idx, int docID) {

        short catID = Short.MIN_VALUE;
        IClassifier c = (IClassifier) _map.get(catID);
        Double maxScore = -Double.MAX_VALUE;
        short maxRealCatID = -1;
        ClassificationResult uselessRes = new ClassificationResult();
        // Nreak id are no more levels in the hierarchy.
        while (c != null) {
            // Call classifier.
            ClassificationResult r = c.classify(idx, docID);
            maxRealCatID = -1;
            maxScore = -Double.MAX_VALUE;
            for (short i = 0; i < r.categoryID.size(); i++) {
                short curCatID = r.categoryID.get(i);

                TreeBoostClassifierAddress addr = new TreeBoostClassifierAddress();
                addr.level = catID;
                addr.categoryID = curCatID;
                short realCatID = _mapCatLevel.get2(addr);
                if (r.score.get(i) > maxScore) {
                    maxScore = r.score.get(i);
                    maxRealCatID = realCatID;
                }
                if (!uselessRes.categoryID.contains(realCatID)) {
                    uselessRes.categoryID.add(realCatID);
                    uselessRes.score.add(r.score.get(i));
                }
            }
            catID = maxRealCatID;
            c = (IClassifier) _map.get(maxRealCatID);
        }

        ClassificationResult res = new ClassificationResult();
        res.documentID = docID;
        res.categoryID.add(maxRealCatID);
        res.score.add(maxScore);
        return res;
    }

    public ClassifierRange getClassifierRange(short catID) {
        TreeBoostClassifierAddress addr = _mapCatLevel.get1(catID);
        IClassifier c = (IClassifier) _map.get(addr.level);
        return c.getClassifierRange(addr.categoryID);
    }

    @Override
    public int getCategoryCount() {
        return _mapCatLevel.size();
    }

    @Override
    public IShortIterator getCategories() {
        TShortArrayList l = new TShortArrayList();
        for (short i = 0; i < _mapCatLevel.size(); i++)
            l.add(i);

        return new TShortArrayListIterator(l);
    }

    @Override
    public void destroy() {
        TShortObjectIterator<IClassifier> it = _map.iterator();
        while (it.hasNext()) {
            it.advance();
            IClassifier c = (IClassifier) it.value();
            c.destroy();
        }
    }

}
