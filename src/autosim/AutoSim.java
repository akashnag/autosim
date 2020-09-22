// ---------------------------------------------------------------------------------------------
//  Copyright (c) Akash Nag. All rights reserved.
//  Licensed under the MIT License. See LICENSE.md in the project root for license information.
// ---------------------------------------------------------------------------------------------

package autosim;

public class AutoSim
{
    protected static final char LAMBDA_CHAR = '~';
    protected static final char FULL_ALPHABET_CHAR = '*';
    protected static final String INITIAL_STATE_NAME = "INITIAL";
    protected static final String FINAL_STATE_NAME = "FINAL";
    
    protected static final char DELTA_PRINT_CHAR = 'δ';
    protected static final char LAMBDA_PRINT_CHAR = 'ε';
    protected static final char NULL_PRINT_CHAR = 'Φ';
    protected static final char ARROW_PRINT_CHAR = '→';
    
    public static void main(String args[])
    {
        // Arguments:
        // -<automaton> -desc <desc-file> -input <string> [-trace]
        // OR
        // -<automaton> -desc <desc-file> -data <input-file> [-trace]
	// OR
	// -<automaton> -desc <desc-file> -output <output-file> [-trace]
	// OR
	// -nfa-to-regex -desc <desc-file> [-trace]
	
	/*
	args = new String[] {
	    "-nfa-to-regex", "-desc", 
	    "/home/akash/MY PROJECTS/JAVA/AutoSim/nfa1.nfa", "-trace"
	};
	*/

	if(args.length==1 && args[0].equalsIgnoreCase("-help"))
        {
            printHelp();
            return;
        }
	
	if(args.length==0)
	{
	    printUsage();
	    return;
	}
	
	String automataType = args[0].trim().toLowerCase().substring(1);
	String descFile = null;
	String input = null;
	String inputFile = null;
	String outputFile = null;
	boolean trace = false;
	
        for(int i=1; i<args.length; i++)
	{
	    args[i] = args[i].trim().toLowerCase();
	    if(args[i].equals("-desc")) descFile = args[++i];
	    if(args[i].equals("-input")) input = args[++i];
	    if(args[i].equals("-data")) inputFile = args[++i];
	    if(args[i].equals("-output")) outputFile = args[++i];
	    if(args[i].equals("-trace")) trace = true;
	}
	
	if(input==null && inputFile!=null) input = FileIO.readAll(inputFile);
	if(input!=null && input.equals(String.valueOf(LAMBDA_CHAR))) input="";
        
	if(descFile == null)
	{
	    printUsage();
	    return;
	}
	
	if(input==null)
	{
	    String requiresInput[] = { "dfa", "nfa", "dpda", "npda", "stm", "cfg", "moore", "mealy" };
	    for(int i=0; i<requiresInput.length; i++)
	    {
		if(automataType.equals(requiresInput[i]))
		{
		    System.out.println("ERROR: Input/Input-File required");
		    printUsage();
		    return;
		}
	    }
	}
	
	if(outputFile==null)
	{
	    String requiresOutput[] = { "nfa-to-dfa", "clean-cfg", "cfg-to-cnf", "cfg-to-gnf", "cfg-to-npda" };
	    for(int i=0; i<requiresOutput.length; i++)
	    {
		if(automataType.equals(requiresOutput[i]))
		{
		    System.out.println("ERROR: Output file required");
		    printUsage();
		    return;
		}
	    }
	}
	
        printVersion();
        if(automataType.equals("dfa")) 
        {            
            (new DFA(descFile, input, trace)).simulate();
        } else if(automataType.equals("nfa")) {            
            (new NFA(descFile, input, trace)).simulate();
        } else if(automataType.equals("dpda")) {            
            (new DPDA(descFile, input, trace)).simulate();
        } else if(automataType.equals("npda")) {            
            (new NPDAParser(descFile, input, trace)).simulate();
        } else if(automataType.equals("stm")) {            
            (new STM(descFile, input, trace)).simulate();
        } else if(automataType.equals("cfg")) {            
            (new CFG(descFile, input, trace)).simulate();
        } else if(automataType.equals("moore")) {            
            (new MooreMachine(descFile, input, trace)).simulate();
        } else if(automataType.equals("mealy")) {            
            (new MealyMachine(descFile, input, trace)).simulate();
        } else if(automataType.equals("nfa-to-regex")) {            
            (new NFAToRegexConverter(descFile, trace)).simulate();
        } else if(automataType.equals("nfa-to-dfa")) {            
            //(new NFAToDFAConverter(descFile, outputFile, trace)).simulate();
	} else if(automataType.equals("clean-cfg")) {            
            //(new CleanCFG(descFile, outputFile, trace)).simulate();
        } else if(automataType.equals("cfg-to-cnf")) {            
            //(new CFGToCNFConverter(descFile, outputFile, trace)).simulate();
        } else if(automataType.equals("cfg-to-gnf")) {            
            //(new CFGToGNFConverter(descFile, outputFile, trace)).simulate();
        } else if(automataType.equals("cfg-to-npda")) {            
            //(new CFGToNPDAConverter(descFile, outputFile, trace)).simulate();
        }
    }
    
