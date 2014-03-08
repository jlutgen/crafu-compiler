package decaf;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Set;
import java.util.Deque;
import java.util.ArrayDeque;

public class CodeGen {
	
	private IrNode highIr; // root of high-order internal representation tree
	private boolean debug;
	private List<LowIrNode> lowIr;
	//private PrintWriter out;
	
	public CodeGen(IrNode highIr, boolean debug) {
		this.highIr = highIr;
		this.debug = debug;
		this.lowIr = new ArrayList<LowIrNode>();
		//this.out = new PrintWriter(new FileWriter("/tmp/codegen.out"))); 
	}
	
	public void printLowIr() {
		for (int i=0; i<lowIr.size(); i++)
			printLirTree(lowIr.get(i));
	}
	
	public void printLirTree(LowIrNode n) {
		if (n == null) {
			System.out.println("<null>");
			return;
		}
		System.out.println(n);
		for (int i=0; i<n.children.size(); i++) {
			printLirTree(n.children.get(i));
		}
	}
	public void genLowIr() {
		for (int i=0; i<highIr.numChildren(); i++) {
			IrNode n = highIr.child(i);
			if (n instanceof IrFieldDecl) {
				lowIr.add(genField((IrFieldDecl) n));
			}
			if (n instanceof IrMethodDecl) {
				lowIr.add(genMethod((IrMethodDecl) n));
			}
		}
	}
	
	private String getJump(int op, boolean tf) {
		// jump instructions corresponding to rel/eq ops in IrOps
		String[] trueJump = {"X", "X", "X", "X", "X", "X", "X", 
				"JL", "JLE", "JG", "JGE", "JE", "JNE"};
		String[] falseJump = {"X", "X", "X", "X", "X", "X", "X", 
				"JNL", "JNLE", "JNG", "JNGE", "JNE", "JE"};
		if (tf)
			return trueJump[op];
		return falseJump[op];
	}
	
	private LowIrNode genField(IrFieldDecl n) {
		if (n instanceof IrArrayFieldDecl) {
			return new LirField(n.getId().getIdString(), 
									((IrArrayFieldDecl) n).getSize());
		}
		else {
			return new LirField(n.getId().getIdString(), 1);
		}
	}
	
	private LowIrNode genMethod(IrMethodDecl n) {	
		int numParams = n.numChildren() - 1;
		//Deque<HashMap<String, Integer>> symStack = 
		//			new ArrayDeque<HashMap<String, Integer>>();
		//HashMap<String, Integer> syms = new HashMap<String, Integer>();
		List<String> args = new ArrayList<String>();
		for (int i=0; i<numParams; i++) {
			String id = ((IrMethodArg) n.child(i)).getArgId().getIdString();
			args.add(id);
			//syms.put(id, syms.size()+1);
		}
		
		//symStack.push(syms);
		LowIrNode method = new LirMethod(n.getId().getIdString(), args);
		//((LirMethod) method).syms = syms;
		LowIrNode block = genBlock((IrBlock) n.child(n.numChildren()-1));
		method.children.add(block);
		
		return method;
		
	}
	
	private LowIrNode genBlock(IrBlock n) {
		//int curr = syms.size() + 1;
		//HashMap<String, Integer> newSyms = new HashMap<String, Integer>(syms); // copy
		List<String> vars = new ArrayList<String>();
		LirBlock b = new LirBlock();
		for (int i=0; i<n.numChildren(); i++) {
			if (n.child(i) instanceof IrVarDecl) {
				String id = ((IrVarDecl) n.child(i)).getVarId().getIdString();
				vars.add(id);
				//newSyms.put(id, curr);
				//curr++;
			}
			if (n.child(i) instanceof IrStatement) {
				//LowIrNode child = genStatement((IrStatement) n.child(i), newSyms);
				LowIrNode child = genStatement((IrStatement) n.child(i));
				b.children.add(child);
			}
		}
		b.vars = vars;
		//((LirBlock) b).syms = newSyms;
		return (LowIrNode) b;
	}
	
