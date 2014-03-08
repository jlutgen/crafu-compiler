package decaf;

import java.util.ArrayList;
import java.util.List;

public abstract class Ir
{
	static final int ERROR = 0;
    static final int VOID = 1;
    static final int BOOL = 2;
    static final int CHAR = 3;
    static final int INT = 4;
    static final int STRING = 5;
    static final int BOOLARRAY = 6;
    static final int INTARRAY = 7;
    static final int METHOD = 8;
    
    static final String[] TYPE = {"<error>", "<void>", "<bool>", "<char>", "<int>",
    	"<string>", "<bool[]>", "<int[]>", "<method>"};
    
    static boolean isArrayType(int type) {
    	return (type == BOOLARRAY || type == INTARRAY);
    }
    
    static boolean isIntType(int type) {
    	return (type == INT || type == INTARRAY);
    }
    
    static boolean isBoolType(int type) {
    	return (type == BOOL || type == BOOLARRAY);
    }
}

abstract class IrOps extends Ir
{
	static final int AND = 0;
	static final int OR = 1;
	static final int PLUS = 2;
	static final int MINUS = 3;
	static final int TIMES = 4;
	static final int DIV = 5;
	static final int MOD = 6;
	static final int LESS = 7;
	static final int LESSEQ = 8;
	static final int GREATER = 9;
	static final int GREATEREQ = 10;
	static final int EQUAL = 11;
	static final int NOTEQUAL = 12;
	static final String[] SYM = {"&&", "||", "+", "-", "*", "/", "%", "<", 
								"<=", ">", ">=", "==", "!="};
	
	static boolean isCond(int op) {
		return (op == AND || op == OR);
	}
	static boolean isArith(int op) {
		return (op == PLUS || op == MINUS || op == TIMES || op == DIV || op == MOD);
	}
	static boolean isRel(int op) {
		return (op == LESS || op == LESSEQ || op == GREATER || op == GREATEREQ);
	}
	static boolean isEq(int op) {
		return (op == EQUAL || op == NOTEQUAL);
	}
}

abstract class IrNode
{
    private IrType type;
    private IrNode parent;
    private List<IrNode> children;
    private int lineNum;

    public IrNode() {
    	setType(Ir.VOID);
    	this.parent = null;
    	this.children = null;
    	this.lineNum = -1;
    }
    public IrNode(IrNode parent) {
    	setType(Ir.VOID);
    	this.parent = parent;
    	this.children = new ArrayList<IrNode>();
    }
    public int getType() {
        return this.type.getTypeCode();
    }
    public void setType(int typeCode) {
        this.type = new IrType(typeCode);
    }
    public void setLineNum(int n) {
    	this.lineNum = n;
    }
    public int getLineNum() {
    	return this.lineNum;
    }
    public IrNode parent() {
    	return this.parent;
    }
    public void setParent(IrNode parent) {
    	this.parent = parent;
    }
    public void setChild(int index, IrNode n) {
    	this.children.set(index, n);
    }
    public int indexOf(IrNode child) {
    	return this.children.indexOf(child);
    }
    public List<IrNode> children() {
    	return this.children;
    }
    public void addChild(IrNode child) {
    	this.children.add(child);
    }
    public int numChildren() {
		return this.children.size();
	}
	
	public IrNode child(int i) {
		return this.children.get(i);
	}
}

abstract class IrExpression extends IrNode
{
	public IrExpression(IrNode parent) {
    	super(parent);
    }
}

abstract class IrLiteral extends IrExpression
{
    public IrLiteral(IrNode parent) {
    	super(parent);
    }
}

class IrIntLiteral extends IrLiteral
{
	// Leaf, type INT
    private final String intString; // "987", "0xbeef"
    private int value; 

    public IrIntLiteral(IrNode parent, String intString) {
    	super(parent);
        this.intString = intString;
        setType(Ir.INT);
        this.value = 0; // will be changed by semantic checker after range check
    }
    public String getIntString() {
        return this.intString;
    }
    
    public void setValue(int value) {
    	this.value = value;
    }
    