    private static void printVersion()
    {
        System.out.println("\nAutoSim v3.0\n~ Akash Nag\n");
    }
    
    private static void printUsage()
    {
        printVersion();
        System.out.println("java -jar autosim.jar <OPTIONS>");
        System.out.println("\nOPTIONS:\n-<automaton> -desc <desc-file> -input <string> [-trace]");
        System.out.println("OR:\n-<automaton> -desc <desc-file> -data <input-file> [-trace] [-output <output-file>]\nOR:\n-help\n");
        System.out.println("<automaton> = dfa/nfa/dpda/npda/stm/cfg/moore/mealy/clean-cfg");
	System.out.println("              nfa-to-regex/nfa-to-dfa/cfg-to-cnf/cfg-to-gnf/cfg-to-npda");
	System.out.println("<desc-file> = filename where the automaton is described");
        System.out.println("<string> = the input string to the automaton");
        System.out.println("<input-file> = specify the file to read the input from (rather than console)");
	System.out.println("<output-file> = specify the file to write the output to (required for all cleaning and conversion operations)");
	System.out.println("-trace = [OPTIONAL] to show the progress of the machine through the states");        
        System.out.println("Use -help to view the manual on how to write automata descriptions.\n");        
    }
    
    private static void printHelp()
    {
        printVersion();
        System.out.println(LAMBDA_CHAR + " is used in place of "+AutoSim.LAMBDA_PRINT_CHAR+" in NFA, NPDA and STM.");
        System.out.println(AutoSim.FULL_ALPHABET_CHAR+" is used to represent the full set of characters in the\ninput/stack/tape alphabet except "+AutoSim.LAMBDA_PRINT_CHAR+".");
        System.out.println("The following symbols: comma, space, "+LAMBDA_CHAR+", *, =, /, (, ), {, and } cannot be\nused in either the input or the stack/tape alphabet.");
        System.out.println("Comments begin with // and span till the end of the line.\nBlank lines are ignored.\nDefine the other elements of the automata before defining the transition rules.\n");
        System.out.println("Q - set of internal states, e.g. Q = { q0, q1, q2 }");
        System.out.println("E - input alphabet, e.g. E = { a, b }");
        System.out.println("T - stack/tape alphabet/set of terminals (CFG)/output-alphabet(moore,mealy), e.g. T = { 0, 1 }");
        System.out.println("z - stack/tape start symbol, e.g. z = 0");
	System.out.println("V - set of variables of CFG, e.g. V = { a, b }");
	System.out.println("S - starting symbol of CFG, e.g. S = E");
        System.out.println("i - initial state, e.g. i = q0");
        System.out.println("F - set of final states, e.g. F = { q1, q2 }");
        System.out.println("d - transition-rule/production-rule/delta-function\nExamples:");
        System.out.println("DFA:\td(q0, a) = q1");
        System.out.println("NFA:\td(q0, a) = { q1, q2 }");
        System.out.println("NFA:\td(q0, *) = { q1, q2 }");
        System.out.println("NFA:\td(q0, ~) = { q1, q2 }");
        System.out.println("DPDA:\td(q0, a, 0) = (q1, 110)");
        System.out.println("STM:\td(q0, a) = (q1, 0, L)");
        System.out.println("STM:\td(q0, b) = (q1, 1, R)");
	System.out.println("Mealy machine:\td(q0, b) = (q1, 1)");
	System.out.println("o - output-function of Mealy machine, e.g.: o(q0)=1");
        System.out.println("CFG Rule syntax:\tS -> aSb | ~");
    }
}