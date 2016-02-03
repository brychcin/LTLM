package sr.utils;

import java.util.ArrayList;
import java.util.List;



public class NGramUtils {

	public static int[] getHistory(int[] array) {
		int[] newArray = new int[array.length-1];
		for (int i=0; i<newArray.length; i++) {
			newArray[i] = array[i];	
		}
		return newArray;
	}
		
	public static int[] getBackoff(int[] array) {
		int[] newArray = new int[array.length-1];
		for (int i=0; i<newArray.length; i++) {
			newArray[i] = array[i+1];	
		}
		return newArray;
	}	
	
	public static int[] skipItem(int[] array, int position) {
		int[] newArray = new int[array.length-1];
		int j = 0;
		for (int i=0; i<array.length; i++) {
			if (i!=position) {
				newArray[j] = array[i];	
				j++;
			} 
		}
		return newArray;
	}
	
	public static int[] addItem(int[] array, int position, int key) {
		if (position > array.length) throw new IllegalArgumentException("Array is too short ("+array.length+") to add item on position "+position);
		
		List<Integer> newList = new ArrayList<Integer>();	
		for (int i=0; i<array.length; i++) newList.add(array[i]);
		newList.add(position, key);
		
		int[] newArray = new int[array.length+1];
		for (int i=0; i<newArray.length; i++) newArray[i] = newList.get(i);
		return newArray;
	}
	
	public static int[] lastNItems(int[] array, int n) {
		if (n > array.length) throw new IllegalArgumentException("Array is too short ("+array.length+") to get last "+n+" items!");
		//System.out.println("original: "+Arrays.toString(array));
		int[] newArray = new int[n];
		int j=0;
		for (int i=array.length-n; i<array.length; i++) {
			newArray[j] = array[i];
			j++;
		}
		//System.out.println("new: "+Arrays.toString(newArray));
		return newArray;
	}
	
	
}
