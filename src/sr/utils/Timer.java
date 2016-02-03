package sr.utils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

public class Timer {

	static Logger logger = Logger.getLogger(Timer.class);
	
	static class TaskStats {
		String name;
		long totalMiliseconds;
		int totalCalls;
		long start;
		
		TaskStats(String name) {
			this.name = name;
			this.totalMiliseconds = 0;
			this.totalCalls = 0;
		}
		
		void call(long miliseconds) {
			totalCalls++;
			totalMiliseconds += miliseconds;
		}
		
		void start() {
			this.start = new Date().getTime();
		}
		
		void stop() {
			call(new Date().getTime() - start);
		}
	}
	
	private static Map<String, TaskStats> stats = new HashMap<String, TaskStats>();
	
	
	public static void start(String name) {
		TaskStats taskStats = stats.get(name);
		if (taskStats == null) {
			taskStats = new TaskStats(name);
			stats.put(name, taskStats);
		}
		
		taskStats.start();
	}
	
	public static void stop(String name) {
		TaskStats taskStats = stats.get(name);
		if (taskStats == null) {
			System.out.println("nezavolal si start");
			System.exit(0);
		}
		
		taskStats.stop();
	}
	
	public static void print() {
		for (TaskStats taskStats : stats.values()) {
			logger.info(taskStats.name+" calls="+taskStats.totalCalls+" averageTime="+(double)taskStats.totalMiliseconds/(double)taskStats.totalCalls+" totalTime="+taskStats.totalMiliseconds);
		}
	}
}
