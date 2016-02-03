package sr.data;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import sr.Sentence;
import sr.Vocabulary;
import sr.preprocessing.Preprocessing;

public class BasicDataProvider implements DataProvider {

	private BufferedReader reader;
	private Preprocessing preprocessing;
	private Vocabulary vocabulary;
	private String fileName;
	private final int MAX_SENTENCE_LENGTH;
	private final int MIN_SENTENCE_LENGTH;
	
	
	public BasicDataProvider(String fileName, Preprocessing preprocessing, Vocabulary vocabulary, int MIN_SENTENCE_LENGTH, int MAX_SENTENCE_LENGTH) throws UnsupportedEncodingException, FileNotFoundException {
		this.preprocessing = preprocessing;
		this.vocabulary = vocabulary;
		this.fileName = fileName;
		this.reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "UTF-8"));
		this.MIN_SENTENCE_LENGTH = MIN_SENTENCE_LENGTH;
		this.MAX_SENTENCE_LENGTH = MAX_SENTENCE_LENGTH;
	}
	
	public void reset() throws IOException {
		this.reader.close();
		this.reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "UTF-8"));
	}
	
	@Override
	public Sentence next() throws IOException {
		String line = reader.readLine();
		if (line == null) return null;
		
		String[] words = preprocessing.preprocessText(line);
		if (words.length < MIN_SENTENCE_LENGTH || words.length > MAX_SENTENCE_LENGTH) return next();
		
		int[] keys = vocabulary.getWordKey(words);
		return new Sentence(keys);
	}

	@Override
	public void close() throws IOException {
		reader.close();
	}

}
