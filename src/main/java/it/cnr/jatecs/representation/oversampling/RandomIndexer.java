package it.cnr.jatecs.representation.oversampling;


import it.cnr.jatecs.indexes.DB.interfaces.*;
import it.cnr.jatecs.indexes.DB.troveCompact.*;
import it.cnr.jatecs.representation.vector.SparseVector;
import it.cnr.jatecs.utils.JatecsLogger;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

public class RandomIndexer {
	
	public IIndex reindexDocuments(IIndex index, RandomIndexDictionary featDictionary){
		IFeatureDB latentFeatures=createLatentFeatures(featDictionary.getDimensions());
		ICategoryDB categories=index.getCategoryDB();
		IDomainDB domain=new TroveDomainDB(categories, latentFeatures);
		
		GenericIndexBuilderRows projection=new GenericIndexBuilderRows(domain);
		
		IIntIterator docit=index.getDocumentDB().getDocuments();
		while(docit.hasNext()){
			int docid=docit.next();
			SparseVector docProjection=new SparseVector(featDictionary.getDimensions());
			
			IIntIterator featit=index.getContentDB().getDocumentFeatures(docid);
			while(featit.hasNext()){
				int featid=featit.next();
				double weight=index.getWeightingDB().getDocumentFeatureWeight(docid, featid);
				
				SparseVector randindex=featDictionary.get(featid);
				docProjection.add(randindex, weight);
			}
			
			String docName=index.getDocumentDB().getDocumentName(docid);
			IShortIterator docCats=index.getClassificationDB().getDocumentCategories(docid);
			docProjection.normalize();
			projection.addDocumentRawWeights(docName, docProjection, docCats);
		}
		return projection.getIndex();
	}
	
	public IIndex reindexFeatures(IIndex index, RandomIndexDictionary docDictionary){
		IDocumentDB latentDocuments=createLatentDocuments(docDictionary.getDimensions());
		TroveCategoryDBBuilder emptyCats=new TroveCategoryDBBuilder();
		TroveClassificationDBBuilder emptyClassif=new TroveClassificationDBBuilder(latentDocuments, emptyCats.getCategoryDB());
		GenericIndexBuilderCols projection=new GenericIndexBuilderCols(emptyClassif.getClassificationDB());
		
		int progress=0;
		int total=index.getFeatureDB().getFeaturesCount();
		
		IIntIterator featit=index.getFeatureDB().getFeatures();
		while(featit.hasNext()){
			int featid=featit.next();
			SparseVector featProjection=new SparseVector(docDictionary.getDimensions());
			
			IIntIterator docit=index.getContentDB().getFeatureDocuments(featid);
			while(docit.hasNext()){
				int docid=docit.next();
				double weight=index.getWeightingDB().getDocumentFeatureWeight(docid, featid);
				
				SparseVector randindex=docDictionary.get(docid);
				featProjection.add(randindex, weight);				
			}
			
			featProjection.normalize();
			
			String featName=index.getFeatureDB().getFeatureName(featid);
			projection.addFeatureColWeights(featName, featProjection);
			
			if(++progress%100==0)
				JatecsLogger.status().print("..."+(progress*500/total)+"%");
		}
		JatecsLogger.status().println("[Done]");
		return projection.getIndex();
	}
	
	public RandomIndexDictionary getDocumentRandomDictionary(IDocumentDB documents, int dimensions, int nonZeros, boolean onlyPositive){
		IIntIterator docit=documents.getDocuments();
		return new RandomIndexDictionary(docit, dimensions, nonZeros, onlyPositive);
	}
	
	public RandomIndexDictionary getFeaturesRandomDictionary(IFeatureDB features, int dimensions, int nonZeros, boolean onlyPositive){
		IIntIterator featit=features.getFeatures();
		return new RandomIndexDictionary(featit, dimensions, nonZeros, onlyPositive);
	}
	
	private static IFeatureDB createLatentFeatures(int n){
		TroveFeatureDBBuilder latent=new TroveFeatureDBBuilder();
		for(int i = 0; i < n; i++){
			latent.addFeature("latentFeature_"+i);
		}
		return latent.getFeatureDB();
	}
	
	private static IDocumentDB createLatentDocuments(int n){
		TroveDocumentsDBBuilder latent=new TroveDocumentsDBBuilder();
		for(int i = 0; i < n; i++){
			latent.addDocument("latentDocument_"+i);
		}
		return latent.getDocumentDB();
	}
}
