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

package it.cnr.jatecs.classification.interfaces;

import it.cnr.jatecs.io.IStorageManager;

public interface IDataManager {

    /**
     * Write the classifier data on the specified storage manager and under the
     * specific model name. If the specified model name already exists, it will
     * be overwritten.
     *
     * @param storageManager The storage manager to use.
     * @param modelName      The model name.
     * @param learningData   The classifier to be saved.
     * @throws NullPointerException     Raised if the storage manager is 'null', or the classifier is
     *                                  'null'.
     * @throws IllegalArgumentException Raised if the specified model name is invalid.
     * @throws IllegalStateException    Raised if the storage manager is not open.
     */
    public void write(IStorageManager storageManager, String modelName,
                      IClassifier learningData);

    /**
     * Read the classifier data from the specified storage manager and under the
     * specified model name.
     *
     * @param storageManager The storage manager to use.
     * @param modelName      The model name.
     * @return The read classifier.
     * @throws NullPointerException     Raised if the storage manager is 'null'.
     * @throws IllegalArgumentException Raised if the model name is invalid or the model name can not
     *                                  be find on the storage manager.
     * @throws IllegalStateException    Raised if the storage manager is not open.
     */
    public IClassifier read(IStorageManager storageManager, String modelName);

    /**
     * Write the learner data parameters on the specified storage manager and
     * under the specific model name. If the specified model name already
     * exists, it will be overwritten.
     *
     * @param storageManager The storage manager to use.
     * @param modelName      The model name.
     * @param customizer     The customizer to be saved.
     * @throws NullPointerException     Raised if the storage manager is 'null', or the customizer is
     *                                  'null'.
     * @throws IllegalArgumentException Raised if the specified model name is invalid.
     * @throws IllegalStateException    Raised if the storage manager is not open.
     */
    public void writeLearnerRuntimeConfiguration(
            IStorageManager storageManager, String modelName,
            ILearnerRuntimeCustomizer customizer);

    /**
     * Read the learner data parameters from the specified storage manager and
     * under the specified model name.
     *
     * @param storageManager The storage manager to use.
     * @param modelName      The model name.
     * @return The learner customizer.
     * @throws NullPointerException     Raised if the storage manager is 'null'.
     * @throws IllegalArgumentException Raised if the model name is invalid or the model name can not
     *                                  be find on the storage manager.
     * @throws IllegalStateException    Raised if the storage manager is not open.
     */
    public ILearnerRuntimeCustomizer readLearnerRuntimeConfiguration(
            IStorageManager storageManager, String modelName);

    /**
     * Write the classifier data parameters on the specified storage manager and
     * under the specific model name. If the specified model name already
     * exists, it will be overwritten.
     *
     * @param storageManager The storage manager to use.
     * @param modelName      The model name.
     * @param customizer     The customizer to be saved.
     * @throws NullPointerException     Raised if the storage manager is 'null', or the customizer is
     *                                  'null'.
     * @throws IllegalArgumentException Raised if the specified model name is invalid.
     * @throws IllegalStateException    Raised if the storage manager is not open.
     */
    public void writeClassifierRuntimeConfiguration(
            IStorageManager storageManager, String modelName,
            IClassifierRuntimeCustomizer customizer);

    /**
     * Read the classifier data parameters from the specified storage manager
     * and under the specified model name.
     *
     * @param storageManager The storage manager to use.
     * @param modelName      The model name.
     * @return The classifier customizer.
     * @throws NullPointerException     Raised if the storage manager is 'null'.
     * @throws IllegalArgumentException Raised if the model name is invalid or the model name can not
     *                                  be find on the storage manager.
     * @throws IllegalStateException    Raised if the storage manager is not open.
     */
    public IClassifierRuntimeCustomizer readClassifierRuntimeConfiguration(
            IStorageManager storageManager, String modelName);
}
