package sr.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LearningStats {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub

		Pattern pattern = Pattern.compile(".*ppx=(.*), ppxOOV=(.*), ppxNotOOV=(.*).*");
		Pattern patternIter = Pattern.compile(".*ITERATION (\\d*).*");
		//int roles = 1000;
		int order = 3;
		String lng = "EN";
		
		for (int roles : new int[]{10, 20, 50, 100, 200}) {
		
			BufferedWriter writer1 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("learning/"+lng+"-"+order+"gram-"+roles+"-ppx.txt"), "UTF-8"));
			BufferedWriter writer2 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("learning/"+lng+"-"+order+"gram-"+roles+"-ppxNotOOV.txt"), "UTF-8"));
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream("learning/"+lng+"-"+order+"gram-"+roles+".txt"), "UTF-8"));
			
			int iter = 0;
			String line;
			while ((line = reader.readLine()) != null) {
	
				Matcher m = patternIter.matcher(line);
				if (m.matches()) {
					iter = Integer.parseInt(m.group(1));
				}
				
				m = pattern.matcher(line);
				if (m.matches()) {
					String a = m.group(1);
					String b = m.group(2);
					String c = m.group(3);
					
					writer1.write(a+"\n");
					writer1.flush();
					
					writer2.write(c+"\n");
					writer2.flush();
				}
				
			}
			reader.close();
			writer1.close();
			writer2.close();
		}
	}

}
