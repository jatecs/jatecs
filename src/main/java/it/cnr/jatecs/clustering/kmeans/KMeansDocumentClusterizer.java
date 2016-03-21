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

package it.cnr.jatecs.clustering.kmeans;

import gnu.trove.TIntArrayList;
import it.cnr.jatecs.clustering.BaseDocumentClusterizer;
import it.cnr.jatecs.clustering.ClusterDocumentDescriptor;
import it.cnr.jatecs.clustering.DocumentCentroid;
import it.cnr.jatecs.clustering.interfaces.IDocumentClusterizerRuntimeCustomizer;
import it.cnr.jatecs.clustering.utils.Clustering;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.iterators.TIntArrayListIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;

import java.util.Random;
import java.util.Vector;

public class KMeansDocumentClusterizer extends BaseDocumentClusterizer {

    /**
     * The customizer used by this instance.
     */
    protected KMeansDocumentCustomizer _customizer;

    public KMeansDocumentClusterizer() {
        _customizer = new KMeansDocumentCustomizer();
    }

    public Vector<ClusterDocumentDescriptor> clusterize(IIntIterator documents,
                                                        IIndex index) {

        // Create the clusters
        DocumentCentroid[] centroids = new DocumentCentroid[_customizer
                .getNumberOfClusters()];
        Vector<ClusterDocumentDescriptor> clusters = new Vector<ClusterDocumentDescriptor>();
        for (int i = 0; i < _customizer.getNumberOfClusters(); i++) {
            ClusterDocumentDescriptor d = new ClusterDocumentDescriptor();
            d.description = "Cluster " + (i + 1);
            clusters.add(d);

            centroids[i] = new DocumentCentroid(index.getFeatureDB()
                    .getFeaturesCount());
        }

        // Compute intial centroids.
        computeInitialCentroids(centroids, documents, index);

        // Start a new evaluation for algorithm convergence criterion.
        _customizer.getConvergenceCriterion().beginEvaluation(documents, index);

        boolean stop = false;
        while (!stop) {
            documents.begin();
            while (documents.hasNext()) {
                int docID = documents.next();

                // Find the best cluster for current document.
                double best = _customizer.getSimilarityFunction()
                        .compareSimilarity(0, Double.MAX_VALUE) == 1 ? Double.MAX_VALUE
                        : 0;
                int whichCluster = 0;
                for (int i = 0; i < centroids.length; i++) {
                    double score = _customizer.getSimilarityFunction().compute(
                            centroids[i].features, docID, index);

                    if (_customizer.getSimilarityFunction().compareSimilarity(
                            score, best) > 0) {
                        best = score;
                        whichCluster = i;
                    }
                }

                // Assign the document to found best cluster.
                centroids[whichCluster].documents.add(docID);

            }

            // DEBUG
            System.out.println("Fatto passo");

            // Check if the algorithm converged or need more iterations.
            documents.begin();
            stop = _customizer.getConvergenceCriterion()
                    .isConverging(centroids);

            // Reset clusters document.
            if (!stop) {
                for (int i = 0; i < _customizer.getNumberOfClusters(); i++) {
                    centroids[i].documents.clear();
                }
            }
        }

        // Fill found clusters.
        for (int i = 0; i < clusters.size(); i++) {
            ClusterDocumentDescriptor cd = clusters.get(i);

            cd.documents = new TIntArrayList(
                    centroids[i].documents.toNativeArray());
        }

        return clusters;
    }

    protected void computeInitialCentroids(DocumentCentroid[] centroids,
                                           IIntIterator documents, IIndex index) {
        TIntArrayList initial = new TIntArrayList();
        documents.begin();
        while (documents.hasNext()) {
            initial.add(documents.next());
        }

        Random r = new Random();
        for (int i = 0; i < centroids.length; i++) {
            // Choose random document.
            int nextDoc = initial.get(r.nextInt(initial.size()));

            TIntArrayList ar = new TIntArrayList();
            ar.add(nextDoc);
            centroids[i].features = Clustering.computeDocumentCentroid(
                    new TIntArrayListIterator(ar), index);
        }
    }

    protected void computeInitialCentroidsNormal(DocumentCentroid[] centroids,
                                                 IIntIterator documents, IIndex index) {
        TIntArrayList initial = new TIntArrayList();
        documents.begin();
        while (documents.hasNext()) {
            initial.add(documents.next());
        }

        Random r = new Random();
        for (int i = 0; i < centroids.length; i++) {
            // Choose random document.
            int nextDoc = initial.get(r.nextInt(initial.size()));

            TIntArrayList ar = new TIntArrayList();
            ar.add(nextDoc);
            centroids[i].features = Clustering.computeDocumentCentroid(
                    new TIntArrayListIterator(ar), index);
        }
    }

    public IDocumentClusterizerRuntimeCustomizer getClusterizerRuntimeCustomizer() {
        return _customizer;
    }

    public void setClusterizerRuntimeCustomizer(
            IDocumentClusterizerRuntimeCustomizer customizer) {
        _customizer = (KMeansDocumentCustomizer) customizer;
    }

}
