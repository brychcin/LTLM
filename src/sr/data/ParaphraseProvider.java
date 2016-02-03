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

public class ParaphraseProvider {

	private BufferedReader reader;
	private Preprocessing preprocessing;
	private Vocabulary vocabulary;
	private String fileName;
	
	public ParaphraseProvider(String fileName, Preprocessing preprocessing, Vocabulary vocabulary) throws UnsupportedEncodingException, FileNotFoundException {
		this.preprocessing = preprocessing;
		this.vocabulary = vocabulary;
		this.fileName = fileName;
		this.reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "UTF-8"));
	}
	
	public Paraphrase next() throws IOException {
		String line = reader.readLine();
		if (line == null) return null;
		
		String[] parts = line.split("\t");
		
		String[] words1 = preprocessing.preprocessText(parts[3]);
		int[] keys1 = vocabulary.getWordKey(words1);
		Sentence sentence1 = new Sentence(keys1);
		
		String[] words2 = preprocessing.preprocessText(parts[4]);
		int[] keys2 = vocabulary.getWordKey(words2);
		Sentence sentence2 = new Sentence(keys2);
		
		return new Paraphrase(sentence1, sentence2, (parts[0].equals("1") ? true : false));
	}
	
	public void close() throws IOException {
		reader.close();
	}
	
}
