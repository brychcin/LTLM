package sr.data;

import java.io.IOException;

import sr.Sentence;

public interface DataWriter {

	public void write(Sentence sentence) throws IOException;

	public void close() throws IOException;
	
	public void reset() throws IOException;
	
}
