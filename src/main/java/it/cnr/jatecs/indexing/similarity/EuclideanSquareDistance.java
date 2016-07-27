package it.cnr.jatecs.indexing.similarity;

import gnu.trove.TIntDoubleHashMap;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;

public class EuclideanSquareDistance extends BaseSimilarityFunction {

	public EuclideanSquareDistance(){		
	}

	public double compute(TIntDoubleHashMap doc1, TIntDoubleHashMap doc2, IIntIterator features) {

		double dist = 0;

		features.begin();
		while(features.hasNext()){
			int featID = features.next();
			dist += Math.pow(doc1.get(featID)-doc2.get(featID), 2);
		}

		return dist;
	}

	public int compareSimilarity(double score1, double score2) {
		return -(Double.compare(score1, score2));
	}

	@Override
	public double compute(int doc1, int doc2, IIndex index){
		return compute(doc1, index, doc2, index);
	}

	@Override
	public double compute(int doc1, IIndex idx1, int doc2, IIndex idx2) 
	{
		IIntIterator feats1 = idx1.getContentDB().getDocumentFeatures(doc1);
		IIntIterator feats2 = idx2.getContentDB().getDocumentFeatures(doc2);

		int feat1=(feats1.hasNext()?feats1.next():Integer.MAX_VALUE);
		int feat2=(feats2.hasNext()?feats2.next():Integer.MAX_VALUE);
		double score = 0;
		
		while(feat1 != Integer.MAX_VALUE || feat2 != Integer.MAX_VALUE){
			double w1=0;
			double w2=0;
			
			if (feat1 <= feat2){
				w1=idx1.getWeightingDB().getDocumentFeatureWeight(doc1, feat1);
				feat1=(feats1.hasNext()?feats1.next():Integer.MAX_VALUE);
			}
			
			if (feat2 <= feat1){
				w2=idx2.getWeightingDB().getDocumentFeatureWeight(doc2, feat2);
				feat2=(feats2.hasNext()?feats2.next():Integer.MAX_VALUE);
			}			
			
			double dif=(w1-w2);
			score+=(dif*dif);
			
		}

		return score;
	}


}
