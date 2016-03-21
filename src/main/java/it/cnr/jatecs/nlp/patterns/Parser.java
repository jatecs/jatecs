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

package it.cnr.jatecs.nlp.patterns;

import edu.stanford.nlp.ling.TaggedWord;
import it.cnr.jatecs.nlp.lexicon.GI;
import it.cnr.jatecs.nlp.lexicon.SWN3;
import it.cnr.jatecs.nlp.lexicon.ShlomoArgamonExtractor;
import it.cnr.jatecs.nlp.utils.StanfordPOSTagger;

import java.util.*;

/*
 * Class Parser
 * LL(1) parser that parse our grammar
 */

public class Parser {

    public static String[] negatives = new String[]{
            "hardly", "lack", "lacking", "lacks", "neither", "nor", "never", "no", "nobody",
            "none", "nothing", "nowhere", "not", "n�t", "n't", "aint", "cant", "cannot", "darent",
            "dont", "doesnt", "didnt", "hadnt", "hasnt", "havnt", "havent", "isnt", "mightnt",
            "mustnt", "neednt", "oughtnt", "shant", "shouldnt", "wasnt", "wouldnt", "without"
    };
    private final ShlomoArgamonExtractor she = new ShlomoArgamonExtractor();
    private final GI gi = new GI();
    private final SWN3 swn3 = new SWN3();
    private final StanfordPOSTagger tagger = new StanfordPOSTagger();
    public Vector<String> extractedPatterns = null;
    public boolean useSAE = false;
    ;
    public boolean useGI = false;
    public boolean useSWN = false;
    public boolean useCombined = true;
    private int[] normalization_NF;
    private Vector<int[]> normalization_ADJF;
    private int[] normalization_V;

    public Parser() {

    }

    public static void main(String[] args) {

        Parser p = new Parser();
        Vector<String> patt = p.extract(
                "The Park Hyatt Milan is truly one of the best properties in Milano. Our stay was wonderful and having visited both the Four Seasons and the Bulgari Hotels in Milan, prefer this property over any other. The hotel has a very modern, intimate feel and is in a superb location near the Duomo and the Galleria. Having booked through the American Express Fine Hotels Resorts program, the included amenities are worthwhile and well-delivered." +
                        "Staff: I must comment on the entire staff at this property - they are by far extremely friendly and attentive to any needs. Having interacted with numerous general managers and management at various hotels, I can truly say that the GM and top management at this property are by far one of the best that I have ever encountered and their excellent service mentality trickles down to each member of the team. Every individual we encountered was willing to help and provide excellent service. The concierge team was great and arranged many items for us before and during our time in Milano, all without hesitation. Service is a key component to ensure a great stay and this property truly exemplifies excellent standards and hospitality." +
                        "Room: We stayed in a \"Park Junior Suite\" overlooking the inner courtyard of the hotel. The room was modern and extremely spacious and comfortable with a large living area. As other reviewers have mentioned, the bathrooms are excellent, with ours having two sinks, a large tub, and excellent shower. There is also plenty of storage space for your belongings. Rooms are nicely furnished and elegantly modern, as are most Park Hyatt properties." +
                        "Restaurant: Food is great overall in the main \"Park\" restaurant and the bar area as well. Breakfasts in La Cupola were satisfying and include a not good selection of food." +
                        "This property should not be overlooked when planning a trip to Milano."
        );

        for (Iterator<String> iterator = patt.iterator(); iterator.hasNext(); ) {
            String string = (String) iterator.next();
            System.out.println(string);
        }
    }

    /************************************************************
     * Parse a single review and returns the patterns extracted	*
     * *
     *
     * @param review the review were to extract the patterns	*
     * @return patterns the patterns extracted   				*
     ***********************************************************/
    public Vector<String> extract(String review) {
        extractedPatterns = new Vector<String>();

        Vector<ArrayList<TaggedWord>> taggedSentences = new Vector<ArrayList<TaggedWord>>();
        taggedSentences = tagger.tag(review);
        for (ArrayList<TaggedWord> tSentence : taggedSentences)
            extractedPatterns.addAll(parse(tSentence));
        return extractedPatterns;
    }

