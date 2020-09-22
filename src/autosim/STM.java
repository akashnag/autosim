package autosim;

import java.util.HashSet;
import java.util.Iterator;

class STMTarget
{
    protected final String nextState;
    protected final char replacerSymbol;
    protected final TMDirection direction;
    
    public STMTarget(String ns, char sym, char dir)
    {
        nextState=ns;
        replacerSymbol=sym;
        direction = TMDirection.fromChar(dir);
    }
    
    @Override
    public String toString()
    {
        return "("+nextState+","+replacerSymbol+","+direction.toChar()+")";
    }
}

enum TMDirection 
{
    LEFT, RIGHT;
    
    public char toChar()
    {
        return toString().charAt(0);
    }
    
    public static TMDirection fromChar(char dir)
    {
        char d = Character.toUpperCase(dir);
        if(d=='L')
            return TMDirection.LEFT;
        else if(d=='R')
            return TMDirection.RIGHT;
        else
            return null;
    }
}

public class STM
{
    public static final int TAPE_LENGTH = 10000;
    
    private final String descFile;
    private final String input;
    private FileIO fio;
    
    private boolean hasSpec[];
    private final boolean trace;
    
    private HashSet<String> internalStates;
    private HashSet<String> finalStates;
    private HashSet<Character> inputAlphabet;
    private HashSet<Character> tapeAlphabet;
    
    private char blankSymbol;
    private String initialState;
    
    private String states[];
    private char allSymbols[];      // bcoz once inside tape, input and tape symbols are indistinguishable
    private STMTarget table[][];
    
    private char tape[];
    private int tapeMarker;
    
    public STM(String descFile, String input, boolean trace)
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
        tapeAlphabet = new HashSet<Character>();
        
