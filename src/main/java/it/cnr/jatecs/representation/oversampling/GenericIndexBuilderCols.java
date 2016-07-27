package it.cnr.jatecs.representation.oversampling;

import it.cnr.jatecs.indexes.DB.generic.GenericIndex;
import it.cnr.jatecs.indexes.DB.interfaces.*;
import it.cnr.jatecs.indexes.DB.troveCompact.*;
import it.cnr.jatecs.representation.vector.SparseVector;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;

public class GenericIndexBuilderCols {
	
		private IDocumentDB documents;		
		private ICategoryDB categories;
		private IClassificationDB classification;		
		private TroveFeatureDBBuilder features;
		private TroveDomainDB domain;		
		private TroveContentDBBuilder content;
		private TroveWeightingDBBuilder weighting;
		private int[] documentIDs;
		
		public GenericIndexBuilderCols(IClassificationDB classification){
			this.documents=classification.getDocumentDB();
			this.categories=classification.getCategoryDB();
			this.classification=classification;
			
			features=new TroveFeatureDBBuilder();
			this.domain=new TroveDomainDB(this.categories, features.getFeatureDB());
			content=new TroveContentDBBuilder(this.documents, features.getFeatureDB());
			weighting=new TroveWeightingDBBuilder(content.getContentDB());
			
			documentIDs=new int[documents.getDocumentsCount()];
			int pos=0;
			for(IIntIterator docs=documents.getDocuments(); docs.hasNext(); )
				documentIDs[pos++]=docs.next();
		}
		
		public int addFeature(String featname){
			return features.addFeature(featname);
		}
		
		public void setDocumentFeatureFrequency(int docID, int featID, int freq){
			content.setDocumentFeatureFrequency(docID, featID, freq);
		}
		
		public void incrementDocumentFeatureFrequency(int docID, int featID){
			int old=content.getContentDB().getDocumentFeatureFrequency(docID, featID);
			setDocumentFeatureFrequency(docID, featID, old+1);
		}
		
		public void setDocumentFeatureWeight(int docID, int featID, double weight){
			this.weighting.setDocumentFeatureWeight(docID, featID, weight);
		}
		
		public IIndex getIndex(){
			return new GenericIndex(features.getFeatureDB(), 
					documents, 
					categories, 
					domain, 
					content.getContentDB(), 
					weighting.getWeightingDB(), 
					classification);
		}

		public IIntIterator getFeatures() {
			return domain.getFeatureDB().getFeatures();
		}
		
		public int addFeatureColFrequencies(String featName, SparseVector featRep) {
			int featID=features.addFeature(featName);
			for(int dim:featRep.getNonZeroDimensions()){
				int docID = documentIDs[dim];
				this.setDocumentFeatureFrequency(docID, featID, (int)featRep.get(dim));
			}	
			return featID;
		}
		
		public int addFeatureColWeights(String featName, SparseVector featRep) {
			int featID=features.addFeature(featName);
			for(int dim:featRep.getNonZeroDimensions()){
				int docID = documentIDs[dim];
				this.setDocumentFeatureFrequency(docID, featID, 1);
				this.setDocumentFeatureWeight(docID, featID, featRep.get(dim));
			}	
			return featID;
		}
	}