    public int getValue() {
    	return this.value;
    }
    
    @Override
	public String toString() {
    	//return "IntLiteral: " + (new Integer(value)).toString();
    	return "IntLiteral: \"" + intString + "\": " + value;
    }
}

class IrCharLiteral extends IrLiteral
{
	// leaf, type CHAR
    private final char value;

    public IrCharLiteral(IrNode parent, char value) {
    	super(parent);
        this.value = value;
        setType(Ir.CHAR);
    }
    public char getValue() {
        return this.value;
    }
}

class IrBooleanLiteral extends IrLiteral
{
	// leaf, type BOOL
    private final boolean value;

    public IrBooleanLiteral(IrNode parent, boolean value) {
    	super(parent);
        this.value = value;
        setType(Ir.BOOL);
    }
    public boolean getValue() {
        return this.value;
    }
    @Override
	public String toString() {
    	return "BooleanLiteral: " + (new Boolean(value)).toString();
    }
}

class IrStringLiteral extends IrLiteral
{
	// leaf, type STRING
    private final String value;

    public IrStringLiteral(IrNode parent, String value) {
    	super(parent);
        this.value = value;
        setType(Ir.STRING);
    }
    public String getValue() {
        return this.value;
    }
    @Override
	public String toString() {
    	return "StringLiteral: " + value;
    }
}

abstract class IrCallExpr extends IrExpression
{
	public IrCallExpr(IrNode parent) {
    	super(parent);
    }
}

class IrMethodCallExpr extends IrCallExpr
{
	// type INT, BOOL, or ERROR
	// children: IrExpression_1, IrExpression_2, ...
    private final IrId id;
   
    public IrMethodCallExpr(IrNode parent, IrId id) {
    	super(parent);
        this.id = id;
    }
    public IrId getId() {
        return this.id;
    }
    @Override
	public String toString() {
		return "MethodCallExpr: " + id.toString();
	}
}

class IrCalloutExpr extends IrCallExpr
{
	// type INT or ERROR
	// children: arg1, arg2, ... (IrExpression or IrStringLiteral)
    private final String callout;
   
    public IrCalloutExpr(IrNode parent, String callout) {
    	super(parent);
        this.callout = callout;
    }
    public String getCallout() {
        return this.callout;
    }
    @Override
	public String toString() {
		return "CalloutExpr: " + callout;
	}
}

class IrBinopExpr extends IrExpression
{
	// type INT, BOOL, or ERROR
	// children: IrExpression_L, IrExpression_R
    private final int operator;

    public IrBinopExpr(IrNode parent, int operator) {
        super(parent);
    	this.operator = operator;
    }
    public int getOperator() {
        return this.operator;
    }
    @Override
	public String toString() {
		return "BinopExpr: " + IrOps.SYM[operator];
	}
}

class IrNotExpr extends IrExpression
{
	// type BOOL or ERROR
    // child: IrExpression
    public IrNotExpr(IrNode parent) {
    	super(parent);
    }
    @Override
	public String toString() {
		return "NotExpr";
	}
}

class IrNegativeExpr extends IrExpression
{
	// type INT or ERROR
	// child: IrExpression
    public IrNegativeExpr(IrNode parent) {
    	super(parent);
    }
    @Override
	public String toString() {
		return "NegativeExpr (" + Ir.TYPE[this.getType()] + ")" ;
	}
}

class IrLocationExpr extends IrExpression
{
	// type INT, BOOL, or ERROR
    private IrId id;
    
    public IrLocationExpr(IrNode parent, IrId id) {
    	super(parent);
    	this.id = id;
    }
    public IrId getId() {
        return this.id;
    }
    @Override
	public String toString() {
		return "LocationExpr: " + id.toString();
	}
}

class IrArrayLocationExpr extends IrLocationExpr
{               
	// type INT, BOOL, or ERROR
    // inherits id, getId()
	// child: IrExpression (index)
    public IrArrayLocationExpr(IrNode parent, IrId id) {
    	super(parent, id);
    }
    @Override
	public String toString() {
		return "ArrayLocationExpr: " + getId().toString() + "[...]";
	}
}

