package sr.eval;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import sr.Sentence;
import sr.data.ConllDataProvider;
import sr.data.DataProvider;
import sr.lm.Model;
import sr.utils.IOUtils;

public class ModelEvaluator {

	private static final transient Logger logger = Logger.getLogger(ModelEvaluator.class);
	
	public static void test(Model model, DataProvider provider) throws IOException {
		Evaluator evaluator = new Evaluator(model, false);
		
		int counter = 0;
		Sentence sentence = null;
		while ((sentence = provider.next()) != null) {
			evaluator.processSentence(sentence);
			//if (counter % 1000 == 0) evaluator.print();
			counter++;
		}
		evaluator.print();
	}
	
	public static void main(String[] args) throws ClassNotFoundException, IOException {
		PropertyConfigurator.configure("log4j.properties");
		
		final int ROLES = 500;
		final String lng = "cs";
		boolean bigram = true;
		String dir = "infer/exact-bigram-new";
		
		Model ltlm = null;
		if (bigram) {
			ltlm = IOUtils.load2Gram("models/2gram_"+lng+"_LTLM_"+ROLES+"roles.bin");
		} else {
			ltlm = IOUtils.load3Gram("models/3gram_"+lng+"_LTLM_"+ROLES+"roles.bin");
		}
		
		DataProvider provider = null;
		if (bigram) {
			provider = new ConllDataProvider(dir+"/"+lng+"-test-"+ROLES+".txt", ltlm.getVocabulary());
		} else {
			provider = new ConllDataProvider(dir+"/"+lng+"-test-trigram-"+ROLES+".txt", ltlm.getVocabulary());
		}
		
		logger.info("LTLM "+ROLES+" test");
		test(ltlm, provider);
		
	}
	
}
