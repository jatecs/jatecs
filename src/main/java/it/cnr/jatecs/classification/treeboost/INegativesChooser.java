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

import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.iterators.TIntArrayListIterator;

public interface INegativesChooser {

    /**
     * Initialize the negatives chooser with the given index. Eventually perform some
     * global operations preliminary to use of selectNegatives() method.
     *
     * @param index The index over which the chooser will operate.
     */
    public void initialize(IIndex index);


    /**
     * Select the negatives example for specified category ID. The index used is the index passed to
     * to last call of initialize() method.
     *
     * @param category The category name.
     * @return The list of negatives documents for the category.
     */
    public TIntArrayListIterator selectNegatives(String category);


    /**
     * Release resources acquired during initialization phase.
     */
    public void release();
}
