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

package it.cnr.jatecs.indexes.DB.interfaces;

import it.cnr.jatecs.indexes.utils.LanguageLabel;

import java.util.HashSet;
import java.util.Iterator;

public interface IDocumentLanguageDB {

    public IDocumentDB getDocumentDB();

    public ILanguageDB getLanguageDB();

    public int getLanguagesCount();

    public LanguageLabel getDocumentLanguage(int doc);

    public HashSet<LanguageLabel> getDocumentLanguages(int doc);

    public boolean isMonolingualDocument(int doc);

    public boolean isMultilingualDocument(int doc);

    public HashSet<Integer> getDocumentsInLanguage(LanguageLabel lang);

    public boolean isDocumentLabeled(int doc);

    public Iterator<Integer> getDocumentsWithLangLabel();

    public boolean removeDocument(int doc);

    public IDocumentLanguageDB cloneDB(IDocumentDB docs, ILanguageDB langs);

}
