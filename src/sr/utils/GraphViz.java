package sr.utils;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import sr.Role;
import sr.Sentence;
import sr.Token;
import sr.Vocabulary;

public class GraphViz {

	private Sentence sentence;
	private Vocabulary vocabulary;
	
	public GraphViz(Vocabulary vocabulary, Sentence sentence) {
		this.sentence = sentence;
		this.vocabulary = vocabulary;
	}
	
	public void write(String fileName) throws IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName),"UTF-8"));

		writer.write("digraph sentence {\n");
		//writer.write("\trankdir=LR;\n");
		//writer.write("\tnode [shape = rectangle];\n");

		for (Token token : sentence.getTokens()) {
			Role role = token.getRole();
			writer.write("\""+role.toGraphVizString(vocabulary)+"\"\n");
		}
		
		//root
		writer.write("\""+sentence.getRoot().getRole().toGraphVizString(vocabulary)+"\"[color=red, weight=3]\n");
		for (Role child : sentence.getRoot().getRole().getChildrens()) {
			writer.write("\""+sentence.getRoot().getRole().toGraphVizString(vocabulary)+"\" -> \""+child.toGraphVizString(vocabulary)+"\"[color=black, weight=1]\n");
		}
		
		//slova ve vete
		for (Token token : sentence.getTokens()) {
			Role role = token.getRole();
			
			for (Role child : role.getChildrens()) {
				if (child.isOnLeft()) {
					writer.write("\""+role.toGraphVizString(vocabulary)+"\" -> \""+child.toGraphVizString(vocabulary)+"\"[color=red, weight=1]\n");
				} else {
					writer.write("\""+role.toGraphVizString(vocabulary)+"\" -> \""+child.toGraphVizString(vocabulary)+"\"[color=blue, weight=1]\n");
				}
			}
		}

		writer.write("}");
		
		writer.flush();
		writer.close();
	}

	public static void save(Sentence sentence, Vocabulary vocabulary, String dot, String png) throws IOException {
		GraphViz graph = new GraphViz(vocabulary, sentence);
		graph.write(dot);
		Runtime.getRuntime().exec("dot -Tpng "+dot+" -o "+png);
	}
	
}
