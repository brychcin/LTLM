package sr.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WordCloudGenerator {

	
	public static int sample(List<Double> scores, double sum) {
		Random rnd = new Random ();

		double sampleScore = rnd.nextDouble () * sum ;
		int sample = -1;
		while ( sampleScore > 0.0) {
		sample ++;
		sampleScore -= scores.get(sample);
		}
		
		return sample ;
	}
	
	
	
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub

		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream("C:/Users/Brych/Desktop/cloud.txt"), "UTF-8"));
		String line = null;
		
		List<String> words = new ArrayList<String>();
		List<Double> counts = new ArrayList<Double>();
		double sum = 0;
		
		while ((line = reader.readLine()) != null) {
			line = line.trim();
			if (line.trim().length() == 0) continue;
			
			String[] parts = line.split("\\t");
			
			words.add(parts[1]);
			counts.add(Double.parseDouble(parts[0]));
			sum += Double.parseDouble(parts[0]);
		}
		
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("C:/Users/Brych/Desktop/text.txt"), "UTF-8"));
		for (int i=0; i<1000; i++) {
			writer.write(words.get(sample(counts, sum))+"\n");
			writer.flush();
		}
		
		writer.close();
		
	}

}
