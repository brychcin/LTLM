package sr.utils;

import java.io.IOException;

import org.apache.log4j.PropertyConfigurator;

import sr.Sentence;
import sr.infer.ExactInferencer3gram;
import sr.infer.Inferencer;
import sr.lm.LTLM2sides3gram;


public class Example {

	public static void main(String[] args) throws ClassNotFoundException, IOException {
		// TODO Auto-generated method stub

		PropertyConfigurator.configure("log4j.properties");
		LTLM2sides3gram ltlm = IOUtils.load3Gram("models/3gram_en_LTLM_50roles.bin");
		Inferencer infer = new ExactInferencer3gram(ltlm, 30);
		
		String[] words = "Everything has beauty , but not everyone sees it .".toLowerCase().split(" ");
		int[] keys = ltlm.getVocabulary().getWordKey(words);
		Sentence sentence = new Sentence(keys);
		infer.infer(sentence, false, false);
		
		GraphViz.save(sentence, ltlm.getVocabulary(), "example.dot", "example.png");
		
		//ltlm.print();
		
		
		
	}

}
