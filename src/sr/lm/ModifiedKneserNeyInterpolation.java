package sr.lm;

import gnu.trove.TObjectFloatHashMap;
import gnu.trove.TObjectIntHashMap;
import gnu.trove.TObjectIntIterator;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import sr.Sentence;
import sr.Token;
import sr.Vocabulary;
import sr.data.BasicDataProvider;
import sr.data.DataProvider;
import sr.eval.Evaluator;
import sr.preprocessing.BasicPreprocessingWithoutTokenization;
import sr.preprocessing.Preprocessing;
import sr.utils.IOUtils;
import sr.utils.NGramUtils;

public class ModifiedKneserNeyInterpolation extends Model {

	private static final long serialVersionUID = 5966522752913645872L;
	private static final transient Logger logger = Logger.getLogger(ModifiedKneserNeyInterpolation.class);
	
	private final Vocabulary vocabulary;
	private final int order;
	private final float[] unigramProbabilities;
	private final TObjectIntHashMap[] counts;
	private final TObjectFloatHashMap[] alphas;
	
	public static final float discout1 = 0.7f;
	public static final float discout2 = 1.0f;
	public static final float discout3 = 1.3f;
//	public static final float[][] discout = new float[][]{
//		{0.34915346f, 0.82251894f, 1.1768692f},
//		{0.7074834f, 1.0834556f, 1.4235232f},
//		{0.8214226f, 1.1596595f, 1.4106247f},
//		{0.8214165f, 1.0727904f, 1.3267367f}
//	};
	
	public ModifiedKneserNeyInterpolation(Vocabulary vocabulary, int order) {
		this.vocabulary = vocabulary;
		this.order = order;
		this.unigramProbabilities = new float[vocabulary.size()];
		this.counts = new TObjectIntHashMap[order];
		for (int i=0; i<order; i++) counts[i] = new TObjectIntHashMap();
		
		this.alphas = new TObjectFloatHashMap[order-1];
		for (int i=0; i<order-1; i++) alphas[i] = new TObjectFloatHashMap();
	}
	
	private double getProbability(int[] ngram) {
		double p;
		if (ngram.length > 1) {
			p = getSingleLevelProbability(ngram);
		} else {
			return unigramProbabilities[ngram[0]];
		}
		
		float alpha = 1.0f - alphas[ngram.length-2].get(arrayToLong(NGramUtils.getHistory(ngram)));
		
		return p + (alpha * getProbability(NGramUtils.getBackoff(ngram)));
	}
	
	private float getSingleLevelProbability(int[] ngram) {
		//pokud je to OOV tak vrat 0
		if (ngram[ngram.length-1] == 1) return 0;
		
		BigInteger key = arrayToLong(ngram);
		BigInteger historyKey = arrayToLong(NGramUtils.getHistory(ngram));
		
		int count = this.counts[ngram.length-1].get(key);
		int historyCount = this.counts[ngram.length-2].get(historyKey);
		if (count == 0 || historyCount == 0) return 0;
		
		float dis = getDiscount(count, ngram.length);

		return ((float)count - dis)/(float)historyCount;
	}
	
	private float getDiscount(int freq, int order) {
		//if (freq >= 3) freq = 3;
		//if (order >= 4) order = 4;
		//return discout[order-1][freq-1];
		
		
		switch (freq) {
			case 1 : return discout1;
			case 2 : return discout2;
			default : return discout3;
		}
		
	}
	
	@Override
	public void learnCountsFromInferedData(List<Sentence> sentences) {
		for (Sentence sentence : sentences) processSentence(sentence);
	}
	
	@Override
	public void learnCountsFromInferedData(Sentence sentence) {
		processSentence(sentence);
	}
	
	public void processSentence(Sentence sentence) {
		for (int o=1; o<=order; o++) {
			List<Token[]> ngrams = sentence.extractNGramsFromSentence(o, order-1);
			addNGrams(ngrams, o);
		}
	}
	
