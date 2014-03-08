header 
{
    package decaf;
    import java.util.List;
    import java.util.ArrayList;
    import java.util.Deque;
    import java.util.ArrayDeque;
}

options
{
 	mangleLiteralPrefix = "TK_";
  	language="Java";
}

class DecafParser extends Parser;
options
{
	importVocab=DecafScanner;
 	k=3;
}

{
    private boolean debug;
    private SymbolTable syms;
    private IrNode parent, child;
    private Deque<IrNode> stack, auxStack; 

    public DecafParser(TokenStream lexer, boolean debug) {
        this(lexer, 3);
        this.debug = debug;
        this.syms = new SymbolTable();
        this.parent = null; // root of IR tree after program() finishes
        this.child = null;
        this.stack = new ArrayDeque<IrNode>();
        this.auxStack = new ArrayDeque<IrNode>(); 
    }
    
    private void put(Token t, int type) {
        IrId id = new IrId(t.getText());
        switch (type) {
            case TK_int:
                syms.put(id, new IntEntry());
                if (debug) syms.print();
                break;
            case TK_boolean:
                syms.put(id, new BoolEntry());
                if (debug) syms.print();
                break;
        }
    }
    
    private void put(Token t, int type, String aSizeStr) {
    	int aSize = Integer.parseInt(aSizeStr);
        IrId id = new IrId(t.getText());
        switch (type) {
            case TK_int:
                syms.put(id, new IntArrayEntry(aSize));
                if (debug) syms.print();
                break;
            case TK_boolean:
                syms.put(id, new BoolArrayEntry(aSize));
                if (debug) syms.print();
                break;
        }
    }
    
    private void put(Token t, int retType, List<IrType> args) {
        IrId id = new IrId(t.getText());
        IrType rt = tkToIrType(retType);
        MethodSignature sig = new MethodSignature(rt, args);
        syms.put(id, new MethodEntry(sig));
        if (debug) syms.print();
    }
    
    private void put(List<IrType> argTypes, List<IrId> argIds) {
    	int typeCode;
    	for (int i=0; i<argTypes.size(); i++) {
    		typeCode = argTypes.get(i).getTypeCode();
    		if (typeCode == Ir.INT)
    			syms.put(argIds.get(i), new IntEntry());
    		else
    			syms.put(argIds.get(i), new BoolEntry());
    	}
    }
    
    private IrId tokenToIrId (Token t) {
    	return new IrId(t.getText());
    }
    
    private IrType tkToIrType(int tk) {
    	int irt;
        if (tk == TK_int)
            irt = Ir.INT;
        else if (tk == TK_boolean)
            irt = Ir.BOOL;
        else
            irt = Ir.VOID;
        return new IrType(irt);
    }
    
    public IrNode getIrTree() {
    	return this.parent;
    }
    
    public void printIr(IrNode root) {
    	System.out.println("============ IR TREE ==========================");
    	printIrR(root, 0);
    }
    
    private void printIrR(IrNode node, int level) {
    	String indent = "";
		for (int i=0; i<level; i++) {
			indent += "  ";
		}
		System.out.println(indent + node);
    	for (int i=0; i<node.numChildren(); i++)
    		printIrR(node.child(i), level+1);
    }
}

program: 
    {
    	syms.beginScope();
    	parent = new IrClassDecl(null);
    	stack.push(parent);
    } 
    TK_class id:ID {id.getText().equals("Program")}?
    LCURLY 
    (
    field_decl
    	{
    		while ( stack.peek() instanceof IrFieldDecl ) 
    			auxStack.push(stack.pop());
    		parent = stack.pop();
    		while ( !auxStack.isEmpty() ) {
    			child = auxStack.pop();
    			child.setParent(parent);
    			parent.addChild(child);
    		}
    		stack.push(parent);
    	}
    )* 		
    (
    method_decl
    	{
    		child = stack.pop();
    		parent = stack.pop();
    		child.setParent(parent);
    		parent.addChild(child);
    		stack.push(parent);
    	}
    )* RCURLY EOF
    {
        syms.endScope();
        parent = stack.pop();
        if (debug) {
        	printIr(parent);
        }
    }
    ;

