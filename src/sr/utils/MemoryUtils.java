package sr.utils;

public class MemoryUtils {

	public static String usedMemoryToString() {
		int mb = 1024*1024;
        Runtime runtime = Runtime.getRuntime();
        return "Used Memory:" + (runtime.totalMemory() - runtime.freeMemory()) / mb+" MB";
	}
	
}
