package autosim;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

class Rule
{
    protected char left;
    protected String right;
    
    public Rule(char l, String r)
    {
	this.left = l;
	this.right = r;
    }
    
    public boolean canBeEmpty()
    {
	return(this.right.equals(String.valueOf(AutoSim.LAMBDA_CHAR)));
    }
    
    @Override
    public String toString()
    {
	return left + " " + AutoSim.ARROW_PRINT_CHAR + " " + (right.equals(String.valueOf(AutoSim.LAMBDA_CHAR)) ? AutoSim.LAMBDA_PRINT_CHAR : right);
    }
}

class DTreeNode
{
    protected DTreeNode parent;
    protected String sentence;
    protected Rule ruleApplied;
    protected ArrayList<DTreeNode> children;
    
    public DTreeNode(DTreeNode p, String s, Rule r)
    {
	this.parent = p;
	this.sentence = s;
	this.ruleApplied = r;
	this.children = null;
    }    
}

class CFG
{
    private final String descFile;
    private final String input;
    private FileIO fio;
    
    private boolean hasSpec[];
    private final boolean trace;
    
    private HashSet<Character> variables;
    private HashSet<Character> terminals;
    private char startingSymbol;
    
    private ArrayList<Rule> rules;
    private Rule[][] groupedRules;
    private char symbols[];
    private HashMap<Character,Integer> symbolMap;
    
    public CFG(String descFile, String input, boolean trace)
    {
        this.descFile = descFile;
        this.input = input;
        this.trace = trace;
        
        this.hasSpec = new boolean[3];       // V, T, S
        this.fio = new FileIO();
        this.fio.openFile(this.descFile);
        
        this.variables = new HashSet<Character>();
        this.terminals = new HashSet<Character>();
        this.startingSymbol = 0;
	this.rules = new ArrayList<Rule>();
    }
    
    public static int countOccurrences(String s, HashSet<Character> sym)
    {
	int n = s.length(), c=0;
	for(int i=0; i<n; i++)
	{
	    if(sym.contains(s.charAt(i))) c++;
	}
	return c;
    }
    
    private boolean parseCFG()
    {
	String s = null;
        while((s=fio.readNextLine())!=null)
        {
            if(s==null || s.length()==0) continue;
            
	    if(s.startsWith("V={"))
	    {
		if(!parseVariables(s)) return false;
	    } else if(s.startsWith("T={")) {
		if(!parseTerminals(s)) return false;
	    } else if(s.startsWith("S=")) {
		if(!parseStartingSymbol(s)) return false;
	    } else {
		if(!parseRule(s)) return false;
	    }
        }
        
        fio.closeFile();        
        return true;
    }
    
    private boolean parseRule(String s)
    {
	if(s.indexOf("->") != 1)
	{
	    System.out.println("P:ERROR in Line "+fio.getLineNumber()+": Correct syntax: S -> ... | ...");
	    return false;
	}
	
	char left = s.charAt(0);
	
	if(!variables.contains(left))
	{
	    System.out.println("P:ERROR in Line "+fio.getLineNumber()+": Expected variable on the left side of the production rule");
	    return false;
	}
	String right[] = s.substring(3).split(Pattern.quote("|"));
	
	for(int i=0; i<right.length; i++)
	{
	    String rightRule = right[i];
	    int n = rightRule.length();
	    for(int j=0; j<n; j++)
	    {
		char c = rightRule.charAt(j);
		if(c!=AutoSim.LAMBDA_CHAR && !terminals.contains(c) && !variables.contains(c))
		{
		    System.out.println("P:ERROR in Line "+fio.getLineNumber()+": Expected variable/terminal combination as part of rule: " + left + " -> " + rightRule);
		    return false;
		}
	    }
	    
	    rules.add(new Rule(left,rightRule));
	}
	
	return true;
    }
    
    private boolean parseVariables(String s)
    {
	if(!s.startsWith("V={") || !s.endsWith("}"))
	{
	    System.out.println("V:ERROR in Line "+fio.getLineNumber()+": Correct syntax: V={ S, A, B, ... }");
	    return false;
	}
	
	String vars[] = s.substring(3, s.length()-1).split(",");
	for(int i=0; i<vars.length; i++)
	{
	    if(vars[i].length() > 1)
	    {
		System.out.println("V:ERROR in Line "+fio.getLineNumber()+": Variables must contain a single character");
		return false;
	    }
	    variables.add(vars[i].charAt(0));
	}
	
	hasSpec[0] = true;
	return true;
    }
    
    private boolean parseTerminals(String s)
    {
	if(!s.startsWith("T={") || !s.endsWith("}"))
	{
	    System.out.println("T:ERROR in Line "+fio.getLineNumber()+": Correct syntax: T={ a, b, ... }");
	    return false;
	}
	
	String term[] = s.substring(3, s.length()-1).split(",");
	for(int i=0; i<term.length; i++)
	{
	    if(term[i].length() > 1)
	    {
		System.out.println("T:ERROR in Line "+fio.getLineNumber()+": Terminals must contain a single character");
		return false;
	    }
	    terminals.add(term[i].charAt(0));
	}
	
	hasSpec[1] = true;
	return true;
    }
    