field_decl
	: 
    {
    	int t=-1;
    	String aSizeStr="-1"; 
    	boolean wasArray=false;
    } 
    t=type id:ID (aSizeStr=array {wasArray=true;})? 
    {
        if (wasArray) {
            put(id, t, aSizeStr);
            parent = new IrArrayFieldDecl(null, tkToIrType(t), 
            					tokenToIrId(id), Integer.parseInt(aSizeStr));
        }
        else {
            put(id, t);
        	parent = new IrFieldDecl(null, tkToIrType(t), tokenToIrId(id));
        }
        parent.setLineNum(id.getLine()); 
        wasArray = false;
        stack.push(parent);
    }    
    (
        COMMA id2:ID (aSizeStr=array {wasArray=true;})?
        {
            if (wasArray) {
                put(id2, t, aSizeStr);
            	parent = new IrArrayFieldDecl(null, tkToIrType(t), 
            						tokenToIrId(id2), Integer.parseInt(aSizeStr)); 
            }
            else {
                put(id2, t);
                parent = new IrFieldDecl(null, tkToIrType(t), tokenToIrId(id2));
            }
            parent.setLineNum(id2.getLine()); 
            wasArray = false;
            stack.push(parent);
        }
    )* 
    SEMI
    ;

method_decl
	: 
    {
    	int rt=TK_void, t=TK_void; 
    	List<IrType> argTypes=new ArrayList<IrType>();
    	List<IrId> argIds=new ArrayList<IrId>();
    	IrMethodDecl methodDecl;
    }
    (rt=type | TK_void) mid:ID LPAREN 
    ( t=type aid:ID 
        {
        	argTypes.add(tkToIrType(t));
        	argIds.add(new IrId(aid.getText()));
        }
        (COMMA t=type aid2:ID
            {
            	argTypes.add(tkToIrType(t));
            	argIds.add(new IrId(aid2.getText()));
            }
        )* 
    )? RPAREN 
    {
    	parent = new IrMethodDecl(null, tkToIrType(rt), tokenToIrId(mid));
    	parent.setLineNum(mid.getLine());
    	for (int i = 0; i < argIds.size(); i++) 
    		parent.addChild(new IrMethodArg(parent, argTypes.get(i), argIds.get(i)));
    	stack.push(parent);
    	put(mid, rt, argTypes);
    	syms.beginScope();
    	put(argTypes, argIds);
    }
    block[true]
    {
    	syms.endScope();
    	
    	child = stack.pop();
    	parent = stack.pop();
    	child.setParent(parent);
    	parent.addChild(child);
    	stack.push(parent);
    }
    ;

block
[boolean methodOrForBlock]
	: 
    lc:LCURLY 
    {
        if (!methodOrForBlock)
        	syms.beginScope();
        parent = new IrBlock(null);
        parent.setLineNum(lc.getLine());
        stack.push(parent);
    }
    (
    var_decl
    	{
    		while ( stack.peek() instanceof IrVarDecl ) 
    			auxStack.push(stack.pop());
    		parent = stack.pop();
    		while ( !auxStack.isEmpty() ) {
    			child = auxStack.pop();
    			child.setParent(parent);
    			parent.addChild(child);
    		}
    		stack.push(parent);
    	}
    )* 
    (
    statement
    	{
    		child = stack.pop();
    		parent = stack.pop();
    		child.setParent(parent);
    		parent.addChild(child);
    		stack.push(parent);
    	}
    )* RCURLY
    {
        if (!methodOrForBlock)
        	syms.endScope();
    }
    ;

var_decl: 
    { 
    	int t; 
    	IrVarDecl vd;
    }
    t=type id:ID 
    { 
    	put(id, t);
    	parent = new IrVarDecl(null, tkToIrType(t), tokenToIrId(id));
    	parent.setLineNum(id.getLine());
    	stack.push(parent);
    }
    (
        COMMA id2:ID 
        { 
        	put(id2, t); 
        	parent = new IrVarDecl(null, tkToIrType(t), tokenToIrId(id2));
    		parent.setLineNum(id2.getLine());
    		stack.push(parent);
        }
    )* 
    SEMI
    ;

