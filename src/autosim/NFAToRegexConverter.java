// ---------------------------------------------------------------------------------------------
//  Copyright (c) Akash Nag. All rights reserved.
//  Licensed under the MIT License. See LICENSE.md in the project root for license information.
// ---------------------------------------------------------------------------------------------

package autosim;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

class RegexTransition
{
    protected final String fromState;
    protected final String toState;
    protected String onRegex;
    
    public RegexTransition(String f, String t, String r)
    {
	this.fromState = f;
	this.toState = t;
	this.onRegex = r;
    }
    
    @Override
    public int hashCode()
    {
	return (fromState+toState).hashCode();
    }
    
    @Override
    public boolean equals(Object x)
    {
	if(x==null) return false;
	if(x instanceof RegexTransition)
	{
	    RegexTransition r = (RegexTransition)x;
	    return(r.fromState.equals(fromState) && r.toState.equals(toState) && r.onRegex.equals(onRegex));
	} else return false;
    }
    
    @Override
    public String toString()
    {
	return (onRegex.length()==1 ? "d(" : "d*(") + fromState + ", " + onRegex.replace(AutoSim.LAMBDA_CHAR, AutoSim.LAMBDA_PRINT_CHAR) + ") = " + toState;
    }
}

public class NFAToRegexConverter
{
    private final String descFile;
    private FileIO fio;
    
    private boolean hasSpec[];
    private final boolean trace;
    
    private HashSet<String> internalStates;
    private HashSet<String> finalStates;
    private HashSet<Character> alphabet;
    private String initialState;
    
    private String states[];
    private char symbols[];
    private Object table[][];
    
    public NFAToRegexConverter(String descFile, boolean trace)
    {
        this.descFile = descFile;
        this.trace = trace;
        
        hasSpec = new boolean[4];       // Q, E, I(q0), F
        fio = new FileIO();
        fio.openFile(this.descFile);
        
        internalStates = new HashSet<String>();
        finalStates = new HashSet<String>();
        alphabet = new HashSet<Character>();
        
        table=null;
    }
    
    public void simulate()
    {
        if(!fio.isReadyForReading()) return;
        if(!parseNFA()) return;
        
        if(table==null)
        {
            System.out.println("ERROR: Incomplete specification of NFA");
            return;
        }
        
	// Satisfy preconditions for beginning State-Elimination Method:
	
	// add the initial state and mark it as initial
	HashSet<String> temp1 = new HashSet<String>();
	HashSet<String> temp2 = new HashSet<String>();
	
	temp1.add(initialState);
	temp2.add(AutoSim.FINAL_STATE_NAME);
	
	int initialRow = getIndexOfState(AutoSim.INITIAL_STATE_NAME);
	int lambdaCol = getIndexOfSymbol(AutoSim.LAMBDA_CHAR);
	table[initialRow][lambdaCol] = temp1;
	initialState = AutoSim.INITIAL_STATE_NAME;
	
	// add the transitions to the single final state
	Iterator<String> allFinals = finalStates.iterator();
	while(allFinals.hasNext())
	{
	    int rowIndex = getIndexOfState(allFinals.next());
	    table[rowIndex][lambdaCol] = temp2;
	}
	
	// make others non-final
	finalStates.clear();
	finalStates.add(AutoSim.FINAL_STATE_NAME);
	
	// Start State-Elimination Method:
        simulateNFAToRegexConversion();
    }
    
    private boolean parseNFA()
    {
        String s = null;
        while((s=fio.readNextLine())!=null)
        {
            if(s.length()==0) continue;
            
            String x = s.substring(0,2).toUpperCase();
            
            if(x.equals("Q="))
            {
                if(!parseInternalStates(s)) { fio.closeFile(); return false; }
            } else if(x.equals("E=")) {
                if(!parseAlphabet(s)) { fio.closeFile(); return false; }
            } else if(x.equals("I=")) {
                if(!parseInitialState(s)) { fio.closeFile(); return false; }
            } else if(x.equals("F=")) {
                if(!parseFinalStates(s)) { fio.closeFile(); return false; }
            } else if(x.equals("D(")) {
                if(!parseRule(s)) { fio.closeFile(); return false; }
            }
            
            if(isSpecReady() && table==null) makeTable();
        }
        
        fio.closeFile();        
        return true;
    }
    
