package sr.utils;

import sr.Sentence;


public class Cloner {

	public static int[] copyArray(int[] toCopy) {
		int[] copy = new int[toCopy.length];
		System.arraycopy(toCopy, 0, copy, 0, toCopy.length);
		return copy;
	}
	
	public static int[][] copyArray(int[][] toCopy) {
		/*
		int[][] copy = new int[toCopy.length][];
		for (int i=0; i<toCopy.length; i++) {
			copy[i] = new int[toCopy[i].length];
			for (int j=0; j<toCopy[i].length; j++) {
				copy[i][j] = toCopy[i][j];
			}
		}
		return copy;
		*/
		
		int[][] copy = new int[toCopy.length][toCopy[0].length];
		for (int i=0; i<toCopy.length; i++) {
			System.arraycopy(toCopy[i], 0, copy[i], 0, toCopy[i].length);
		}
		return copy;
		
	}
	
	public static int[][][] copyArray(int[][][] toCopy) {
		/*
		int[][][] copy = new int[toCopy.length][][];
		
		for (int i=0; i<toCopy.length; i++) {
			copy[i] = new int[toCopy[i].length][];
			
			for (int j=0; j<toCopy[i].length; j++) {
				copy[i][j] = new int[toCopy[i][j].length];
				
				for (int k=0; k<toCopy[i][j].length; k++) {
					copy[i][j][k] = toCopy[i][j][k];
				}
			}
		}
		return copy;
		*/
		
		int[][][] copy = new int[toCopy.length][toCopy[0].length][toCopy[0][0].length];
		
		for (int i=0; i<toCopy.length; i++) {
			for (int j=0; j<toCopy[i].length; j++) {
				System.arraycopy(toCopy[i][j] , 0, copy[i][j] , 0, toCopy[i][j] .length);
			}
		}
		return copy;
	}
	
	public static Sentence clone(Sentence sentence) {
		
		
		
		return null;
	}
	
	public static void main(String[] args) {
		
		int[][][] a = new int[][][]{{{1,2,3}, {4,5,6}}};
		
		int[][][] b = copyArray(a);
	
		
	}
	
	
	
}
