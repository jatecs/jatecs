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

package it.cnr.jatecs.indexes.DB.troveCompact;

/**
 * Allowed types of parallelism for indexed multilingual corpora.
 * A multilingual corpus could be of one among the following types of parallelisms (i) parallel at 
 * context-level (whatever the context means; here is considered to mean 'document' by default, but 
 * this depends mainly on the way the index is constructed), contexts in two documents are said to be
 * parallel in this sense when they are direct translation among each other; and (ii) parallel at the 
 * topic-level, a.k.a. 'comparable' (i.e., the documents are supposed to deal with certain common topics, 
 * or aspects, but there is no guarantee that two parallel documents in this sense are direct translations 
 * among each other.
 *  
 * */
public enum TroveParallelCorpusType {
	/**
	 * Parallel here means that two documents are said to be parallel at document-level, i.e., 
	 * if two documents are linked to each other in a parallel db of this type, the documents
	 * are meant to be direct translation among each other.  
	 * */
    Parallel,
    
    /**
     * Comparable here means that towo documents are parallel at the topic-level, i.e.,
     * if two documents are linked to each other in a parallel db of this type, the documents
     * are assumed to deal with the same topics or aspects, but not as direct translations
     * among each other.
     * */
    Comparable
}
