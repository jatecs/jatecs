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

import it.cnr.jatecs.classification.interfaces.IClassifier;

public class TreeNode {
    private short[] _positiveCategories;
    private short[] _negativeCategories;
    private IClassifier _classifier;
    private TreeNode _positiveChild;
    private TreeNode _negativeChild;

    public TreeNode(short[] positiveCategories, short[] negativeCategories, TreeNode positiveChild, TreeNode negativeChild) {
        _positiveCategories = positiveCategories;
        _negativeCategories = negativeCategories;
        _classifier = null;
        _positiveChild = positiveChild;
        _negativeChild = negativeChild;
    }

    public IClassifier getClassifier() {
        return _classifier;
    }

    public void setClassifier(IClassifier classifier) {
        _classifier = classifier;
    }

    public TreeNode getPositiveChild() {
        return _positiveChild;
    }

    public TreeNode getNegativeChild() {
        return _negativeChild;
    }

    public short[] getPositiveCategories() {
        return _positiveCategories;
    }

    public short[] getNegativeCategories() {
        return _negativeCategories;
    }
}
