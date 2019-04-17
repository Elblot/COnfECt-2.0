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
import main.MainC;


/**
 * KTail miner
 * 
 * original @author Sylvain Lamprier
 * fixed and modified by Elliott Blot
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

	/*  make loop when see call */
	@Override
	public FSA transform(ArrayList<Trace> traces) {
		StateNode root=new StateNode(); 
		HashSet<StateNode> all_states=new HashSet<StateNode>();
		HashSet<State> initial_states=new HashSet<State>();
		HashSet<State> final_states=new HashSet<State>();
		boolean vide = false;
		boolean mergeinit = true;
		StateNode source=new StateNode();
		all_states.add(source);
		ArrayList<Statement> tInitial=new ArrayList<Statement>();		
		/***** initial state can merge only if it have only one kfuture *****/
		tInitial.add(traces.get(0).getByIndex(1));
		if (traces.get(0).getSize() >= 2) {
			tInitial.add(traces.get(0).getByIndex(2));
		}
		else {
			tInitial.add(traces.get(0).getByIndex(1));
		}
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
				tVerif.add(t.getByIndex(1));
			}
			if(!tInitial.equals(tVerif)) {
				tInitial.clear();
				tInitial.add(new Statement(new Method("nope")));
				mergeinit= false;
				break;
			}
		}
		if (mergeinit){
			source = root.getSetStateNode(new ArrayList<Statement>(tInitial));
		}
		for(Trace t:traces){
			String call = "";
			int size=t.getSize();
			if (size==0){
				vide=true;
			}
			ArrayList<Statement> thorizon=new ArrayList<Statement>();
			ArrayList<Statement> callLoop = new ArrayList<Statement>();
			thorizon.add(new Statement(new Method("initKtail")));
			callLoop.add(new Statement(new Method("initKtail")));
			int i = 1;
			Statement st=t.getByIndex(i);
			thorizon.add(st);
			callLoop.add(st);
			i++;
			String callstrong = "";
			/**********************loop for the initial state*****************/
			while (thorizon.size() < horizon +1 && i <= size) {
				if (t.getByIndex(i).toString().contains("call") || thorizon.get(thorizon.size()-1).toString().contains("return")) {
					call = t.getByIndex(i).toString().toString();
					i += 2;
				}
				if (!call.equals("")){
					callstrong = call;
					if(i < size) {
						st=new Statement(new Method(call + "|||" + t.getByIndex(i).toString()));
					}
					else {
						st=new Statement(new Method(call + "|||"));
					}
				}
				else {
					st=t.getByIndex(i);
				}
				callLoop.add(st);
				if(i <= size) {
					thorizon.add(t.getByIndex(i));
				}
				else {
					thorizon.add(t.getByIndex(1));
				}
				i++;
				call = "";
			}
			if (thorizon.size() < horizon +1 && i >= size) {
				thorizon.add(t.getByIndex(1));
				callLoop.add(t.getByIndex(1));
			}
			if (MainC.algo.equals("strong")) {
				if (!thorizon.get(1).toString().contains("call") && !callstrong.equals("")) {
					StateNode call_node;
					String id = callstrong.substring(4);
					ArrayList<Statement> mergecall = new ArrayList<Statement>();
					call_node = root.getSetStateNode(new ArrayList<Statement>(mergecall));
					all_states.add(call_node);
					source.addTransition(new Statement(new Method("call"   + id)), call_node);
					call_node.addTransition(new Statement(new Method("return" + id)), source);
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
			callLoop.remove(0);
			while (i <= size) {
				if (t.getByIndex(i).toString().contains("call") && !thorizon.get(horizon - 1).toString().contains("return")) {
					call = t.getByIndex(i).toString();
					i += 2;
					if (i <= size) {
						callLoop.add(new Statement(new Method(call + "|||" + t.getByIndex(i).toString())));
					}
					else {
						callLoop.add(new Statement(new Method(call + "|||")));
					}
				}	
				else {
					callLoop.add(t.getByIndex(i));
				}
				if (i <= size) {
					st=t.getByIndex(i);
					thorizon.add(st);
				}	
				else {
					thorizon.add(t.getByIndex(1));
				}
				Statement first=thorizon.remove(0);
				callLoop.remove(0);
				StateNode stnode =new StateNode();
				if(callLoop.equals(tInitial)) {
					stnode = source;
				}	
				else {
					stnode=root.getSetStateNode(new ArrayList<Statement>(callLoop));
				}	
				all_states.add(last_node);
				last_node.addTransition(first,stnode);
				if (callLoop.get(0).toString().contains("|||")) {
					String id = getID(callLoop.get(0).toString());
					StateNode call_node;
					ArrayList<Statement> mergecall = new ArrayList<Statement>();
					mergecall.add(new Statement(new Method("return" + id)));
					mergecall.add(thorizon.get(0));
					call_node = root.getSetStateNode(new ArrayList<Statement>(mergecall));
					all_states.add(call_node);
					stnode.addTransition(new Statement(new Method("call"   + id)), call_node);
					call_node.addTransition(new Statement(new Method("return" + id)), stnode);
				}
				last_node=stnode;
				i++;
			}
			/***** cyclic: the last state merge with the initial state *****/		
			for (int j = 1; j<=horizon;j++) {
				if (size >= j) {
					thorizon.add(new Statement(new Method("|")));
					callLoop.add(new Statement(new Method("|")));
				}
				else {
					thorizon.add(new Statement(new Method("|")));
					callLoop.add(new Statement(new Method("|")));
				}
				Statement first=thorizon.remove(0);
				callLoop.remove(0);
				StateNode stnode =new StateNode();
				stnode=root.getSetStateNode(new ArrayList<Statement>(callLoop));
				all_states.add(last_node);
				last_node.addTransition(first,stnode);
				if (j==horizon) {
					final_states.add(stnode);
				}
				if (callLoop.get(0).toString().contains("|||")) {
					String id = getID(callLoop.get(0).toString());
					StateNode call_node;
					ArrayList<Statement> mergecall = new ArrayList<Statement>();
					mergecall.add(new Statement(new Method("return" + id)));
					mergecall.add(thorizon.get(0));
					call_node = root.getSetStateNode(new ArrayList<Statement>(mergecall));
					all_states.add(call_node);
					stnode.addTransition(new Statement(new Method("call"   + id)), call_node);
					call_node.addTransition(new Statement(new Method("return" + id)), stnode);
				}
				last_node=stnode;
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

	/***TODO clean this version, some line useless as created to consider Call loops ***/
	/* no loop at call */
	public FSA transformStrict(ArrayList<Trace> traces) {
		StateNode root=new StateNode(); 
		System.out.println(this.toString());
		HashSet<StateNode> all_states=new HashSet<StateNode>();
		HashSet<State> initial_states=new HashSet<State>();
		HashSet<State> final_states=new HashSet<State>();
		boolean vide = false;
		boolean mergeinit = true;
		StateNode source=new StateNode();
		all_states.add(source);
		ArrayList<Statement> tInitial=new ArrayList<Statement>();
		/***** initial state can merge only if it have only one kfuture *****/
		tInitial.add(traces.get(0).getByIndex(1));
		if (traces.get(0).getSize() >= 2) {
			tInitial.add(traces.get(0).getByIndex(2));
		}
		else {
			tInitial.add(traces.get(0).getByIndex(1));
		}
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
				tVerif.add(t.getByIndex(1));
			}
			if(!tInitial.equals(tVerif)) {
				tInitial.clear();
				tInitial.add(new Statement(new Method("nope")));
				mergeinit= false;
				break;
			}
		}
		if (mergeinit){
			source = root.getSetStateNode(new ArrayList<Statement>(tInitial));
		}
		for(Trace t:traces){
			String call = "";
			int size=t.getSize();
			if (size==0){
				vide=true;
			}
			ArrayList<Statement> thorizon=new ArrayList<Statement>();
			ArrayList<Statement> callLoop = new ArrayList<Statement>();
			thorizon.add(new Statement(new Method("initKtail")));
			callLoop.add(new Statement(new Method("initKtail")));
			int i = 1;
			Statement st=t.getByIndex(i);
			thorizon.add(st);
			callLoop.add(st);
			i++;
			/**********************loop for the initial state*****************/
			while (thorizon.size() < horizon +1 && i <= size) {
				if (!call.equals("")){
					if(i < size) {
						st=new Statement(new Method(call + "|||" + t.getByIndex(i).toString()));
					}
					else {
						st=new Statement(new Method(call + "|||"));
					}
				}
				else {
					st=t.getByIndex(i);
				}
				callLoop.add(st);
				if(i <= size) {
					thorizon.add(t.getByIndex(i));
				}
				else {
					thorizon.add(t.getByIndex(1));
				}
				i++;
				call = "";
			}
			if (thorizon.size() < horizon +1 && i >= size) {
				thorizon.add(t.getByIndex(1));
				callLoop.add(t.getByIndex(1));
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
			callLoop.remove(0);
			while (i <= size) {
				callLoop.add(t.getByIndex(i));
				if (i <= size) {
					st=t.getByIndex(i);
					thorizon.add(st);
				}	
				else {
					thorizon.add(t.getByIndex(1));
				}
				Statement first=thorizon.remove(0);
				callLoop.remove(0);
				StateNode stnode =new StateNode();
				if(callLoop.equals(tInitial)) {
					stnode = source;
				}	
				else {
					stnode=root.getSetStateNode(new ArrayList<Statement>(callLoop));
				}	
				all_states.add(last_node);
				last_node.addTransition(first,stnode);
				last_node=stnode;
				i++;
			}
			/***** cyclic: the last state merge with the initial state *****/		
			for (int j = 1; j<=horizon;j++) {
				if (size >= j) {
					thorizon.add(new Statement(new Method("|")));
					callLoop.add(new Statement(new Method("|")));
				}
				else {
					thorizon.add(new Statement(new Method("|")));
					callLoop.add(new Statement(new Method("|")));
				}
				Statement first=thorizon.remove(0);
				callLoop.remove(0);
				StateNode stnode =new StateNode();
				stnode=root.getSetStateNode(new ArrayList<Statement>(callLoop));
				all_states.add(last_node);
				last_node.addTransition(first,stnode);
				if (j==horizon) {
					final_states.add(stnode);
				}
				last_node=stnode;
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

	static String getID(String trans) {
		String call = trans.substring(trans.indexOf("_"), trans.indexOf("|"));
		return call;
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
