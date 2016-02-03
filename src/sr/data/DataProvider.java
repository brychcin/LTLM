package sr.data;

import java.io.IOException;

import sr.Sentence;

public interface DataProvider {

	public Sentence next() throws IOException;

	public void close() throws IOException;
	
	public void reset() throws IOException;
	
}
