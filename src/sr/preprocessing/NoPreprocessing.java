package sr.preprocessing;

import sr.utils.Tokenizer;

public class NoPreprocessing implements Preprocessing {

	@Override
	public String preprocessWord(String word) {
		return word;
	}

	@Override
	public String[] preprocessText(String text) {
		return Tokenizer.tokenize(Tokenizer.defaultRegex, text);
	}


}
