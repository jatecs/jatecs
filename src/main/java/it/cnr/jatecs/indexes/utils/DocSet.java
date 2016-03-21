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

package it.cnr.jatecs.indexes.utils;

import it.cnr.jatecs.indexes.DB.interfaces.IClassificationDB;
import it.cnr.jatecs.indexes.DB.interfaces.IContentDB;
import it.cnr.jatecs.indexes.DB.interfaces.IDocumentDB;
import it.cnr.jatecs.indexes.DB.interfaces.IIndex;
import it.cnr.jatecs.utils.iterators.IntArrayIterator;
import it.cnr.jatecs.utils.iterators.interfaces.IIntIterator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class DocSet {	
	private HashSet<Integer> docset;	
	
	public DocSet(){
		this.docset=new HashSet<Integer>();
	}
	
	public DocSet(Collection<Integer> col){
		this.docset=new HashSet<Integer>(col);
	}
	
	public static DocSet genFeatDocset(IIndex index, int featid){
		return genFeatDocset(index.getContentDB(), featid);
	}
	
	public static DocSet genFeatDocset(IContentDB content, int featid){
		DocSet set = new DocSet();
		set.docset=new HashSet<Integer>();
		
		IIntIterator docit = content.getFeatureDocuments(featid);
		while(docit.hasNext()){
			set.docset.add(docit.next());
		}
		
		return set;
	}
	
	public static DocSet genCatDocset(IIndex index, short catid){
		DocSet set = new DocSet();
		set.docset=new HashSet<>();
		
		IIntIterator docit = index.getClassificationDB().getCategoryDocuments(catid);
		while(docit.hasNext()){
			set.docset.add(docit.next());
		}
		
		return set;
	}
	
	public static DocSet genNegativeDocset(IIndex index, short catid) {
		DocSet alldocs = DocSet.genDocset(index.getDocumentDB());
		DocSet posdocs = DocSet.genCatDocset(index, catid);
		return DocSet.minus(alldocs, posdocs);
	}
	
	public static DocSet genCatDocset(IClassificationDB classification, short catid){
		DocSet set = new DocSet();
		set.docset=new HashSet<>();
		
		IIntIterator docit = classification.getCategoryDocuments(catid);
		while(docit.hasNext()){
			set.docset.add(docit.next());
		}
		
		return set;
	}
	
	public static DocSet genDocset(IDocumentDB documents){
		DocSet set = new DocSet();
		
		IIntIterator docit = documents.getDocuments();
		while(docit.hasNext()){
			set.docset.add(docit.next());
		}
		
		return set;
	}
	
	public int size(){
		return docset.size();
	}
	
	public int intersectionSize(DocSet other){
		if(this.size()<other.size()){
			return DocSet.intersectionSize(this, other);
		}
		else{
			return DocSet.intersectionSize(other, this);
		}
	}
	private static int intersectionSize(DocSet smallest, DocSet largest){
		int count=0;
		for(int el : smallest.docset){
			if(largest.docset.contains(el)){
				count++;
			}
		}
		return count;
	}

	public static DocSet union(DocSet docset1, DocSet docset2) {
		DocSet union = new DocSet();
		union.docset.addAll(docset1.docset);
		union.docset.addAll(docset2.docset);
		return union;
	}	
	
	public void minus(DocSet rem){
		this.docset.removeAll(rem.docset);		
	}
	
	public void addAll(DocSet add){
		this.docset.addAll(add.docset);
	}

	public void intersection(DocSet validDocs) {
		this.docset.retainAll(validDocs.docset);
	}
	
	public Iterator<Integer> getIterator(){
		return this.docset.iterator();
	}

	public void add(int docid) {
		this.docset.add(docid);
	}

	public static DocSet minus(DocSet set1, DocSet set2) {
		DocSet minus = new DocSet();
		minus.docset.addAll(set1.docset);
		minus.minus(set2);
		return minus;
	}

	public static DocSet intersection(DocSet set1, DocSet set2) {
		DocSet inter = new DocSet();
		inter.docset.addAll(set1.docset);
		inter.intersection(set2);
		return inter;
	}

	public Collection<Integer> randomSelectionNoRep(int n_sel) {
		Random r = new Random();
		ArrayList<Integer> elements = new ArrayList<Integer>(this.docset);
		HashSet<Integer> selected = new HashSet<Integer>();
		while(selected.size()<n_sel && !elements.isEmpty()){
			int pos = r.nextInt(elements.size());
			int el = elements.remove(pos);
			selected.add(el);
		}
		return selected;
	}
	
	public Collection<Integer> randomSampling(int n_sel) {
		Random r = new Random();
		ArrayList<Integer> elements = new ArrayList<Integer>(this.docset);
		List<Integer> selected = new ArrayList<Integer>(n_sel);
		while(selected.size()<n_sel){
			int pos = r.nextInt(elements.size());
			int el = elements.get(pos);
			selected.add(el);
		}
		return selected;
	}

	public void removeAll(Collection<Integer> els) {
		this.docset.removeAll(els);		
	}

	public IIntIterator getIIntIterator() {
		int n_docs = this.size();
		ArrayList<Integer> docsArray = new ArrayList<Integer>(n_docs);
		
		for(Iterator<Integer> docit = this.getIterator(); docit.hasNext(); ){
			int docid = docit.next();
			docsArray.add(docid);
		}
		
		Collections.sort(docsArray);
		return IntArrayIterator.List2IntArrayIterator(docsArray);
	}

	public void delete(int todelete) {
		this.docset.remove(todelete);		
	}
	
	public List<Integer> asList(){
		return new ArrayList<Integer>(this.docset);
	}
	
	
	public boolean equals(Object other){
		if(other instanceof DocSet){
			DocSet o=(DocSet)other;
			if(this.size()==o.size())
				return this.size()==this.intersectionSize(o);
		}
		
		return false;
	}

	public List<Integer> asListRandom() {
		List<Integer> aslist=this.asList();
		Collections.shuffle(aslist);
		return aslist;
	}


}
