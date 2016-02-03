package sr;

import java.util.List;

import org.apache.log4j.Logger;

import sr.infer.Inferencer;


public class ThreadForTreeTraining extends Thread {

	static Logger logger = Logger.getLogger(ThreadForTreeTraining.class);
	private Inferencer inferencer;
	private List<Sentence> sentences;
	private boolean best;
	
	public ThreadForTreeTraining(Inferencer inferencer, List<Sentence> sentences, boolean best) {
		this.best = best;
		this.sentences = sentences;
		this.inferencer = inferencer;		
	}
	
	@Override
	public void run() {
		for (Sentence s : sentences) {
			inferencer.infer(s, best, true);
		}
		//logger.info("inference finished ("+sentences.size()+" sentences)");
	}
	
}
