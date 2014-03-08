package decaf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class SymbolTable
{
	private Scope global; // root of tree is global scope
	private Scope curr;

	public SymbolTable() {
		this.global = null;
		this.curr = null;
	}

	public SymbolTableEntry lookup(IrId id) {
		SymbolTableEntry e;
        for (Scope s = curr; s != null; s = s.parent()) {
        	if ((e = s.get(id)) != null)
        		return e;
        }
        return null;
    }

	public boolean isInScope(IrId id) {
		return curr.get(id) != null;		
	}
	
    public void put(IrId id, SymbolTableEntry entry) {
        curr.put(id, entry);
    }

    public void putGlobal(IrId id, SymbolTableEntry entry) {
    	global.put(id, entry);
    }
    
    public void beginScope() {
    	if (global == null) {
    		global = new Scope(null);
    		curr = global;
    	}
    	else {
	    	Scope newChild = new Scope(curr);
	        curr.addChild(newChild);
	        curr = newChild;
    	}
    }

    public void endScope() {
        curr = curr.parent();
    }
    
    public void print() {
    	System.out.println("============ SCOPES ==========================");
    	printR(global, 0);
    }
    private void printR(Scope s, int level) {
    	s.print(level);
    	for (int i=0; i<s.numChildren(); i++)
    		printR(s.child(i), level+1);
    }
}

class Scope {
	private HashMap<IrId, SymbolTableEntry> map;
	private Scope parent;
	private List<Scope> children;
	
	public Scope(Scope parent) {
		this.map = new HashMap<IrId, SymbolTableEntry>();
		this.parent = parent; // null if this is root
		this.children = new ArrayList<Scope>();
	}
	
	public Scope parent() {
		return this.parent;
	}
	
	public int numChildren() {
		return this.children.size();
	}
	
	public Scope child(int i) {
		return this.children.get(i);
	}
	
	public void addChild(Scope child) {
		this.children.add(child);
	}
	
	public void put(IrId id, SymbolTableEntry entry) {
		this.map.put(id, entry);
	}
	
	public SymbolTableEntry get(IrId id) {
		return this.map.get(id);
	}	
	
	public void print(int indentLevel) {
		String indent = "";
		for (int i=0; i<indentLevel; i++) {
			indent += "  ";
		}
		System.out.println(indent + "- - - - - - - - - - - - -");
        Set<IrId> keys = map.keySet();
        for (IrId id : keys) {
            System.out.println(indent + id + " -> " + map.get(id));
        }
    }
}

abstract class SymbolTableEntry
{
	private IrType type;
    private boolean initialized;
    
    public int getType() {
        return this.type.getTypeCode();
    }
    public void setType(int typeCode) {
        this.type = new IrType(typeCode);
    }
    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }
    public boolean isInitialized() {
        return this.initialized;
    }
}
class IntEntry extends SymbolTableEntry
{
    private int value;

    public IntEntry() {
        setType(Ir.INT);
        setInitialized(false);
        this.value = 0;
    }
    public IntEntry(int value) {
        setInitialized(true);
        setType(Ir.INT);
        this.value = value;             
    }
    public int getValue() {
        return this.value;
    }
    public String toString() {
        String s = "INT";
        if (!isInitialized())
            s += " (not initialized)";
        else
            s += ", value="+value;
        return s;
    }
}
class BoolEntry extends SymbolTableEntry
{
    private boolean value;

    public BoolEntry() {
        setType(Ir.BOOL);
        setInitialized(false);
        this.value = false;
    }
    public BoolEntry(boolean value) {
        setType(Ir.BOOL);
        this.value = value;
    }
    public boolean getValue() {
        return this.value;
    }
    public String toString() {
        String s = "BOOL";
        if (!isInitialized())
            s += " (not initialized)";
        else
            s += ", value="+value;
        return s;
    }
}
class IntArrayEntry extends SymbolTableEntry
{
    private List<Integer> list;
    private long declSize = 0;

    public IntArrayEntry(long declSize) {
        setType(Ir.INTARRAY);
        this.declSize = declSize;
        setInitialized(false);
        this.list = null;
    }
    public IntArrayEntry(List<Integer> list) {
        setType(Ir.INTARRAY);
        this.list = new ArrayList<Integer>(list);
    }
    public long getSize() {
        return this.list.size();
    }
    public int getValue(int index) {
        return this.list.get(index).intValue();
    }
    public String toString() {
        String s = "INTARRAY["+declSize+"]";
        if (!isInitialized())
            s += " (not initialized)";
        else
            s += ", value="+list;
        return s;
    }
}
class BoolArrayEntry extends SymbolTableEntry
{
    private List<Boolean> list;
    private long declSize = 0;

    public BoolArrayEntry(long declSize) {
        setType(Ir.BOOLARRAY);
        this.declSize = declSize;
        setInitialized(false);
        this.list = null;
    }
    public BoolArrayEntry(List<Boolean> list) {
        setType(Ir.BOOLARRAY);
        this.list = new ArrayList<Boolean>(list);
    }
    public int getSize() {
        return this.list.size();
    }
    public boolean getValue(int index) {
        return this.list.get(index).booleanValue();
    }
    public String toString() {
        String s = "BOOLARRAY"+declSize+"]";
        if (!isInitialized())
            s += " (not initialized)";
        else
            s += ", value="+list;
        return s;
    }
}
class MethodEntry extends SymbolTableEntry
{
    private MethodSignature sig;

    public MethodEntry(MethodSignature sig) {
        setType(Ir.METHOD);
        this.sig = sig;
    }
    public MethodSignature getSig() {
        return this.sig;
    }
    public String toString() {
        return "METHOD, sig="+sig.toString();
    }
}

class MethodSignature {
    private IrType returnType;
    private List<IrType> argTypes;

    public MethodSignature(IrType returnType, List<IrType> argTypes) {
        this.returnType = returnType;
        this.argTypes = new ArrayList<IrType>(argTypes);
    }
    public IrType returnType() {
        return this.returnType; // void/int/boolean
    }
    public int getReturnType() {
        return this.returnType.getTypeCode();
    }
    public int numArgs() {
        return this.argTypes.size();
    }
    public int getArgType(int index) {
        return this.argTypes.get(index).getTypeCode();
    }
    public String toString() {
        String s = "";
        s += "("+returnType+") (";
        if (!argTypes.isEmpty())
            s += argTypes.get(0).toString();
        for (int i=1; i < argTypes.size(); i++)
            s += ", " + argTypes.get(i).toString();
        s += ")";
        return s;
    }
}