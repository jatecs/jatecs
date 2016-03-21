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

package it.cnr.jatecs.classification.adaboost;

import it.cnr.jatecs.classification.regression.TreeNode;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;

public class AdaBoostRegressionLearnerCustomizer extends
        AdaBoostLearnerCustomizer {

    protected IIndex _originalIndex;
    protected TreeNode _node;
    protected float _p;

    public AdaBoostRegressionLearnerCustomizer() {
        super();
        _originalIndex = null;
        _node = null;
        _p = 1.0f;
    }

    public float getP() {
        return _p;
    }

    public void setP(float p) {
        _p = p;
    }

    public IIndex getOriginalIndex() {
        return _originalIndex;
    }

    public TreeNode getTreeNode() {
        return _node;
    }

    public void setOriginalIndex(IIndex originalIndex, TreeNode node) {
        assert (originalIndex != null);
        assert (node != null);
        _originalIndex = originalIndex;
        _node = node;
    }

}
