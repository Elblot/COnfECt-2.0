package main;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.String;
import java.util.ArrayList;
import java.util.Arrays;

public class CorrelationID {

	private static double seuil = MainC.coeff;
	
	//separate traces with the correlation coefficient between 2 line in a row.
	public static String analysis(String[] traces){
		String name, sigma;
		int j = 1;
		String fName = createDir();
		ArrayList<ArrayList<String>> alFiles = loadFiles(traces);
		for(ArrayList<String> alFile:alFiles) {
			String[] sequences = new String[0];
			sigma = alFile.get(0)+"\n";
			for(int i = 1; i < alFile.size(); i++) {
				if (coefficientID(alFile.get(i-1), alFile.get(i)) >= seuil){ /* coefficient here */
					sigma = sigma + alFile.get(i) + "\n";
				}
				else {
					sequences = Utility.stringAdd(sequences, sigma);
					sigma = alFile.get(i) + "\n";
				}
			}
			sequences = Utility.stringAdd(sequences, sigma);
			name = fName + "/trace" + j;
			extract(alFiles, sequences, name, fName);
			j++;
		}
		return fName;
	}
	
	
	public static ArrayList<ArrayList<String>> loadFiles(String[] traces){
		ArrayList<ArrayList<String>> alFiles = new ArrayList<ArrayList<String>>();
		String line;
		try {
			for(int i = 0; i < traces.length; i++) {
				ArrayList<String> alFile = new ArrayList<String>();
				File f = new File(traces[i]);
				BufferedReader br = new BufferedReader(new FileReader(f));
				line = br.readLine();
				while(line != null) {
					alFile.add(line);
					line = br.readLine();
				}
				alFiles.add(alFile);
				br.close();
			}
			return alFiles;
		}catch(Exception e) {}
		return null;
	}
	

    private static String createDir() {
    	String tmpName = null, fName = "COnfECt/"+MainC.dest;
    	int i = 1;
    	File x = new File(fName);
		while(x.exists()) {
			tmpName = fName+i;
			x = new File(tmpName);
			i++;
		}
		if (tmpName != null) {
			fName = tmpName;
		}
		MainC.dest = fName;
		
		fName = fName+"/trace";
		x = new File(fName);
		x.mkdirs();
		return fName;
	}


    public static void extract(ArrayList<ArrayList<String>> alFiles, String[] sequences, String file, String fName){
		try{
			int i;
			int n = 1;
			int id = 0;
			int k = sequences.length;
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(file)));
			String Tn = file.substring(file.indexOf("T") + 1);
			File f;
			String sigma1;
			String sigma2;		
			if (!file.contains("trace/trace")){
				bw.write("call_T" + Tn + "\n");
			}

			bw.write(sequences[0]);
			while (id < k - 1){
				f = new File(fName + "/T" + n);
				while (f.exists()){
					++n;
					f = new File(fName+"/T" + n);
				}
				String name = fName+"/T" + n;
				
				f = new File(name);
				boolean find = false;
				sigma1 = getLast(sequences[id]); ;
				for (i = id + 1; i < k; ++i){
					sigma2 = getFirst(sequences[i]);
					if (coefficientID(/*alFiles,*/ sigma1, sigma2) >= seuil){ /* coefficient here */
						find = true;
						break;
					}
				}
				bw.write("call_T" + n + "\n"
					 + "return_T" + n + "\n"); 
				if (find){
					extract(alFiles, Arrays.copyOfRange(sequences, id + 1, i), name, fName);
					bw.write(sequences[i]);
					id = i;
				}
				else {
					extract(alFiles, Arrays.copyOfRange(sequences, id + 1, k), name, fName);
					id = k;
				}
			}
			if (!file.contains("trace/trace")){
				bw.write("return_T" + Tn + "\n");
			}
			bw.close();
		} catch (FileNotFoundException e){
    		System.out.println("file not found " + e);
    	} catch (IOException e){
    		System.out.println("error " + e);
    	}
    }


    /* get the first event of a sequence */
    private static String getFirst(String sequence){
    	int end = sequence.indexOf("\n");
    	String res = sequence.substring(0, end);
    	return res;
    }

    
    /* get the last event of a sequence */
    private static String getLast(String sequence){
    	int i = sequence.length() - 2;
    	while ( i > 0 && sequence.charAt(i) != '\n'){
    		i -= 1;
    	}
    	String res = sequence.substring(i, sequence.length() - 1);
    	if (res.indexOf("\n") != -1){
    		res = res.substring(1, res.length());
    	}
    	return res;
    }
    
    /* get the correlation coefficient between two events based on ID,
     *  here in the variable Host and Dest in the traces               */
    private static float coefficientID(String event1, String event2) {
    	String Host1 = "????";
    	String Dest1 = "????";
    	String Host2 = "????";
    	String Dest2 = "????";
    	int h1 = event1.indexOf("Host=");
    	if (h1 != -1) {
    		Host1 = event1.substring(h1 + 5, event1.indexOf(";", h1+5));
    	}
    	int h2 = event2.indexOf("Host=");
    	if (h2 != -1) {
    		Host2 = event2.substring(h2 + 5, event2.indexOf(";", h2+5));
    	}
    	int d1 = event1.indexOf("Dest=");
    	if (d1 != -1) {
    		Dest1 = event1.substring(d1 + 5, event1.indexOf(";", d1+5));
    	}
    	int d2 = event2.indexOf("Dest=");
    	if (d2 != -1) {
    		Dest2 = event2.substring(d2 + 5, event2.indexOf(";", d2+5));
    	}
    	String[] ID1 = {Host1, Dest1};
    	String[] ID2 = {Host2, Dest2};
    	Arrays.sort(ID1);
    	Arrays.sort(ID2);	 	
    	if (Arrays.equals(ID1, ID2)) {
    		return 1;
    	}
    	return 0;
    }
    
    
    /* get the correlation coefficient between two events based on ID,
     *  here in the variable role in the traces               */
    /*private static float coefficientID(String event1, String event2) {
    	String role1 = "????";
    	String role2 = "????";
    	int h1 = event1.indexOf("role=");
    	if (h1 != -1) {
    		role1 = event1.substring(h1 + 5, event1.indexOf(")", h1+5));
    	}
    	int h2 = event2.indexOf("role=");
    	if (h2 != -1) {
    		role2 = event2.substring(h2 + 5, event2.indexOf(")", h2+5));
    	}
    	String[] ID1 = { role1 };
    	String[] ID2 = { role2 };
    	Arrays.sort(ID1);
    	Arrays.sort(ID2);	 	
    	if (Arrays.equals(ID1, ID2)) {
    		return 1;
    	}
    	return 0;
    }*/
    
}