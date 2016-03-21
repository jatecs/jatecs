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

package it.cnr.jatecs.clustering.interfaces;

import it.cnr.jatecs.clustering.ClusterDocumentDescriptor;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;

import java.util.Vector;

public interface IDocumentClusterizer {

    /**
     * Cluster all documents contained in specified index.
     *
     * @param index The index containing documents to be clusterized.
     * @return The list of clusters found.
     */
    public Vector<ClusterDocumentDescriptor> clusterize(IIndex index);


    /**
     * Clusters all documents specified from "documents" parameter and accessible by the passed index.
     *
     * @param documents The document to be clusterized.
     * @param index     The index describing the wanted documents.
     * @return
     */
    public Vector<ClusterDocumentDescriptor> clusterize(IIntIterator documents, IIndex index);

    /**
     * Get the runtime customizer used by this clusterizer.
     *
     * @return The runtime customizer used by this clusterizer.
     */
    public IDocumentClusterizerRuntimeCustomizer getClusterizerRuntimeCustomizer();

    /**
     * Set the rutime customizer for this clusterizer.
     *
     * @param customizer The customizer to use when cluster documents.
     */
    public void setClusterizerRuntimeCustomizer(IDocumentClusterizerRuntimeCustomizer customizer);


}
