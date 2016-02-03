package sr.utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.log4j.PropertyConfigurator;

import sr.Sentence;
import sr.data.BasicDataProvider;
import sr.eval.Evaluator;
import sr.lm.Model;
import sr.lm.ModifiedKneserNeyInterpolation;
import sr.preprocessing.BasicPreprocessingWithoutTokenization;
import sr.preprocessing.Preprocessing;

public class MichalPokus {

	public static void main(String[] args) throws ClassNotFoundException, IOException {
		PropertyConfigurator.configure("log4j.properties");
		
		Preprocessing preprocessing = new BasicPreprocessingWithoutTokenization(true, false);
		
		//Model model = IOUtils.load2Gram("models/2gram_en_LTLM_100roles.bin");
		Model model = IOUtils.loadLM("models/3gram_en_MKN.bin");
		BasicDataProvider provider = new BasicDataProvider("cwi_training.txt", preprocessing, model.getVocabulary(), 0, Integer.MAX_VALUE);
				
		Evaluator evaluator = new Evaluator(model, false);
		
		Sentence sentence = null;
		while ((sentence = provider.next()) != null) {
			evaluator.processSentence(sentence);
		}
		evaluator.print();
		
		
		
		
	}

}
