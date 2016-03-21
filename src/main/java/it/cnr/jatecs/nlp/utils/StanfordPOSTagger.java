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

package it.cnr.jatecs.nlp.utils;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import it.cnr.jatecs.utils.FileUtils;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class StanfordPOSTagger {

    private static MaxentTagger tagger = null;

    private static String _model = FileUtils
            .resolveFilenameFromResource("/edu/stanford/nlp/models/pos-tagger/english-left3words/english-left3words-distsim.tagger");

    public StanfordPOSTagger() {
        if (tagger == null) {
            tagger = new MaxentTagger(_model);
        }
    }

    public String getModel() {
        return _model;
    }

    public void setModel(String model) {
        _model = model;
    }

    public Vector<ArrayList<TaggedWord>> tag(String input) {
        Vector<ArrayList<TaggedWord>> returnVector = new Vector<ArrayList<TaggedWord>>();
        List<List<HasWord>> sentences = MaxentTagger
                .tokenizeText(new BufferedReader(new StringReader(input)));
        for (List<? extends HasWord> sentence : sentences) {
            returnVector.add(tagger.tagSentence(sentence));
        }
        return returnVector;
    }

}