    private Vector<String> parse(ArrayList<TaggedWord> taggedWords) {
        Vector<ArrayList<TaggedWord>> sentences = findAppositives(taggedWords);
        Vector<String> patterns = new Vector<String>();
        for (Iterator<ArrayList<TaggedWord>> iterator = sentences.iterator(); iterator.hasNext(); ) {
            Vector<String> pattern = null;
            taggedWords = (ArrayList<TaggedWord>) iterator.next();
            IntegerMangi index = new IntegerMangi(0);
            while (index.get() < taggedWords.size()) {
                pattern = A(taggedWords, index);
                if (pattern == null)
                    pattern = B(taggedWords, index);
                if (pattern == null)
                    pattern = C(taggedWords, index);
                if (pattern != null)
                    patterns.addAll(pattern);
                else
                    index.incr();
            }
        }
        return patterns;
    }

    private Vector<String> A(ArrayList<TaggedWord> sentence, IntegerMangi index) {
        normalization_NF = new int[2];
        normalization_ADJF = new Vector<int[]>();
        int startIndex = index.get();
        if (ATTF(sentence, index)) {
            normalization_NF[0] = index.get();
            if (NF(sentence, index)) {
                normalization_NF[1] = index.get();
                return TaggedWordsArrayToString(sentence, "A");
            }
        }
        index.set(startIndex);
        return null;
    }

    private Vector<String> B(ArrayList<TaggedWord> sentence, IntegerMangi index) {
        normalization_NF = new int[2];
        normalization_ADJF = new Vector<int[]>();
        int startIndex = index.get();
        normalization_NF[0] = startIndex;
        if (NF(sentence, index)) {
            normalization_NF[1] = index.get();
            if (match(sentence, index, "V"))
                if (ATTF(sentence, index))
                    return TaggedWordsArrayToString(sentence, "B");

        }
        index.set(startIndex);
        return null;
    }

	/*
     * ATTF: ADJF ATTF1
	 */

    private Vector<String> C(ArrayList<TaggedWord> sentence, IntegerMangi index) {
        normalization_V = new int[2];
        normalization_NF = new int[2];
        normalization_ADJF = new Vector<int[]>();
        int startIndex = index.get();
        normalization_V[0] = startIndex;
        if (match(sentence, index, "V")) {
            normalization_V[1] = index.get();
            if (ATTF(sentence, index)) {
                normalization_NF[0] = index.get();
                if (NF(sentence, index)) {
                    normalization_NF[1] = index.get();
                    return TaggedWordsArrayToString(sentence, "C");
                }
            }
        }
        index.set(startIndex);
        return null;
    }

	/*
	 * ATTF1: CJF ATTF1 | epsilon
	 */

    private boolean ATTF(ArrayList<TaggedWord> sentence, IntegerMangi index) {
        int[] temp = new int[2];
        temp[0] = index.get();
        if (ADJF(sentence, index)) {
            temp[1] = index.get();
            normalization_ADJF.add(temp);
            return ATTF1(sentence, index);
        }
        return false;
    }

    private boolean ATTF1(ArrayList<TaggedWord> sentence, IntegerMangi index) {
        if (CJF(sentence, index))
            return ATTF(sentence, index);
        return true;
    }

    private boolean ADJF(ArrayList<TaggedWord> sentence, IntegerMangi index) {
        if (match(sentence, index, "RB"))
            return ADJF(sentence, index);
        return match(sentence, index, "JJ");
    }

    private boolean NF(ArrayList<TaggedWord> sentence, IntegerMangi index) {
        if (match(sentence, index, "DT"))
            return NF(sentence, index);
        if (match(sentence, index, "NN"))
            return NF1(sentence, index);
        if (match(sentence, index, "JJ"))
            return match(sentence, index, "NN");
        return false;
    }

    private boolean NF1(ArrayList<TaggedWord> sentence, IntegerMangi index) {
        match(sentence, index, "NN");
        return true;
    }

