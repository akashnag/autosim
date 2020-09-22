package autosim;

import java.util.HashSet;
import java.util.Iterator;

class NFA
{
    private final String descFile;
    private final String input;
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
    
    public NFA(String descFile, String input, boolean trace)
    {
        this.descFile = descFile;
        this.input = input;
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
        
        if(!isTableComplete())
        {
            System.out.println("WARNING: Incomplete specification of NFA. One or more rules are undefined.");
        }
        
        if(!verifyInput())
        {
            System.out.println("ERROR: Input string contains symbols not defined in the input alphabet");
            return;
        }
        
        simulateNFA();
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
        return true;
    }
    
    private boolean verifyInput()
    {
        int n = input.length();
        for(int i=0; i<n; i++)
        {
            if(!alphabet.contains(input.charAt(i))) return false;
        }
        return true;
    }
    
    private void simulateNFA()
    {
        int n = input.length();
        int lambdaIndex = getIndexOfSymbol(AutoSim.LAMBDA_CHAR);
        
        HashSet<String> currentState = new HashSet<String>();
        currentState.add(initialState);
        
        for(int i=0; i<n; i++)
        {
            char currentSymbol = input.charAt(i);
            
            // perform lambda-transition before normal-transition (reqd. if initial state contains lambda-transitions)
            HashSet<String> nextState = new HashSet<String>();
            performLambdaTransitions(lambdaIndex,currentState,nextState);
            currentState = nextState;
            
            nextState = new HashSet<String>();      // clean next-state
            Iterator<String> it1 = currentState.iterator();
            
            while(it1.hasNext())
            {
                String state = it1.next();
                                
                int row = getIndexOfState(state);
                int col = getIndexOfSymbol(currentSymbol);
                
                if(col == -1)
                {
                    System.out.println("ERROR: undefined symbol '" + currentSymbol + "'");
                    return;
                }
            
                // <------------------------- For normal transitions --------------------------->
                if(table[row][col]==null)
                {
                    if(trace) System.out.println("Executing Rule: "+AutoSim.DELTA_PRINT_CHAR+"("+state+", "+currentSymbol+") = undefined");
                    continue;
                } else {            
                    if(trace) System.out.println("Executing Rule: "+AutoSim.DELTA_PRINT_CHAR+"("+state+", "+currentSymbol+") = " + getStateList(table[row][col]));
                
                    HashSet<String> targetStates = (HashSet<String>)table[row][col];
                    Iterator<String> it2 = targetStates.iterator();
                    while(it2.hasNext()) nextState.add(it2.next());
                }
            }
            
            currentState = nextState;       // assign changes (after normal transition)
            
            // perform lambda-transition after normal transition
            nextState = new HashSet<String>();
            performLambdaTransitions(lambdaIndex,currentState,nextState);
            currentState = nextState;            
        }
                
        // perform lambda-transition after completion (reqd. when input = lambda, the for-loop above will never execute)
        HashSet<String> nextState = new HashSet<String>();
        performLambdaTransitions(lambdaIndex,currentState,nextState);
        currentState = nextState;            
        
        // <---------------- check final state --------------------->
        Iterator<String> it = currentState.iterator();
        while(it.hasNext())
        {
            String state = it.next();
            if(finalStates.contains(state))
            {
                System.out.println("String accepted: NFA halted in state '" + state + "'");
                return;
            }
        }
        
        System.out.println("String rejected: NFA halted in states: " + getStateList(currentState));
    }
    
    private void performLambdaTransitions(int lambdaIndex, HashSet<String> currentState, HashSet<String> nextState)
    {
        HashSet<String> cs = currentState;
        
        while(true)
        {
            Iterator<String> it1 = cs.iterator();
            while(it1.hasNext())
            {
                String state = it1.next();

                int row = getIndexOfState(state);

                // <------------------------- For lambda transitions --------------------------->
                if(table[row][lambdaIndex]!=null)
                {
                    if(trace) System.out.println("Executing Rule: "+AutoSim.DELTA_PRINT_CHAR+"("+state+", "+AutoSim.LAMBDA_PRINT_CHAR+") = " + getStateList(table[row][lambdaIndex]));

                    HashSet<String> targetStates = (HashSet<String>)table[row][lambdaIndex];
                    Iterator<String> it2 = targetStates.iterator();
                    while(it2.hasNext()) nextState.add(it2.next());                    
                }
                
                nextState.add(state);       // current state is also present: as on lambda-transition: control is divided: one stays there, one goes forward
            }
            
            if(nextState.equals(cs)) return;
            cs = nextState;
        }
    }
    
    
    
    private String getStateList(Object x)
    {
        String s="";
        HashSet<String> set = (HashSet<String>)x;
        Iterator<String> it = set.iterator();
        while(it.hasNext()) s += (it.next() + ", ");
        
        s=s.trim();
        
        if(s.length()==0) return "" + AutoSim.NULL_PRINT_CHAR;
        
        s=s.substring(0,s.length()-1);
        return "{ " + s + " }";
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
    
    private String removeAllChars(String s, char c)
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