package sr.eval;

import java.io.IOException;

import org.apache.log4j.PropertyConfigurator;

import sr.Sentence;
import sr.data.BasicDataProvider;
import sr.data.DataProvider;
import sr.infer.ExactInferencer2gram;

import sr.infer.ExactInferencer3gram;
import sr.infer.Inferencer;
import sr.infer.PseudoInferencer2gram;
import sr.infer.PseudoInferencer3gram;
import sr.lm.LTLM2sides2gram;
import sr.lm.LTLM2sides3gram;
import sr.lm.Model;
import sr.preprocessing.BasicPreprocessingWithoutTokenization;
import sr.preprocessing.Preprocessing;
import sr.utils.GraphViz;
import sr.utils.IOUtils;

public class ExactInferencerConsistencyTest {

	public static double likelihood(Model model, Sentence sentence) {
		double log = 0;
		for (int i=0; i<sentence.size(); i++) {
			log += Math.log(model.getProbabilityAtPosition(sentence, i, true));
		}
		return log;
	}
	
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		// TODO Auto-generated method stub
		
		PropertyConfigurator.configure("log4j.properties");
		LTLM2sides3gram ltlm = IOUtils.load3Gram("models/3gram_en_LTLM_10roles.bin");
		Inferencer exactInfer = new ExactInferencer3gram(ltlm, 30);
		Inferencer pseudoInfer = new PseudoInferencer3gram(ltlm, 30);

		Preprocessing preprocessing = new BasicPreprocessingWithoutTokenization(true, false);
		DataProvider provider = new BasicDataProvider("data/test.txt", preprocessing, ltlm.getVocabulary(), 3, 30);
		
		//u bigramu ma to vyjit -45.24681049339285
		//u trigramu ma to vyjit -46.269415511823375
		//Sentence sentence = new Sentence(ltlm.getVocabulary().getWordKey(new String[]{"the", "electronic", "lock", "on", "the", "door", "clicked", "."}));
		
		Sentence sentence = null;
		while ((sentence = provider.next()) != null) {
			System.out.println("----------------------------------");
			System.out.println(sentence.getText(ltlm.getVocabulary()));
			
			exactInfer.infer(sentence, false, false);
			double log1 = likelihood(ltlm, sentence);
			System.out.println("exact: "+log1);
			sentence.treeControll();
			//GraphViz.save(sentence, ltlm.getVocabulary(), "consistency/dot1.dot", "consistency/png1.png");
			
			
			pseudoInfer.infer(sentence, false, false);
			double log2 = likelihood(ltlm, sentence);
			System.out.println("pseudo: "+log2);
			sentence.treeControll();
			//GraphViz.save(sentence, ltlm.getVocabulary(), "consistency/dot2.dot", "consistency/png2.png");
			
			if (log2 > log1) {
				System.err.println("BLBE");
				System.exit(1);
			}
			
		}
		
	}

}
