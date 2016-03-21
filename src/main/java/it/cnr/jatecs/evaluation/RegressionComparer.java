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

package it.cnr.jatecs.evaluation;

import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDB;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;

public class RegressionComparer {

    protected IClassificationDB _experiment;
    protected IClassificationDB _goldStandard;
    protected RegressionResultSet _tableSet;

    public RegressionComparer(IClassificationDB experiment, IClassificationDB goldStandard) {
        super();
        _experiment = experiment;
        _goldStandard = goldStandard;
    }

    public IClassificationDB getEvaluated() {
        return _experiment;
    }

    public IClassificationDB getTarget() {
        return _goldStandard;
    }

    public RegressionResultSet evaluate() {
        RegressionResultSet tableSet = new RegressionResultSet(_goldStandard.getCategoryDB().getCategoriesCount());
        IIntIterator docIt = _experiment.getDocumentDB().getDocuments();

        while (docIt.hasNext()) {
            int document = docIt.next();
            short realCategory = _goldStandard.getDocumentCategories(document).next();
            short expCategory = _experiment.getDocumentCategories(document).next();
            String catName = _goldStandard.getCategoryDB().getCategoryName(realCategory);
            int distance = Math.abs(realCategory - expCategory);
            tableSet.add(realCategory, catName, distance);
        }
        return tableSet;
    }
}