        table=null;
    }
    
    public void simulate()
    {
        if(!fio.isReadyForReading()) return;
        if(!parseSTM()) return;
        
        if(table==null)
        {
            System.out.println("ERROR: Incomplete specification of STM");
            return;
        }
        
        if(!isTableComplete())
        {
            System.out.println("WARNING: Incomplete specification of STM. One or more rules are undefined.");
        }
        
        if(!verifyInput())
        {
            System.out.println("ERROR: Input string contains symbols not defined in the input alphabet");
            return;
        }
        
        simulateSTM();
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
        return true;
    }
      
    
    private void simulateSTM()
    {
        int n = input.length();
        int touchedMin = TAPE_LENGTH, touchedMax = -1;
        
        // Initialize the tape
        tape = new char[TAPE_LENGTH];
        for(int i=0; i<TAPE_LENGTH; i++) tape[i]=blankSymbol;
        int m = TAPE_LENGTH/2;
        for(int i=(m-(n/2)), j=0; j<n; j++, i++)
        {
            tape[i]=input.charAt(j);
            
            touchedMin = (i < touchedMin ? i : touchedMin);
            touchedMax = (i > touchedMax ? i : touchedMax);
        }
        tapeMarker = (m-(n/2));
                
        
        String currentState = initialState;
        
        while(true)
        {
            if(tapeMarker<0 || tapeMarker>=TAPE_LENGTH)
            {
                System.out.println("ERROR: read/write head exceeded tape bounds.");
                return;
            }
            
            char currentTapeSymbol = tape[tapeMarker];
            
            touchedMin = (tapeMarker < touchedMin ? tapeMarker : touchedMin);
            touchedMax = (tapeMarker > touchedMax ? tapeMarker : touchedMax);
            
            int index1 = getIndexOfState(currentState);
            int index2 = getIndexOfSymbol(currentTapeSymbol);
                        
            if(index2 == -1)
            {
                System.out.println("ERROR: undefined symbol '" + currentTapeSymbol + "'");
                return;
            }
            
            if(table[index1][index2]==null) break;      // END OF EXECUTION
            
            if(trace) 
            {
                System.out.println("Executing Rule: δ("+currentState+","+currentTapeSymbol+") = " + table[index1][index2]);
            }

            currentState = table[index1][index2].nextState;
            tape[tapeMarker] = table[index1][index2].replacerSymbol;
            
            if(trace)
            {
                System.out.println("\tTape contents after execution: " + tapeToString(tape,touchedMin,touchedMax));
            }
            
            TMDirection dir = table[index1][index2].direction;
            if(dir==TMDirection.LEFT)
                tapeMarker--;
            else if(dir==TMDirection.RIGHT)
                tapeMarker++;
            else {
                System.out.println("ERROR: In Rule δ("+currentState+","+currentTapeSymbol+"): invalid direction specified.");
                return;
            }
        }
        
        if(finalStates.contains(currentState))
        {
            System.out.println("String accepted: STM halted in state '" + currentState + "'");
        } else {
            System.out.println("String rejected: STM halted in state '" + currentState + "'");
        }
        
        System.out.println("Tape contents: " + tapeToString(tape,touchedMin,touchedMax));
    }
    
    private String tapeToString(char tape[], int min, int max)
    {
        StringBuilder sb = new StringBuilder(max-min+3);
        for(int i=min-1; i<=max+1; i++) sb.append(tape[i]);
        return sb.toString();
    }
    
    private boolean parseSTM()
    {
        String s = null;
        while((s=fio.readNextLine())!=null)
        {
            s = s.replace('\t',' ').trim();
            int comPos = s.indexOf("//");
            if(comPos > -1) s=s.substring(0,comPos).trim();
            
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
                if(!parseTapeAlphabet(s)) { fio.closeFile(); return false; }
            } else if(x.equals("Z=")) {
                if(!parseBlankSymbol(s)) { fio.closeFile(); return false; }
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
        
    private boolean parseInputAlphabet(String s)
    {
        if(hasSpec[1])
        {
            System.out.println("ERROR in Line "+fio.getLineNumber()+": Duplicate definition of 'E - input alphabet'");
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
    
    private boolean parseTapeAlphabet(String s)
    {
        if(hasSpec[2])
        {
            System.out.println("ERROR in Line "+fio.getLineNumber()+": Duplicate definition of 'T - tape alphabet'");
            return false;
        } else {
            if(s.charAt(2) != '{' || s.charAt(s.length()-1) != '}')
            {
                System.out.println("ERROR in Line "+fio.getLineNumber()+": invalid syntax.\nCorrect syntax: E = { a, b, ... }");
                return false;
            } else {
                String a[] = s.substring(3,s.length()-1).split(",");
                for(String symbol: a) tapeAlphabet.add(new Character(symbol.charAt(0)));
            }
        }
        
        hasSpec[2]=true;
        return true;
    }
    
    private boolean parseBlankSymbol(String s)
    {
        if(hasSpec[3])
        {
            System.out.println("ERROR in Line "+fio.getLineNumber()+": Duplicate definition of 'Z - blank symbol'");
            return false;
        } else {
            char symbol = s.charAt(2);
            if(tapeAlphabet.contains(symbol))
                blankSymbol = symbol;
            else {
                System.out.println("ERROR in Line "+fio.getLineNumber()+": Symbol '" + symbol + "' not found in tape alphabet");
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
        
        hasSpec[4]=true;
        return true;
    }
        
    private boolean parseFinalStates(String s)
    {
        if(hasSpec[5])
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
        
        hasSpec[5]=true;
        return true;
    }
    
    private boolean parseRule(String s)
    {
        if(!isSpecReady())
        {
            System.out.println("ERROR in Line "+fio.getLineNumber()+": Q, E, T, Z, I and F must be defined prior to defining the transition rules.");
            return false;
        } else {
            int b1 = s.indexOf('('), b2 = s.indexOf(','), b3 = s.indexOf(')'), b4 = s.indexOf('=');
            
            String target = s.substring(b4+1);
            int b5 = target.indexOf('('), b6 = target.indexOf(',');
            int b7 = target.lastIndexOf(','), b8 = target.indexOf(')');
            
            if(target.charAt(0)!='(' || target.charAt(target.length()-1)!=')')
            {
                printRuleSyntax();
                return false;
            }
            
            if(b1==-1 || b2==-1 || b3==-1 || b4==-1 || b5==-1 || b6==-1 || b7==-1 || b8==-1)
            {
                printRuleSyntax();
                return false;
            }
            
            if(!(b1 < b2 && b2 < b3 && b3 < b4))
            {
                printRuleSyntax();
                return false;
            }
            
            if(!(b5<b6 && b6<b7 && b7<b8))
            {
                printRuleSyntax();
                return false;
            }
                        
            if(!(b3==b2+2 && b4==b3+1 && b7==b6+2 && b8==b7+2))
            {
                printRuleSyntax();
                return false;
            }
            
            String currentState = s.substring(b1+1,b2);
            Character currentTapeSymbol = s.charAt(b2+1);
            String nextState = target.substring(b5+1,b6);
            Character replacerTapeSymbol = target.charAt(b6+1);
            char directionToMove = Character.toUpperCase(target.charAt(b7+1));
            
            if(!internalStates.contains(currentState))
            {
                System.out.println("ERROR in Line "+fio.getLineNumber()+": State '" + currentState + "' not found");
                return false;
            }
            
            if(!inputAlphabet.contains(currentTapeSymbol) && !tapeAlphabet.contains(currentTapeSymbol))
            {
                System.out.println("ERROR in Line "+fio.getLineNumber()+": Symbol '" + currentTapeSymbol + "' not found in input/tape alphabet");
                return false;
            }
            
            if(directionToMove!='L' && directionToMove!='R')
            {
                System.out.println("ERROR in Line "+fio.getLineNumber()+": Symbol '" + directionToMove + "' is invalid for direction");
                return false;
            }
                        
            STMTarget targetState = new STMTarget(nextState, replacerTapeSymbol, directionToMove);
            
            int index1 = getIndexOfState(currentState);
            int index2 = getIndexOfSymbol(currentTapeSymbol);
            
            table[index1][index2]=targetState;
        }
        
        return true;
    }
    
    private void makeTable()
    {
        int n1 = internalStates.size();
        
        states = new String[n1];
        
        int i=-1, j=-1;
        
        Iterator<String> it1 = internalStates.iterator();
        while(it1.hasNext()) states[++i]=it1.next();
        
        HashSet<Character> allSym = new HashSet<Character>();
        Iterator<Character> it2 = inputAlphabet.iterator();
        while(it2.hasNext()) allSym.add(it2.next());
        Iterator<Character> it3 = tapeAlphabet.iterator();
        while(it3.hasNext()) allSym.add(it3.next());
        
        allSymbols = new char[allSym.size()];
        table = new STMTarget[n1][allSym.size()];
        
        Iterator<Character> it4 = allSym.iterator();
        while(it4.hasNext()) allSymbols[++j]=it4.next();
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
        for(int i=0; i<allSymbols.length; i++)
        {
            if(symbol==allSymbols[i]) return i;
        }
        return -1;
    }
        
    private void printRuleSyntax()
    {
        System.out.println("ERROR in Line "+fio.getLineNumber()+": Invalid syntax.\nCorrect Syntax: d(q0, a) = (q1, 1, R)");
    }
    
    private boolean isSpecReady()
    {
        return(hasSpec[0] && hasSpec[1] && hasSpec[2] && hasSpec[3] && hasSpec[4] && hasSpec[5]);
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