package sr;

import org.apache.log4j.Logger;

import sr.infer.Inferencer;

public class ThreadForTreeTrainingMemorySafe extends Thread {

	static Logger logger = Logger.getLogger(ThreadForTreeTrainingMemorySafe.class);
	private Inferencer inferencer;
	private QueueOfSentences queue;
	private boolean best;
	
	private static final int WAIT_MILISECONDS_IF_NO_SENTENCE = 10;
	
	public ThreadForTreeTrainingMemorySafe(Inferencer inferencer, QueueOfSentences queue, boolean best) {
		this.best = best;
		this.queue = queue;
		this.inferencer = inferencer;		
	}
	
	@Override
	public void run() {
		while (true) {
			Sentence sentence = queue.getSentencesToProcess().poll();
			if (sentence == null && queue.isFinished()) return;
			if (sentence != null) {
				inferencer.infer(sentence, best, true);
				queue.getSentencesToSave().add(sentence);
			} else {
				try {
					Thread.sleep(WAIT_MILISECONDS_IF_NO_SENTENCE);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
}
