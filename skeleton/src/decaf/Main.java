package decaf;

import java.io.*;
import antlr.Token;
import java6035.tools.CLI.*;

class Main {
    public static void main(String[] args) {
        try {
        	CLI.parse (args, new String[0]);
        	
        	InputStream inputStream = args.length == 0 ?
                    System.in : new java.io.FileInputStream(CLI.infile);

        	if (CLI.target == CLI.SCAN)
        	{
        		DecafScanner lexer = new DecafScanner(new DataInputStream(inputStream));
        		Token token;
        		boolean done = false;
        		while (!done)
        		{
        			try
        			{
		        		for (token=lexer.nextToken(); token.getType()!=DecafParserTokenTypes.EOF; token=lexer.nextToken())
		        		{
		        			String type = "";
		        			String text = token.getText();
		
		        			switch (token.getType())
		        			{
		        			case DecafScannerTokenTypes.ID:
		        				type = " IDENTIFIER";
		        				break;
		        			case DecafScannerTokenTypes.CHAR:
		        				type = " CHARLITERAL";
		        				break;
		        			case DecafScannerTokenTypes.TRUE:
		        			case DecafScannerTokenTypes.FALSE:
		        				type = " BOOLEANLITERAL";
		        				break;
		        			case DecafScannerTokenTypes.HEX:
		        			case DecafScannerTokenTypes.DECIMAL:
		        				type = " INTLITERAL";
		        				break;
		        			case DecafScannerTokenTypes.STRING:
		        				type = " STRINGLITERAL";
		        				break;
		        			}
		        			System.out.println (token.getLine() + type + " " + text);
		        		}
		        		done = true;
        			} catch(Exception e) {
        	        	// print the error:
        	            System.out.println(CLI.infile+" "+e);
        	            lexer.consume ();
        	        }
        		}
        	}
        	else if (CLI.target == CLI.PARSE || CLI.target == CLI.DEFAULT)
        	{
        		DecafScanner lexer = new DecafScanner(new DataInputStream(inputStream));
        		DecafParser parser = new DecafParser (lexer, CLI.debug);
        		//DecafParser parser = new DecafParser (lexer);
                parser.program(); 
        	}
        	else if (CLI.target == CLI.INTER)
        	{
        		DecafScanner lexer = new DecafScanner(new DataInputStream(inputStream));
        		DecafParser parser = new DecafParser (lexer, CLI.debug);
                parser.program(); 
                IrNode irRoot = parser.getIrTree();
                SemanticChecker checker = new SemanticChecker(CLI.infile, CLI.debug);
                if (CLI.debug) System.out.println("--- checking -----");
                checker.checkProgram((IrClassDecl) irRoot);
        	}
        	else if (CLI.target == CLI.LOWIR) {
        		DecafScanner lexer = new DecafScanner(new DataInputStream(inputStream));
        		DecafParser parser = new DecafParser (lexer, CLI.debug);
                parser.program(); 
                IrNode irRoot = parser.getIrTree();
                SemanticChecker checker = new SemanticChecker(CLI.infile, CLI.debug);
                if (CLI.debug) System.out.println("--- checking -----");
                checker.checkProgram((IrClassDecl) irRoot);
                CodeGen codegen = new CodeGen(irRoot, CLI.debug);
                codegen.genLowIr();
                codegen.printLowIr();
        	}
        } catch(Exception e) {
        	// print the error:
            System.out.println(CLI.infile+" "+e);
        }
    }
}

