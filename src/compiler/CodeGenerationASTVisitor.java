package compiler;

import compiler.AST.*;
import compiler.lib.*;
import compiler.exc.*;
import svm.ExecuteVM;

import java.util.ArrayList;
import java.util.List;

import static compiler.lib.FOOLlib.*;

public class CodeGenerationASTVisitor extends BaseASTVisitor<String, VoidException> {

	private List<List<String>> dispatchTables = new ArrayList<>();
  	CodeGenerationASTVisitor() {}
  	CodeGenerationASTVisitor(boolean debug) {super(false,debug);} //enables print for debugging

	@Override
	public String visitNode(ProgLetInNode n) {
		if (print) printNode(n);
		String declCode = null;
		for (Node dec : n.declist) declCode=nlJoin(declCode,visit(dec));
		return nlJoin(
			"push 0",
			declCode, // generate code for declarations (allocation)
			visit(n.exp),
			"halt",
			getCode()
		);
	}

	@Override
	public String visitNode(ProgNode n) {
		if (print) printNode(n);
		return nlJoin(
			visit(n.exp),
			"halt"
		);
	}

	@Override
	public String visitNode(FunNode n) {
		if (print) printNode(n,n.id);
		String declCode = null, popDecl = null, popParl = null;
		for (Node dec : n.declist) {
			declCode = nlJoin(declCode,visit(dec));
			popDecl = nlJoin(popDecl,"pop");
		}
		for (int i=0;i<n.parlist.size();i++) popParl = nlJoin(popParl,"pop");
		String funl = freshFunLabel();
		putCode(
			nlJoin(
				funl+":",
				"cfp", // set $fp to $sp value
				"lra", // load $ra value
				declCode, // generate code for local declarations (they use the new $fp!!!)
				visit(n.exp), // generate code for function body expression
				"stm", // set $tm to popped value (function result)
				popDecl, // remove local declarations from stack
				"sra", // set $ra to popped value
				"pop", // remove Access Link from stack
				popParl, // remove parameters from stack
				"sfp", // set $fp to popped value (Control Link)
				"ltm", // load $tm value (function result)
				"lra", // load $ra value
				"js"  // jump to to popped address
			)
		);
		return "push "+funl;
	}

	@Override
	public String visitNode(VarNode n) {
		if (print) printNode(n,n.id);
		return visit(n.exp);
	}

	@Override
	public String visitNode(PrintNode n) {
		if (print) printNode(n);
		return nlJoin(
			visit(n.exp),
			"print"
		);
	}

	@Override
	public String visitNode(IfNode n) {
		if (print) printNode(n);
	 	String l1 = freshLabel();
	 	String l2 = freshLabel();
		return nlJoin(
			visit(n.cond),
			"push 1",
			"beq "+l1,
			visit(n.el),
			"b "+l2,
			l1+":",
			visit(n.th),
			l2+":"
		);
	}

	@Override
	public String visitNode(EqualNode n) {
		if (print) printNode(n);
	 	String l1 = freshLabel();
	 	String l2 = freshLabel();
		return nlJoin(
			visit(n.left),
			visit(n.right),
			"beq "+l1,
			"push 0",
			"b "+l2,
			l1+":",
			"push 1",
			l2+":"
		);
	}

	@Override
	public String visitNode(GreaterEqualNode n) throws VoidException {
		if (print) printNode(n);
		String l1 = freshLabel();
		String l2 = freshLabel();
		return nlJoin(
				visit(n.right),
				visit(n.left),
				"bleq "+l1,
				"push 0",
				"b "+l2,
				l1+":",
				"push 1",
				l2+":"
		);
	}

	@Override
	public String visitNode(LessEqualNode n) throws VoidException {
		if (print) printNode(n);
		String l1 = freshLabel();
		String l2 = freshLabel();
		return nlJoin(
				visit(n.left),
				visit(n.right),
				"bleq "+l1,
				"push 0",
				"b "+l2,
				l1+":",
				"push 1",
				l2+":"
		);
	}

	@Override
	public String visitNode(AndNode n) throws VoidException {
		if (print) printNode(n);
		String l1 = freshLabel();
		String l2 = freshLabel();
		return nlJoin(
				visit(n.left),
				visit(n.right),
				"mult"
		);
	}