abstract class IrStatement extends IrNode
{
	// type INT, BOOL, VOID, or ERROR
	public IrStatement(IrNode parent) {
    	super(parent);
    }
}

class IrAssignStmt extends IrStatement
{
	// type INT, BOOL, or ERROR
	// children: lhs=IrLocation, rhs=IrExpression
    public IrAssignStmt(IrNode parent) {
    	super(parent);
    }
    @Override
	public String toString() {
		return "AssignStmt";
	}
}

class IrPlusAssignStmt extends IrStatement
{
	// type INT or ERROR
	// children: lhs=IrLocation, rhs=IrExpression
    public IrPlusAssignStmt(IrNode parent) {
    	super(parent);
    }
    @Override
	public String toString() {
		return "IrPlusAssignStmt";
	}
}

class IrMinusAssignStmt extends IrStatement
{
	// type INT or ERROR
	// children: lhs=IrLocation, rhs=IrExpression
    public IrMinusAssignStmt(IrNode parent) {
        super(parent);
    }
    @Override
	public String toString() {
		return "IrMinusAssignStmt";
	}
}

class IrBreakStmt extends IrStatement
{
	// leaf
	// type VOID or ERROR
	public IrBreakStmt(IrNode parent) {
    	super(parent);
    }
	@Override
	public String toString() {
		return "BreakStmt";
	}
}

class IrIfStmt extends IrStatement
{
	// type VOID or ERROR
	// children: IrExpr, IrBlock, IrBlock? (optional else-block)
    public IrIfStmt(IrNode parent) {
        super(parent);      
    }
   
    @Override
	public String toString() {
		return "IfStmt";
	}
}

class IrForStmt extends IrStatement
{	
	// type VOID or ERROR
	// children IrExpression (init expr), IrExpression (end expr), IrBlock
    private final IrId initId;
    
    public IrForStmt(IrNode parent, IrId initId) {
        super(parent);
    	this.initId = initId;
    }
    public IrId getInitId() {
        return this.initId;
    }
    @Override
	public String toString() {
		return "ForStmt";
	}
}

class IrReturnStmt extends IrStatement
{
	// type VOID, INT, BOOL, or ERROR
	// child: IrExpression? 
    public IrReturnStmt(IrNode parent) {
    	super(parent);
    }
    @Override
	public String toString() {
		return "ReturnStmt";
	}
}

class IrContinueStmt extends IrStatement
{
	// leaf
	// type VOID or ERROR
	public IrContinueStmt(IrNode parent) {
    	super(parent);
    }
	@Override
	public String toString() {
		return "ContinueStmt";
	}
}

class IrInvokeStmt extends IrStatement
{
	// type VOID or ERROR
	// child: IrCallExpr (IrMethodCallExpr or IrCalloutExpr)

    public IrInvokeStmt(IrNode parent) {
        super(parent);
    }
    @Override
	public String toString() {
		return "InvokeStmt";
	}
}

class IrBlock extends IrStatement
{
	// type VOID or ERROR
	// children: IrVarDecl's, IrStatement's 
    public IrBlock(IrNode parent) {
        super(parent);
    }
    @Override
	public String toString() {
		return "Block";
	}
}

class IrClassDecl extends IrNode
{
	// type VOID or ERROR
	// children: FieldDecl's, MethodDecl's
	public IrClassDecl(IrNode parent) {
    	super(parent);
    }
	@Override
	public String toString() {
		return "ClassDecl: Program";
	}
}

abstract class IrMemberDecl extends IrNode
{
	public IrMemberDecl(IrNode parent) {
    	super(parent);
    }
}

class IrFieldDecl extends IrMemberDecl
{	
	// leaf, type INT, BOOL, or ERROR
	private final IrType 	fieldType; // INT or BOOL
    private final IrId  	id;

