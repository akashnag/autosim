// ---------------------------------------------------------------------------------------------
//  Copyright (c) Akash Nag. All rights reserved.
//  Licensed under the MIT License. See LICENSE.md in the project root for license information.
// ---------------------------------------------------------------------------------------------

package autosim;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

class MooreMachine
{
    private final String descFile;
    private final String input;
    private FileIO fio;
    
    private boolean hasSpec[];
    private final boolean trace;
    
    private HashSet<String> internalStates;
    private HashSet<Character> inputAlphabet;
    private HashSet<Character> outputAlphabet;
    private HashMap<String, Character> outputFunction;
    private String initialState;
    
    private String states[];
    private char symbols[];
    private String table[][];
    
    public MooreMachine(String descFile, String input, boolean trace)
    {
        this.descFile = descFile;
        this.input = input;
        this.trace = trace;
        
        hasSpec = new boolean[4];       // Q, E, I(q0), T
        fio = new FileIO();
        fio.openFile(this.descFile);
        
        internalStates = new HashSet<String>();
        inputAlphabet = new HashSet<Character>();
        outputAlphabet = new HashSet<Character>();
        outputFunction = new HashMap<String, Character>();
        
        table=null;
    }
    
    public void simulate()
    {
        if(!fio.isReadyForReading()) return;
        if(!parseMoore()) return;
        
        if(table==null)
        {
            System.out.println("ERROR: Incomplete specification of Moore Machine");
            return;
        }
        
        if(!isTableComplete())
        {
            System.out.println("WARNING: Incomplete specification of Moore Machine. One or more rules are undefined.");
        }
        
        if(!verifyInput())
        {
            System.out.println("ERROR: Input string contains symbols not defined in the input alphabet");
            return;
        }
        
        simulateMooreMachine();
    }
        
    private boolean verifyInput()
    {
        int n = input.length();
        for(int i=0; i<n; i++)
        {
            if(!inputAlphabet.contains(input.charAt(i))) return false;
        }
        return true;
    }
    
    private boolean isTableComplete()
    {
        if(table==null) return false;
        
	for(int i=0; i<table.length; i++)
        {
            for(int j=0; j<table[i].length; j++)
            {
                if(table[i][j]==null) return false;
            }
        }
	
	Iterator<String> it = internalStates.iterator();
	while(it.hasNext())
	{
	    if(!outputFunction.containsKey(it.next())) return false;
	}
	
        return true;
    }
    
    private void simulateMooreMachine()
    {
        int n = input.length();
        String currentState = initialState;
	StringBuilder sb = new StringBuilder();
	
	sb.append(outputFunction.get(currentState));
        if(trace) System.out.println("Initial state : " + currentState + "\t\t\tOutput: " + outputFunction.get(currentState));
	
        for(int i=0; i<n; i++)
        {
            char currentSymbol = input.charAt(i);
            
            int row = getIndexOfState(currentState);
            int col = getIndexOfSymbol(currentSymbol);
            
            if(col == -1)
            {
                System.out.println("ERROR: undefined symbol '" + currentSymbol + "'");
                return;
            }
            
            if(table[row][col]==null)
            {
                System.out.println("ERROR: no rule defined for δ("+currentState+","+currentSymbol+")");
                return;
            }
            
            if(trace) System.out.print("Executing Rule: δ("+currentState+", "+currentSymbol+") = " + table[row][col]);
            currentState = table[row][col];
	    
	    char os = outputFunction.get(currentState);
	    sb.append(os);
	    if(trace) System.out.println("\t\tOutput: " + os);
        }
        
	System.out.println("\nOutput:\n" + sb.toString());
    }
    
    private boolean parseMoore()
    {
        String s = null;
        while((s=fio.readNextLine())!=null)
        {
            if(s==null || s.length()==0) continue;
            
            String x = s.substring(0,2);
            
            if(x.equals("Q="))
            {
                if(!parseInternalStates(s)) { fio.closeFile(); return false; }
            } else if(x.equals("E=")) {
                if(!parseInputAlphabet(s)) { fio.closeFile(); return false; }
            } else if(x.equals("T=")) {
                if(!parseOutputAlphabet(s)) { fio.closeFile(); return false; }
            } else if(x.equals("I=")) {
                if(!parseInitialState(s)) { fio.closeFile(); return false; }
            } else if(x.equals("d(")) {
                if(!parseRule(s)) { fio.closeFile(); return false; }
            } else if(x.equals("o(")) {
                if(!parseOutputFunction(s)) { fio.closeFile(); return false; }
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
        
        hasSpec[0]=true;
        return true;
    }
        
    private boolean parseInputAlphabet(String s)
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
                for(String symbol: a) inputAlphabet.add(new Character(symbol.charAt(0)));
            }
        }
        
