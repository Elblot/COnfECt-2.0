package main;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.lang.ArrayUtils;

import traces.Method;
import traces.Statement;
import traces.Trace;



public class Group {

	double similarity = 0.4; // clustering initial traces
	String s;
	int sizeTracei;
	int[] clusters;


	public Group(String s) {
		this.s = s;
	}

	public ArrayList<ArrayList<Trace>> Synchronization() throws Exception {
		int index = 0;
		ArrayList<String[]> components = new ArrayList<String[]>();
		ArrayList<ArrayList<String>> newtraces = addFileClust();
		clusters = new int[newtraces.size()];
		for (ArrayList<String> trace : newtraces) {
			String[] compnb = GetNumber(trace);
			if (!NewContains(components, compnb)) {
				components.add(compnb);
			}
			clusters[index] = NewIndexOf(components, compnb);
			index++;
		}
		if (MainC.algo.equals("strong")) {
			newtraces = complete(newtraces, clusters);
		}
		ArrayList<ArrayList<Trace>> alTraces = finalClustering(clusters, newtraces );
		return alTraces;
	}

	/* add call retrun between each event in the strong strategy */
	public ArrayList<ArrayList<String>> complete(ArrayList<ArrayList<String>> newtraces, int[] clusters){
		ArrayList<ArrayList<String>> result = new ArrayList<ArrayList<String>>();
		ArrayList<String> call = new ArrayList<String>();
		int c = 0;
		int n = getNbTraces();
		/* get which call the component can do */
		while (ArrayUtils.contains(clusters, c)) {
			String callcomp = "";
			for (int i=0; i<clusters.length; ++i) {
				if (clusters[i] == c) {
					ArrayList<String> trace = newtraces.get(i);
					for (int j=1; j < trace.size(); ++j) {
						String line = trace.get(j);
						if (line.contains("call")) {
							String str = line.substring(line.indexOf("T")+1);
							int called = Integer.parseInt(str);
							called = clusters[called + n -1]+1;
							if (!callcomp.contains("_C"+called)) {
								callcomp = callcomp + "_C" + called;
							}
						}
					}
				}
			}
			call.add(callcomp);
			c++;
		}
		/* write them between each event of the traces */
		for (int i=0; i<clusters.length; ++i) {
			if (!call.get(clusters[i]).equals("")) {
				ArrayList<String> oldTrace = newtraces.get(i);
				ArrayList<String> strongTrace = new ArrayList<String>();
				for (int j=0; j<oldTrace.size(); ++j) {
					if (!oldTrace.get(j).contains("call") || j==0 ) {
						strongTrace.add(oldTrace.get(j));
						if (!oldTrace.get(j).contains("return")) {
							strongTrace.add("call" + call.get(clusters[i]));
							strongTrace.add("return" + call.get(clusters[i]));
						}
					}
					else {
						j++; //skip the return next
					}

				}
				result.add(strongTrace);
			}
			else {
				ArrayList<String> oldTrace = newtraces.get(i);
				result.add(oldTrace);
			}
		}
		return result;
	}

	/* get the number of trace in T1, the original traces */
	public int getNbTraces() {
		int n=1;
		File f = new File(s + "/trace" + n);
		while (f.exists()){
			++n;
			f = new File(s + "/trace" + n);
		}
		return n-1;
	}

	/* checkk if the compnb is in components */
	public static boolean NewContains(ArrayList<String[]> components, String[] compnb) {
		for (String[] component : components) {
			if (Arrays.equals(component, compnb)) {
				return true;
			}
		}
		return false;
	}

	/* get the index of compnb in components */
	public static int NewIndexOf(ArrayList<String[]> components, String[] compnb) {
		int index = 0;
		for (String[] component : components) {
			if (Arrays.equals(component, compnb)) {
				return index;
			}
			index++;
		}
		return -9000;
	}

	/* get the id of a trace, in Host and Dest in the traces */
	public static String[] GetNumber(ArrayList<String> trace) {
		String Host = "????";
		String Dest = "????";
		String event = "";
		int index = 0;
		while (event == "") {
			if (trace.get(index).contains("call_") || trace.get(index).contains("return_")) {
				index++;
			} else {
				event = trace.get(index);
			}
		}
		int h = event.indexOf("Host=");
		if (h != -1) {
			Host = event.substring(h + 5, event.indexOf(";", h + 5));
		}
		int d = event.indexOf("Dest=");
		if (d != -1) {
			Dest = event.substring(d + 5, event.indexOf(";", d + 5));
		}
		String[] ID = { Host, Dest };
		Arrays.sort(ID);
		return ID;
	}
	

