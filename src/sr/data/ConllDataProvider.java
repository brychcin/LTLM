package sr.data;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.PropertyConfigurator;

import sr.Role;
import sr.Sentence;
import sr.Token;
import sr.Vocabulary;
import sr.lm.LTLM2sides3gram;
import sr.utils.IOUtils;

public class ConllDataProvider implements DataProvider {

	private BufferedReader reader;
	private Vocabulary vocabulary;
	private String fileName;
	
	public ConllDataProvider(String fileName, Vocabulary vocabulary) throws UnsupportedEncodingException, FileNotFoundException {
		this.vocabulary = vocabulary;
		this.fileName = fileName;
		this.reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "UTF-8"));
	}
	
	@Override
	public Sentence next() throws IOException {
		
		List<String> words = new ArrayList<String>();
		List<Short> roles = new ArrayList<Short>();
		List<Short> parents = new ArrayList<Short>();
		
		
		while (true) {
			String line = reader.readLine();
			if (line == null) return null;
			line = line.trim();
			if (line.length()==0) break;
			
			String[] parts = line.split("\t");
			
			//short position = Short.parseShort(parts[0]);
			String word = parts[1];
			short role =  Short.parseShort(parts[2]);
			short parent = Short.parseShort(parts[3]);
			
			words.add(word);
			roles.add(role);
			parents.add(parent);
		}
		

		int[] keysArrays = new int[words.size()];
		for (int i=0; i<keysArrays.length; i++) keysArrays[i] = vocabulary.getWordKey(words.get(i));

		Sentence sentence = new Sentence(keysArrays);
		
		for (short i=0; i<keysArrays.length; i++) {
			sentence.getTokens()[i].setRole(new Role(roles.get(i), sentence.getTokens()[i], i));
		}
		
		for (int i=0; i<keysArrays.length; i++) {
			short parent = parents.get(i);
			Token parentToken = (parent >= 0 ? sentence.getTokens()[parent] : sentence.getRoot());
			parentToken.getRole().setChild(sentence.getTokens()[i].getRole());
		}
		
		sentence.getRoot().getRole().calculateMinMaxPositions();
		return sentence;
	}

	@Override
	public void close() throws IOException {
		reader.close();
	}

	public void reset() throws IOException {
		this.reader.close();
		this.reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "UTF-8"));
	}

	public static void main(String[] args) throws IOException, ClassNotFoundException {
		PropertyConfigurator.configure("log4j.properties");
		//LTLM2sides2gram ltlm = IOUtils.load2Gram("models/2gram_en_LTLM_100roles.bin");
		LTLM2sides3gram ltlm = IOUtils.load3Gram("models/3gram_en_LTLM_50roles.bin");
		
		DataProvider provider = new ConllDataProvider("pokus.txt", ltlm.getVocabulary());
		
		Sentence sentence = null;
		while ((sentence = provider.next()) != null) {

			System.out.println(sentence.getText(ltlm.getVocabulary()));
			System.out.println(sentence.toPennFormat());
			
			double log = 0;
			for (int i=0; i<sentence.size(); i++) {
				log += Math.log(ltlm.getProbabilityAtPosition(sentence, i, true));
			}
			System.out.println("log="+log);
		}
		
	}
}
