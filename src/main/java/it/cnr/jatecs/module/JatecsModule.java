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

/*
 * Created on 1-dic-2004
 *
 */
package it.cnr.jatecs.module;

import it.cnr.jatecs.indexes.DB.interfaces.IIndex;

/**
 * @author Tiziano Fagni, Andrea Esuli
 */
public abstract class JatecsModule {
    /**
     * The index object used by this module.
     */
    protected IIndex _index;
    /**
     * The name of the module
     */
    private String _name;
    /**
     * The description of the module
     */
    private String _description;
    /**
     * Indicate if this module has been executed correctly.
     */
    private boolean _hasBeenExecuted;

    /**
     * Construct a new indexing phase object.
     *
     * @param index The index object used by this module.
     * @param name  The name of the module.
     */
    public JatecsModule(IIndex index, String name) {

        _hasBeenExecuted = false;
        _index = index;
        _name = name;
    }

    /**
     * Get the name of the module
     *
     * @return the name of the module
     */
    public String name() {
        return _name;
    }

    /**
     * Set the name of the module.
     *
     * @param name name of the module.
     */
    public void setName(String name) {
        _name = name;
    }

    /**
     * Get the description of the module
     *
     * @return the description of the module
     */
    public String description() {
        return _description;
    }

    /**
     * Set the description of the module.
     *
     * @param description description of the module.
     */
    public void setDescription(String description) {
        _description = description;
    }

    /**
     * Get the index object where the module will operate.
     *
     * @return The index object where the module will operate or "null" if the
     * module does not operate directly over an index.
     */
    public IIndex index() {
        return _index;
    }

    /**
     * Set the index processed by the module.
     *
     * @param index The processed by the module.
     */
    public void setIndex(IIndex index) {
        _index = index;
        _name = "Generic module";
    }

    /**
     * Start processing the module over the documents DB specified in the object
     * constructor. The execution process consists in three main phases
     * (protected) executed in this sequential order:
     * <p>
     * - initData() Initialize the data structures used by this module. -
     * processIndex() Process the index specified by the contained documents DB.
     * - cleanupData() Cleanup all temporary and unnecessary data module.
     */
    public void exec() {
        // Init the module data.
        initData();

        // Process data.
        processModule();

        // Clean up the temporary and unneeded data module.
        cleanupData();

        // Signal that this module has been executed correctly.
        signalExecuted();
    }

    /**
     * Initialize the data structures used by this module. The default
     * implementation does nothing so if you want to do some particular
     * initialization code in a subclass, you need to override it.
     */
    protected void initData() {

    }

    /**
     * Start processing this module. You must implement this method in a
     * subclass to specify the behaviour of a particular module.
     */
    protected abstract void processModule();

    /**
     * Cleanup the data structures used by this module. The default
     * implementation does nothing so if you want to do some particular cleanup
     * code in a subclass, you need to override it.
     */
    protected void cleanupData() {

    }

    /**
     * Signal that this module has been exexuted correctly.
     */
    protected void signalExecuted() {
        _hasBeenExecuted = true;
    }

    /**
     * Indicate if this module has been executed correctly.
     *
     * @return True if this module has been processed correctly, false
     * otherwise.
     */
    public boolean hasBeenExecuted() {
        return _hasBeenExecuted;
    }

}