    private boolean parseInternalStates(String s)
    {
        if(hasSpec[0])
        {
            System.out.println("ERROR in Line "+fio.getLineNumber()+": Duplicate definition of 'Q - set of internal states'");
            return false;
        } else {
            if(s.charAt(2) != '{' || s.charAt(s.length()-1) != '}')
            {
                System.out.println("ERROR in Line "+fio.getLineNumber()+": invalid syntax.\nCorrect syntax: Q = { q0, q1, ... }");
                return false;
            } else {
                String a[] = s.substring(3,s.length()-1).split(",");
                for(String state: a) internalStates.add(state);
            }
        }
        
	if(internalStates.contains(AutoSim.INITIAL_STATE_NAME) || internalStates.contains(AutoSim.FINAL_STATE_NAME))
	{
	    System.out.printf("ERROR in Line "+fio.getLineNumber()+": states cannot be named '%s' or '%s' to facilitate NFA-to-Regex conversion\n", AutoSim.INITIAL_STATE_NAME, AutoSim.FINAL_STATE_NAME);
            return false;
	}
	
	internalStates.add(AutoSim.INITIAL_STATE_NAME);
	internalStates.add(AutoSim.FINAL_STATE_NAME);
	
        hasSpec[0]=true;
        return true;
    }
        
    private boolean parseAlphabet(String s)
    {
        if(hasSpec[1])
        {
            System.out.println("ERROR in Line "+fio.getLineNumber()+": Duplicate definition of 'E - alphabet / set of symbols'");
            return false;
        } else {
            if(s.charAt(2) != '{' || s.charAt(s.length()-1) != '}')
            {
                System.out.println("ERROR in Line "+fio.getLineNumber()+": invalid syntax.\nCorrect syntax: E = { a, b, ... }");
                return false;
            } else {
                String a[] = s.substring(3,s.length()-1).split(",");
                for(String symbol: a) alphabet.add(new Character(symbol.charAt(0)));
            }
        }
        
	if(!alphabet.contains(AutoSim.LAMBDA_CHAR)) alphabet.add(AutoSim.LAMBDA_CHAR);
        hasSpec[1]=true;
        return true;
    }
    
    private boolean parseInitialState(String s)
    {
        if(hasSpec[2])
        {
            System.out.println("ERROR in Line "+fio.getLineNumber()+": Duplicate definition of 'I - initial state'");
            return false;
        } else {
            String stateName = s.substring(2);
            if(internalStates.contains(stateName))
                initialState = stateName;
            else {
                System.out.println("ERROR in Line "+fio.getLineNumber()+": State '" + stateName + "' not found");
                return false;
            }
        }
        
        hasSpec[2]=true;
        return true;
    }
        
    private boolean parseFinalStates(String s)
    {
        if(hasSpec[3])
        {
            System.out.println("ERROR in Line "+fio.getLineNumber()+": Duplicate definition of 'F - set of final states'");
            return false;
        } else {
            if(s.charAt(2) != '{' || s.charAt(s.length()-1) != '}')
            {
                System.out.println("ERROR in Line "+fio.getLineNumber()+": invalid syntax.\nCorrect syntax: F = { q1, q2, ... }");
                return false;
            } else {
                String a[] = s.substring(3,s.length()-1).split(",");
                for(String state: a) 
                {
                    if(!internalStates.contains(state))
                    {
                        System.out.println("ERROR in Line "+fio.getLineNumber()+": State '" + state + "' not found");
                        return false;
                    } else {
                        finalStates.add(state);
                    }
                }
            }
        }
        
        hasSpec[3]=true;
        return true;
    }
    
    private boolean parseRule(String s)
    {
        if(!isSpecReady())
        {
            System.out.println("ERROR in Line "+fio.getLineNumber()+": Q, E, I and F must be defined prior to defining the transition rules.");
            return false;
        } else {
            int b1 = s.indexOf('('), b3 = s.indexOf(')'), b4 = s.lastIndexOf('='), b2 = s.indexOf(',');
            if(b1==-1 || b2==-1 || b3==-1 || b4==-1)
            {
                printRuleSyntax();
                return false;
            }
            
            if(!(b1 < b2 && b2 < b3 && b3 < b4 && b3==b2+2))
            {
                printRuleSyntax();
                return false;
            }
            
            String tss = s.substring(b4+1);
            if(tss.charAt(0)!='{' || tss.charAt(tss.length()-1)!='}')
            {
                printRuleSyntax();
                return false;
            }
                
            String targetStates[]=tss.substring(1,tss.length()-1).split(",");            
            String currentState = s.substring(b1+1,b2);
            Character currentSymbol = s.charAt(b2+1);
            
            for(String targetState: targetStates)
            {
                if(!internalStates.contains(targetState))
                {
                    System.out.println("ERROR in Line "+fio.getLineNumber()+": State '" + targetState + "' not found");
                    return false;
                }
            }
            
            if(!internalStates.contains(currentState))
            {
                System.out.println("ERROR in Line "+fio.getLineNumber()+": State '" + currentState + "' not found");
                return false;
            }
            
            if(!alphabet.contains(currentSymbol))
            {
                System.out.println("ERROR in Line "+fio.getLineNumber()+": Symbol '" + currentSymbol + "' not found");
                return false;
            }
            
            // find row where to insert the rule in table
            int row = getIndexOfState(currentState);
            
            // create the target-set of states to move-to
            HashSet<String> set = new HashSet<String>();
            for(String targetState: targetStates) set.add(targetState);
                
            // when symbol = '*': it means any character (except lambda)
            if(currentSymbol == AutoSim.FULL_ALPHABET_CHAR)
            {
                // all symbols except lambda
                int sc = symbols.length;
                for(int k=0; k<sc; k++)
                {
                    if(symbols[k]==AutoSim.LAMBDA_CHAR) continue;
                    
                    int col = getIndexOfSymbol(symbols[k]);
                    table[row][col]=set;
                }
            } else {
                int col = getIndexOfSymbol(currentSymbol);
                table[row][col]=set;            
            }
        }
        
	return true;
    }
    
