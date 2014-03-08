package decaf;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Set;


public abstract class LowIrNode {
	public List<LowIrNode> children;
	public static int seqNum = 0;
	public String label;
	
	public LowIrNode() {
		this.children = new ArrayList<LowIrNode>();
		label = ".NODE_" + seqNum;
		seqNum++;
	}
}

class SimpleNode extends LowIrNode {
	public LowIrNode end;
	public String code;
	
	public SimpleNode() {
		super();
		end = null;
		code = null;
	}
	
	public String toString() {
		return code;
	}
}

class TFNode extends LowIrNode {
	public LowIrNode t, f;
	public String code;
	
	public TFNode() {
		super();
		t = null;
		f = null;
		code = null;
	}
	
	public String toString() {
		return code;
	}	
}

class Nop extends SimpleNode {
	public Nop() {
		super();
		code = "NOP";
	}
}

class LirField extends LowIrNode {
	public String name;
	public int numSlots;
	
	
	public LirField(String name, int numSlots) {
		super();
		this.name = name;
		this.numSlots = numSlots;
	}
	
	public String toString() {
		return "GLOBAL, " + name + ", " + 8*numSlots;
	}
}

class LirMethod extends LowIrNode {
	public String name;
	//public HashMap<String, Integer> syms;
	public List<String> args;
	
	public LirMethod(String name, List<String> args) {
		super();
		this.name = name;
		this.args = args;
		//this.syms = null;
	}
	
	public String toString() {
		String s = "METHOD " + name;
//		Set<String> keys = syms.keySet();
//        for (String id : keys) {
//            s += "\n\t" + id + " -> " + syms.get(id);
//        }
		for (String arg : args) {
			s += "\n\tPARAM " + arg;
		}
		return s;
	}
}

class LirBlock extends SimpleNode {
	//public HashMap<String, Integer> syms;
	public List<String> vars;
	
	public LirBlock() {
		super();
		this.vars = null;
		//this.syms = null;
	}
	
	public String toString() {
		String s = "BLOCK ";
//		Set<String> keys = syms.keySet();
//        for (String id : keys) {
//        	s += "\n\t" + id + " -> " + syms.get(id);
//        }
		for (String var : vars) {
			s += "\n\tARG " + var;
		}
		return s;
	}
}

class LirAssignStmt extends LowIrNode {
	public String lhs;
	public String rhs;
	public String jumpInstr;
	private static int seqNum = 0;
	
	public LirAssignStmt(String lhs, String rhs, String jumpInstr) {
		super();
		this.lhs = lhs;
		this.rhs = rhs;
		this.jumpInstr = jumpInstr;
	}
	
	public String toString() {
		String s = "ASSIGN STMT";
		if (jumpInstr != null) {
			String trueLabel = ".ASSIGN_TRUE_" + seqNum;
			String doneLabel = ".ASSIGN_DONE_" + seqNum;
			seqNum++;
			s += rhs;
			s += "\n" + jumpInstr + " " + trueLabel;
			s += "\n" + lhs + " = $0";
			s += "\nJMP " + doneLabel; 
			s += "\n" + trueLabel  + ":";
			s += "\n" + lhs + " = $1";
			s += "\n" + doneLabel  + ":";
		}
		else { // rhs was int
			if (rhs == null)
				s += "<RHS EXPR CODE (result in T0)>\n";
			else
				s += rhs;
			s += "\n" + lhs +  " = T0"; 
		}
		return s;
	}
}

class LirPlusAssignStmt extends LowIrNode {
	public String lhs;
	public String rhs;
	
	public LirPlusAssignStmt(String lhs, String rhs) {
		super();
		this.lhs = lhs;
		this.rhs = rhs;
	}
	
	public String toString() {
		String s = "PLUSASSIGN STMT\n";
		if (rhs == null)
			s += "<RHS EXPR CODE (result in T0)>\n";
		else
			s += rhs;
		s += "\n" + lhs + " = " + lhs + " + T0";
		return s;
	}
}

class LirMinusAssignStmt extends LowIrNode {
	public String lhs;
	public String rhs;
	
	public LirMinusAssignStmt(String lhs, String rhs) {
		super();
		this.lhs = lhs;
		this.rhs = rhs;
	}
	
	public String toString() {
		String s = "MINUSASSIGN STMT\n";
		if (rhs == null)
			s += "<RHS EXPR CODE (result in T0)>\n";
		else
			s += rhs;
		s += "\n" + lhs + " = " + lhs + " - T0";
		return s;
	}
}

abstract class LirExpression extends LowIrNode {
	
}
class LirIntLiteral extends LirExpression {
	public String intString;
	public int value;
	
	public LirIntLiteral(String intString, int value) {
		this.intString = intString;
		this.value = value;
	}
	
	public String toString() {
		return "$" + this.intString;
	}
}

