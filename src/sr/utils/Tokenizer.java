package sr.utils;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tokenizer {
	//cislo |  | html | tecky a sracky
	public static final String defaultRegex = "(\\d+[.,](\\d+)?)|([\\p{L}\\d]+)|(<.*?>)|([\\p{Punct}])";
	
	public static String[] tokenize(String regex, String text){
		Pattern pattern = Pattern.compile(regex);
		
		ArrayList<String> words = new ArrayList<String>();
		
		Matcher matcher = pattern.matcher(text);
		while(matcher.find()){
			int start = matcher.start();
			int end = matcher.end();
			
			words.add(text.substring(start, end));
		}
		
		String[] ws = new String[words.size()];
		ws = words.toArray(ws);
		
		return ws;
	}
    public static String removeAccents(String text) {
        return text == null ? null : Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }
    
    public static void main(String[] args) {
    	System.out.println(Arrays.toString(Tokenizer.tokenize(Tokenizer.defaultRegex, "When the interaction was present, there was no effect of scale at high food abundance, but when food abundance was low, container size affected male and female development time and mass of males .")));
    }
}