    private void makeTable()
    {
        alphabet.add(new Character(AutoSim.LAMBDA_CHAR));       
        
        int n1 = internalStates.size(), n2 = alphabet.size();
        
        states = new String[n1];
        symbols = new char[n2];
        
        int i=-1, j=-1;
        
        Iterator<String> it1 = internalStates.iterator();
        while(it1.hasNext()) states[++i]=it1.next();
        
        Iterator<Character> it2 = alphabet.iterator();
        while(it2.hasNext()) symbols[++j]=it2.next();  
        
        table = new Object[n1][n2];
    }
    
    private int getIndexOfState(String stateName)
    {
        for(int i=0; i<states.length; i++)
        {
            if(stateName.equals(states[i])) return i;
        }
        return -1;
    }
    
    private int getIndexOfSymbol(char symbol)
    {
        for(int i=0; i<symbols.length; i++)
        {
            if(symbol==symbols[i]) return i;
        }
        return -1;
    }
        
    private void printRuleSyntax()
    {
        System.out.println("ERROR in Line "+fio.getLineNumber()+": Invalid syntax.\nCorrect Syntax: d(q0, a) = { q1, q2, ... }");
    }
    
    private boolean isSpecReady()
    {
        return(hasSpec[0] && hasSpec[1] && hasSpec[2] && hasSpec[3]);
    }
    
    private void printTransitionFunction(ArrayList<RegexTransition> transitions)
    {
	System.out.println("Transition Function:");
	int n = transitions.size();
	for(int i=0; i<n; i++)
	{
	    System.out.println(transitions.get(i));
	}
	System.out.println();
    }
    
    private ArrayList<RegexTransition> getTransitionFunction()
    {
	ArrayList<RegexTransition> trans = new ArrayList<RegexTransition>();
	
	for(int i=0; i<table.length; i++)
	{
	    String fromState = states[i];
	    for(int j=0; j<table[i].length; j++)
	    {
		if(table[i][j]==null) continue;
		String onRegex = String.valueOf(symbols[j]);
		
		HashSet<String> targets =  (HashSet<String>)table[i][j];
		Iterator<String> it = targets.iterator();
		while(it.hasNext())
		{
		    String toState = it.next();
		    addTransition(trans, fromState, toState, onRegex);
		}
	    }
	}
	
	return trans;
    }
    
    private ArrayList<RegexTransition> filterTransitions(ArrayList<RegexTransition> trans, String stateName, boolean incoming)
    {
	int n = trans.size();
	ArrayList<RegexTransition> x = new ArrayList<RegexTransition>();
	
	for(int i=0; i<n; i++)
	{
	    RegexTransition t = trans.get(i);
	    if( (incoming && t.toState.equals(stateName)) || (!incoming && t.fromState.equals(stateName)))
	    {
		// don't include self-loops
		if(!t.fromState.equals(t.toState)) x.add(t);
	    }
	}
	
	if(x.size()==0) return null;
	return x;
    }
    
    private RegexTransition getSelfLoopTransition(ArrayList<RegexTransition> trans, String stateName)
    {
	int n = trans.size();
	ArrayList<RegexTransition> x = new ArrayList<RegexTransition>();
	
	for(int i=0; i<n; i++)
	{
	    RegexTransition t = trans.get(i);
	    if(t.fromState.equals(stateName) && t.toState.equals(stateName)) return t;
	}
	
	return null;
    }
    
