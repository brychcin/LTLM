package sr.eval;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import sr.Sentence;
import sr.data.BasicDataProvider;
import sr.data.DataProvider;
import sr.infer.PseudoInferencer3gram;
import sr.lm.LTLM2sides3gram;
import sr.lm.Model;
import sr.preprocessing.BasicPreprocessingWithoutTokenization;
import sr.preprocessing.Preprocessing;
import sr.utils.IOUtils;

public class ProbabilityEvaluator {

	static Logger logger = Logger.getLogger(ProbabilityEvaluator.class);
	private PseudoInferencer3gram inferencer;
	private Model model;
	private int MAX_SENTENCE_LENGTH;
	
	public ProbabilityEvaluator(PseudoInferencer3gram inferencer, Model model, int MAX_SENTENCE_LENGTH) {
		this.inferencer = inferencer;
		this.model = model;
		this.MAX_SENTENCE_LENGTH = MAX_SENTENCE_LENGTH;
	}
	
	public void process(DataProvider provider) throws IOException {
		logger.info("===START EVALUATION===");
		
		int counter = 0;
		Sentence sentence = null;
		while ((sentence = provider.next()) != null) {
			if (sentence.size() > MAX_SENTENCE_LENGTH) continue;
			if (inferencer != null) inferencer.infer(sentence, false, false);

			for (int pos = 0; pos < sentence.size(); pos++) {
				double sum = 0;
				for (int key=1; key<model.getVocabulary().size(); key++) {
					sum += model.getProbabilityAtPosition(sentence, pos, key, false);
				}
				
				if (sum < 0.99d || sum > 1.01d) {
					System.out.println("scita to do "+sum);
					System.exit(1);
				}
			}

			logger.info(counter+" sentences processed");
			counter++;
		}
		provider.close();

	}
	
	public static void main(String[] args) throws ClassNotFoundException, IOException {
		PropertyConfigurator.configure("log4j.properties");
		
		int MAX_SENTENCE_LENGTH = 30;
		LTLM2sides3gram LTLM3gram = IOUtils.load3Gram("models/LTLM3gram.bin");
		
		Preprocessing preprocessing = new BasicPreprocessingWithoutTokenization(true, true);
		DataProvider provider = new BasicDataProvider("data/test.txt", preprocessing, LTLM3gram.getVocabulary(), 3, MAX_SENTENCE_LENGTH);
		
		ProbabilityEvaluator eval = new ProbabilityEvaluator(new PseudoInferencer3gram(LTLM3gram, MAX_SENTENCE_LENGTH), LTLM3gram, MAX_SENTENCE_LENGTH);
		eval.process(provider);
		
		
	}
	
	
}
