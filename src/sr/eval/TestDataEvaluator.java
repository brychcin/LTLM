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


public class TestDataEvaluator {

	static Logger logger = Logger.getLogger(TestDataEvaluator.class);
	private PseudoInferencer3gram inferencer;
	private Model model;
	private int MAX_SENTENCE_LENGTH;
	
	public TestDataEvaluator(PseudoInferencer3gram inferencer, Model model, int MAX_SENTENCE_LENGTH) {
		this.inferencer = inferencer;
		this.model = model;
		this.MAX_SENTENCE_LENGTH = MAX_SENTENCE_LENGTH;
	}
	
	public void process(DataProvider provider) throws IOException {
		logger.info("===START TEST DATA EVALUATION===");
		
		Evaluator eval = new Evaluator(model, false);
		eval.newRound();
		
		int counter = 0;
		Sentence sentence = null;
		while ((sentence = provider.next()) != null) {
			if (sentence.size() > MAX_SENTENCE_LENGTH) continue;
			inferencer.infer(sentence, false, false);
			eval.processSentence(sentence);
			
			logger.info(counter+" sentences processed");
			eval.print();
			counter++;
		}
		provider.close();
		
		eval.print();
	}
	
	public static void main(String[] args) throws ClassNotFoundException, IOException {
		PropertyConfigurator.configure("log4j.properties");
		
		int MAX_SENTENCE_LENGTH = 30;
		LTLM2sides3gram LTLM3gram = IOUtils.load3Gram("models/LTLM100czengQuarter50000.bin");
		
		Preprocessing preprocessing = new BasicPreprocessingWithoutTokenization(true, true);
		DataProvider provider = new BasicDataProvider("D:/korpusy/czeng/etest.txt", preprocessing, LTLM3gram.getVocabulary(), 3, MAX_SENTENCE_LENGTH);
		
		TestDataEvaluator eval = new TestDataEvaluator(new PseudoInferencer3gram(LTLM3gram, MAX_SENTENCE_LENGTH), LTLM3gram, MAX_SENTENCE_LENGTH);
		eval.process(provider);
		
		
	}
	
}