	/* get the id, in the variable role in the traces */
	/*public static String[] GetNumber(ArrayList<String> trace) {
		String role = "????";
		String event = "";
		int index = 0;
		while (event == "") {
			if (trace.get(index).contains("call_") || trace.get(index).contains("return_")) {
				index++;
			} else {
				event = trace.get(index);
			}
		}
		int h = event.indexOf("role=");
		if (h != -1) {
			role = event.substring(h + 5, event.indexOf(")", h + 5));
		}
		String[] ID = { role };
		Arrays.sort(ID);
		return ID;
	}*/
	
	//read file to stock lines in an ArrayList.
	public ArrayList<ArrayList<String>> addFileClust() throws Exception {
		int n = 1;
		ArrayList<ArrayList<String>> newtraces = new ArrayList<ArrayList<String>>();
		File f = new File(s + "/trace" + n);
		while (f.exists()){
			BufferedReader br = new BufferedReader(new FileReader(f));
			ArrayList<String> alString = new ArrayList<String>();
			String line = br.readLine();
			while(line != null) {
				alString.add(line);
				line = br.readLine();
			}
			br.close();
			newtraces.add(alString);
			++n;
			f = new File(s + "/trace" + n);
		}
		sizeTracei = newtraces.size();
		switch(MainC.algo) {
		case "weak":
		case "strong":
			int k = 1;
			File Tn = new File(s + "/T" + k);
			while (Tn.exists()){
				BufferedReader br = new BufferedReader(new FileReader(Tn));
				ArrayList<String> alString = new ArrayList<String>();
				String line = br.readLine();
				while(line != null) {
					alString.add(line);
					line = br.readLine();
				}
				br.close();
				newtraces.add(alString);
				++k;
				Tn = new File(s + "/T" + k);
			}
		}
		return newtraces;
	}



	//sort files in a ArrayList (depends of the algorithm)
	public ArrayList<ArrayList<Trace>> finalClustering(int[] clusters, ArrayList<ArrayList<String>> newtraces){
		int nbClust = Utility.maxArray(clusters);
		ArrayList<ArrayList<Trace>> alTrace = new ArrayList<ArrayList<Trace>>();
		for(int i = 0; i <= nbClust; i++) {
			ArrayList<Trace> a = new ArrayList<Trace>();
			for(int j = 0; j < clusters.length; j++) {
				if (clusters[j] == i) {
					if (j >= sizeTracei) {
						Trace t = stringToTrace(newtraces.get(j));
						a.add(t);
					}
					else {
						Trace t = stringToTrace(newtraces.get(j));
						a.add(t);
					}
				}
			}
			alTrace.add(a);
		}
		switch(MainC.algo) {
		case "strict" :
			int i = 1 ;
			try {
				ArrayList<String> traceString = getStringFromTn(i);
				Trace t = stringToTrace(traceString);
				while(!traceString.isEmpty()) {
					ArrayList<Trace> a = new ArrayList<Trace>();
					a.add(t);
					alTrace.add(a);
					i++;
					traceString = getStringFromTn(i);
					t = stringToTrace(traceString);

				}
			}catch (IOException e){
				System.out.println("can't read all generated traces with the strict strategy");
				System.exit(3);
			}
		}
		return alTrace;
	}

	/* get the traces that are not in newtraces with the strict strategy */
	private ArrayList<String> getStringFromTn(int n) throws IOException {
		ArrayList<String> trace = new ArrayList<String>(); 
		File Tn = new File(s + "/T" + n);
		if (Tn.exists()){
			BufferedReader br = new BufferedReader(new FileReader(Tn));
			String line = br.readLine();
			while(line != null) {
				trace.add(line);
				line = br.readLine();
			}
			br.close();
		}
		return trace;
	}

	//read String to transform them in Trace.
	private Trace stringToTrace(ArrayList<String> file) {
		Trace trace = new Trace();
		int c = 0, i, j = 0;
		while (c<file.size()) {
			String line = file.get(c);
			if (line.contains("call_T")) {
				String str = line.substring(line.indexOf("T")+1);
				i = Integer.parseInt(str);
				switch(MainC.algo) {
				case "weak":
				case "strong":
					j = clusters[sizeTracei+i-1]+1;
					break;				
				case "strict":
					j = Utility.maxArray(clusters) +1 +i;
					break;
				}
				line = "call_C"+j;
			}
			if (line.contains("return_T")) {
				String str = line.substring(line.indexOf("T")+1);
				i = Integer.parseInt(str);
				switch(MainC.algo) {
				case "weak":
				case "strong":
					j = clusters[sizeTracei+i-1]+1;
					break;
				case "strict":
					j = Utility.maxArray(clusters) +1 +i;
					break;
				}
				line = "call_C"+j;
				line = "return_C"+j;	
			}
			Method m = new Method(line);
			Statement st = new Statement(m);
			trace.add(st);
			c++;			
		}
		return trace;
	}
}

