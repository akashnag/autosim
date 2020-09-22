package autosim;

class PDATarget
{
    protected final String nextState;
    protected final String symbols;
    
    public PDATarget(String ns, String sym)
    {
        nextState=ns;
        symbols=sym;
    }
    
    @Override
    public String toString()
    {
        String ps = symbols;
        if(symbols.equals("")) ps = ""+AutoSim.LAMBDA_PRINT_CHAR;
        return "("+nextState+","+ps+")";
    }
}