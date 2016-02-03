package sr.eval;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import sr.Sentence;
import sr.data.BasicDataProvider;
import sr.data.ConllDataProvider;
import sr.data.DataProvider;
import sr.lm.LinearInterpolation;
import sr.lm.Model;
import sr.lm.ModifiedKneserNeyInterpolation;
import sr.preprocessing.BasicPreprocessingWithoutTokenization;
import sr.preprocessing.Preprocessing;
import sr.utils.IOUtils;

public class InferencerEvaluator {

	private static final transient Logger logger = Logger.getLogger(InferencerEvaluator.class);
	
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
	
	public static void optimizeLI(LinearInterpolation li, Preprocessing preprocessing, boolean bigram, int ROLES, String lng, String dir) throws UnsupportedEncodingException, FileNotFoundException, IOException {
		DataProvider provider = null;
		if (bigram) {
			provider = new ConllDataProvider(dir+"/"+lng+"-heldout-"+ROLES+".txt", li.getVocabulary());
		} else {
			provider = new ConllDataProvider(dir+"/"+lng+"-heldout-trigram-"+ROLES+".txt", li.getVocabulary());
		}
		
		li.optimizeWeights(provider);
	}
	
	public static void main(String[] args) throws ClassNotFoundException, IOException {
		
		PropertyConfigurator.configure("log4j.properties");

		final String lng = "cs";
		boolean bigram = true;
		boolean em = true;
		String dir = "infer/exact-bigram-new";
		
		Preprocessing preprocessing = new BasicPreprocessingWithoutTokenization(true, false);
		ModifiedKneserNeyInterpolation mkn = IOUtils.loadLM("models/4gram_"+lng+"_MKN.bin");
		logger.info("MKN test");
		test(mkn, new BasicDataProvider("D:/korpusy/czeng/"+lng+"-test/99etest.surf."+lng, preprocessing, mkn.getVocabulary(), 3, 30));
		
		for (int ROLES : new int[]{10, 20, 50, 100, 200, 500, 1000}) {
			Model ltlm = null;
			
			if (bigram) {
				ltlm = IOUtils.load2Gram("models/2gram_"+lng+"_LTLM_"+ROLES+"roles.bin");
			} else {
				ltlm = IOUtils.load3Gram("models/3gram_"+lng+"_LTLM_"+ROLES+"roles.bin");
			}
			
			LinearInterpolation li = new LinearInterpolation(new Model[]{mkn, ltlm});
			if (em) optimizeLI(li, preprocessing, bigram, ROLES, lng, dir);
			
			DataProvider provider = null;
			if (bigram) {
				provider = new ConllDataProvider(dir+"/"+lng+"-test-"+ROLES+".txt", ltlm.getVocabulary());
			} else {
				provider = new ConllDataProvider(dir+"/"+lng+"-test-trigram-"+ROLES+".txt", ltlm.getVocabulary());
			}
			
			logger.info("LTLM "+ROLES+" test");
			test(ltlm, provider);
			provider.reset();
			logger.info("MKN + LTLM "+ROLES+" test");
			test(li, provider);
			provider.close();
		}
		
	}
	
	
}