    public IrFieldDecl(IrNode parent, IrType fieldType, IrId id) {
    	super(parent);
    	this.fieldType = fieldType;
    	this.id = id;
    }
    public IrType getFieldType() {
    	return this.fieldType;
    }
    public IrId getId() {
        return this.id;
    }
    @Override
	public String toString() {
		return "FieldDecl: " + fieldType.toString() + " " + id.toString();
	}
}

class IrArrayFieldDecl extends IrFieldDecl
{
	// leaf, type INTARRAY, BOOLARRAY, or ERROR (for case when size <= 0)
    // inherits getId, getFieldType
    private final int size;

    public IrArrayFieldDecl(IrNode parent, IrType fieldType, IrId id, int size) {
        super(parent, fieldType, id);
        this.size = size;
    }
    public int getSize() {
        return this.size;
    }
    @Override
    public String toString() {
		return "FieldDecl: " + getFieldType().toString() + " " + getId().toString()
				+ "[" + size + "]";
	}
}

class IrMethodDecl extends IrMemberDecl
{
	// type VOID or ERROR
	// children: IrMethodArg_1, IrMethodArg_2, ... , IrMethodArg_N, IrBlock
    private final IrType            returnType; // allow for void
    private final IrId              id;

    public IrMethodDecl(IrNode parent, IrType returnType, IrId id) {
    	super(parent);
    	this.returnType = returnType;
    	this.id = id;
    }
    public IrId getId() {
    	return this.id;
    }
    public IrType getReturnType() {
    	return this.returnType;
    }
    @Override
	public String toString() {
    	return "MethodDecl: " + returnType.toString() + " " + id.toString();
    }
}

class IrMethodArg extends IrNode {
	// leaf, type INT or BOOLEAN
    private final IrType    argType;
    private final IrId      argId;

    public IrMethodArg(IrNode parent, IrType argType, IrId argId) {
    	super(parent);
        this.argType = argType;
        this.argId = argId;
    }
    public IrType getArgType() {
    	return this.argType;
    }
    public IrId getArgId() {
        return this.argId;
    }
    @Override
	public String toString() {
    	return "MethodArg: " + argType.toString() + " " + argId.toString();
    }
}

class IrVarDecl extends IrNode
{
	// leaf, type INT or BOOLEAN	
	// exactly like an IrMethodArg
	private final IrType    varType;
    private final IrId      varId;

    public IrVarDecl(IrNode parent, IrType varType, IrId varId) {
    	super(parent);
        this.varType = varType;
        this.varId = varId;
    }
    public IrType getVarType() {
    	return this.varType;
    }
    public IrId getVarId() {
    	return this.varId;
    }
    @Override
	public String toString() {
    	return "VarDecl: " + varType.toString() + " " + varId.toString();
    }
}

// ------------------------ NON-NODE TYPES ---------------------
class IrType extends Ir
{	// not a tree node
	
    private int typeCode;

    public IrType(int typeCode) {
        this.typeCode = typeCode;
    }
    public int getTypeCode() {
        return this.typeCode;
    }
    @Override
	public String toString() {
        String s;
        switch (typeCode) {
            case Ir.VOID:
                s = "VOID";  
                break;
            case Ir.INT:
                s = "INT";  
                break;
            case Ir.BOOL:
                s = "BOOL";
                break;
            case Ir.INTARRAY:
                s = "INTARRAY";
                break;
            case Ir.BOOLARRAY:
                s = "BOOLARRAY";
                break;
            case Ir.METHOD:
                s = "METHOD";
                break;
            default:
                s = "< UNKNOWN TYPE >";
        }
        return s;
    }
}
class IrId extends Ir 
{
	// not a tree node
	
    private String id;

    public IrId(String id) {
        this.id = id;
    }
    public String getIdString() {
        return this.id;
    }
    public void setId(String id) {
        this.id = id;
    }
    @Override
    public String toString() {
        return this.id;
    }
    @Override
    public boolean equals(Object other) {
        if (other instanceof IrId) {
            IrId otherId = (IrId) other;
            return this.id.equals(otherId.id);
        }
        return false;
    }
    @Override
    public int hashCode() {
        return this.id.hashCode();
    }
}

