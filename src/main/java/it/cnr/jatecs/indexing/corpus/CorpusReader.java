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
 * Created on 29-nov-2004
 *
 */
package it.cnr.jatecs.indexing.corpus;

import it.cnr.jatecs.indexes.DB.interfaces.ICategoryDB;

/**
 * @author Tiziano Fagni, Andrea Esuli
 */
public abstract class CorpusReader {

    /**
     * The categories DB to use.
     */
    private ICategoryDB _catsDB;

    /**
     * The type of set that the reader must consider.
     */
    private SetType _setType;

    /**
     * The name of this corpus reader.
     */
    private String _name;

    /**
     * The description of this corpus reader.
     */
    private String _description;

    /**
     * Create a generic corpus reader that consider documents belong to training
     * set.
     *
     * @param catsDB The initial categories DB to fill.
     */
    public CorpusReader(ICategoryDB catsDB) {
        _catsDB = catsDB;
        _setType = SetType.TRAINING;
        _name = "";
        _description = "";
    }

    public CorpusReader() {
    }

    protected void init(ICategoryDB catsDB) {
        _catsDB = catsDB;
        _setType = SetType.TRAINING;
        _name = "";
        _description = "";
    }

    /**
     * Get the type of the documents handled by this reader.
     *
     * @return The type of documents handled.
     */
    public SetType getDocumentSetType() {
        return _setType;
    }

    /**
     * Handle the documents of the specified type.
     *
     * @param t The type of documents to consider.
     */
    public void setDocumentSetType(SetType t) {
        _setType = t;
    }

    /**
     * Get the categories database to use to validate the categories of the
     * document.
     *
     * @return
     */
    public ICategoryDB getCategoryDB() {
        return _catsDB;
    }

    /**
     * Set the categories database to use to validate the categories of the
     * document.
     *
     * @return
     */
    public void setCategoryDB(ICategoryDB categoriesDB) {
        _catsDB = categoriesDB;
    }

    /**
     * Reset a reader to the begin of corpus data.
     */
    public abstract void begin();

    /**
     * Close the reader and release the stream
     */
    public abstract void close();

    /**
     * Read the next document in the corpus dataset. If there are no more
     * documents to read, it returns "null".
     *
     * @return The next document in the collection or "null" if there are no
     * more documents to read.
     */
    public abstract CorpusDocument next();

    /**
     * Set the corpus reader name.
     *
     * @param name The corpus reader name.
     */
    protected void setName(String name) {
        _name = name;
    }

    /**
     * Get the name of corpus reader object.
     *
     * @return The name of the corpus reader obejct.
     */
    public String name() {
        String toReturn = _name;
        if (toReturn.equals(""))
            toReturn = this.getClass().getName();

        return toReturn;
    }

    /**
     * Set the description of corpus reader object.
     *
     * @param description The description of corpus reader object.
     */
    protected void setDescription(String description) {
        _description = description;
    }

    /**
     * Get the description of the corpus reader object.
     *
     * @return The description of corpus reader.
     */
    public String description() {
        String toReturn = _description;
        if (toReturn.equals(""))
            toReturn = "Not available.";

        return toReturn;
    }

    /**
     * Write the actual configuration of the corpus reader on a string and
     * return it to the caller. The default implementation print the name and
     * the description of the corpus reader. If you want to provide some
     * additional useful informations, you must override the method in your
     * subclass.
     *
     * @return A string containing a textual representation of the actual
     * configuration of the module.
     */
    public String printConfiguration() {
        String str = "";

        str = "Name: " + name() + ".\n";
        str += "Description: " + description() + ".\n";
        str += "Documents set type handled: " + getDocumentSetType().toString()
                + ".\n";

        return str;
    }

    /**
     * Get a string signature that represents a value that identify an instance
     * of this class but considering all the internal class fields values which
     * determine the object state. Used internally to help to determine if an
     * index need to be regenerated.
     *
     * @return The signature of the instance.
     */
    public String signature() {
        return "" + System.currentTimeMillis();
    }
}
