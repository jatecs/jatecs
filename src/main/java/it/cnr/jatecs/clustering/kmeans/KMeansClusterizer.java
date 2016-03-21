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

import gnu.trove.TDoubleArrayList;
import gnu.trove.TIntIntHashMap;
import it.cnr.jatecs.clustering.Centroid;
import it.cnr.jatecs.clustering.ClusterDescriptor;
import it.cnr.jatecs.clustering.interfaces.IClusterizer;
import it.cnr.jatecs.clustering.interfaces.IClusterizerRuntimeCustomizer;
import it.cnr.jatecs.weighting.interfaces.IWeighting3D;

import java.util.Vector;

public class KMeansClusterizer implements IClusterizer {

    protected KMeansCustomizer _customizer;


    public KMeansClusterizer() {
        _customizer = new KMeansCustomizer();
    }


    public Vector<ClusterDescriptor> clusterize(IWeighting3D dists) {
        // Create the clusters.
        Centroid[] centroids = new Centroid[_customizer.getNumberOfClusters()];
        Vector<ClusterDescriptor> clusters = new Vector<ClusterDescriptor>();
        for (int i = 0; i < _customizer.getNumberOfClusters(); i++) {
            ClusterDescriptor d = new ClusterDescriptor();
            d.description = "Cluster " + (i + 1);
            clusters.add(d);

            centroids[i] = new Centroid(dists.getSecondDimensionSize());
        }

        // Initialize the clusters.
        if (_customizer.getCentroids() == null)
            _customizer.getClusterizerInitializer().initializeClusters(dists, centroids);
        else {
            for (int i = 0; i < _customizer.getNumberOfClusters(); i++) {
                double[] centroid = _customizer.getCentroids()[i];
                centroids[i].features = centroid;
            }
        }

        // Keep track of assigned clusters.
        TIntIntHashMap clustersAssigned = new TIntIntHashMap();
        for (int id = 0; id < dists.getFirstDimensionSize(); id++) {
            clustersAssigned.put(id, id % _customizer.getNumberOfClusters());
        }


        boolean toUpdate = true;
        while (toUpdate) {
            for (int i = 0; i < centroids.length; i++) {
                centroids[i].documents.clear();
            }

            // Distribute items among clusters.
            for (int docID = 0; docID < dists.getFirstDimensionSize(); docID++) {
                double best = _customizer.getSimilarityFunction().compareSimilarity(0, Double.MAX_VALUE) == 1 ? Double.MAX_VALUE : 0;
                int whichCluster = 0;
                for (int i = 0; i < centroids.length; i++) {
                    double score = _customizer.getSimilarityFunction().computeSimilarity(centroids[i].features, dists, (int) docID);

                    if (_customizer.getSimilarityFunction().compareSimilarity(score, best) > 0) {
                        best = score;
                        whichCluster = i;
                    }
                }

                centroids[whichCluster].documents.add(docID);
                // Add centroid update to each add.
                //centroids[whichCluster].computeCentroid(dists);
            }


            // Compute if the algorithm had been converged.
            int reassigned = 0;
            for (int i = 0; i < clusters.size(); i++) {
                Centroid cc = centroids[i];
                for (int j = 0; j < cc.documents.size(); j++) {
                    int docID = cc.documents.get(j);
                    if (clustersAssigned.get(docID) != i) {
                        clustersAssigned.put(docID, i);
                        reassigned++;
                    }
                }
            }

            System.out.print(" " + reassigned);
            if (reassigned <= _customizer.getStopCriterion()) {
                toUpdate = false;
                continue;
            }


            // Update the centroids.
            for (int i = 0; i < centroids.length; i++)
                centroids[i].computeCentroid(dists);
        }


        for (int i = 0; i < clusters.size(); i++) {
            ClusterDescriptor cd = clusters.get(i);
            cd.centroid = centroids[i].features;
            cd.documents = centroids[i].documents;
            cd.distance = new TDoubleArrayList(cd.documents.size());
            for (int j = 0; j < cd.documents.size(); j++) {
                double score = _customizer.getSimilarityFunction().computeSimilarity(centroids[i].features, dists, (int) cd.documents.get(j));
                cd.distance.add(score);
            }
        }


        return clusters;
    }

    public KMeansCustomizer getClusterizerRuntimeCustomizer() {
        return _customizer;
    }

    public void setClusterizerRuntimeCustomizer(
            IClusterizerRuntimeCustomizer customizer)

    {
        _customizer = (KMeansCustomizer) customizer;
    }


}
