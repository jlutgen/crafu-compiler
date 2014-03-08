package decaf;
//import java.io.*;
import java.util.List;
import java.util.ArrayList;

public class SemanticChecker {
	
	private String filename;
	private SymbolTable syms;
	private boolean debug;

	public SemanticChecker(String filename, boolean debug) {
		this.filename = filename;
		this.syms  = new SymbolTable();
		this.debug = debug;
		if(debug) System.out.println("DEBUGGING");
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
    
	private void put(IrFieldDecl fd) {
		switch (fd.getType()) {
		case Ir.INT:
			syms.put(fd.getId(), new IntEntry());
			break;
		case Ir.BOOL:
			syms.put(fd.getId(), new BoolEntry());
			break;
		case Ir.INTARRAY:
			syms.put(fd.getId(), new IntArrayEntry(((IrArrayFieldDecl) fd).getSize()));
			break;
		case Ir.BOOLARRAY:
			syms.put(fd.getId(), new BoolArrayEntry(((IrArrayFieldDecl) fd).getSize()));
			break;
		default:
			System.out.println("put FieldDecl: this shouldn't happen");
		}
		if (debug) syms.print();
    }
    
	private void put(IrVarDecl vd) {
		switch (vd.getVarType().getTypeCode()) {
		case Ir.INT:
			syms.put(vd.getVarId(), new IntEntry());
			break;
		case Ir.BOOL:
			syms.put(vd.getVarId(), new BoolEntry());
			break;
		default:
			System.out.println("put VarDecl: this shouldn't happen");
		}
		if (debug) syms.print();
    }
	
	private void put(IrMethodArg a) {
		switch (a.getArgType().getTypeCode()) {
		case Ir.INT:
			syms.put(a.getArgId(), new IntEntry());
			break;
		case Ir.BOOL:
			syms.put(a.getArgId(), new BoolEntry());
			break;
		default:
			System.out.println("put MethodArg: this shouldn't happen");
		}
		if (debug) syms.print();
    }
	
	private void putGlobal(IrMethodDecl m, MethodSignature sig) {
		syms.putGlobal(m.getId(), new MethodEntry(sig));
	}
	
//    private void put(Token t, int retType, List<IrType> args) {
//        IrId id = new IrId(t.getText());
//        IrType rt = tkToIrType(retType);
//        MethodSignature sig = new MethodSignature(rt, args);
//        syms.put(id, new MethodEntry(sig));
//        if (debug) syms.print();
//    }
//    
//    private void put(List<IrType> argTypes, List<IrId> argIds) {
//    	int typeCode;
//    	for (int i=0; i<argTypes.size(); i++) {
//    		typeCode = argTypes.get(i).getTypeCode();
//    		if (typeCode == Ir.INT)
//    			syms.put(argIds.get(i), new IntEntry());
//    		else
//    			syms.put(argIds.get(i), new BoolEntry());
//    	}
//    }
	
	public void checkProgram(IrClassDecl classDecl) {
		syms.beginScope();
		int numChildren = classDecl.numChildren();
		boolean hasMain = false;
		if (debug)
			System.out.println(classDecl.getLineNum() + ":" + 
				classDecl + ", numChildren: " + numChildren);
		// check field decls, method decls
		for (int i=0; i<numChildren; i++) {
			IrNode n = classDecl.child(i);
			if (n instanceof IrFieldDecl) {
				checkFieldDecl((IrFieldDecl) n);
			} else if (n instanceof IrMethodDecl) {
				checkMethodDecl((IrMethodDecl) n);
				// check for 0-argument main method
				if (((IrMethodDecl) n).getId().toString().equals("main")
						&& n.numChildren() == 1) {
					hasMain = true;
				}
			} else {
				System.out.println("checkProgram: this shouldn't happen");
			}
			if (n.getType() == Ir.ERROR) {
				classDecl.setType(Ir.ERROR);
			}
		}
		if (!hasMain) {
			error(classDecl, "no zero-argument main() method defined");
		}
		if (classDecl.getType() == Ir.ERROR) {
			System.out.println("FAIL: compilation failed due to one or more semantic errors");
		}
		syms.endScope();
		if (debug) syms.print();
		if (debug) printIr(classDecl);
	}
	
	private void checkFieldDecl(IrFieldDecl fieldDecl) {
		// leaf
		if (debug)
			System.out.println(fieldDecl.getLineNum() + ":" + fieldDecl);
		if (fieldDecl instanceof IrArrayFieldDecl) {
			// check size is positive
			int size = ((IrArrayFieldDecl) fieldDecl).getSize();
			if (size <= 0) {
				fieldDecl.setType(Ir.ERROR);
				error(fieldDecl, "declared array size must be positive");
			}
		}
		if (syms.isInScope(fieldDecl.getId())) {
			error(fieldDecl, "redeclaration of identifier `" + 
						fieldDecl.getId() + "`");
			return;
		}
		int fType = fieldDecl.getFieldType().getTypeCode();
		if (fieldDecl instanceof IrArrayFieldDecl) {
			switch (fType) {
			case Ir.INT:
				fieldDecl.setType(Ir.INTARRAY);
				break;
			case Ir.BOOL:
				fieldDecl.setType(Ir.BOOLARRAY);
				break;
			default:
				System.out.println("checkFieldDecl: this shouldn't happen");
			}
		}
		else {
			fieldDecl.setType(fType);
		}
		put(fieldDecl);
	}
	
	private void checkMethodDecl(IrMethodDecl methodDecl) {
		int numChildren = methodDecl.numChildren();
		int numArgs = numChildren - 1;
		if (debug)
			System.out.println(methodDecl.getLineNum() + ":" + 
				methodDecl + ", numArgs: " + numArgs);
		if (syms.isInScope(methodDecl.getId())) {
			error(methodDecl, "redeclaration of method `" + 
						methodDecl.getId() + "`");
			return;
		}
		syms.beginScope();
		// check args
		List<IrType> argTypes = new ArrayList();
		int aType;
		IrId aId;
		for (int i=0; i<numArgs; i++) {
			IrMethodArg arg = (IrMethodArg) methodDecl.child(i);
			checkMethodArg(arg);
			aId = arg.getArgId();
			aType = arg.getArgType().getTypeCode();
			if (syms.isInScope(aId)) {
				error(methodDecl, "duplicate parameter identifier `" + 
							aId + "`");
			}
			put(arg);
			arg.setType(aType);
			argTypes.add(new IrType(aType));
		}
		IrType rType = methodDecl.getReturnType();
		MethodSignature sig = new MethodSignature(rType, argTypes);
		putGlobal(methodDecl, sig);
		// check block
		IrBlock b = (IrBlock) methodDecl.child(numChildren - 1);
		checkBlock(b);
		if (b.getType() == Ir.ERROR) {
			methodDecl.setType(Ir.ERROR);
		}
		syms.endScope();
	}
	
	private void checkMethodArg(IrMethodArg arg) {
		// leaf
		if (debug)
			System.out.println(arg.getLineNum() + ":" + arg);
	}
	
	private void checkBlock(IrBlock b) {
		if (!(b.parent() instanceof IrMethodDecl) && !(b.parent() instanceof IrForStmt)) {
			syms.beginScope();
		}
		int numChildren = b.numChildren();
		if (debug)
			System.out.println(b.getLineNum() + ":" + b + ", numChildren: " + numChildren);
		// check var decls, statements
		for (int i=0; i<numChildren; i++) {
			IrNode n = b.child(i);
			if (n instanceof IrVarDecl) {
				checkVarDecl((IrVarDecl) n);
			} else if (n instanceof IrStatement) {
				checkStatement((IrStatement) n);
			} else {
				System.out.println("checkBlock: this shouldn't happen");
			}
			if (n.getType() == Ir.ERROR) {
				b.setType(Ir.ERROR);
			}
		}
		if (!(b.parent() instanceof IrMethodDecl) && !(b.parent() instanceof IrForStmt)) {
			syms.endScope();
		}
	}
	
	private void checkVarDecl(IrVarDecl v) {
		// leaf
		if (debug)
			System.out.println(v.getLineNum() + ":" + v);
		if (syms.isInScope(v.getVarId())) {
			error(v, "redeclaration of identifier `" + 
						v.getVarId() + "`");
			return;
		}
		put(v);
		v.setType(v.getVarType().getTypeCode());
		
	}
	
	private void checkStatement(IrStatement s) {
		if (s instanceof IrAssignStmt) {
			checkAssignStmt((IrAssignStmt) s);
		} else if (s instanceof IrPlusAssignStmt) {
			checkPlusAssignStmt((IrPlusAssignStmt) s);
		} else if (s instanceof IrMinusAssignStmt) {
			checkMinusAssignStmt((IrMinusAssignStmt) s);
		} else if (s instanceof IrBreakStmt) {
			checkBreakStmt((IrBreakStmt) s);
		} else if (s instanceof IrIfStmt) {
			checkIfStmt((IrIfStmt) s);
		} else if (s instanceof IrForStmt) {
			checkForStmt((IrForStmt) s);
		} else if (s instanceof IrReturnStmt) {
			checkReturnStmt((IrReturnStmt) s);
		} else if (s instanceof IrContinueStmt) {
			checkContinueStmt((IrContinueStmt) s);
		} else if (s instanceof IrInvokeStmt) {
			checkInvokeStmt((IrInvokeStmt) s);
		}  else if (s instanceof IrBlock) {
			checkBlock((IrBlock) s);
		} else {
			System.out.println("checkStatement: this shouldn't happen");
		}
	}
	
	private void checkAssignStmt(IrAssignStmt s) {
		int numChildren = s.numChildren();
		if (debug)
			System.out.println(s.getLineNum() + ":" + s + ", numChildren: " + numChildren);
		IrLocationExpr loc = (IrLocationExpr) s.child(0);
		IrExpression e = (IrExpression) s.child(1);
		checkLocationExpr(loc);
		checkExpression(e);
		// tree may have changed, so get child again
		e = (IrExpression) s.child(1);
		int locType = loc.getType();
		int eType = e.getType();
		if (locType == Ir.ERROR || eType == Ir.ERROR) {
			s.setType(Ir.ERROR);
			return;
		}
		if (locType != eType) {
			error(s, "cannot assign value of type " + Ir.TYPE[eType] +
					" to variable `" + loc + "` of type " + Ir.TYPE[locType]);
			return;
		}
		s.setType(loc.getType());
	}

	private void checkPlusAssignStmt(IrPlusAssignStmt s) {
		int numChildren = s.numChildren();
		if (debug)
			System.out.println(s.getLineNum() + ":" + s + ", numChildren: " + numChildren);
		IrLocationExpr loc = (IrLocationExpr) s.child(0);
		IrExpression e = (IrExpression) s.child(1);
		checkLocationExpr(loc);
		checkExpression(e);
		// tree may have changed, so get child again
		e = (IrExpression) s.child(1);
		int locType = loc.getType();
		int eType = e.getType();
		if (locType == Ir.ERROR || eType == Ir.ERROR) {
			s.setType(Ir.ERROR);
			return;
		}
		if (locType != Ir.INT || eType != Ir.INT) {
			error(s, "both sides of plus-assignment statement must have int type");
			return;
		}
		s.setType(loc.getType());
	}
	
	private void checkMinusAssignStmt(IrMinusAssignStmt s) {
		int numChildren = s.numChildren();
		if (debug)
			System.out.println(s.getLineNum() + ":" + s + ", numChildren: " + numChildren);
		IrLocationExpr loc = (IrLocationExpr) s.child(0);
		IrExpression e = (IrExpression) s.child(1);
		checkLocationExpr(loc);
		checkExpression(e);
		// tree may have changed, so get child again
		e = (IrExpression) s.child(1);
		int locType = loc.getType();
		int eType = e.getType();
		if (locType == Ir.ERROR || eType == Ir.ERROR) {
			s.setType(Ir.ERROR);
			return;
		}
		if (locType != Ir.INT || eType != Ir.INT) {
			error(s, "both sides of minus-assignment statement must have int type");
			return;
		}
		s.setType(loc.getType());
	}

	private void checkBreakStmt(IrBreakStmt s) {
		// leaf
		if (debug)
			System.out.println(s.getLineNum() + ":" + s);
		IrNode n = s;
		while (n.parent() != null) {
			if (n.parent() instanceof IrForStmt) {
				return;
			}
			n = n.parent();
		}
		error(s, "break statement not enclosed in a for-loop body");
	}

	private void checkIfStmt(IrIfStmt s) {
		int numChildren = s.numChildren();
		if (debug)
			System.out.println(s.getLineNum() + ":" + s + ", numChildren: " + numChildren);
		IrExpression e = (IrExpression) s.child(0);
		checkExpression(e);
		e = (IrExpression) s.child(0);
		if (e.getType() == Ir.ERROR) {
			e.setType(Ir.ERROR);
		}
		else if (e.getType() != Ir.BOOL) {
			error(s, "test in if-statement has type " + Ir.TYPE[e.getType()] + 
					", but must be <boolean>");
		}
		IrBlock b = (IrBlock) s.child(1);
		checkBlock(b);
		if (e.getType() == Ir.ERROR) {
			e.setType(Ir.ERROR);
		}
		if (numChildren == 3) { // has else-block
			b = (IrBlock) s.child(2);
			checkBlock(b);
			if (e.getType() == Ir.ERROR) {
				e.setType(Ir.ERROR);
			}
		}
	}

	private void checkForStmt(IrForStmt s) {
		syms.beginScope();
		int numChildren = s.numChildren();
		if (debug)
			System.out.println(s.getLineNum() + ":" + s + ", numChildren: " + numChildren);
		syms.put(s.getInitId(), new IntEntry());
		if (debug) syms.print();
		IrExpression begin = (IrExpression) s.child(0);
		IrExpression end = (IrExpression) s.child(1);
		IrBlock b = (IrBlock) s.child(2);
		checkExpression (begin);
		checkExpression(end);
		checkBlock(b);
		begin = (IrExpression) s.child(0);
		end = (IrExpression) s.child(1);
		if (begin.getType() == Ir.ERROR || end.getType() == Ir.ERROR
				|| b.getType() == Ir.ERROR) {
			s.setType(Ir.ERROR);
			syms.endScope();
			return;
		}
		if (begin.getType() != Ir.INT || end.getType() != Ir.INT) {
			error(s, "begin/end expressions in for-loop must have type int");
		}
		syms.endScope();
	}

	private void checkReturnStmt(IrReturnStmt s) {
		int numChildren = s.numChildren();
		if (debug)
			System.out.println(s.getLineNum() + ":" + s + ", numChildren: " + numChildren);
		for (int i=0; i<numChildren; i++) {
			IrExpression e = (IrExpression) s.child(i);
			checkExpression(e);
			// tree may have changed, so get child again
			e = (IrExpression) s.child(i);
			if (e.getType() == Ir.ERROR) {
				s.setType(Ir.ERROR);
			}
		}
		IrNode n = s;
		boolean foundMethodDecl = false;
		while (n.parent() != null) {
			if (n.parent() instanceof IrMethodDecl) {
				n = n.parent();
				foundMethodDecl = true;
				break;
			}
			n = n.parent();
		}
		if (!foundMethodDecl) {
			error(s, "return statement not enclosed in a method body");
			return;
		}
		int retType = ((IrMethodDecl) n).getReturnType().getTypeCode();
		if (s.numChildren() == 0 && retType != Ir.VOID) {
			error(s, "must return a value from non-void method " + 
					((IrMethodDecl) n).getId() + "()");
		} 
		if (s.numChildren() == 1) {
			
			int eType = s.child(0).getType();
			IrId mId = ((IrMethodDecl) n).getId();
			if (retType == Ir.VOID) {
				error(s, "cannot return a value from void method " + 
					 mId + "()");
			}
			else if (retType != eType) {
				error(s, "cannot return value of type " + Ir.TYPE[eType] +
					" from method `" + mId + "` declared with return type " + Ir.TYPE[retType]);
			}
		}
	}

	private void checkContinueStmt(IrContinueStmt s) {
		// leaf
		if (debug)
			System.out.println(s.getLineNum() + ":" + s);
		IrNode n = s;
		while (n.parent() != null) {
			if (n.parent() instanceof IrForStmt) {
				return;
			}
			n = n.parent();
		}
		error(s, "continue statement not enclosed in a for-loop body");
	}

	private void checkInvokeStmt(IrInvokeStmt s) {
		int numChildren = s.numChildren();
		if (debug)
			System.out.println(s.getLineNum() + ":" + s + ", numChildren: " + numChildren);
		IrCallExpr e = (IrCallExpr) s.child(0);
		checkExpression(e);
		if (e.getType() == Ir.ERROR) {
			s.setType(Ir.ERROR);
		}
	}

	private void checkExpression(IrExpression e) {
		if (e instanceof IrIntLiteral) {
			checkIntLiteral((IrIntLiteral) e);
		} else if (e instanceof IrBooleanLiteral) {
			checkBooleanLiteral((IrBooleanLiteral) e);
		} else if (e instanceof IrCharLiteral) {
			checkCharLiteral((IrCharLiteral) e);
		} else if (e instanceof IrStringLiteral) {
			checkStringLiteral((IrStringLiteral) e);
		} else if (e instanceof IrMethodCallExpr) {
			checkMethodCallExpr((IrMethodCallExpr) e);
		} else if (e instanceof IrCalloutExpr) {
			checkCalloutExpr((IrCalloutExpr) e);
		} else if (e instanceof IrBinopExpr) {
			checkBinopExpr((IrBinopExpr) e);
		} else if (e instanceof IrNotExpr) {
			checkNotExpr((IrNotExpr) e);
		} else if (e instanceof IrNegativeExpr) {
			checkNegativeExpr((IrNegativeExpr) e);
		} else if (e instanceof IrLocationExpr) {
			checkLocationExpr((IrLocationExpr) e);
		} else {
			System.out.println("checkExpression: this shouldn't happen");
		}
	}

	private void checkLocationExpr(IrLocationExpr loc) {
		int numChildren = loc.numChildren();
		if (debug)
			System.out.println(loc.getLineNum() + ":" + loc + ", numChildren: " + numChildren);
		IrId id = loc.getId();
		if (syms.lookup(id) == null) {
			error(loc, "variable `" + id + "` used before being declared");
			return;
		}
		int type = syms.lookup(id).getType();
		if (loc instanceof IrArrayLocationExpr) {
			// check the expression inside the []
			IrExpression e = (IrExpression) loc.child(0);
			checkExpression(e);
			// tree may have changed so get child again
			e = (IrExpression) loc.child(0);
			if (e.getType() == Ir.ERROR) {
				loc.setType(Ir.ERROR);
			}
			if (e.getType() != Ir.INT) {
				error(loc, "array index expression must have int type");
			}
			if (!Ir.isArrayType(type)) {
				error(loc, "variable `" + id + "` is not of array type");
			}
			if (loc.getType() != Ir.ERROR && Ir.isIntType(type)) {
				loc.setType(Ir.INT);
			} 
			else if (loc.getType() != Ir.ERROR && Ir.isBoolType(type)) {
				loc.setType(Ir.BOOL);
			}
		}
		else if (Ir.isArrayType(type)) {
			error(loc, "array identifier `" + id + "` cannot be used as location");
		}
		else if (loc.getType() != Ir.ERROR) {
			loc.setType(type);
		}
	}

	private void checkIntLiteral(IrIntLiteral e) {
		// leaf
		if (debug)
			System.out.println(e.getLineNum() + ":" + e);
		//if (!(e.parent() instanceof IrNegativeExpr)) {
			// safe to go ahead and set int value, after
			// checking range.
			String valStr = e.getIntString();
			try {
				int val;
				if (valStr.startsWith("0x")) {
					val = Integer.parseInt(valStr.substring(2), 16);
				}
				else if (valStr.startsWith("-0x")) {
					val = Integer.parseInt("-" + valStr.substring(3), 16);
				}
				else {
					val = Integer.parseInt(valStr);
				}
				e.setValue(val);
			} catch (NumberFormatException exc) {
				error(e, "integer literal `" + valStr + "` out of range");
			}
		//}
	}

	private void checkBooleanLiteral(IrBooleanLiteral e) {
		// leaf
		if (debug)
			System.out.println(e.getLineNum() + ":" + e);
	}

	private void checkCharLiteral(IrCharLiteral e) {
		// leaf
		if (debug)
			System.out.println(e.getLineNum() + ":" + e);
	}

	private void checkStringLiteral(IrStringLiteral e) {
		// leaf
		if (debug)
			System.out.println(e.getLineNum() + ":" + e);
	}

	private void checkMethodCallExpr(IrMethodCallExpr e) {
		int numChildren = e.numChildren();
		if (debug)
			System.out.println(e.getLineNum() + ":" + e + ", numChildren: " + numChildren);
		IrId id = e.getId();
		SymbolTableEntry entry = syms.lookup(id);
		if (syms.lookup(id) == null) {
			error(e, "method `" + id + "` called before being declared");
			return;
		}
		if (!(entry instanceof MethodEntry)) {
			error(e, "identifier `" + id + "` in method call is not a method");
			return;
		}
		MethodSignature sig = ((MethodEntry) entry).getSig();
		int retType = sig.getReturnType();
		if (retType == Ir.VOID && !(e.parent() instanceof IrInvokeStmt)) {
			error(e, "void method `" + id + "` cannot be used as expression");
		}
		if (sig.numArgs() != numChildren) {
			error(e, "method `" + id + "` called with " + numChildren + 
					" arguments, but expected " + sig.numArgs() + " arguments");
			return;
		}
		for (int i=0; i<numChildren; i++) {
			IrExpression arg = (IrExpression) e.child(i);
			checkExpression(arg);
			arg = (IrExpression) e.child(i);
			if (arg.getType() == Ir.ERROR) {
				e.setType(Ir.ERROR);
				continue;
			}
		}
		for (int i=0; i<sig.numArgs(); i++) {
			int formalType = sig.getArgType(i);
			int actualType = e.child(i).getType();
			if (formalType != actualType) {
				error(e, "argument " + (i+1) + " of method `" + id + "` has type " 
						+ Ir.TYPE[actualType] +
						" , but expected " + Ir.TYPE[formalType]);
			}
		}
		if (e.getType() == Ir.ERROR) {
			return;
		}
		e.setType(sig.getReturnType());
	}

	private void checkCalloutExpr(IrCalloutExpr e) {
		int numChildren = e.numChildren();
		if (debug)
			System.out.println(e.getLineNum() + ":" + e + ", numChildren: " + numChildren);
		for (int i=0; i<numChildren; i++) {
			IrExpression arg = (IrExpression) e.child(i);
			checkExpression(arg);
			arg = (IrExpression) e.child(i);
			if (arg.getType() == Ir.ERROR) {
				e.setType(Ir.ERROR);
				return;
			}
		}
		e.setType(Ir.INT);
	}

	private void checkBinopExpr(IrBinopExpr e) {
		int numChildren = e.numChildren();
		if (debug)
			System.out.println(e.getLineNum() + ":" + e + ", numChildren: " + numChildren);
		IrExpression lhs = (IrExpression) e.child(0);
		IrExpression rhs = (IrExpression) e.child(1);
		checkExpression(lhs);
		checkExpression(rhs);
		// tree may have changed (see checkNegExpr) so get children again
		lhs = (IrExpression) e.child(0);
		rhs = (IrExpression) e.child(1);
		int lhsT = lhs.getType();
		int rhsT = rhs.getType();
		if (lhsT == Ir.ERROR || rhsT == Ir.ERROR) {
			e.setType(Ir.ERROR);
			return;
		}
		int op = e.getOperator();
		if (IrOps.isArith(op) || IrOps.isRel(op)) {
			if (lhsT == Ir.INT && rhsT == Ir.INT) 
				if (IrOps.isArith(op)){
					e.setType(Ir.INT);
				}
				else {
					e.setType(Ir.BOOL);
				}
			else
				error(e, "both sides of operator " + IrOps.SYM[op] + " must have type int");
		}
		else if (IrOps.isEq(op)) {
			if ((lhsT == Ir.INT && rhsT == Ir.INT) || (lhsT == Ir.BOOL && rhsT == Ir.BOOL))
				e.setType(Ir.BOOL);
			else
				error(e, "both sides of operator " + IrOps.SYM[op] + 
						" must have same type (int or boolean)");
		}
		else if (IrOps.isCond(op)) {
			if (lhsT == Ir.BOOL && rhsT == Ir.BOOL) 
				e.setType(Ir.BOOL);
			else
				error(e, "both sides of operator " + IrOps.SYM[op] + " must have type boolean");
		}
		else 
			System.out.println("checkBinopExpr: this shouldn't happen");
	}

	private void checkNotExpr(IrNotExpr e) {
		int numChildren = e.numChildren();
		if (debug)
			System.out.println(e.getLineNum() + ":" + e + ", numChildren: " + numChildren);
		IrExpression child = (IrExpression) e.child(0);
		checkExpression(child);
		int childT = child.getType();
		if (childT == Ir.ERROR) {
			e.setType(Ir.ERROR);
			return;
		}
		if (childT != Ir.BOOL) {
			error(e, "operand of operator ! must have type boolean");
		}
		else {
			e.setType(Ir.BOOL);
		}
	}

	private void checkNegativeExpr(IrNegativeExpr e) {
		int numChildren = e.numChildren();
		if (debug)
			System.out.println(e.getLineNum() + ":" + e + ", numChildren: " + numChildren);
		IrExpression child = (IrExpression) e.child(0);
		child = (IrExpression) e.child(0);
		int childT = child.getType();
		if (childT == Ir.ERROR) {
			e.setType(Ir.ERROR);
			return;
		}
		if (child instanceof IrIntLiteral) {
			// replace subtree with int literal node having opposite value
			IrNode parent = e.parent();
			String value = ((IrIntLiteral) child).getIntString();
			value = "-" + value;
			IrIntLiteral newChild = new IrIntLiteral(parent, value);
			parent.setChild(parent.indexOf(e), newChild);
			checkIntLiteral(newChild);
			parent.setType(newChild.getType());
		}
		else {
			checkExpression(child);
			// child may have changed
			child = (IrExpression) e.child(0);
			e.setType(child.getType());
		}
	}

	private void error(IrNode n, String message) {
		n.setType(Ir.ERROR);
		System.out.println("ERROR: at " + filename + 
				":" + n.getLineNum() + ": " + message);
	}
	
}