package sr;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Vocabulary implements Serializable {

	private static final long serialVersionUID = 4269486928598428641L;
	private String[] vocabulary;
	private Map<String, Integer> wordToKeyMap;
	public static final String UNKNOWN = "<unk>";
	public static final String SENTENCE = "<s>";
	
	public Vocabulary(Set<String> wordSet) {
		init(wordSet);
	}
	
	public Vocabulary(String fileName) throws IOException {
		Set<String> wordSet = new HashSet<String>();
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "UTF-8"));
		String line;
		while ((line = bufferedReader.readLine()) != null) {
			String word = line.trim();
			if (word.equals("")) continue;
			wordSet.add(word);
		}
		
		init(wordSet);
	}
	
	private void init(Set<String> wordSet) {
		this.vocabulary = new String[wordSet.size()+3];
		this.wordToKeyMap = new HashMap<String, Integer>();
		
		this.vocabulary[0] = UNKNOWN;
		this.vocabulary[1] = SENTENCE;
		
		wordToKeyMap.put(UNKNOWN, 1);
		wordToKeyMap.put(SENTENCE, 2);
		
		List<String> wordList = new ArrayList<String>(wordSet);
		Collections.sort(wordList);
		
		int i=3;
		for (String word : wordList) {
			vocabulary[i-1] = word;
			wordToKeyMap.put(word, i);
			i++;
		}
	}
	//-1
	public String getWordByKey(int key) {
		if (key < 1 || key >= vocabulary.length) throw new IllegalArgumentException("word key ("+key+") must range between 1 and "+(vocabulary.length));
		return vocabulary[key-1];
	}
	
	public String[] getWordByKey(int key[]) {
		String[] words = new String[key.length];
		for (int i=0; i<words.length; i++) words[i] = getWordByKey(key[i]);
		return words;
	}
	
	public int getWordKey(String word) {
		Integer key = this.wordToKeyMap.get(word);
		return (key != null ? key : 1);
	}
	
	public int[] getWordKey(String[] words) {
		int[] keys = new int[words.length];
		for (int i=0; i<words.length; i++) keys[i] = getWordKey(words[i]);
		return keys;
	}
	
	public int size() {
		return this.vocabulary.length;
	}

	public String[] getVocabulary() {
		return vocabulary;
	}
}