	private LowIrNode genStatement(IrStatement s) {
		if (s instanceof IrAssignStmt) {
			return genAssignStmt((IrAssignStmt) s);
		} 
		else if (s instanceof IrPlusAssignStmt) {
			return genPlusAssignStmt((IrPlusAssignStmt) s);
		} 
		else if (s instanceof IrMinusAssignStmt) {
			return genMinusAssignStmt((IrMinusAssignStmt) s);
		} 
//			else if (s instanceof IrBreakStmt) {
//			return genBreakStmt((IrBreakStmt) s);
//		} 
		else if (s instanceof IrIfStmt) {
			return genIfStmt((IrIfStmt) s);
		} 
//		else if (s instanceof IrForStmt) {
//			return genForStmt((IrForStmt) s);
//		} else if (s instanceof IrReturnStmt) {
//			return genReturnStmt((IrReturnStmt) s);
//		} else if (s instanceof IrContinueStmt) {
//			return genContinueStmt((IrContinueStmt) s);
//		} else if (s instanceof IrInvokeStmt) {
//			return genInvokeStmt((IrInvokeStmt) s);
//		}
		else if (s instanceof IrBlock) {
			return genBlock((IrBlock) s);
		} 
		else {
			System.out.println("genStatement: skipping");
			return null;
		}
	}

	private LowIrNode genAssignStmt(IrAssignStmt s) {
		IrLocationExpr lhs = (IrLocationExpr) s.child(0);
		IrExpression rhs = (IrExpression) s.child(1);
		String loc = lhs.getId().getIdString();
//		String tLoc;
		String jump = null;
		// TODO: deal with array location, deal with boolean type
//		if (syms.get(loc) == null) 
//			tLoc = "GLOBAL_" + loc;
//		else
//			tLoc = "LOCAL_" + syms.get(loc);
		String exp = genExpression(rhs, 0);
		if (rhs.getType() == Ir.BOOL) {
			if (rhs instanceof IrBinopExpr) {
				int op = ((IrBinopExpr) rhs).getOperator();
				if (IrOps.isRel(op) || IrOps.isEq(op)) {
					jump = getJump(op, true);
				}
			}
		}
		return new LirAssignStmt(loc, exp, jump);
	}
	
	private LowIrNode genPlusAssignStmt(IrPlusAssignStmt s) {
		IrLocationExpr lhs = (IrLocationExpr) s.child(0);
		IrExpression rhs = (IrExpression) s.child(1);
		String loc = lhs.getId().getIdString();
//		String tLoc = "LOC_" + syms.get(loc);
		String exp = genExpression(rhs, 0);
		return new LirPlusAssignStmt(loc, exp);
	}
	
	private LowIrNode genMinusAssignStmt(IrMinusAssignStmt s) {
		IrLocationExpr lhs = (IrLocationExpr) s.child(0);
		IrExpression rhs = (IrExpression) s.child(1);
		String loc = lhs.getId().getIdString();
//		String tLoc = "LOC_" + syms.get(loc);
		String exprCode = genExpression(rhs, 0);
		return new LirMinusAssignStmt(loc, exprCode);
	}
	
	private LowIrNode genIfStmt(IrIfStmt s) {
		IrExpr c = (IrExpr) s.child(0);
		IrBlock trueB = (IrBlock) s.child(1);
		IrBlock falseB = null;
		if (s.numChildren() == 3) {
			falseB = (IrBlock) s.child(2);
		}
		LirBlock tb = (LirBlock) genBlock(trueB);
		LirBlock fb;
		if (falseB == null) {
			fb = new Nop();
		}
		else {
			fb = (LirBlock) genBlock(falseB);
		}
		Nop e = new Nop();
		tb.next = e;
		fb.next = e;
		
		
	}
	
