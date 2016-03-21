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

public class BalancedKMeansClusterizer implements IClusterizer {


    protected BalancedKMeansCustomizer _customizer;


    public BalancedKMeansClusterizer() {
        _customizer = new BalancedKMeansCustomizer();
    }


    protected void swapItems(Centroid[] centroids, int whichCluster, int maxNumberItems,
                             boolean[] visitedClusters, IWeighting3D dists) {
        if (centroids[whichCluster].documents.size() <= maxNumberItems) {
            // Update centroid for this cluster.
            //centroids[whichCluster].computeCentroid(dists);

            return;
        }

        double max = 0;
        max = _customizer.getRD().compareDistance(-Double.MAX_VALUE, Double.MAX_VALUE) < 0 ? -Double.MAX_VALUE : Double.MAX_VALUE;

        // Compute the current centroid.
        //for (int i = 0; i < centroids.length; i++)
        //	centroids[i].computeCentroid(dists);

        // Find the category that should be removed from this cluster.
        int curDocID = -1;
        int idxToRemove = -1;
        for (int i = 0; i < centroids[whichCluster].documents.size(); i++) {
            int docID = centroids[whichCluster].documents.get(i);
            double distance = _customizer.getRD().computeDistance(centroids, whichCluster, docID, dists);

            if (_customizer.getRD().compareDistance(distance, max) > 0) {
                max = distance;
                curDocID = docID;
                idxToRemove = i;
            }
        }

        // Find the new cluster from the remaining where to allocate this category.
        centroids[whichCluster].documents.remove(idxToRemove);
        centroids[whichCluster].distances.remove(idxToRemove);
        visitedClusters[whichCluster] = true;

        // Update centroid for this cluster.
        //centroids[whichCluster].computeCentroid(dists);

        double best = _customizer.getSimilarityFunction().compareSimilarity(0, Double.MAX_VALUE) == 1 ? Double.MAX_VALUE : 0;
        int idx = -1;
        for (int i = 0; i < centroids.length; i++) {
            if (visitedClusters[i])
                continue;

            double score = _customizer.getSimilarityFunction().computeSimilarity(centroids[i].features, dists, (int) curDocID);

            Double s = new Double(score);
            if (s.isNaN())
                System.out.println("Centroid " + i + " Score: " + s);

            if (_customizer.getSimilarityFunction().compareSimilarity(score, best) > 0) {
                best = score;
                idx = i;
            }
        }

        assert (idx != -1);

        centroids[idx].documents.add(curDocID);
        centroids[idx].distances.add(best);

        // Call the method recursively.
        swapItems(centroids, idx, maxNumberItems, visitedClusters, dists);
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
        for (int i = 0; i < dists.getFirstDimensionSize(); i++) {
            clustersAssigned.put(i, 0);
        }


        int maxNumberItems = dists.getFirstDimensionSize() / _customizer.getNumberOfClusters();
        if (dists.getFirstDimensionSize() % _customizer.getNumberOfClusters() != 0)
            maxNumberItems++;

        int lastUpdated = -1;
        int sameUpdated = 0;
        boolean toUpdate = true;
        int maxNumIterations = 50;
        int numIterations = 0;
        while (toUpdate) {
            for (int i = 0; i < centroids.length; i++) {
                centroids[i].documents.clear();
                centroids[i].distances.clear();
            }

            // Distribute the categories among clusters.
            for (int doc = 0; doc < dists.getFirstDimensionSize(); doc++) {
                double best = _customizer.getSimilarityFunction().compareSimilarity(0, Double.MAX_VALUE) > 0 ? Double.MAX_VALUE : 0;
                int whichCluster = 0;
                for (int i = 0; i < centroids.length; i++) {
                    double score = _customizer.getSimilarityFunction().computeSimilarity(centroids[i].features, dists, doc);

                    if (_customizer.getSimilarityFunction().compareSimilarity(score, best) > 0) {
                        best = score;
                        whichCluster = i;
                    }
                }

                centroids[whichCluster].documents.add(doc);
                centroids[whichCluster].distances.add(best);

                boolean[] visitedClusters = new boolean[centroids.length];

                // If we have reached maximum capacity, we must swap the the most improper category (according
                // to some function) from this cluster.
                swapItems(centroids, whichCluster, maxNumberItems, visitedClusters, dists);

            }


            // Compute if the algorithm had been converged.
            int reassigned = 0;
            for (int i = 0; i < clusters.size(); i++) {
                Centroid cc = centroids[i];
                for (int j = 0; j < cc.documents.size(); j++) {
                    int catID = cc.documents.get(j);
                    if (clustersAssigned.get(catID) != i) {
                        clustersAssigned.put(catID, i);
                        reassigned++;
                    }
                }
            }

            System.out.print(" " + reassigned);

            numIterations++;

            if (lastUpdated == reassigned)
                sameUpdated++;
            else {
                lastUpdated = reassigned;
                sameUpdated = 0;
            }

            if (sameUpdated == 5) {
                // Reached fixed point. Exit!
                toUpdate = false;
                continue;
            }


            if (reassigned <= _customizer.getStopCriterion() || numIterations == maxNumIterations) {
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


    public IClusterizerRuntimeCustomizer getClusterizerRuntimeCustomizer() {
        return _customizer;
    }

    public void setClusterizerRuntimeCustomizer(
            IClusterizerRuntimeCustomizer customizer)

    {
        _customizer = (BalancedKMeansCustomizer) customizer;
    }

}