	@Override
	public String visitNode(OrNode n) throws VoidException {
		if (print) printNode(n);
		String l1 = freshLabel();
		String l2 = freshLabel();
		return nlJoin(
				visit(n.left),
				visit(n.right),
				"add",
				"push 0",
				"beq " + l1,	// entrambi falsi

				"push 1",		// almeno uno dei due era un vero
				"b "+l2,

				l1+":",
				"push 0",

				l2+":"
		);
	}

	@Override
	public String visitNode(NotNode n) throws VoidException {
		return nlJoin(
				visit(n.exp),
				"push -1",
				"mult",
				"push 1",
				"add"
		);
	}

	@Override
	public String visitNode(TimesNode n) {
		if (print) printNode(n);
		return nlJoin(
			visit(n.left),
			visit(n.right),
			"mult"
		);
	}

	@Override
	public String visitNode(DivNode n) throws VoidException {
		if (print) printNode(n);
		return nlJoin(
				visit(n.left),
				visit(n.right),
				"div"
		);
	}

	@Override
	public String visitNode(PlusNode n) {
		if (print) printNode(n);
		return nlJoin(
			visit(n.left),
			visit(n.right),
			"add"
		);
	}

	@Override
	public String visitNode(MinusNode n) throws VoidException {
		if (print) printNode(n);
		return nlJoin(
				visit(n.left),
				visit(n.right),
				"sub"
		);
	}

	@Override
	public String visitNode(CallNode n) {
		if (print) printNode(n,n.id);
		String argCode = null, getAR = null;
		for (int i=n.arglist.size()-1;i>=0;i--) argCode=nlJoin(argCode,visit(n.arglist.get(i)));
		for (int i = 0;i<n.nl-n.entry.nl;i++) getAR=nlJoin(getAR,"lw");

		if (n.entry.type instanceof MethodTypeNode tt) {

			return nlJoin(
					"lfp", // load Control Link (pointer to frame of function "id" caller)
					argCode, // generate code for argument expressions in reversed order
					"lfp", getAR,
					"stm", // set $tm to popped value (with the aim of duplicating top of stack)
					"ltm", // load Access Link (pointer to frame of function "id" declaration)
					"ltm", // duplicate top of stack

					"lw",	// dispatch pointer

					"push " + n.entry.offset, "add", // compute address of "id" declaration from dispatch table
					"lw", // load address of "id" function
					"js"  // jump to popped address (saving address of subsequent instruction in $ra)
			);

		} else {
			return nlJoin(
					"lfp", // load Control Link (pointer to frame of function "id" caller)
					argCode, // generate code for argument expressions in reversed order
					"lfp", getAR, // retrieve address of frame containing "id" declaration
					// by following the static chain (of Access Links)
					"stm", // set $tm to popped value (with the aim of duplicating top of stack)
					"ltm", // load Access Link (pointer to frame of function "id" declaration)
					"ltm", // duplicate top of stack
					"push " + n.entry.offset, "add", // compute address of "id" declaration
					"lw", // load address of "id" function
					"js"  // jump to popped address (saving address of subsequent instruction in $ra)
			);
		}
	}

	@Override
	public String visitNode(IdNode n) {
		if (print) printNode(n,n.id);
		String getAR = null;
		for (int i = 0;i<n.nl-n.entry.nl;i++) getAR=nlJoin(getAR,"lw");
		return nlJoin(
			"lfp", getAR, // retrieve address of frame containing "id" declaration
			              // by following the static chain (of Access Links)
			"push "+n.entry.offset, "add", // compute address of "id" declaration
			"lw" // load value of "id" variable
		);
	}

	@Override
	public String visitNode(BoolNode n) {
		if (print) printNode(n,n.val.toString());
		return "push "+(n.val?1:0);
	}

	@Override
	public String visitNode(IntNode n) {
		if (print) printNode(n,n.val.toString());
		return "push "+n.val;
	}

	// OBJECT-ORIENTED EXTENSION


