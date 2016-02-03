package sr;

import java.io.IOException;

public class Script {

	public static void main(String[] args) throws ClassNotFoundException, IOException {
		// TODO Auto-generated method stub

		final String lng = "en";
		int order = 3;
		
		for (int ROLES : new int[]{10}) {
		
			args = new String[]{
					"-input", "D:/korpusy/czeng/"+lng+"-test/99etest.surf."+lng,
					"-output", "infer/exact-trigram-new/"+lng+"-test-trigram-"+ROLES+".txt",
					"-model", "models/"+order+"gram_"+lng+"_LTLM_"+ROLES+"roles.bin",
					"-order", order+"",
					"-exact",
					};
			
			Main.main(args);
		
		}
	}

}