	public void process() {
		logger.info("MKN smoothing");
		Arrays.fill(unigramProbabilities, 0.0f);

		int counter = 0;
		for (TObjectIntIterator itWic = counts[1].iterator(); itWic.hasNext(); ) {
			itWic.advance();
			BigInteger key = (BigInteger) itWic.key();
			int[] ngram = longToArray(key);
			unigramProbabilities[ngram[1]]++;
			counter++;
		}
		
		//pro OOV slova
		for (int i=0; i<unigramProbabilities.length;i++) {
			if (unigramProbabilities[i] == 0) {
				unigramProbabilities[i] = 1;
				counter++;
			}
		}
		
		for (int i=0; i<unigramProbabilities.length;i++) {
			unigramProbabilities[i] = (float) (unigramProbabilities[i] / (double)(counter));
		}
		
		for (int o=1; o<order; o++) {
			for (TObjectIntIterator itWic = counts[o].iterator(); itWic.hasNext(); ) {
				itWic.advance();
				BigInteger key = (BigInteger) itWic.key();
				int[] ngram = longToArray(key);
				//pokud je to OOV tak vrat 0
				if (ngram[ngram.length-1] == 1) continue;
				
				int[] history = NGramUtils.getHistory(ngram);
				BigInteger historyKey = arrayToLong(history);
				
				float prob = getSingleLevelProbability(ngram);
				float oldAlpha = this.alphas[o-1].get(historyKey);
				this.alphas[o-1].put(historyKey, oldAlpha + prob);
			}
		}
	}
	
	private void addNGrams(List<Token[]> ngrams, int order) {
		for (Token[] ngram : ngrams) {
			int[] keys = new int[ngram.length];
			
			//kdyz je v ngramu OOV slovo, tak se neprida
			boolean ok = true;
			for (int i=0; i<ngram.length; i++) {
				keys[i] = ngram[i].getKey();
				if (keys[i] == 1/* && order > 2*/) {
					ok = false;
					break;
				}
			}
			if (!ok) continue;

			//if (keys[keys.length-1] == 1) continue;
			
			TObjectIntHashMap map = counts[order-1];
			BigInteger key = arrayToLong(keys);
			int oldCount = map.get(key);
			map.put(key, oldCount+1);
		}
	}
	
	public BigInteger arrayToLong(int[] array) {
		BigInteger key = new BigInteger("0");
		for (int i=0; i<array.length; i++) {
			BigInteger pow = new BigInteger(vocabulary.size()+"");
			pow = pow.pow(i);
			pow = pow.multiply(new BigInteger(""+array[i]));
			key = key.add(pow);
			
			//key += array[i] * Math.pow(vocabulary.size(), i);
		}
		
		return key;
	}
	
	public int[] longToArray(BigInteger key) {
		LinkedList<BigInteger> list = new LinkedList<BigInteger>();
		while (true) {
			BigInteger rest = key.mod(new BigInteger(""+vocabulary.size()));
			
			//int rest = (int) (key % vocabulary.size());
			list.addLast(rest);
			key = key.add(rest.negate()).divide(new BigInteger(""+vocabulary.size()));
			
			//key = (key - rest) / vocabulary.size();

			if (key.equals(BigInteger.ZERO)) break;
			//if (key == 0) break;
		}
		

		int[] array = new int[list.size()];
		for (int i=0; i<array.length; i++) array[i] = list.get(i).intValue();
		return array;
	}

	@Override
	public double getProbabilityAtPosition(Sentence sentence, int position, boolean joint) {
		int[] ngram = new int[order];
		
		int a = 0;
		for (int i=position-order+1; i<=position; i++) {
			if (i < 0) {
				ngram[a] = 2;
			} else {
				ngram[a] = sentence.getTokens()[i].getKey();
			}
			a++;
		}

		return getProbability(ngram);
	}
	
	@Override
	public double getProbabilityAtPosition(Sentence sentence, int position, int key, boolean joint) {
		int[] ngram = new int[order];
		
		int a = 0;
		for (int i=position-order+1; i<=position; i++) {
			if (i < 0) {
				ngram[a] = 2;
			} else {
				ngram[a] = sentence.getTokens()[i].getKey();
			}
			a++;
		}
		ngram[ngram.length-1] = key;

		return getProbability(ngram);
	}
	
