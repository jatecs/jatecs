package it.cnr.jatecs.representation.oversampling;

import it.cnr.jatecs.indexes.DB.generic.GenericIndex;
import it.cnr.jatecs.indexes.DB.interfaces.IDomainDB;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveClassificationDBBuilder;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveContentDBBuilder;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveDocumentsDBBuilder;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveDomainDB;
import it.cnr.jatecs.indexes.DB.troveCompact.TroveWeightingDBBuilder;
import it.cnr.jatecs.representation.vector.DenseVector;
import it.cnr.jatecs.representation.vector.SparseVector;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IShortIterator;

public class GenericIndexBuilderRows {
	
		private TroveDocumentsDBBuilder documents;
		private TroveDomainDB domain;
		private TroveClassificationDBBuilder classification;
		private TroveContentDBBuilder content;
		private TroveWeightingDBBuilder weighting;
		private int[] featureIDs;
		
		public GenericIndexBuilderRows(IDomainDB domain){
			this.domain=new TroveDomainDB(domain.getCategoryDB(), domain.getFeatureDB());
			documents=new TroveDocumentsDBBuilder();
			classification=new TroveClassificationDBBuilder(documents.getDocumentDB(), this.domain.getCategoryDB());
			content=new TroveContentDBBuilder(documents.getDocumentDB(), domain.getFeatureDB());
			weighting=new TroveWeightingDBBuilder(content.getContentDB());
			
			featureIDs=new int[this.domain.getFeatureDB().getFeaturesCount()];
			int pos=0;
			for(IIntIterator feats=domain.getFeatureDB().getFeatures(); feats.hasNext(); )
				featureIDs[pos++]=feats.next();
		}
		
		public int addDocument(String docname){
			return documents.addDocument(docname);
		}
		
		public void setDocumentCategory(int docID, short catID){
			classification.setDocumentCategory(docID, catID);
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
			GenericIndex gi = new GenericIndex(domain.getFeatureDB(), 
					documents.getDocumentDB(), 
					classification.getClassificationDB().getCategoryDB(), 
					domain, 
					content.getContentDB(), 
					weighting.getWeightingDB(), 
					classification.getClassificationDB());
			
			return gi;
		}

		public IIntIterator getFeatures() {
			return domain.getFeatureDB().getFeatures();
		}
		
		public int addDocumentRawFrequencies(String docName, SparseVector docRep, IShortIterator docCats) {
			int docID = this.addDocument(docName);
			for(int dim:docRep.getNonZeroDimensions()){
				int featID=featureIDs[dim];
				this.setDocumentFeatureFrequency(docID, featID, (int)docRep.get(dim));
			}
			docCats.begin();
			while(docCats.hasNext()){
				short cat=docCats.next();
				this.setDocumentCategory(docID, cat);
			}		
			return docID;
		}
		
		public int addDocumentRawFrequencies(String docName, SparseVector docRep, short cat) {
			int docID = this.addDocument(docName);
			for(int dim:docRep.getNonZeroDimensions()){
				int featID=featureIDs[dim];
				this.setDocumentFeatureFrequency(docID, featID, (int)docRep.get(dim));
			}
			this.setDocumentCategory(docID, cat);		
			return docID;
		}
		
		public int addDocumentRawWeights(String docName, SparseVector docRep, IShortIterator docCats) {
			int docID = this.addDocument(docName);
			for(int dim:docRep.getNonZeroDimensions()){
				int featID=featureIDs[dim];
				this.setDocumentFeatureFrequency(docID, featID, 1);
				this.setDocumentFeatureWeight(docID, featID, docRep.get(dim));
			}
			docCats.begin();
			while(docCats.hasNext()){
				short cat=docCats.next();
				this.setDocumentCategory(docID, cat);
			}	
			
			return docID;
		}
		
		public int addDocumentRawWeights(String docName, DenseVector docRep, IShortIterator docCats) {
			int docID = this.addDocument(docName);
			for(int dim = 0; dim < docRep.size(); dim++){
				int featID=featureIDs[dim];
				this.setDocumentFeatureFrequency(docID, featID, 1);
				this.setDocumentFeatureWeight(docID, featID, docRep.get(dim));
			}
			docCats.begin();
			while(docCats.hasNext()){
				short cat=docCats.next();
				this.setDocumentCategory(docID, cat);
			}	
			return docID;
		}
		
		public boolean hasDocumentNamed(String name){
			int docid = this.documents.getDocumentDB().getDocument(name);
			return this.documents.getDocumentDB().isValidDocument(docid);
		}
	}