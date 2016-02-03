package sr.utils;


/*
public class CZENGTest {

	
	
	public static void main(String[] args) throws IOException {
		
		Vocabulary vocabulary = new Vocabulary("data/vocabulary.txt");		
		Preprocessing preprocessing = new BasicPreprocessingWithoutTokenization(true, true);
		DataProvider provider = new BasicDataProvider("D:/korpusy/czeng/train.txt", preprocessing, vocabulary);
		
		List<Sentence> sentences = new ArrayList<Sentence>();

		int counter = 0;
		Sentence sentence = null;
		while ((sentence = provider.next()) != null) {
			if (sentence.size() > 30) continue;
			sentences.add(sentence);
			
			if (counter % 10000 == 0) {
				System.out.println(counter+": "+MemoryUtils.usedMemoryToString());
			}
			
			counter++;
		}
		provider.close();
		
		
		provider = new BasicDataProvider("data/train.txt", preprocessing, vocabulary);
		
		ModifiedKneserNeyInterpolation mkn = new ModifiedKneserNeyInterpolation(vocabulary, 4);
		while ((sentence = provider.next()) != null) {
			mkn.processSentence(sentence);
		}
		provider.close();
		mkn.process();
		
		for(int i=0; i<10; i++) {
			System.out.println("pred eval: "+MemoryUtils.usedMemoryToString());
			Timer.start("eval");
			Evaluator eval = new Evaluator(mkn, false);
			
			for (Sentence s : sentences) {
				eval.processSentence(s);
			}
			eval.print();
	
			Timer.stop("eval");
			System.out.println("po eval: "+MemoryUtils.usedMemoryToString());
			Timer.print();
		}
	}
	
}
*/