	/*
	@Override
	public double getLogLikelihood(Sentence sentence) {
		double log = 0d;

		List<Token[]> ngrams = sentence.extractNGramsFromSentence(order, order-1);
		for (Token[] tokenNgram : ngrams) {
			int[] ngram = new int[order];
			
			for (int i=0; i<order; i++) {
				ngram[i] = tokenNgram[i].getKey();
			}
			
			double prob = getProbability(ngram);
			
			log += Math.log(prob);
		}
		
		return log;
	}*/

	@Override
	public Vocabulary getVocabulary() {
		return this.vocabulary;
	}
	
	@Override
	public void optimizeHyperParameters() {}
	
	public boolean test() {
		boolean test1 = true;
		double sum = 0;
		for (float p : unigramProbabilities) sum+= p;
		if (sum < 0.99d || sum > 1.01d) {
			System.out.println(sum);
			test1 = false;
		}
		
		boolean test2 = true;
		for (int w1=1; w1<vocabulary.size(); w1++) {
			for (int w2=1; w2<vocabulary.size(); w2++) {
				for (int w3=1; w3<vocabulary.size(); w3++) {
					sum = 0;
					for (int w4=1; w4<vocabulary.size(); w4++) {
						//if (w4==2) continue;
						sum += getProbability(new int[]{w1, w2, w3, w4});
					}
		
					//System.out.println(sum);
					if (sum < 0.99d || sum > 1.01d) {
						System.out.println(sum);
						test2 = false;
						//break;
					}
				}
			}
		}

		return test1 && test2;
	}
	
	@Deprecated
	@Override
	public Model createCopy() {
		return null;
	}
	
	@Override
	public void initialize() {}
	
	public int getOrder() {
		return order;
	}
	
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		PropertyConfigurator.configure("log4j.properties");
		
		final String lng = "en";
		//int ORDER = 3;
		
		/*
		for (int ORDER : new int[]{4}) {
		
			Preprocessing preprocessing = new BasicPreprocessingWithoutTokenization(true, false);
			Vocabulary vocabulary = new Vocabulary("D:/korpusy/czeng/"+lng+"-vocabularyFull100000.txt");
			DataProvider provider = new BasicDataProvider("D:/korpusy/czeng/"+lng+"-trainFull.txt", preprocessing, vocabulary, 3, 30);

			ModifiedKneserNeyInterpolation mkn = new ModifiedKneserNeyInterpolation(vocabulary, ORDER);
			
			int counter = 0;
			List<Sentence> sentences = new ArrayList<Sentence>();
			Sentence sentence;
			while ((sentence = provider.next()) != null) {
				mkn.processSentence(sentence);
				sentences.add(sentence);
				
				ModifiedKneserNeyInterpolation mkn = new ModifiedKneserNeyInterpolation(vocabulary, ORDER);
				
				int counter = 0;
				List<Sentence> sentences = new ArrayList<Sentence>();
				Sentence sentence = null;
				while ((sentence = provider.next()) != null) {
					mkn.processSentence(sentence);
					sentences.add(sentence);
					
					if (counter % 10000 == 0) logger.info(counter+" sentences processed");
					counter++;
				}
				provider.close();
				
				mkn.process();
				IOUtils.saveLM("models/"+mkn.getOrder()+"gram_"+lng+"_MKN.bin", mkn);
			}
		*/
		
		Preprocessing preprocessing = new BasicPreprocessingWithoutTokenization(true, false);
		ModifiedKneserNeyInterpolation mkn = IOUtils.loadLM("models/4gram_"+lng+"_MKN.bin");
		DataProvider provider = new BasicDataProvider("D:/korpusy/czeng/"+lng+"-test/99etest.surf."+lng, preprocessing, mkn.getVocabulary(), 3, 30);
		Evaluator eval = new Evaluator(mkn, false);
		
		int counter = 0;
		Sentence sentence = null;
		while ((sentence = provider.next()) != null) {
			eval.processSentence(sentence);
			if (counter % 1000 == 0) eval.print();
			counter++;
		}
		provider.close();
		eval.print();
				
	}

}
