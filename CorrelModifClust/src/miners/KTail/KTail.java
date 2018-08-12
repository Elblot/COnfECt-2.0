/*
 *  KTAIL.java
 * 
 *  Copyright (C) 2012-2013 Sylvain Lamprier, Tewfik Ziaidi, Lom Messan Hillah and Nicolas Baskiotis
 * 
 *  This file is part of CARE.
 * 
 *   CARE is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   CARE is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with CARE.  If not, see <http://www.gnu.org/licenses/>.
 */


package miners.KTail;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import miners.FSAminer;
import traces.Statement;
import traces.Method;
import traces.Trace;
import fsa.FSA;
import fsa.State;
import fsa.Transition;


/**
 * KTail miner
 * 
 * @author Sylvain Lamprier
 * modified by Elliott Blot
 *
 */
public class KTail implements FSAminer{
	private static final long serialVersionUID = 1L;
	int horizon;
	String algo;
	
	public KTail(Integer k, String algo){
		horizon=k;
		this.algo = algo;
	}
	
	
	public String getName(){
		return("KTail_k="+horizon);
	}
	
	public String toString(){
		return getName();
	}
	
	@Override
	public FSA transform(ArrayList<Trace> traces) {
	
		StateNode root=new StateNode(); 
		HashSet<StateNode> all_states=new HashSet<StateNode>();
		HashSet<State> initial_states=new HashSet<State>();
		HashSet<State> final_states=new HashSet<State>();
		ArrayList<Statement> last_call = new ArrayList<Statement>();
		ArrayList<Statement> last_return = new ArrayList<Statement>();
		boolean vide=false;
		StateNode source=new StateNode();
		all_states.add(source);
		ArrayList<Statement> tInitial=new ArrayList<Statement>();
		if (traces.get(0).getSize() >= 2) {
			tInitial.add(traces.get(0).getByIndex(1));
			tInitial.add(traces.get(0).getByIndex(2));
			for(Trace t:traces){
				int size=t.getSize();
				if (size==0){
					vide=true;
				}	
				ArrayList<Statement> tVerif=new ArrayList<Statement>();
				tVerif.add(t.getByIndex(1));
				if(t.getSize() >= 2) {
					tVerif.add(t.getByIndex(2));
				}
				else {
					tVerif.add(new Statement());
				}
				if(!tInitial.equals(tVerif)) {
					tInitial.clear();
					tInitial.add(new Statement(new Method("nope")));
					break;
				}
			}
		}
		for(Trace t:traces){
			String str = "";
			boolean b = false;
			boolean b3 = false;
			int c = 0;
			int size=t.getSize();
			if (size==0){
				vide=true;
			}
			ArrayList<Statement> thorizon=new ArrayList<Statement>();
			thorizon.add(new Statement(new Method("initKtail")));
			Statement st=t.getByIndex(1);
			thorizon.add(st);
			
			/****************************loop for the initial state*****************************/
			
			for(int i=2;((i<=size) && (i<=horizon));i++){
				st=t.getByIndex(i);
				switch(algo){
					case "weak":
						if (b && c == 0) {
							thorizon.remove(0);
							thorizon.remove(0);
							thorizon.add(t.getByIndex(i+horizon -2));
							thorizon.add(t.getByIndex(i+horizon -1));
							b = false;
						}
						if (st.toString().contains("call")) {
							if (c == 1) {
								thorizon.remove(1);
								thorizon.add(t.getByIndex(i+horizon -1));
							}
							c = 2;
							i = i + 1;
							str = st.toString();
							if (i+horizon-1 < size) {
								last_call.add(t.getByIndex(i-1));
								last_return.add(t.getByIndex(i));
								thorizon.add(new Statement(new Method(str + t.getByIndex(i+1).toString())));
							}
							else {
								last_call.add(t.getByIndex(i-1));
								last_return.add(t.getByIndex(i));
								thorizon.add(new Statement(new Method(str)));
								b3 = true;
							}
							continue;
						}
						else if (c != 0) {
							thorizon.add(new Statement(new Method(str + t.getByIndex(i+horizon).toString())));
							c--;
						}
						else {
							thorizon.add(st);
						}
						break;	
				
					default :
						thorizon.add(st);
						break;	
				}
			}
			
			/****************************************************************/

			StateNode last_node = source;
			if (thorizon.get(0).toString().equals("initKtail")){
				last_node = source;
			}
			else {
				last_node = root.getSetStateNode(new ArrayList<Statement>(thorizon));
			}
			all_states.add(last_node);
			initial_states.add(last_node);
			thorizon.remove(0);
			boolean b2 = false;
			b = false;
			for(int i=1;i<=size;i++){
				switch(algo){
					case "weak":
						if (b3) {
							b3 = false;
							i = size;
						}
						if (i<=(size-horizon)){
							st=t.getByIndex(i+horizon);
							if (b && c == 0) {
								thorizon.remove(0);
								thorizon.remove(0);
								thorizon.add(t.getByIndex(i+horizon -2));
								thorizon.add(t.getByIndex(i+horizon -1));
								str="";
								b = false;
							}
							if (st.toString().contains("call")) {
								if (c == 1) {
									thorizon.remove(1);
									thorizon.add(t.getByIndex(i+horizon -1));
								}
								c = 2;
								i = i + 1;
								str = st.toString();
								last_call.add(t.getByIndex(i+horizon-1));
								last_return.add(t.getByIndex(i+horizon));
								continue;
							}
							else if (c != 0) {
								if (i<=2) {
									c--;
									i = i+2;
									thorizon.remove(1);
									thorizon.add(t.getByIndex(i+horizon -1));
									thorizon.add(t.getByIndex(i+horizon ));
									b = true;
								}
								else {
									thorizon.add(new Statement(new Method(str + t.getByIndex(i+horizon).toString())));
									c--;
								}
							}
							else {
								thorizon.add(st);
							}
						}
						else{
							if (b2) {
								thorizon.remove(0);
								thorizon.remove(0);
								thorizon.add(t.getByIndex(i+horizon -2));
								thorizon.add(new Statement());
								b2 = false;						
							}
							if (b && c == 0) {
								thorizon.remove(0);
								thorizon.remove(0);
								thorizon.add(t.getByIndex(i+horizon -2));
								thorizon.add(t.getByIndex(i+horizon -1));
								str="";
								b = false;
							}
							if (c == 1) {
								b2 = true;
								str="";
							}
							thorizon.add(new Statement(new Method(str)));
						}
						break;

					default:
						if (i<=(size-horizon)){
							st=t.getByIndex(i+horizon);
							thorizon.add(st);
						}
						else{
							thorizon.add(new Statement());
						}
						break;
				}
				Statement first=thorizon.remove(0);
				StateNode stnode =new StateNode();
				if(thorizon.equals(tInitial)) {
					stnode = source;
				}
				else {
					stnode=root.getSetStateNode(new ArrayList<Statement>(thorizon));
				}
				all_states.add(last_node);
				if (i==size){
					   final_states.add(stnode);
					   all_states.add(stnode);
				}
				last_node.addTransition(first,stnode);
				last_node=stnode;
				if (( b || i >= size) && !last_call.isEmpty()) {
					String lc = last_call.get(0).toString();
					if(!last_node.getTransitions().toString().contains(lc)) {	
						StateNode call_node = new StateNode();
						all_states.add(call_node);
						last_node.addTransition(last_call.get(0),call_node);//call
						call_node.addTransition(last_return.get(0), last_node);//return
					}
					last_call.remove(0);
					last_return.remove(0);
				}
				if (c == 1) {
					b = true;
				}
			}
		}
		
		FSA fsa=new FSA();
		fsa.addStates(new ArrayList<State>(all_states));
		fsa.setInitialState(source);
		fsa.setFinalStates(new ArrayList<State>(final_states));
		if (vide){
			fsa.addFinalState(source);
		}
		for(State s:initial_states){
			if (s != source) {
				Transition tr=new Transition(source,new Statement(),s);
				fsa.addTransition(tr);
			}
		}
		for(StateNode s:all_states){
			fsa.addTransitions(new ArrayList<Transition>(s.getTransitions()));
		}
		return(fsa);
	}
	
	static void deleteRecursive(File f)  {
		if (f.exists()){
			if (f.isDirectory()){
				File[] childs=f.listFiles();
				int i=0;
				for(i=0;i<childs.length;i++){
					deleteRecursive(childs[i]);
				}
			}
			f.delete();
		}
	}
}