statement:
	(
    location assign_op 
    	{
    		parent = stack.pop();
    		child = stack.pop();
    		child.setParent(parent);
    		parent.addChild(child);
    		stack.push(parent);
    	}
    	expr SEMI
    	{	
    		child = stack.pop();
    		parent = stack.pop();
    		child.setParent(parent);
    		parent.addChild(child);
    		stack.push(parent);
    	}
    | 
    	{
    		parent = new IrInvokeStmt(null);
    		stack.push(parent);
    		// set line number after we see SEMI
    	}
    method_call s:SEMI
    	{
    		child = stack.pop();
    		parent = stack.pop();
    		parent.setLineNum(s.getLine());
    		child.setParent(parent);
    		parent.addChild(child);
    		stack.push(parent);
    	}
    | ti:TK_if LPAREN expr RPAREN block[false] 
    	{
    		parent = new IrIfStmt(null);
    		parent.setLineNum(ti.getLine());
    		IrNode trueBlock = stack.pop();
    		IrNode ifExpr = stack.pop();
    		trueBlock.setParent(parent);
    		ifExpr.setParent(parent);
    		parent.addChild(ifExpr);
    		parent.addChild(trueBlock);
    		stack.push(parent);
    	}
    (
    TK_else block[false]
    	{	
    		child = stack.pop();
    		parent = stack.pop();
    		child.setParent(parent);
    		parent.addChild(child);
    		stack.push(parent);
    	}
    )?
    | tf:TK_for id:ID 
    	{ 
    		syms.beginScope(); 
    		put(id, TK_int);
    	}
    ASSIGN expr COMMA expr block[true]
    	{
    		parent = new IrForStmt(null, tokenToIrId(id));
    		parent.setLineNum(tf.getLine());
    		IrNode b = stack.pop();
    		IrNode endExpr = stack.pop();
    		IrNode initExpr = stack.pop();
    		b.setParent(parent);
    		endExpr.setParent(parent);
    		initExpr.setParent(parent);
    		parent.addChild(initExpr);
    		parent.addChild(endExpr);
    		parent.addChild(b);
    		stack.push(parent);
    		
    		syms.endScope();
    	}
    | t:TK_return 
    	{
    		parent = new IrReturnStmt(null);
    		parent.setLineNum(t.getLine());
    		stack.push(parent);
    	}
    (
    expr
    	{
    		child = stack.pop();
    		parent = stack.pop();
    		child.setParent(parent);
    		parent.addChild(child);
    		stack.push(parent);
    	}
    )? SEMI
    | tb:TK_break SEMI
    	{	
    		parent = new IrBreakStmt(null);
    		parent.setLineNum(tb.getLine());
    		stack.push(parent);
    	}
    | tc:TK_continue SEMI
    	{
    		parent = new IrContinueStmt(null);
    		parent.setLineNum(tc.getLine());
    		stack.push(parent);
    	}
    | block[false]
    	{
    		//child = stack.pop();
    		//parent = stack.pop();
    		//child.setParent(parent);
    		//parent.addChild(child);
    		//stack.push(parent);
    	}
    )
    ;

assign_op
	: 
	(
	a:ASSIGN 
	{	
		parent = new IrAssignStmt(null);
		parent.setLineNum(a.getLine());
	}
	| ma:MINUSASSIGN 
	{
		parent = new IrMinusAssignStmt(null);
		parent.setLineNum(ma.getLine());
	}
	| pa:PLUSASSIGN
	{
		parent = new IrPlusAssignStmt(null);
		parent.setLineNum(pa.getLine());
	}
	)
	{	
    	stack.push(parent);
	}
	;