    private boolean parseStartingSymbol(String s)
    {
	if(!s.startsWith("S="))
	{
	    System.out.println("SS:ERROR in Line "+fio.getLineNumber()+": Correct syntax: S=S");
	    return false;
	}
	
	String x = s.substring(2);
	if(x.length() > 1)
	{
	    System.out.println("SS:ERROR in Line "+fio.getLineNumber()+": Starting symbol must be a variable");
	    return false;
	}
	
	startingSymbol = x.charAt(0);
	
	hasSpec[2] = true;
	return true;
    }
    
    
    
    public void simulate()
    {
	if(!fio.isReadyForReading()) return;
        if(!parseCFG()) return;
        if(!verifyInput()) return;
	groupRules();
	
	DTreeNode root = new DTreeNode(null, ""+startingSymbol, null);
	ArrayDeque<DTreeNode> q = new ArrayDeque<DTreeNode>();
	q.add(root);
	
	boolean found=false;
	DTreeNode correctNode=null;
	
	while(!q.isEmpty())
	{
	    DTreeNode node = q.pollFirst();
	    
	    if(expand(node))
	    {
		int n = node.children.size();
		for(int i=0; i<n; i++)
		{
		    q.add(node.children.get(i));
		}
	    } else {
		if(node.sentence.equals(input))
		{
		    found=true;
		    correctNode = node;
		    break;
		}
	    }
	}
	
	if(!found)
	{
	    System.out.println("The given string does not belong to the language specified by the given CFG");
	    return;
	} else {
	    System.out.println("The string can be derived from the given CFG");
	}
	
	if(this.trace)
	{
	    System.out.println("Derivation:");
	    printDerivation(correctNode);
	}
    }
    
    private void printDerivation(DTreeNode node)
    {
	if(node==null) return;
	
	printDerivation(node.parent);
	System.out.printf("%-" + (2*input.length()) + "s", node.sentence);
	if(node.ruleApplied != null)
	{
	    System.out.println("\t\tusing: " + node.ruleApplied.toString());
	} else {
	    System.out.println();
	}
    }
    
    private void groupRules()
    {
	symbols = new char[variables.size()];
	symbolMap = new HashMap<Character, Integer>();
	
	Iterator<Character> it = variables.iterator();
	for(int i=0; it.hasNext(); i++) 
	{
	    symbols[i] = it.next();
	    symbolMap.put(symbols[i], i);	    
	}
	
	groupedRules = new Rule[symbols.length][];
	for(int i=0; i<symbols.length; i++)
	{
	    char left = symbols[i];
	    groupedRules[i] = filterRules(left);
	}
    }
    
    private boolean verifyInput()
    {
	int n = input.length();
	for(int i=0; i<n; i++)
	{
	    if(!terminals.contains(input.charAt(i)))
	    {
		System.out.println("ERROR: input contains 1 or more non-terminals");
		return false;
	    }
	}
	
	if(!variables.contains(startingSymbol))
	{
	    System.out.println("ERROR: Starting symbol must be a variable!");
	    return false;
	}
	
	Iterator<Character> it = terminals.iterator();
	while(it.hasNext())
	{
	    if(variables.contains(it.next()))
	    {
		System.out.println("ERROR: terminal can not also be a variable!");
		return false;
	    }
	}
	
	return true;
    }
    
    
    private Rule[] filterRules(char var)
    {
	ArrayList<Rule> result = new ArrayList<Rule>();
	int n = rules.size();
	for(int i=0; i<n; i++)
	{
	    if(rules.get(i).left == var)
	    {
		result.add(rules.get(i));
	    }
	}
	
	Rule filteredRules[] = new Rule[result.size()];
	filteredRules = result.toArray(filteredRules);
	return filteredRules;
    }
    
    private int getMinimumSentenceLength(String sentence)
    {
	int n = sentence.length(), cnt = 0;
	for(int i=0; i<n; i++)
	{
	    char c = sentence.charAt(i);
	    if(terminals.contains(c)) cnt++;
	}
	return cnt;
    }
    
    private boolean expand(DTreeNode node)
    {
	if(node.children != null) return false;   // already expanded
	if(countOccurrences(node.sentence, variables)==0) return false;	    // no variables to replace
	
	int n = node.sentence.length();
	
	node.children = new ArrayList<DTreeNode>();
	
	for(int i=0; i<n; i++)
	{
	    char left = node.sentence.charAt(i);
	    if(!variables.contains(left)) continue;
	    
	    int ruleIndex = symbolMap.get(left);
	    Rule allRules[] = groupedRules[ruleIndex];
	    
	    for(int j=0; j<allRules.length; j++)
	    {
		String newSentence = node.sentence.substring(0,i) + (allRules[j].canBeEmpty() ? "" : allRules[j].right) + node.sentence.substring(i+1);
		if(getMinimumSentenceLength(newSentence) > input.length()) continue;	// no point in expanding if it cannot derive given string
		
		Rule appliedRule = allRules[j];
		DTreeNode newChild = new DTreeNode(node, newSentence, appliedRule);
		node.children.add(newChild);
	    }
	}
	
	return true;
    }
}