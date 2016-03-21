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

package it.cnr.jatecs.classification.validator;

import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.Pair;

public interface IDataSetGenerator {
    /**
     * Initialize the data set generator by using the specified index.
     *
     * @param index The index to be used.
     */
    public void begin(IIndex index);


    /**
     * Indicate if there are or not no more pairs of training/test set to be generated.
     *
     * @return True if the are other pairs to be generated, false otherwise.
     */
    public boolean hasNext();


    /**
     * Get the next pair of training/test set. The first item in the pair is the training set while the
     * second item is test set.
     *
     * @return The next pair of training/test set.
     */
    public Pair<IIndex, IIndex> next();
}
