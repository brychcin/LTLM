package sr.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class CZENGparser {

	public static void createDataForLTLM(File[] files, String fileName) throws IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName), "UTF-8"));
		
		for (File file : files) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
			
			String line;
			while ((line = reader.readLine()) != null) {
				writer.write(line.toLowerCase()+"\n");
				writer.flush();
			}
			
			reader.close();
		}
		
		writer.close();
		
	}
	
	public static void createTrainDataForLTLM() throws IOException {
		File directory = new File("D:/korpusy/czeng/cs-train");
		createDataForLTLM(directory.listFiles(), "D:/korpusy/czeng/cs-trainFull.txt");
	}
	
	public static void main(String args[]) throws IOException {
		
		
		createTrainDataForLTLM();
		
		
		/*
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream("D:/korpusy/czeng/source/data-export-format.99etest"), "UTF-8"));
		
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("D:/korpusy/czeng/source/99etest.surf.cs"), "UTF-8"));
		
		String line;
		while ((line = reader.readLine()) != null) {
			String[] parts = line.toLowerCase().split("\t");
			
			String[] wordParts = parts[2].split("[ \\t\\s\\n]");
			
			String[] words = new String[wordParts.length];
			for (int i=0; i<wordParts.length; i++) {
				String[] p = wordParts[i].split("\\|");
				words[i] = p[0];
			}
			//System.out.println(Arrays.toString(words));
			
			for (String word : words) {
				writer.write(word+" ");
			}
			writer.write("\n");
			writer.flush();
		}
		
		writer.close();
		
		
		*/
		
		
		
	}
	
	
}
