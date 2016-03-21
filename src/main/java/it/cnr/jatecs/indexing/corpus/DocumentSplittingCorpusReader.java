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

package it.cnr.jatecs.indexing.corpus;

/**
 * This reader splits a text into chunks, in which each chunk is heuristically
 * separated from the other by a list of "context-switching" words and
 * punctuation.
 */
public class DocumentSplittingCorpusReader extends CorpusReader {

    private CorpusReader reader;

    private CorpusDocument currentDocument;

    private String[] chunks;
    private int position;

    private String splitters = "\\.|!|;|\\?|/|\\bbut\\b|however|nevertheless|notwithstanding|nonetheless|despite|albeit|though";

    public DocumentSplittingCorpusReader(CorpusReader reader) {
        super(reader.getCategoryDB());

        this.reader = reader;

        currentDocument = null;
        position = -1;
        chunks = null;
    }

    @Override
    public void begin() {
        reader.begin();
    }

    @Override
    public void close() {
        reader.close();
    }

    @Override
    public CorpusDocument next() {
        if (currentDocument == null) {
            currentDocument = reader.next();
            if (currentDocument == null)
                return null;
            chunks = currentDocument.content().split(splitters);
            position = -1;
        }

        ++position;
        if (position >= chunks.length) {
            currentDocument = null;
            return next();
        }

        return new CorpusDocument(currentDocument.documentType(),
                currentDocument.name() + "_" + position, chunks[position],
                currentDocument.categories());
    }

}