method_call: 
	(
    id:ID 
    	{
    		parent = new IrMethodCallExpr(null, tokenToIrId(id));
    		parent.setLineNum(id.getLine());
    		stack.push(parent);
    	}
    LPAREN 
    	( 
    	expr 
    		{
    			child = stack.pop();
    			parent = stack.pop();
    			child.setParent(parent);
    			parent.addChild(child);
    			stack.push(parent);
    		}
    		(
    		COMMA 
    		expr
    			{
    				child = stack.pop();
    				parent = stack.pop();
    				child.setParent(parent);
    				parent.addChild(child);
    				stack.push(parent);
    			}
    		)* 
    	)? 
    RPAREN 
    | tc:TK_callout LPAREN string_literal 
    	{	
    		IrStringLiteral sLit = (IrStringLiteral) stack.pop();
    		parent = new IrCalloutExpr(null, sLit.getValue());
    		parent.setLineNum(tc.getLine());
    		stack.push(parent);
    	}
    	(
    	COMMA callout_arg 
    		{
    			child = stack.pop();
    			parent = stack.pop();
    			child.setParent(parent);
    			parent.addChild(child);
    			stack.push(parent);
    		}
    		(
    		COMMA callout_arg
    			{
    				child = stack.pop();
    				parent = stack.pop();
    				child.setParent(parent);
    				parent.addChild(child);
    				stack.push(parent);
    			}
    		)* 
    	)? 
    	RPAREN
    )
    ;

location
	: 
	(
    id:ID
    	{
    		parent = new IrLocationExpr(null, tokenToIrId(id));
    		parent.setLineNum(id.getLine());
    		stack.push(parent);
    	}
    | id2:ID LSQUARE expr RSQUARE
    	{
    		parent = new IrArrayLocationExpr(null, tokenToIrId(id2));
    		parent.setLineNum(id2.getLine());
    		child = stack.pop();
    		child.setParent(parent);
    		parent.addChild(child);
    		stack.push(parent);
    	}
    )
    ;

expr: expr1;
expr1: 
	expr2 
	(or:OR expr2
    	{	
    		IrNode rhs = stack.pop();
    		IrNode lhs = stack.pop();
    		parent = new IrBinopExpr(null, IrOps.OR);
    		parent.setLineNum(or.getLine());
    		lhs.setParent(parent);
    		rhs.setParent(parent);
    		parent.addChild(lhs);
    		parent.addChild(rhs);
    		stack.push(parent);
    	}
    )*
    ;
expr2: 
	expr3 
	(and:AND expr3
		{	
    		IrNode rhs = stack.pop();
    		IrNode lhs = stack.pop();
    		parent = new IrBinopExpr(null, IrOps.AND);
    		parent.setLineNum(and.getLine());
    		lhs.setParent(parent);
    		rhs.setParent(parent);
    		parent.addChild(lhs);
    		parent.addChild(rhs);
    		stack.push(parent);
    	}
    )*
    ;
expr3: 
	expr4 
	(
	eq_op
	expr4
		{	
    		IrNode rhs = stack.pop();
    		parent = stack.pop();
    		IrNode lhs = stack.pop();
    		lhs.setParent(parent);
    		rhs.setParent(parent);
    		parent.addChild(lhs);
    		parent.addChild(rhs);
    		stack.push(parent);
    	}
	)*
	;
expr4: 
	expr5 
	(
	rel_op 
	expr5
		{	
    		IrNode rhs = stack.pop();
    		parent = stack.pop();
    		IrNode lhs = stack.pop();
    		lhs.setParent(parent);
    		rhs.setParent(parent);
    		parent.addChild(lhs);
    		parent.addChild(rhs);
    		stack.push(parent);
    	}
	)*
	;
expr5: 
	{ IrNode tmp = null; }
	expr6 
	( 
		(
		p:PLUS 
			{ 
				tmp = new IrBinopExpr(null, IrOps.PLUS);
				tmp.setLineNum(p.getLine());
			}
		| m:MINUS  
			{ 
				tmp = new IrBinopExpr(null, IrOps.MINUS);
				tmp.setLineNum(m.getLine()); 
			}
		) 
	expr6 
		{	
    		IrNode rhs = stack.pop();
    		IrNode lhs = stack.pop();
    		lhs.setParent(tmp);
    		rhs.setParent(tmp);
    		tmp.addChild(lhs);
    		tmp.addChild(rhs);
    		stack.push(tmp);
    	}
	)*
	;