    private boolean CJF(ArrayList<TaggedWord> sentence, IntegerMangi index) {
        return match(sentence, index, ",") || match(sentence, index, "CC");
    }

    private boolean match(ArrayList<TaggedWord> sentence, IntegerMangi index, String tag) {
        if (index.get() < sentence.size() && sentence.get(index.get()).tag().startsWith(tag)) {
            index.incr();
            return true;
        }
        return false;
    }

    private Vector<String> TaggedWordsArrayToString(ArrayList<TaggedWord> sentence, String pattern) {
        HashSet<String> ret = new HashSet<String>();
        //Vector<String> ret = new Vector<String>();
        Vector<String> retwithpos = new Vector<String>();
        String noun = "";
        String nounwithpos = "";
        String verb = "";
        String verbwithpos = "";
        Vector<String> adj = new Vector<String>();
        Vector<String> adjwithpos = new Vector<String>();
        for (int i = normalization_NF[0]; i < normalization_NF[1]; i++)
            if (!sentence.get(i).tag().startsWith("DT")) {
                noun += sentence.get(i).word() + " ";
                nounwithpos += sentence.get(i).word() + "#" + sentence.get(i).tag() + " ";
            }
        noun = noun.trim();
        nounwithpos = nounwithpos.trim();
        for (Iterator<int[]> iterator = normalization_ADJF.iterator(); iterator.hasNext(); ) {
            int[] indices = (int[]) iterator.next();
            String temp = "";
            String tempwithpos = "";
            for (int i = indices[0]; i < indices[1]; i++) {
                temp += sentence.get(i).word() + " ";
                tempwithpos += sentence.get(i).word() + "#" + sentence.get(i).tag() + " ";
            }
            adj.add(temp);
            adjwithpos.add(tempwithpos);
        }
        if (pattern == "C")
            for (int i = normalization_V[0]; i < normalization_V[1]; i++) {
                verb += sentence.get(i).word() + " ";
                verbwithpos += sentence.get(i).word() + "#" + sentence.get(i).tag() + " ";
            }
        for (Iterator<String> iterator = adj.iterator(); iterator.hasNext(); ) {
            String a = (String) iterator.next();
            ret.add(verb + a + noun);
        }
        for (Iterator<String> iterator = adjwithpos.iterator(); iterator.hasNext(); ) {
            String a = (String) iterator.next();
            retwithpos.add(verbwithpos + a + nounwithpos);
        }
        if (useSAE)
            ret.addAll(extractSentimentABES(retwithpos, true));
        if (useGI)
            ret.addAll(extractSentimentGI(retwithpos));
        if (useSWN)
            ret.addAll(extractSentimentSWN3(retwithpos));
        if (useCombined)
            ret.addAll(extractSentimentCombined(retwithpos));
        Vector<String> temp = new Vector<String>();
        for (Iterator<String> iterator = ret.iterator(); iterator.hasNext(); ) {
            String v = (String) iterator.next();
            temp.add(v);
        }
        return temp;
    }

    private boolean isNegation(String word) {
        for (int i = 0; i < negatives.length; i++)
            if (negatives[i].equalsIgnoreCase(word))
                return true;
        return false;
    }

    private Vector<String> extractSentimentABES(Vector<String> patternwithpos, boolean sumFlip) {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("positive", "negative");
        map.put("negative", "positive");
        map.put("increase", "decrease");
        map.put("decrease", "increase");
        Vector<String> patterns = she.extract(patternwithpos);
        Vector<String> ret = new Vector<String>();
        for (Iterator<String> iterator = patterns.iterator(); iterator.hasNext(); ) {
            String[] words = ((String) iterator.next()).split(" ");
            boolean flip = false;
            String pattern = "";
            for (int i = 0; i < words.length; i++) {
                if (flip && map.containsKey(words[i]))
                    pattern += map.get(words[i]) + " ";
                else if (words[i].equalsIgnoreCase("flip"))
                    if (sumFlip)
                        flip = true;
                    else
                        pattern += "flip ";
                else
                    pattern += words[i] + " ";
            }
            ret.add(pattern.trim());
        }
        return ret;
    }

