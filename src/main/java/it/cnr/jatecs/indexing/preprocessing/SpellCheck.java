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

package it.cnr.jatecs.indexing.preprocessing;

import com.swabunga.spell.engine.SpellDictionary;
import com.swabunga.spell.engine.SpellDictionaryHashMap;
import com.swabunga.spell.engine.Word;
import com.swabunga.spell.event.SpellCheckEvent;
import com.swabunga.spell.event.SpellCheckListener;
import com.swabunga.spell.event.SpellChecker;
import com.swabunga.spell.event.StringWordTokenizer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class SpellCheck implements SpellCheckListener {

    private SpellChecker spellChecker = null;
    private SpellCorrectionType _correctionType;

    public SpellCheck(String dictFile) {
        super();
        SpellDictionary dictionary = null;
        try {
            dictionary = new SpellDictionaryHashMap(new File(dictFile), null);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        spellChecker = new SpellChecker(dictionary);
        spellChecker.addSpellCheckListener(this);
        _correctionType = SpellCorrectionType.Add;
    }

    public void setSpellCorrectionType(SpellCorrectionType correctionType) {
        _correctionType = correctionType;
    }

    public String spellCorrect(String text) {
        StringWordTokenizer swt = new StringWordTokenizer(text);
        spellChecker.checkSpelling(swt);
        return swt.getContext();
    }

    public void spellingError(SpellCheckEvent event) {
        @SuppressWarnings("unchecked")
        List<Word> suggestions = (List<Word>) event.getSuggestions();
        if (suggestions.size() > 0) {
            Iterator<Word> suggestedWord = suggestions.iterator();
            if (suggestedWord.hasNext()) {
                String orig = event.getInvalidWord();
                String sugg = suggestedWord.next().getWord();
                if (_correctionType == SpellCorrectionType.Add)
                    event.replaceWord(sugg + " ( " + orig + " ) ", false);
                else if (_correctionType == SpellCorrectionType.Replace)
                    event.replaceWord(sugg, false);
            }
        }
    }

    public enum SpellCorrectionType {
        Replace, Add;
    }

}