        hasSpec[1]=true;
        return true;
    }
    
    private boolean parseOutputAlphabet(String s)
    {
        if(hasSpec[3])
        {
            System.out.println("ERROR in Line "+fio.getLineNumber()+": Duplicate definition of T - output alphabet");
            return false;
        } else {
            if(s.charAt(2) != '{' || s.charAt(s.length()-1) != '}')
            {
                System.out.println("ERROR in Line "+fio.getLineNumber()+": invalid syntax.\nCorrect syntax: T = { a, b, ... }");
                return false;
            } else {
                String a[] = s.substring(3,s.length()-1).split(",");
                for(String symbol: a) outputAlphabet.add(new Character(symbol.charAt(0)));
            }
        }
        
        hasSpec[3]=true;
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
            
            String targetState = s.substring(b4+1);
            String currentState = s.substring(b1+1,b2);
            Character currentSymbol = s.charAt(b2+1);
            
            if(!internalStates.contains(targetState))
            {
                System.out.println("ERROR in Line "+fio.getLineNumber()+": State '" + targetState + "' not found");
                return false;
            }
            
            if(!internalStates.contains(currentState))
            {
                System.out.println("ERROR in Line "+fio.getLineNumber()+": State '" + currentState + "' not found");
                return false;
            }
            
            if(!inputAlphabet.contains(currentSymbol))
            {
                System.out.println("ERROR in Line "+fio.getLineNumber()+": Symbol '" + currentSymbol + "' not found");
                return false;
            }
            
            int row = getIndexOfState(currentState);
            int col = getIndexOfSymbol(currentSymbol);
            table[row][col]=targetState;
        }
        
        return true;
    }
    
    private boolean parseOutputFunction(String s)
    {
        if(!isSpecReady())
        {
            System.out.println("ERROR in Line "+fio.getLineNumber()+": Q, E, I and F must be defined prior to defining the output function.");
            return false;
        } else {
            int b1 = s.indexOf('('), b3 = s.indexOf(')'), b4 = s.lastIndexOf('=');
            if(b1==-1 || b3==-1 || b4==-1)
            {
                printOutputFunctionSyntax();
                return false;
            }
            
            if(!(b1 < b3 && b3 < b4 && b4==b3+1))
            {
                printOutputFunctionSyntax();
                return false;
            }
            
            String currentState = s.substring(b1+1,b3);
            Character outputSymbol = s.charAt(b4+1);
            
            if(!internalStates.contains(currentState))
            {
                System.out.println("ERROR in Line "+fio.getLineNumber()+": State '" + currentState + "' not found");
                return false;
            }
            
            if(!outputAlphabet.contains(outputSymbol))
            {
                System.out.println("ERROR in Line "+fio.getLineNumber()+": Symbol '" + outputSymbol + "' not found");
                return false;
            }
            
	    outputFunction.put(currentState, outputSymbol);
        }
        
        return true;
    }
    
    private void makeTable()
    {
        int n1 = internalStates.size(), n2 = inputAlphabet.size();
        
        states = new String[n1];
        symbols = new char[n2];
        
        int i=-1, j=-1;
        
        Iterator<String> it1 = internalStates.iterator();
        while(it1.hasNext()) states[++i]=it1.next();
        
        Iterator<Character> it2 = inputAlphabet.iterator();
        while(it2.hasNext()) symbols[++j]=it2.next();  
        
        table = new String[n1][n2];
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
        System.out.println("ERROR in Line "+fio.getLineNumber()+": Invalid syntax.\nCorrect Syntax: d(q0, a) = q1");
    }
    
    private void printOutputFunctionSyntax()
    {
        System.out.println("ERROR in Line "+fio.getLineNumber()+": Invalid syntax.\nCorrect Syntax: o(q0) = 1");
    }
    
    private boolean isSpecReady()
    {
        return(hasSpec[0] && hasSpec[1] && hasSpec[2] && hasSpec[3]);
    }
}