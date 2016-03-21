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

import it.cnr.jatecs.classification.interfaces.ILearnerRuntimeCustomizer;

import java.util.Vector;

public class CommitteeLearnerCustomizer implements ILearnerRuntimeCustomizer {

    protected Vector<LRC> _cust;

    public CommitteeLearnerCustomizer() {
        _cust = new Vector<LRC>();
    }

    public ILearnerRuntimeCustomizer cloneObject() {
        CommitteeLearnerCustomizer c = new CommitteeLearnerCustomizer();
        for (int i = 0; i < _cust.size(); i++) {
            LRC lrc = _cust.get(i);
            LRC toAdd = new LRC();
            toAdd.index = lrc.index;
            toAdd.customizer = lrc.customizer.cloneObject();
            c._cust.add(toAdd);
        }

        return c;
    }

    public void setInternalRuntimeCustomizer(int whichClassifier, ILearnerRuntimeCustomizer cust) {
        LRC lrc = new LRC();
        lrc.index = whichClassifier;
        lrc.customizer = cust;
        _cust.add(lrc);
    }

    public ILearnerRuntimeCustomizer getInternalRuntimeCustomizer(int whichClassifier) {
        ILearnerRuntimeCustomizer c = null;

        for (int i = 0; i < _cust.size(); i++) {
            LRC lrc = _cust.get(i);
            if (lrc.index == whichClassifier) {
                c = lrc.customizer;
                break;
            }
        }

        return c;
    }

    class LRC {
        int index;
        ILearnerRuntimeCustomizer customizer;
    }

}
