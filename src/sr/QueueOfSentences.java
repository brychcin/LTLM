package sr;

import java.util.concurrent.ConcurrentLinkedQueue;

public class QueueOfSentences {

	private ConcurrentLinkedQueue<Sentence> sentencesToProcess;
	private ConcurrentLinkedQueue<Sentence> sentencesToSave;
	private boolean finished;
	
	public QueueOfSentences() {
		this.sentencesToProcess = new ConcurrentLinkedQueue<Sentence>();
		this.sentencesToSave = new ConcurrentLinkedQueue<Sentence>();
		this.finished = false;
	}

	public ConcurrentLinkedQueue<Sentence> getSentencesToProcess() {
		return sentencesToProcess;
	}

	public void setSentencesToProcess(ConcurrentLinkedQueue<Sentence> sentenceToProcess) {
		this.sentencesToProcess = sentenceToProcess;
	}

	public ConcurrentLinkedQueue<Sentence> getSentencesToSave() {
		return sentencesToSave;
	}

	public void setSentencesToSave(ConcurrentLinkedQueue<Sentence> sentenceToSave) {
		this.sentencesToSave = sentenceToSave;
	}

	public synchronized boolean isFinished() {
		return finished;
	}

	public synchronized void setFinished(boolean finished) {
		this.finished = finished;
	}
	
	public String toString() {
		return sentencesToProcess.size()+" sentences to process, "+sentencesToSave.size()+" sentences to save";
	}

}
