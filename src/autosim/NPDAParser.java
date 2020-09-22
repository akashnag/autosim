package autosim;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;

class NPDAParser
{
    private final String descFile;
    private final String input;
    private FileIO fio;
    
    private boolean hasSpec[];
    private final boolean trace;
    
    private HashSet<String> internalStates;
    private HashSet<String> finalStates;
    private HashSet<Character> inputAlphabet;
    private HashSet<Character> stackAlphabet;
    
    private char stackStartSymbol;
    private String initialState;
    
    private String states[];
    private char inputSymbols[];
    private char stackSymbols[];
    private PDATarget table[][][][];
    
    public NPDAParser(String descFile, String input, boolean trace)
    {
        this.descFile = descFile;
        this.input = input;
        this.trace = trace;
        
        hasSpec = new boolean[6];       // Q, E, T, z, I(q0), F
        fio = new FileIO();
        fio.openFile(this.descFile);
        
        internalStates = new HashSet<String>();
        finalStates = new HashSet<String>();
        inputAlphabet = new HashSet<Character>();
        stackAlphabet = new HashSet<Character>();
        
        table=null;
    }
    
    public void simulate()
    {
        if(!fio.isReadyForReading()) return;
        if(!parseNPDA()) return;
        
        if(table==null)
        {
            System.out.println("ERROR: Incomplete specification of NPDA");
            return;
        }
        
        if(!isTableComplete())
        {
            System.out.println("WARNING: Incomplete specification of NPDA. One or more rules are undefined.");
        }
        
        if(!verifyInput())
        {
            System.out.println("ERROR: Input string contains symbols not defined in the input alphabet");
            return;
        }
        
        simulateNPDA();
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
                for(int k=0; k<table[i][j].length; k++)
                {
                    if(table[i][j][k]==null) return false;
                }
            }
        }
        return true;
    }
      
    private void simulateNPDA()
    {
        Stack<Character> stack = new Stack<Character>();
        stack.push(stackStartSymbol);
        
        NPDAClone parent = new NPDAClone(finalStates, states, inputSymbols, stackSymbols, initialState, table, stack, input, 0, trace);
        Stack<Character> stk = parent.simulate();
        
        System.out.println("String "+(stk!=null ? "accepted" : "rejected")+", NPDA halted in state '" + NPDAClone.haltingState + "'.");
        System.out.println("Stack Contents: " + stackToString(stk));
    }
    
    public static String stackToString(Stack<Character> stk)
    {
        if(stk==null) return "";
        
        Character c[] = new Character[stk.size()];
        StringBuilder sb = new StringBuilder(stk.size());
        c = stk.toArray(c);
        for(char x:c) sb.append(x);
        return sb.toString();
    }
    
    private boolean parseNPDA()
    {
        String s = null;
        while((s=fio.readNextLine())!=null)
        {
            if(s.length()==0) continue;
            s = removeAllChars(s,' ');
            
            String x = s.substring(0,2).toUpperCase();
            
            if(x.equals("Q="))
            {
                if(!parseInternalStates(s)) { fio.closeFile(); return false; }
            } else if(x.equals("E=")) {
                if(!parseInputAlphabet(s)) { fio.closeFile(); return false; }
            } else if(x.equals("I=")) {
                if(!parseInitialState(s)) { fio.closeFile(); return false; }
            } else if(x.equals("F=")) {
                if(!parseFinalStates(s)) { fio.closeFile(); return false; }
            } else if(x.equals("T=")) {
                if(!parseStackAlphabet(s)) { fio.closeFile(); return false; }
            } else if(x.equals("Z=")) {
                if(!parseStackStartSymbol(s)) { fio.closeFile(); return false; }
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
            System.out.println("IS:ERROR in Line "+fio.getLineNumber()+": Duplicate definition of 'Q - set of internal states'");
            return false;
        } else {
            if(s.charAt(2) != '{' || s.charAt(s.length()-1) != '}')
            {
                System.out.println("IS:ERROR in Line "+fio.getLineNumber()+": invalid syntax.\nCorrect syntax: Q = { q0, q1, ... }");
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
            System.out.println("IA:ERROR in Line "+fio.getLineNumber()+": Duplicate definition of 'E - input alphabet'");
            return false;
        } else {
            if(s.charAt(2) != '{' || s.charAt(s.length()-1) != '}')
            {
                System.out.println("IA:ERROR in Line "+fio.getLineNumber()+": invalid syntax.\nCorrect syntax: E = { a, b, ... }");
                return false;
            } else {
                String a[] = s.substring(3,s.length()-1).split(",");
                for(String symbol: a) inputAlphabet.add(new Character(symbol.charAt(0)));
            }
        }
        
        hasSpec[1]=true;
        return true;
    }
    
    private boolean parseStackAlphabet(String s)
    {
        if(hasSpec[2])
        {
            System.out.println("SA:ERROR in Line "+fio.getLineNumber()+": Duplicate definition of 'T - stack alphabet'");
            return false;
        } else {
            if(s.charAt(2) != '{' || s.charAt(s.length()-1) != '}')
            {
                System.out.println("SA:ERROR in Line "+fio.getLineNumber()+": invalid syntax.\nCorrect syntax: E = { a, b, ... }");
                return false;
            } else {
                String a[] = s.substring(3,s.length()-1).split(",");
                for(String symbol: a) stackAlphabet.add(new Character(symbol.charAt(0)));
            }
        }
        
        hasSpec[2]=true;
        return true;
    }
    
    private boolean parseStackStartSymbol(String s)
    {
        if(hasSpec[3])
        {
            System.out.println("SS:ERROR in Line "+fio.getLineNumber()+": Duplicate definition of 'Z - stack start symbol'");
            return false;
        } else {
            char symbol = s.charAt(2);
            if(stackAlphabet.contains(symbol))
                stackStartSymbol = symbol;
            else {
                System.out.println("SS:ERROR in Line "+fio.getLineNumber()+": Symbol '" + symbol + "' not found in stack alphabet");
                return false;
            }
        }
        
        hasSpec[3]=true;
        return true;
    }
    
    private boolean parseInitialState(String s)
    {
        if(hasSpec[4])
        {
            System.out.println("I:ERROR in Line "+fio.getLineNumber()+": Duplicate definition of 'I - initial state'");
            return false;
        } else {
            String stateName = s.substring(2);
            if(internalStates.contains(stateName))
                initialState = stateName;
            else {
                System.out.println("I:ERROR in Line "+fio.getLineNumber()+": State '" + stateName + "' not found");
                return false;
            }
        }
        
        hasSpec[4]=true;
        return true;
    }
        
    private boolean parseFinalStates(String s)
    {
        if(hasSpec[5])
        {
            System.out.println("F1:ERROR in Line "+fio.getLineNumber()+": Duplicate definition of 'F - set of final states'");
            return false;
        } else {
            if(s.charAt(2) != '{' || s.charAt(s.length()-1) != '}')
            {
                System.out.println("F2:ERROR in Line "+fio.getLineNumber()+": invalid syntax.\nCorrect syntax: F = { q1, q2, ... }");
                return false;
            } else {
                String a[] = s.substring(3,s.length()-1).split(",");
                for(String state: a) 
                {
                    if(!internalStates.contains(state))
                    {
                        System.out.println("F3:ERROR in Line "+fio.getLineNumber()+": State '" + state + "' not found");
                        return false;
                    } else {
                        finalStates.add(state);
                    }
                }
            }
        }
        
        hasSpec[5]=true;
        return true;
    }
    
    private boolean parseRule(String s)
    {
        if(!isSpecReady())
        {
            System.out.println("D:ERROR in Line "+fio.getLineNumber()+": Q, E, T, Z, I and F must be defined prior to defining the transition rules.");
            return false;
        } else {
            int epos = s.indexOf('=');
            if(epos == -1)
            {
                printRuleSyntax();
                return false;
            }
            
            String left = s.substring(1,epos);
            String right = s.substring(epos+1);
            
            if(right.charAt(0)!='{' || right.charAt(right.length()-1)!='}')
            {
                printRuleSyntax();
                return false;
            }
            
            if(left.charAt(0)!='(' || left.charAt(left.length()-1)!=')')
            {
                printRuleSyntax();
                return false;
            }
            
            
            left = left.substring(1, left.length()-1);          // remove parenthesis
            right = right.substring(1, right.length()-1);       // remove braces
            
            String rmod = "";
            boolean pOn = false;
            int len = right.length();
            for(int i=0; i<len; i++)
            {
                char c = right.charAt(i);
                
                if(c=='(') pOn = true;
                if(c==')') pOn = false;
                if(c==',' && !pOn) c=';';
                if(c!='(' && c!=')') rmod += c;
            }
            
            String targets[] = rmod.split(";");
            
            int b1 = left.indexOf(','), b2 = left.lastIndexOf(',');
            if(b1==-1 || b2==-1 || b2!=b1+2)
            {
                printRuleSyntax();
                return false;
            }
            
            String currentState = left.substring(0,b1);
            Character currentInputSymbol = left.charAt(b1+1);
            Character currentStackSymbol = left.charAt(b2+1);
            
            if(!internalStates.contains(currentState))
            {
                System.out.println("D2:ERROR in Line "+fio.getLineNumber()+": State '" + currentState + "' not found");
                return false;
            }
            
            if(!inputAlphabet.contains(currentInputSymbol))
            {
                System.out.println("D3:ERROR in Line "+fio.getLineNumber()+": Symbol '" + currentInputSymbol + "' not found");
                return false;
            }
            
            if(!stackAlphabet.contains(currentStackSymbol))
            {
                System.out.println("D4:ERROR in Line "+fio.getLineNumber()+": Symbol '" + currentInputSymbol + "' not found");
                return false;
            }
            
            int index1 = getIndexOfState(currentState);
            int index2 = getIndexOfInputSymbol(currentInputSymbol);
            int index3 = getIndexOfStackSymbol(currentStackSymbol);
            
            table[index1][index2][index3]=new PDATarget[targets.length];
            
            for(int i=0; i<targets.length; i++)
            {
                int cpos = targets[i].indexOf(',');
                if(cpos==-1)
                {
                    printRuleSyntax();
                    return false;
                }
                
                String targetState = targets[i].substring(0,cpos);
                String symbolsToPush = targets[i].substring(cpos+1);
                
                symbolsToPush = removeAllChars(symbolsToPush, AutoSim.LAMBDA_CHAR);
                
                if(!internalStates.contains(targetState))
                {
                    printRuleSyntax();
                    return false;
                }
                
                table[index1][index2][index3][i] = new PDATarget(targetState, symbolsToPush);
            }
        }
        
        return true;
    }
    
    private void makeTable()
    {
        inputAlphabet.add(AutoSim.LAMBDA_CHAR);
        int n1 = internalStates.size(), n2 = inputAlphabet.size(), n3 = stackAlphabet.size();
        
        states = new String[n1];
        inputSymbols = new char[n2];
        stackSymbols = new char[n3];
        
        int i=-1, j=-1, k=-1;
        
        Iterator<String> it1 = internalStates.iterator();
        while(it1.hasNext()) states[++i]=it1.next();
        
        Iterator<Character> it2 = inputAlphabet.iterator();
        while(it2.hasNext()) inputSymbols[++j]=it2.next();  
        
        Iterator<Character> it3 = stackAlphabet.iterator();
        while(it3.hasNext()) stackSymbols[++k]=it3.next();  
        
        table = new PDATarget[n1][n2][n3][];
    }
    
    private int getIndexOfState(String stateName)
    {
        for(int i=0; i<states.length; i++)
        {
            if(stateName.equals(states[i])) return i;
        }
        return -1;
    }
    
    private int getIndexOfInputSymbol(char symbol)
    {
        for(int i=0; i<inputSymbols.length; i++)
        {
            if(symbol==inputSymbols[i]) return i;
        }
        return -1;
    }
        
    private int getIndexOfStackSymbol(char symbol)
    {
        for(int i=0; i<stackSymbols.length; i++)
        {
            if(symbol==stackSymbols[i]) return i;
        }
        return -1;
    }
        
    private void printRuleSyntax()
    {
        System.out.println("ERROR in Line "+fio.getLineNumber()+": Invalid syntax.\nCorrect Syntax: d(q0, a, 0) = { (q1, 10), (q2, 00) }");
    }
    
    private boolean isSpecReady()
    {
        return(hasSpec[0] && hasSpec[1] && hasSpec[2] && hasSpec[3] && hasSpec[4] && hasSpec[5]);
    }
    
    protected static String removeAllChars(String s, char c)
    {
        int n = s.length();
        String r = "";
        for(int i=0; i<n; i++)
        {
            if(s.charAt(i)!=c) r+=s.charAt(i);
        }
        return r;
    }
}