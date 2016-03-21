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

import it.cnr.jatecs.indexes.DB.troveCompact.TroveParallelCorpusType;
import it.cnr.jatecs.indexes.utils.LanguageLabel;

import java.util.HashMap;

public interface IParallelMultilingualIndex extends IMultilingualIndex {
	/**
	 * Gets the type of parallelism of this indexed multilingual corpus. 
	 * @see TroveParallelCorpusType
	 * @return the type of parallelism this multilingual index is assumed to represent.
	 * */
    public TroveParallelCorpusType getIndexType();

    /**
     * Establishes the type of parallelism of the multilingual index
     * @see TroveParallelCorpusType
     * @param corpusType is the type of parallelism this multilingual index is expected to follow  
     * */
    public void setIndexType(TroveParallelCorpusType corpusType);

    /**
     * @return the parallel db to which this db refers
     * */
    public IParallelDB getParallelDB();

    /**
     * A parallel corpus (be it at document-level or at topic-level) could be compacted by
     * joining together the linked documents into a new synthetic one, that is agnostic to
     * the languages the original documents were written in. For example, a parallel index
     * contains two parallel documents: di in Spanish, and dj in Italian. The new compacted
     * document will contain a new document dx as a yuxtaposition of di and dj, and will not
     * contain di and dj as separate documents any more.
     * 
     * @return a compacted version of the original multilingual parallel corpus, that is
     * agnostic to language or parallelism relations
     * */
    public IIndex compactParallelDocuments();

    /**
     * Splits a multilingual parallel index as a set of monolingual indexes, each of which
     * mapped to the language label in which its documents are written in.
     * 
     * @param onlyFullViews indicates whether only full views (i.e., only parallel documents 
     * which have a language-specific version in all languages in the languages db) are to be
     * kept, or not.
     * @return a map of the different monolingual corpora indexed by language.
     * */
    public HashMap<LanguageLabel, IIndex> unfoldTrainingViews(boolean onlyFullViews);

    /**
     * Splits a dependent index (such as an index containing test documents) in monolingual 
     * language-specific indexes, respecting the feature space indicated by the unfolded main indexes
     * (such as an index containing the training documents).
     * 
     * @see IParallelMultilingualIndex#unfoldTrainingViews(boolean)
     * @param onlyFullViews indicates whether only full views (i.e., only parallel documents 
     * which have a language-specific version in all languages in the languages db) are to be
     * kept, or not.
     * @return a map of the different monolingual corpora indexed by language and consistent
     * with the feature spaces of the unfolded main indexes.
     * */
    public HashMap<LanguageLabel, IIndex> unfoldTestingDependantViews(boolean onlyFullViews, 
    		HashMap<LanguageLabel, IIndex> trainingIndex);
}