    private Vector<String> extractSentimentGI(Vector<String> patternwithpos) {
        return gi.extract(patternwithpos);
    }

    private Vector<String> extractSentimentSWN3(Vector<String> patternwithpos) {
        return swn3.extract(patternwithpos);
    }

    private Vector<String> extractSentimentCombined(Vector<String> patternwithpos) {
        Vector<String> ret = new Vector<String>();
        HashMap<String, String> opposite = new HashMap<String, String>();
        opposite.put("positive", "negative");
        opposite.put("negative", "positive");
        opposite.put("increase", "decrease");
        opposite.put("decrease", "increase");
        opposite.put("positive", "negative");
        opposite.put("negative", "positive");
        opposite.put("flip", "flip");


        for (Iterator<String> iterator = patternwithpos.iterator(); iterator.hasNext(); ) {
            String pattern = (String) iterator.next();
            String[] words = pattern.trim().split(" ");
            String temp = "";
            for (int i = 0; i < words.length; i++) {
                String[] word_pos = words[i].split("#");
                String word = "";
                word = she.get(word_pos[0], word_pos[1]);
                if (word == null)
                    word = gi.get(word_pos[0], word_pos[1]);
                if (word == null)
                    word = swn3.get(word_pos[0], word_pos[1]);
                if (word == null)
                    word = word_pos[0];
                words[i] = word;
            }
            for (int i = 0; i < words.length - 1; i++) {
                if (i < words.length - 2 && opposite.containsKey(words[i]) &&
                        opposite.get(words[i]).equalsIgnoreCase(words[i + 1]))
                    //Elimino opposti consecutivi
                    i++;
                else if (i < words.length - 2 && (words[i].equalsIgnoreCase("flip") || isNegation(words[i])) &&
                        opposite.containsKey(words[i + 1]))
                    words[i + 1] = opposite.get(words[i + 1]);
                else
                    temp += words[i] + " ";

            }
            temp += words[words.length - 1];
            ret.add(temp);
        }
        return ret;
    }

    /**
     * L'idea � che un inciso sia una frase che inizia con un pronome e sta tra due ,
     */
    private Vector<ArrayList<TaggedWord>> findAppositives(ArrayList<TaggedWord> sentence) {
        boolean foundFirst = false;
        ArrayList<TaggedWord> mainSentence = new ArrayList<TaggedWord>();
        ArrayList<TaggedWord> temp = new ArrayList<TaggedWord>();
        Vector<ArrayList<TaggedWord>> sentences = new Vector<ArrayList<TaggedWord>>();
        for (Iterator<TaggedWord> iterator = sentence.iterator(); iterator.hasNext(); ) {
            TaggedWord taggedWord = (TaggedWord) iterator.next();
            //Trattamento speciale delle foreign word
            if (taggedWord.tag().startsWith("FW"))
                taggedWord.setTag("NN");
            if (foundFirst) {
                if (taggedWord.tag().equals(",")) {
                    foundFirst = false;
                    sentences.add(temp);
                    temp = new ArrayList<TaggedWord>();
                } else
                    temp.add(taggedWord);
            } else if (taggedWord.tag().equals(",") && iterator.hasNext()) {
                taggedWord = (TaggedWord) iterator.next();
                if (taggedWord.tag().startsWith("W") || taggedWord.tag().startsWith("PRP")) {
                    foundFirst = true;
                    temp.add(taggedWord);
                } else {
                    mainSentence.add(new TaggedWord(",", ","));
                    mainSentence.add(taggedWord);
                }
            } else
                mainSentence.add(taggedWord);
        }
        if (foundFirst) {
            mainSentence.add(new TaggedWord(",", ","));
            mainSentence.addAll(temp);
        }
        sentences.add(mainSentence);
        return sentences;
    }
}

class IntegerMangi {

    final private int[] _val;

    public IntegerMangi(int val) {
        _val = new int[1];
        _val[0] = val;
    }

    public int get() {
        return _val[0];
    }

    public void set(int val) {
        _val[0] = val;
    }

    public void incr() {
        _val[0]++;
    }
}
