package autosim;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Stack;

class NPDAClone 
{
    private Stack<Character> stack;
    private final PDATarget table[][][][];
    private final String input;
    private int inputHead;
    private boolean trace;
    
    private HashSet<String> finalStates;
    private String states[];
    private char inputSymbols[];
    private char stackSymbols[];
        
    private String currentState;
    
    protected static String haltingState = "";    // set only by the clone which halts in final state
    
    public NPDAClone(HashSet<String> fis, String states[], char is[], char ss[], String cs, PDATarget t[][][][], Stack<Character> stk, String inp, int index, boolean trace)
    {
        this.states = states;
        this.finalStates = fis;
        this.inputSymbols = is;
        this.stackSymbols = ss;
        
        this.table = t;
        this.input = inp;
        this.inputHead = index;
        this.currentState = cs;
        this.trace = trace;
        
        this.stack = copyStack(stk);
    }
    
    private Stack<Character> copyStack(Stack<Character> src)
    {
        Stack<Character> target = new java.util.Stack<Character>();
        
        Character x[] = new Character[src.size()];
        x = src.toArray(x);
        for(Character a:x) target.push(a);
        
        return target;
    }
    
    protected Stack<Character> simulate()
    {
        if(stack.isEmpty()) 
        {
            System.out.println("ERROR: Stack exhausted");
            return null;
        }
        char ss = stack.peek();
        
        PDATarget matches1[] = findMatchingLambdaRules(ss);
        if(matches1!=null)
        {
            for(int i=0; i<matches1.length; i++) 
            {
                if(trace)
                {
                    String ps = matches1[i].symbols;
                    if(ps.equals("")) ps = "" + AutoSim.LAMBDA_PRINT_CHAR;
                    System.out.println("Executing Rule: " +AutoSim.DELTA_PRINT_CHAR+"("+currentState+", "+AutoSim.LAMBDA_PRINT_CHAR+", "+ss+") = (" + matches1[i].nextState + ", " + ps+")");
                    printNewStack(stack, ps);
                }

                Stack<Character> stk = spawnClone(inputHead, matches1[i]);
                if(stk!=null) return stk;
            }
        }
                
        // return true if reached final state
        if(inputHead >= input.length()) 
        {
            if(finalStates.contains(currentState)) {
                haltingState = currentState;
                return stack;
            } else
                return null;
        }
                
        char is = input.charAt(inputHead);            
        PDATarget matches2[] = findMatchingRules(is,ss);
        if(matches2!=null)
        {
            for(int i=0; i<matches2.length; i++) 
            {
                if(trace)
                {
                    String ps = matches2[i].symbols;
                    if(ps.equals("")) ps = "" + AutoSim.LAMBDA_PRINT_CHAR;
                    System.out.println("Executing Rule: " +AutoSim.DELTA_PRINT_CHAR+"("+currentState+", "+is+", "+ss+") = (" + matches2[i].nextState + ", " + ps+")");
                    printNewStack(stack, ps);
                }
                
                Stack<Character> stk = spawnClone(inputHead+1, matches2[i]);
                if(stk!=null) return stk;
            }
        }
        
        if(finalStates.contains(currentState)) {
            haltingState = currentState;
            return stack;
        } else
            return null;
    }
    
    private void printNewStack(Stack<Character> stk, String ps)
    {
        String x = NPDAParser.stackToString(stk);
        x = x.substring(0, x.length()-1) + reverse(NPDAParser.removeAllChars(ps,AutoSim.LAMBDA_PRINT_CHAR));
        System.out.println("\tStack contents after execution: " + x);
    }
    
    private String reverse(String x)
    {
        StringBuffer sb = new StringBuffer(x);
        return sb.reverse().toString();
    }
    
    private void addToList(ArrayList<PDATarget> list, PDATarget t[])
    {
        for(int i=0; i<t.length; i++) list.add(t[i]);
    }
    
    private PDATarget[] findMatchingRules(char inps, char stks)
    {
        int index1 = getIndexOfState(currentState);
        int index2 = getIndexOfInputSymbol(inps);
        int index3 = getIndexOfStackSymbol(stks);
        
        return table[index1][index2][index3];
    }
    
    private PDATarget[] findMatchingLambdaRules(char stks)
    {
        int index1 = getIndexOfState(currentState);
        int index2 = getIndexOfInputSymbol(AutoSim.LAMBDA_CHAR);
        int index3 = getIndexOfStackSymbol(stks);
        
        return table[index1][index2][index3];
    }
    
    private Stack<Character> spawnClone(int index, PDATarget target)
    {
        String nextState = target.nextState;
        String popAndPushSymbols = target.symbols;
        
        Stack<Character> newStack = copyStack(stack);
        newStack.pop();
        for(int i=popAndPushSymbols.length()-1; i>=0; i--) newStack.push(popAndPushSymbols.charAt(i));
        
        NPDAClone child=new NPDAClone(finalStates, states, inputSymbols, stackSymbols, nextState, table, newStack, input, index, trace);
        return child.simulate();
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
}
