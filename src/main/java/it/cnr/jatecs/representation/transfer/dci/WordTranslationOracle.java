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

package it.cnr.jatecs.representation.transfer.dci;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * Implements a word-translation oracle as described by 
 * <i>{@code Prettenhofer, P., & Stein, B. (2010, July). 
 * Cross-language text classification using structural 
 * correspondence learning. In Proceedings of the 48th 
 * Annual Meeting of the Association for Computational 
 * Linguistics (pp. 1118-1127). Association for 
 * Computational Linguistics.}</i>
 * */
public class WordTranslationOracle implements IWordTranslationOracle{

	private String _path;
	private boolean _interactiveConstruction=false;
	private HashMap<String, String> _dictionary;
	private HashMap<String, String> _inv_dictionary;
	
	public WordTranslationOracle(String mapPath, boolean interactiveConstruction) throws FileNotFoundException, IOException{
		_path = mapPath;
		_dictionary=new HashMap<String, String>();
		_inv_dictionary=new HashMap<String, String>();
		_interactiveConstruction=interactiveConstruction;
		if(interactiveConstruction){
			File f = new File(_path);
			if(!f.exists()) save();
		}
		try(BufferedReader br = new BufferedReader(new FileReader(mapPath))) {
		    for(String line; (line = br.readLine()) != null; ) {
		        String[] words = line.split("\\t+");
		        assert(words.length==2);
		        _dictionary.put(words[0], words[1]);
		        _inv_dictionary.put(words[1], words[0]);
	        }		   
		}
	}
	
	@Override
	public String translate(String word) {	
		if(_dictionary.containsKey(word)){
			return _dictionary.get(word);
		}
		else if(_interactiveConstruction){
			System.out.println("Enter translation for word: <"+word+">");
			 BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			 String traslation = null;
		      try {
		         traslation = br.readLine();
		         this.add(word, traslation);
		         this.save();
		      } catch (IOException ioe) {
		         System.out.println("Error reading input...abort!");
		         System.exit(1);
		      }
		      return traslation;
		}
		else return null;
			
	}

	@Override
	public boolean canTranslate(String word) {
		if(_interactiveConstruction)
			return true;
		else
			return _dictionary.containsKey(word);
	}
	
	public String invTranslation(String word){
		return _inv_dictionary.get(word);
	}
	
	public void save() throws IOException {
		FileWriter fw = new FileWriter(_path);
		
		ArrayList<String> sourcewords = new ArrayList<String>(_dictionary.keySet());
		Collections.sort(sourcewords);
	 
		for (String sword:sourcewords) {
			fw.write(sword+"\t"+_dictionary.get(sword)+"\n");
		}
	 
		fw.close();
	}
	
	public void add(String s_word, String t_word){
		this._dictionary.put(s_word, t_word);
	}

	@Override
	public boolean isCreateOnDemand(){
		return this._interactiveConstruction;
	}
	
	public String showPath(){
		return this._path;
	}

}
