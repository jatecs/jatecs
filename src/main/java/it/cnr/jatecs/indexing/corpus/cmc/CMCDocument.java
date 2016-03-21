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

package it.cnr.jatecs.indexing.corpus.cmc;

import it.cnr.jatecs.indexing.corpus.CorpusCategory;
import it.cnr.jatecs.indexing.corpus.CorpusDocument;
import it.cnr.jatecs.indexing.corpus.DocumentType;

import java.util.Vector;

public class CMCDocument extends CorpusDocument {
    protected String _impression;

    protected String _clinicalHistory;

    protected Vector<CorpusCategory> _codesA;
    protected Vector<CorpusCategory> _codesB;
    protected Vector<CorpusCategory> _codesC;


    public CMCDocument(String name, DocumentType docType) {
        super(name, docType, "", new Vector<String>());

        _codesA = new Vector<CorpusCategory>();
        _codesB = new Vector<CorpusCategory>();
        _codesC = new Vector<CorpusCategory>();
    }

    public String getImpressionContent() {
        return _impression;
    }

    public void setImpressionContent(String content) {
        _impression = content;
    }

    public String getClinicalHistoryContent() {
        return _clinicalHistory;
    }

    public void setClinicalHistoryContent(String content) {
        _clinicalHistory = content;
    }

    @Override
    public String content() {
        return getClinicalHistoryContent() + " " + getImpressionContent();
    }


    public Vector<CorpusCategory> getCodesA() {
        return _codesA;
    }


    public Vector<CorpusCategory> getCodesB() {
        return _codesB;
    }


    public Vector<CorpusCategory> getCodesC() {
        return _codesC;
    }
}