    private void simulateNFAToRegexConversion()
    {
	ArrayList<RegexTransition> trans = getTransitionFunction();
	
	if(trace)
	{
	    System.out.print("Beginning state-elimination method with ");
	    printTransitionFunction(trans);
	}
	
	// create a hashset from the set of active states
	HashSet<String> activeStates = new HashSet<String>(states.length);
	for(int i=0; i<states.length; i++) activeStates.add(states[i]);
	Iterator<String> it = activeStates.iterator();
	
	while(it.hasNext())
	{
	    String s = it.next();
	    if(s.equals(AutoSim.INITIAL_STATE_NAME) || s.equals(AutoSim.FINAL_STATE_NAME)) continue;
	    
	    // Eliminate state 's'
	    ArrayList<RegexTransition> incoming = filterTransitions(trans, s, true);
	    ArrayList<RegexTransition> outgoing = filterTransitions(trans, s, false);
	    RegexTransition loop = getSelfLoopTransition(trans, s);
	    
	    if(incoming!=null && outgoing!=null)
	    {
		int n1 = incoming.size(), n2 = outgoing.size();
		for(int i=0; i<n1; i++)
		{
		    RegexTransition inc = incoming.get(i);
		    String incomingFrom = inc.fromState;
		    String incomingRegex = inc.onRegex;

		    if(incomingRegex.equals(String.valueOf(AutoSim.LAMBDA_CHAR))) incomingRegex="";
		    
		    for(int j=0; j<n2; j++)
		    {
			RegexTransition out = outgoing.get(j);
			String outgoingTo = out.toState;
			String outgoingRegex = out.onRegex;

			if(outgoingRegex.equals(String.valueOf(AutoSim.LAMBDA_CHAR))) outgoingRegex="";
			
			String loopRegex = (loop==null ? "" : loop.onRegex);
			if(loopRegex.equals(String.valueOf(AutoSim.LAMBDA_CHAR))) 
			    loopRegex="";
			else {
			    if(loopRegex.length()==1)
				loopRegex += "*";
			    else if(loopRegex.length()>1) {
				if(loopRegex.startsWith("(") && loopRegex.endsWith(")*"))
				    loopRegex = loopRegex;  // no change
				else
				    loopRegex = "(" + loopRegex + ")*";
			    }
			}
			
			if(incomingRegex.indexOf('+') > -1) incomingRegex = "(" + incomingRegex + ")";
			if(outgoingRegex.indexOf('+') > -1) outgoingRegex = "(" + outgoingRegex + ")";
			
			String totalRegex = incomingRegex + loopRegex + outgoingRegex;
			if(totalRegex.equals("")) totalRegex = String.valueOf(AutoSim.LAMBDA_CHAR);
			if(totalRegex.startsWith("(") && totalRegex.endsWith(")"))
			{
			    totalRegex = totalRegex.substring(1, totalRegex.length()-1);
			}
			
			addTransition(trans, incomingFrom, outgoingTo, totalRegex);
		    }
		}
	    } else {
		// unreachable state or useless state
	    }
	    
	    removeState(trans, s);
	    if(trace)
	    {
		System.out.print("After eliminating " + s + ", we have the following ");
		printTransitionFunction(trans);
	    }
	}
	
	if(trans.size()!=1)
	{
	    System.out.println("Error! More than 1 transitions remaining...");
	} else {
	    String r = trans.get(0).onRegex.replace(AutoSim.LAMBDA_CHAR, AutoSim.LAMBDA_PRINT_CHAR);
	    
	    System.out.println("Equivalent regular expression:\n" + r);
	}
    }
    
    private void addTransition(ArrayList<RegexTransition> trans, String inc, String out, String regex)
    {
	boolean found=false;
	int n = trans.size();
	for(int i=0; i<n; i++)
	{
	    RegexTransition t = trans.get(i);
	    if(t.fromState.equals(inc) && t.toState.equals(out))
	    {
		found=true;
		
		String left = t.onRegex;
		if(left.length() > 1 && left.indexOf('+') > -1) left = "(" + left + ")";
		
		String right = regex;
		if(right.length() > 1 && right.indexOf('+') > -1) right = "(" + right + ")";
		
		t.onRegex = left + "+" + (right.length()==0 ? AutoSim.LAMBDA_CHAR : right);
		break;
	    }
	}
	
	if(!found)
	{
	    // add a new transition
	    trans.add(new RegexTransition(inc,out,regex));
	}
    }
    
    private void removeState(ArrayList<RegexTransition> trans, String s)
    {
	boolean found=false;
	
	do {
	    found=false;
	    
	    int n = trans.size();
	    for(int i=0; i<n; i++)
	    {
		RegexTransition t = trans.get(i);
		if(t.fromState.equals(s) || t.toState.equals(s)) 
		{
		    found=true;
		    trans.remove(i);
		    i=n;
		}
	    }
	} while(found);
    }
}