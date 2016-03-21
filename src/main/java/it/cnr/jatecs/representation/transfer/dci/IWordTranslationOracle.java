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

package it.cnr.jatecs.representation.transfer.dci;

/**
 * Interface for a word-translator oracle, i.e., a sort of translator,
 * that is usually queried by words, acting as a sort of bilingual
 * dictionary. The particularity is that the oracle might not know
 * the correct translation for some words, but that it is aware of 
 * this fact. Some oracles might be configured to be created on demand,
 * i.e., each time it does not know a word, it could keep and retain
 * the right translation once communicated, e.g., by command line.
 * 
 * */
public interface IWordTranslationOracle {
	
	/**
	 * @param word the wort to be translated
	 * @return the translation of the word, if there is any known one
	 * */
	public String translate(String word);
	
	/**
	 * @param the word to be translated
	 * @return true or false depending on whether the oracle is able 
	 * to translate the given word or not.
	 * */
	public boolean canTranslate(String word);
	
	/**
	 * @return true or false depending on whether the oracle is set 
	 * to be created on demand or not
	 * */
	public boolean isCreateOnDemand();
}
