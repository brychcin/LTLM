package sr.data;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

import sr.Role;
import sr.Sentence;
import sr.Token;
import sr.Vocabulary;

public class ConllDataWriter implements DataWriter {

	private String fileName;
	private BufferedWriter writer;
	private Vocabulary vocabulary;
	
	public ConllDataWriter(String fileName, Vocabulary vocabulary) throws UnsupportedEncodingException, FileNotFoundException {
		this.fileName = fileName;
		this.vocabulary = vocabulary;
		this.writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName), "UTF-8"));
	}
	
	@Override
	public void write(Sentence sentence) throws IOException {
		for (int i=0; i<sentence.size(); i++) {
			Token token = sentence.getTokens()[i];
			Role role = token.getRole();
			
			writer.write(i+"\t");
			writer.write(vocabulary.getWordByKey(token.getKey())+"\t");
			writer.write(role.getKey()+"\t");
			writer.write(role.getParent().getPosition()+"\n");
		}
		writer.write("\n");
		writer.flush();
	}

	@Override
	public void close() throws IOException {
		writer.close();
	}

	@Override
	public void reset() throws IOException {
		writer.close();
		writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName), "UTF-8"));
	}
	
}