	private String genExpression(IrExpression e, int tempNum) {
		String result = null;
		if (e instanceof IrIntLiteral) {
					result = "\nT" + tempNum + " = $" + ((IrIntLiteral) e).getValue();		
		} 
		else if (e instanceof IrBooleanLiteral) {
			result = genBooleanLiteral((IrBooleanLiteral) e, tempNum);
		} 
//		else if (e instanceof IrCharLiteral) {
//			genCharLiteral((IrCharLiteral) e);
//		} 
//		else if (e instanceof IrStringLiteral) {
//			genStringLiteral((IrStringLiteral) e);
//		} 
//		else if (e instanceof IrMethodCallExpr) {
//			genMethodCallExpr((IrMethodCallExpr) e, syms);
//		} 
//		else if (e instanceof IrCalloutExpr) {
//			genCalloutExpr((IrCalloutExpr) e, syms);
//		} 
		else if (e instanceof IrBinopExpr) {
			int op = ((IrBinopExpr) e).getOperator();
			if (IrOps.isArith(op)) {
				result = genArithBinopExpr((IrBinopExpr) e, tempNum);
			}
			if (IrOps.isRel(op) || IrOps.isEq(op)) {
				result = genRelBinopExpr((IrBinopExpr) e, tempNum);
			}
		} 
		else if (e instanceof IrNotExpr) {
			result = genNotExpr((IrNotExpr) e, tempNum);
		} 
		else if (e instanceof IrNegativeExpr) {
			result = genNegativeExpr((IrNegativeExpr) e, tempNum);
		}
//		else if (e instanceof IrArrayLocationExpr) {
//			genArrayLocationExpr((IrArrayLocationExpr) e, syms);
//		}
		else if (e instanceof IrLocationExpr) {
			result = genLocationExpr((IrLocationExpr) e, tempNum);
		} 
		else {
			System.out.println("genExpression: this shouldn't happen");
		}
		return result;
	}
	
	private String genBooleanLiteral(IrBooleanLiteral e, int tempNum) {
		String result = "";
		if (e.getValue())
			result += "\nT" + tempNum + "= $1";
		else
			result += "\nT" + tempNum + "= $0";
		return result;
	}

	private String genArithBinopExpr(IrBinopExpr e, int tempNum) {
		// generate code to put value in TtempNum
		String result; 
		IrExpression lhs = (IrExpression) e.child(0);
		IrExpression rhs = (IrExpression) e.child(1);
		result = genExpression(lhs, tempNum + 1);
		result += genExpression(rhs, tempNum + 2);
		int op = e.getOperator();
		result += "\n";
		result += "T" + tempNum + " = ";
		result += "T" + (tempNum + 1);
		result += " "+ IrOps.SYM[op] + " ";
		result += "T" + (tempNum + 2);
		return result;
	}

	private String genRelBinopExpr(IrBinopExpr e, int tempNum) {
		String r0 = "T" + tempNum;
		String r1 = "T" + (tempNum + 1);
		IrExpression lhs = (IrExpression) e.child(0);
		IrExpression rhs = (IrExpression) e.child(1);
		int op = e.getOperator();
		String result = genExpression(lhs, tempNum);
		result += genExpression(rhs, tempNum + 1);
		result += "\nCMP " + r0 + ", " + r1;
		return result;
	}

	private String genNotExpr(IrNotExpr e, int tempNum) {
		IrExpression child = (IrExpression) e.child(0);
		String result = genExpression(child, tempNum);
		return result; // TODO: this is wrong
	}

	private String genNegativeExpr(IrNegativeExpr e, int tempNum) {
		IrExpression child = (IrExpression) e.child(0);
		String result = genExpression(child, tempNum);
		// T0 has child
		// T1 = $-1
		// T0 = T1 * T0
		result += "\nT" + (tempNum + 1) + " = $-1";
		result += "\nT" + tempNum + " = T" + (tempNum + 1) + " * T" + tempNum;
		return result;
	}

	private String genLocationExpr(IrLocationExpr e, int tempNum) {
		String loc = e.getId().getIdString();
//		String tLoc;
//		if (syms.get(loc) == null) 
//			tLoc = "GLOBAL_" + loc;
//		else
//			tLoc = "LOCAL_" + syms.get(loc);
		return "\nT" + tempNum + " = " + loc;
	}
	
//	private String genAndExpr(IrBinopExpr e, int tempNum, 
//								String trueLabel, String falseLabel) {
//		IrExpression lhs = (IrExpression) e.child(0);
//		IrExpression rhs = (IrExpression) e.child(1);
//		String result = "";
//		String trueLabel2 = trueLabel + "_x";
//		
//		
//	}
}