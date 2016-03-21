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

package it.cnr.jatecs.crosslingual;


import static org.junit.Assert.assertTrue;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveDocumentLanguageDB;
import it.cnr.jatecs.indexes.utils.LanguageLabel;
import it.cnr.jatecs.io.FileSystemStorageManager;
import it.cnr.jatecs.utils.Os;

import java.io.IOException;

import org.junit.Test;

public class LanguageLabelerSingleMapTest {

	//enum langs {en, es, it};
	
	@Test
	public void basic() {
		TroveDocumentLanguageDB map = new TroveDocumentLanguageDB();
		
		map.indexDocLang(0, LanguageLabel.es);
		map.indexDocLang(1, LanguageLabel.en);
		map.indexDocLang(2, LanguageLabel.en);
		map.indexDocLang(3, LanguageLabel.en);
		map.indexDocLang(4, LanguageLabel.af);
		map.indexDocLang(4, LanguageLabel.en);
		
		assertTrue(map.getDocumentLanguage(0).equals(LanguageLabel.es));
		assertTrue(map.getDocumentLanguage(1).equals(LanguageLabel.en));
		assertTrue(map.getDocumentLanguage(2).equals(LanguageLabel.en));
		
		assertTrue(map.getDocumentsInLanguage(LanguageLabel.es).contains(0));
		assertTrue(map.getDocumentsInLanguage(LanguageLabel.en).contains(3));
		assertTrue(map.getDocumentsInLanguage(LanguageLabel.en).size()==4);		
	}
	
	@Test
	public void storage() {
		TroveDocumentLanguageDB map = new TroveDocumentLanguageDB();
		
		map.indexDocLang(0, LanguageLabel.es);
		map.indexDocLang(1, LanguageLabel.en);
		map.indexDocLang(2, LanguageLabel.en);
		map.indexDocLang(3, LanguageLabel.en);
		map.indexDocLang(4, LanguageLabel.af);
		map.indexDocLang(4, LanguageLabel.en);
		
		try {
			FileSystemStorageManager storage = new FileSystemStorageManager(
					Os.getTemporaryDirectory(), false);
			storage.open();
			TroveDocumentLanguageDB.write(storage, map, "lang.txt");

			TroveDocumentLanguageDB map2 = TroveDocumentLanguageDB.read(
					storage, "lang.txt", null);

			storage.close();
			assertTrue(map2.getDocumentsInLanguage(LanguageLabel.es).contains(0));
			assertTrue(map2.getDocumentsInLanguage(LanguageLabel.en).contains(3));
			assertTrue(map2.getDocumentsInLanguage(LanguageLabel.en).size()==4);	
			

		} catch (IOException e) {
			e.printStackTrace();
		}		
		
	}
	
}
