package sr.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.xml.sax.SAXException;

import sr.preprocessing.BasicPreprocessingWithoutTokenization;
import sr.preprocessing.Preprocessing;


public class VocabularyCreator {

	private Map<String, Integer> words;
	private List<Map.Entry<String, Integer>> list;
	private Preprocessing preprocessing;
	private int length = 0;
	
	public VocabularyCreator(String data, Preprocessing preprocessing) throws IOException, SAXException, ClassNotFoundException {
		this.words = new HashMap<String, Integer>();
		this.preprocessing = preprocessing;
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(data), "UTF-8"));
		String line;
		while ((line = reader.readLine()) != null) {
			//String[] parts = Tokenizer.tokenize(Tokenizer.defaultRegex, line);
			String[] parts = line.split("[ \\t\\s\\n]+");
			processWords(parts);
		}
		reader.close();
		
		this.list = new ArrayList<Map.Entry<String, Integer>>();
		list.addAll(words.entrySet());
		Collections.sort(list, new CandidateComparator());
		System.out.println("words "+list.size());
		System.out.println("length "+length);
	}
	
	private void processWords(String[] words) {
		for (String word : words) {
			word = preprocessing.preprocessWord(word);
			processWord(word);
			length++;
		}
	}
	
	private void processWord(String word) {
		Integer oldCount = words.get(word);
		if (oldCount == null) oldCount = 0;
		words.put(word, oldCount+1);
	}
	
		
	public void createCandidateList(String fileName, int number, boolean writeCount) throws IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName), "UTF-8"));
		for (Map.Entry<String, Integer> entry : (list.size() >= number ? list.subList(0, number) : list)) {
			writer.write(entry.getKey());
			if (writeCount) writer.write("\t"+entry.getValue());
			writer.write("\n");
		}
		writer.flush();
		writer.close();
	}
	
	private class CandidateComparator implements Comparator<Map.Entry<String, Integer>> {
		@Override
		public int compare(Entry<String, Integer> a, Entry<String, Integer> b) {
			return b.getValue() - a.getValue();
		}
	}

	public static void main(String[] args) throws IOException, SAXException, ClassNotFoundException {
		Preprocessing preprocessing = new BasicPreprocessingWithoutTokenization(true, false);
		VocabularyCreator vc = new VocabularyCreator("D:/korpusy/czeng/cs-trainFull.txt", preprocessing);
		vc.createCandidateList("D:/korpusy/czeng/cs-vocabularyFull100000.txt", 100000, false);

	}
}