	@Override
	public String visitNode(ClassNode n) throws VoidException {
		if (print) printNode(n,n.id);

		var dTable = new ArrayList<String>();
		dispatchTables.add(dTable);

		if (n.superEntry != null) {
			var superDispatchTable = dispatchTables.get(-n.superEntry.offset -2);
			dTable.addAll(superDispatchTable);
		}

		for (var method : n.methodsList) {
			visit(method);

			if (method.offset >= dTable.size()) {
				dTable.add(method.label);
			} else {
				dTable.set(method.offset, method.label);
			}
		}

		String dTableHeap = null;
		for (var label : dTable) {
			dTableHeap= nlJoin(
					dTableHeap,

					// memorizzo la label in posizione hp
					"push " + label,
					"lhp",
					"sw",

					// incremento hp
					"lhp",
					"push 1",
					"add",
					"shp"
				);
		}

		return nlJoin(
				"lhp", // lascio il dispatch pointer sullo stack
				dTableHeap
		);
	}

	@Override
	public String visitNode(MethodNode n) throws VoidException {
		if (print) printNode(n,n.id);
		String declCode = null, popDecl = null, popParl = null;
		for (Node dec : n.declist) {
			declCode = nlJoin(declCode,visit(dec));
			popDecl = nlJoin(popDecl,"pop");
		}
		for (int i=0;i<n.parlist.size();i++) popParl = nlJoin(popParl,"pop");
		String funl = freshFunLabel();

		n.label = funl;

		putCode(
				nlJoin(
						funl+":",
						"cfp", // set $fp to $sp value
						"lra", // load $ra value
						declCode, // generate code for local declarations (they use the new $fp!!!)
						visit(n.exp), // generate code for function body expression
						"stm", // set $tm to popped value (function result)
						popDecl, // remove local declarations from stack
						"sra", // set $ra to popped value
						"pop", // remove Access Link from stack
						popParl, // remove parameters from stack
						"sfp", // set $fp to popped value (Control Link)
						"ltm", // load $tm value (function result)
						"lra", // load $ra value
						"js"  // jump to to popped address
				)
		);

		return null;
	}

	@Override
	public String visitNode(ClassCallNode n) throws VoidException {
		if (print) printNode(n,n.id);
		String argCode = null, getAR = null;
		for (int i=n.arglist.size()-1;i>=0;i--) argCode=nlJoin(argCode,visit(n.arglist.get(i)));
		for (int i = 0;i<n.nl-n.entry.nl;i++) getAR=nlJoin(getAR,"lw");
		return nlJoin(
				"lfp", // load Control Link (pointer to frame of method "id" caller)
				argCode, // generate code for argument expressions in reversed order
				"lfp", getAR, 					// retrieve address of frame containing "id1" declaration
												// by following the static chain (of Access Links)

				"push "+n.entry.offset, "add", 	// compute address of "id1" declaration
				"lw", 							// load value of "id1" variable

				"stm", // set $tm to popped value (with the aim of duplicating top of stack)
				"ltm", // load Access Link (pointer to frame of "id1" declaration)
				"ltm", // duplicate top of stack

				"lw", // load object pointer

				"push "+n.methodEntry.offset, "add", // compute address of "id2" in dispatch table
				"lw", // load dispatch pointer
				"js"  // jump to popped address (saving address of subsequent instruction in $ra)
		);
	}

	@Override
	public String visitNode(NewNode n) throws VoidException {
		if (print) printNode(n,n.id);

		String argCode = null, moveArgOnHeap = null;
		for (int i=0; i < n.arglist.size(); i++) {
			argCode = nlJoin(argCode, visit(n.arglist.get(i)));

			moveArgOnHeap = nlJoin(
					moveArgOnHeap,
					"lhp",	// mette hp sullo stack
					"sw",			// carica arg in posizione hp

					// incremento hp
					"lhp",
					"push 1",
					"add",
					"shp"
			);
		}

		return nlJoin(
				// 1. copia gli argomenti nell'heap
				argCode,
				moveArgOnHeap,

				// 2. scrive dispatch pointer
				"push " + (ExecuteVM.MEMSIZE + n.entry.offset),
				"lw",		// dispatch pointer su stack
				"lhp",
				"sw",

				// 3. hp su stack
				"lhp",

				// inc hp
				"lhp",
				"push 1",
				"add",
				"shp"
		);
	}

	@Override
	public String visitNode(EmptyNode n) throws VoidException {
		if (print) printNode(n);
		return "push -1";
	}
}