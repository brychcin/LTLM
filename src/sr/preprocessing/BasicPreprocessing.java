package sr.preprocessing;

import sr.utils.Tokenizer;

public class BasicPreprocessing implements Preprocessing {

	private boolean toLowerCase = true;
	private boolean removeAccents = true;
	
	public BasicPreprocessing(boolean toLowerCase, boolean removeAccents) {
		this.toLowerCase = toLowerCase;
		this.removeAccents = removeAccents;
	}
	
	public String[] preprocessText(String text) {
		String[] words = Tokenizer.tokenize(Tokenizer.defaultRegex, text);
		for (int i=0; i<words.length; i++) {
			words[i] = preprocessWord(words[i]);
		}
		return words;
	}
	
	public String preprocessWord(String word) {
		if (toLowerCase) word = word.toLowerCase();
		if (removeAccents) word = Tokenizer.removeAccents(word);
		return word;
	}
	
	
}
