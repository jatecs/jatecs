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

package it.cnr.jatecs.classification.regression;

import it.cnr.jatecs.classification.BaseClassifier;
import it.cnr.jatecs.classification.ClassificationResult;
import it.cnr.jatecs.classification.ClassifierRange;
import it.cnr.jatecs.classification.interfaces.IClassifier;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

public class TreeRegressClassifier extends BaseClassifier {

    TreeNode _root;

    private ClassifierRange _range;

    public TreeRegressClassifier(TreeNode root) {
        _root = root;
        _range = root.getClassifier().getClassifierRange((short) 0);

    }

    public ClassificationResult classify(IIndex testIndex, int docID) {
        return classify(testIndex, docID, _root);
    }

    private ClassificationResult classify(IIndex index, int docID, TreeNode node) {
        IClassifier classifier = node.getClassifier();
        ClassificationResult result = classifier.classify(index, docID);
        if (result.score.get(0) > _range.border) {
            if (node.getPositiveChild() != null)
                return classify(index, docID, node.getPositiveChild());
            else {
                result.categoryID.clear();
                result.categoryID.add(node.getPositiveCategories()[0]);
                return result;
            }
        } else {
            if (node.getNegativeChild() != null)
                return classify(index, docID, node.getNegativeChild());
            else {
                result.score.setQuick(0, -result.score.getQuick(0));
                result.categoryID.clear();
                result.categoryID.add(node.getNegativeCategories()[0]);
                return result;
            }
        }
    }

    public ClassifierRange getClassifierRange(short catID) {
        return _range;
    }

    @Override
    public int getCategoryCount() {
        return 1;
    }

    @Override
    public IShortIterator getCategories() {
        return null;
    }
}