expr6: 
	{ IrNode tmp = null; }
	expr7 
	( 
		(t:TIMES 
			{ 
				tmp = new IrBinopExpr(null, IrOps.TIMES); 
				tmp.setLineNum(t.getLine());
			}
		| d:DIV 
			{ 
				tmp = new IrBinopExpr(null, IrOps.DIV); 
				tmp.setLineNum(d.getLine());
			}
		| m:MOD 
			{ 
				tmp = new IrBinopExpr(null, IrOps.MOD); 
				tmp.setLineNum(m.getLine());
			}
		) 
	expr7
		{	
    		IrNode rhs = stack.pop();
    		IrNode lhs = stack.pop();
    		lhs.setParent(tmp);
    		rhs.setParent(tmp);
    		tmp.addChild(lhs);
    		tmp.addChild(rhs);
    		stack.push(tmp);
    	}
	)*
	;
expr7:
	n:NOT expr7
		{
			parent = new IrNotExpr(null);
			parent.setLineNum(n.getLine());
			child = stack.pop();
			child.setParent(parent);
			parent.addChild(child);
			stack.push(parent);
		}
	| expr8
	;
expr8:
	(
    location
    | method_call
    | literal
    | LPAREN expr RPAREN
    | m:MINUS expr8
      	{
    		parent = new IrNegativeExpr(null);
    		parent.setLineNum(m.getLine());
    		child = stack.pop();
    		child.setParent(parent);
    		parent.addChild(child);
    		stack.push(parent);
    	}
    )
    ;
    
callout_arg: expr | string_literal;

bin_op: arith_op | rel_op | eq_op | cond_op;

arith_op: PLUS | MINUS | TIMES | DIV | MOD;

rel_op: 
	(
	l:LESS 
		{ 
			parent = new IrBinopExpr(null, IrOps.LESS); 
			parent.setLineNum(l.getLine());
		}
	| le:LESSEQ 
		{ 
			parent = new IrBinopExpr(null, IrOps.LESSEQ); 
			parent.setLineNum(le.getLine());
		}
	| g:GREATER 
		{ 
			parent = new IrBinopExpr(null, IrOps.GREATER); 
			parent.setLineNum(g.getLine());
		}
	| ge:GREATEREQ
		{ 
			parent = new IrBinopExpr(null, IrOps.GREATEREQ); 
			parent.setLineNum(ge.getLine());
		}
	)
	{ stack.push(parent); }
	;

eq_op: 
	(
	e:EQUAL 
		{ 
			parent = new IrBinopExpr(null, IrOps.EQUAL); 
			parent.setLineNum(e.getLine());
		}
	| ne:NOTEQUAL 
		{ 
			parent = new IrBinopExpr(null, IrOps.NOTEQUAL); 
			parent.setLineNum(ne.getLine());
		}
	)
	{ stack.push(parent); }
	;

cond_op: 
	(
	a:AND 
		{ 
			parent = new IrBinopExpr(null, IrOps.AND); 
			parent.setLineNum(a.getLine());
		}
	| o:OR 
		{ 
			parent = new IrBinopExpr(null, IrOps.OR); 
			parent.setLineNum(o.getLine());
		}
	)
	{ stack.push(parent); }
	;

literal
	: 
	{
		String s = "0"; char c = '\0'; boolean b = false;
	}
	(
	s=int_literal { parent = new IrIntLiteral(null, s); }
	| char_literal { parent = new IrCharLiteral(null, '\0'); }
	| b=bool_literal { parent = new IrBooleanLiteral(null, b); }
	)
	{
		stack.push(parent);
	}
	;

//int_literal returns [int n=-99] :
int_literal returns [String s="-99"] : 
    (
        //h:HEX { n = Integer.decode(h.getText()).intValue();	}
        //| d:DECIMAL {n = Integer.parseInt(d.getText());} 
        h:HEX { s = h.getText();	}
        | d:DECIMAL { s = d.getText(); } 
    )
    ;

bool_literal returns [boolean b=false]: 
	TRUE {b=true;}
	| FALSE {b=false;}
	;

char_literal: CHAR;

string_literal: t:STRING { stack.push(new IrStringLiteral(null, t.getText())); } ;

type returns [int t=-99] : TK_int { t = TK_int; } | TK_boolean { t = TK_boolean; };

//array returns [int n=-99] : LSQUARE n=int_literal RSQUARE;
array returns [String s="-99"] : LSQUARE s=int_literal RSQUARE;
