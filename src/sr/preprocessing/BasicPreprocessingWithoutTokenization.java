package sr.preprocessing;


public class BasicPreprocessingWithoutTokenization extends BasicPreprocessing {

	public BasicPreprocessingWithoutTokenization(boolean toLowerCase, boolean removeAccents) {
		super(toLowerCase, removeAccents);
	}

	@Override
	public String[] preprocessText(String text) {
		String[] words = text.split("[ \\t\\s\\n]+");
		for (int i=0; i<words.length; i++) {
			words[i] = preprocessWord(words[i]);
		}
		return words;
	}
	
}
