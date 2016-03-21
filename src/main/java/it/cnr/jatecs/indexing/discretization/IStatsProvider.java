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

package it.cnr.jatecs.indexing.discretization;

public interface IStatsProvider {

    /**
     * Get the total number of classes considered.
     *
     * @return The total number of classes considered.
     */
    int getNumOfClasses();

    /**
     * Get the total number of examples available.
     *
     * @return The total number of examples available.
     */
    int getNumTotalExamples();


    /**
     * Get the number of examples belonging to class "classIdx" and located in
     * interval described by discreteBin.
     *
     * @param discreteBin The discrete bin.
     * @param classIdx    The class idx.
     * @return The number of examples belonging to class "classIdx" and located
     * in interval described by discreteBin.
     */
    int getNumExamplesInIntervalClass(DiscreteBin discreteBin, int classIdx);

    /**
     * Get the number of examples located in interval described by discreteBin.
     *
     * @param discreteBin The discrete bin.
     * @return Get the number of examples located in interval described by
     * discreteBin.
     */
    int getNumExamplesInInterval(DiscreteBin discreteBin);


    /**
     * Get the number of examples in class "classIdx".
     *
     * @param classIdx The class idx.
     * @return The number of examples in class "classIdx".
     */
    int getNumExamplesInClass(int classIdx);

}
