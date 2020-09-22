// ---------------------------------------------------------------------------------------------
//  Copyright (c) Akash Nag. All rights reserved.
//  Licensed under the MIT License. See LICENSE.md in the project root for license information.
// ---------------------------------------------------------------------------------------------

package autosim;

import java.io.*;

class FileIO
{
    private BufferedReader br = null;
    private int lineCounter;
    
    public static String readAll(String fileName)
    {
        try {
            BufferedReader in = new BufferedReader(new FileReader(fileName));
            StringBuilder b = new StringBuilder();
            String s = null;
            while((s=in.readLine())!=null) b.append(removeSpaces(s));
            in.close();
            return b.toString();
        } catch(IOException e) {
            return null;
        }
    }
    
    public boolean openFile(String fileName)
    {
        try {
            br = new BufferedReader(new FileReader(fileName));
            lineCounter=0;
            return true;
        } catch(IOException e) {
            System.out.println("ERROR: Cannot open file '" + fileName + "'.");
            return false;
        }
    }
    
    public String readNextLine()
    {
        if(br==null) return null;
        try {
            String x = removeSpaces(br.readLine());
            if(x!=null) lineCounter++;
            return x;
        } catch(IOException e) {
            System.out.println("ERROR: Cannot read from file.");
            return null;
        }
    }
    
    public void closeFile()
    {
        if(br!=null) 
        {
            try {
                br.close();
            } catch(IOException e) {
                System.out.println("ERROR: Cannot close file.");
            } finally {
                br = null;
            }
        }
    }
    
    public boolean isReadyForReading() { return(br!=null); }
    
    protected void finalize()
    {
        closeFile();
    }
    
    public int getLineNumber() { return lineCounter; }
    
    private static String removeSpaces(String s)
    {
	if(s==null) return null;
	s = s.trim();
	int n = s.length();
	StringBuilder sb = new StringBuilder(n);
	for(int i=0; i<n; i++)
	{
	    char c = s.charAt(i);
	    if(c!=' ' && c!='\t') 
	    {
		if(c>0 && c<256) sb.append(c);
	    }
	}
	
	String x = sb.toString();
	int comPos = x.indexOf("//");
        if(comPos > -1) x=x.substring(0,comPos).trim();
            
	return x;
    }
}