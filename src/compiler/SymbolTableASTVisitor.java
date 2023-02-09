package compiler;

import java.util.*;
import compiler.AST.*;
import compiler.exc.*;
import compiler.lib.*;

public class SymbolTableASTVisitor extends BaseASTVisitor<Void,VoidException> {
	
	private List<Map<String, STentry>> symTable = new ArrayList<>();
	private Map<String, Map<String, STentry>> classTable = new HashMap<>();
	private int nestingLevel=0; // current nesting level
	private int decOffset=-2; // counter for offset of local declarations at current nesting level 
	int stErrors=0;

	SymbolTableASTVisitor() {}
	SymbolTableASTVisitor(boolean debug) {super(debug);} // enables print for debugging

	private STentry stLookup(String id) {
		int j = nestingLevel;
		STentry entry = null;
		while (j >= 0 && entry == null) 
			entry = symTable.get(j--).get(id);	
		return entry;
	}

	@Override
	public Void visitNode(ProgLetInNode n) {
		if (print) printNode(n);
		Map<String, STentry> hm = new HashMap<>();
		symTable.add(hm);
	    for (Node dec : n.declist) visit(dec);
		visit(n.exp);
		symTable.remove(0);
		return null;
	}

	@Override
	public Void visitNode(ProgNode n) {
		if (print) printNode(n);
		visit(n.exp);
		return null;
	}
	
	@Override
	public Void visitNode(FunNode n) {
		if (print) printNode(n);
		Map<String, STentry> hm = symTable.get(nestingLevel);
		List<TypeNode> parTypes = new ArrayList<>();  
		for (ParNode par : n.parlist) parTypes.add(par.getType());
		STentry entry = new STentry(nestingLevel, new ArrowTypeNode(parTypes,n.retType),decOffset--);
		//inserimento di ID nella symtable
		if (hm.put(n.id, entry) != null) {
			System.out.println("Fun id " + n.id + " at line "+ n.getLine() +" already declared");
			stErrors++;
		} 
		//creare una nuova hashmap per la symTable
		nestingLevel++;
		Map<String, STentry> hmn = new HashMap<>();
		symTable.add(hmn);
		int prevNLDecOffset=decOffset; // stores counter for offset of declarations at previous nesting level 
		decOffset=-2;
		
		int parOffset=1;
		for (ParNode par : n.parlist)
			if (hmn.put(par.id, new STentry(nestingLevel,par.getType(),parOffset++)) != null) {
				System.out.println("Par id " + par.id + " at line "+ n.getLine() +" already declared");
				stErrors++;
			}
		for (Node dec : n.declist) visit(dec);
		visit(n.exp);

		n.setType(new ArrowTypeNode(parTypes, n.retType));

		//rimuovere la hashmap corrente poiche' esco dallo scope               
		symTable.remove(nestingLevel--);
		decOffset=prevNLDecOffset; // restores counter for offset of declarations at previous nesting level 
		return null;
	}
	
	@Override
	public Void visitNode(VarNode n) {
		if (print) printNode(n);
		visit(n.exp);
		Map<String, STentry> hm = symTable.get(nestingLevel);
		STentry entry = new STentry(nestingLevel,n.getType(),decOffset--);
		//inserimento di ID nella symtable
		if (hm.put(n.id, entry) != null) {
			System.out.println("Var id " + n.id + " at line "+ n.getLine() +" already declared");
			stErrors++;
		}
		return null;
	}

	@Override
	public Void visitNode(PrintNode n) {
		if (print) printNode(n);
		visit(n.exp);
		return null;
	}

	@Override
	public Void visitNode(IfNode n) {
		if (print) printNode(n);
		visit(n.cond);
		visit(n.th);
		visit(n.el);
		return null;
	}
	
	@Override
	public Void visitNode(EqualNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(GreaterEqualNode n) throws VoidException {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(LessEqualNode n) throws VoidException {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(AndNode n) throws VoidException {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(OrNode n) throws VoidException {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(NotNode n) throws VoidException {
		if (print) printNode(n);
		visit(n.exp);
		return null;
	}

	@Override
	public Void visitNode(TimesNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(DivNode n) throws VoidException {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(PlusNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(MinusNode n) throws VoidException {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(CallNode n) {
		if (print) printNode(n);
		STentry entry = stLookup(n.id);
		if (entry == null) {
			System.out.println("Fun id " + n.id + " at line "+ n.getLine() + " not declared");
			stErrors++;
		} else {
			n.entry = entry;
			n.nl = nestingLevel;
		}
		for (Node arg : n.arglist) visit(arg);
		return null;
	}

	@Override
	public Void visitNode(IdNode n) {
		if (print) printNode(n);
		STentry entry = stLookup(n.id);
		if (entry == null) {
			System.out.println("Var or Par id " + n.id + " at line "+ n.getLine() + " not declared");
			stErrors++;
		} else {
			n.entry = entry;
			n.nl = nestingLevel;
		}
		return null;
	}

	@Override
	public Void visitNode(BoolNode n) {
		if (print) printNode(n, n.val.toString());
		return null;
	}

	@Override
	public Void visitNode(IntNode n) {
		if (print) printNode(n, n.val.toString());
		return null;
	}

	// OBJECT-ORIENTED EXTENSION

	@Override
	public Void visitNode(ClassNode n) throws VoidException {
		if (print) printNode(n);

		// nestingLevel always 0 for syntax
		Map<String, STentry> hm = symTable.get(nestingLevel);

		// Creazione SymbolTable livello corrente
		var allFields = new ArrayList<TypeNode>();
		var allMethods = new ArrayList<ArrowTypeNode>();
		if (n.superID != null) {
			var superClassEntry = hm.get(n.superID);

			if (classTable.containsKey(n.superID)) {
				n.superEntry = superClassEntry;
			}

			// controllo se esiste la super class
			if (superClassEntry == null) {
				System.out.println("Super Class id " + n.id + " at line "+ n.getLine() +" not declared");
				stErrors++;
			} else {
				var superClassType = (ClassTypeNode) superClassEntry.type;
				allFields.addAll(new ArrayList<>(superClassType.allFields));
				allMethods.addAll(new ArrayList<>(superClassType.allMethods));
			}
		}

		STentry entry = new STentry(nestingLevel, new ClassTypeNode(allFields, allMethods), decOffset--);
		if (hm.put(n.id, entry) != null) {
			System.out.println("Class id " + n.id + " at line "+ n.getLine() +" already declared");
			stErrors++;
		}

		// Creazione ClassTable
		Map<String, STentry> virtualTable = new HashMap<>();

		if (n.superID != null) {
			var superClassEntry = classTable.get(n.superID);
			if (superClassEntry != null) {
				virtualTable.putAll(superClassEntry);
			}
		}
		classTable.put(n.id, virtualTable);

		// Nuovo livello symbol table
		nestingLevel++;
		symTable.add(virtualTable);

		// Ciclo su parametri classe
		int fieldOffset=-1;
		int prevNLDecOffset=decOffset; // stores counter for offset of declarations at previous nesting level
		decOffset=0;

		if (n.superID != null && classTable.containsKey(n.superID)) {
			var lunghezzaField = ((ClassTypeNode) hm.get(n.superID).type).allFields.size();
			fieldOffset = -lunghezzaField - 1;

			decOffset = ((ClassTypeNode) hm.get(n.superID).type).allMethods.size();
		}

		Set<String> actualField = new HashSet<>();
		for (FieldNode field : n.fieldsList) {

			// Ottimizzazione - evito override interni alla classe
			if (actualField.contains(field.id)) {
				System.out.println("Field " + field.id + " at in class line "+ n.getLine() +" multiple defined");
				stErrors++;
			} else {
				actualField.add(field.id);
			}

			if (virtualTable.containsKey(field.id)) { // field override
				var STentryToOverride = virtualTable.get(field.id);

				if (STentryToOverride.type instanceof MethodTypeNode) { // cannot override field with method
					System.out.println("Cannot override method with field id " + field.id + " at line "+ n.getLine());
					stErrors++;
				} else {
					// 1. Virtual Table update
					virtualTable.put(field.id, new STentry(nestingLevel, field.getType(), STentryToOverride.offset));

					// 2. ClassTypeNode update
					((ClassTypeNode) entry.type).allFields.set(-STentryToOverride.offset-1, field.getType());

					// Optimization
					field.offset = STentryToOverride.offset;
				}
			} else { // no override
				// 1. Virtual Table update
				virtualTable.put(field.id, new STentry(nestingLevel, field.getType(), fieldOffset));

				// 2. ClassTypeNode update
				((ClassTypeNode) entry.type).allFields.add(field.getType());

				// Optimization
				field.offset = fieldOffset--;
			}
		}


		// Ciclo su metodi classe
		Set<String> actualMethod = new HashSet<>();
		for (MethodNode method : n.methodsList) {

			// Ottimizzazione - evito override interni alla classe
			if (actualMethod.contains(method.id)) {
				System.out.println("Method " + method.id + " in class at line "+ n.getLine() +" multiple define");
				stErrors++;
			} else {
				actualMethod.add(method.id);
			}

			var isOverride = virtualTable.containsKey(method.id);
			visitNode(method);

			// avoid error if override method with field
			if (virtualTable.get(method.id).type instanceof MethodTypeNode methodType) {
				if (isOverride) { // Method override
					((ClassTypeNode) entry.type).allMethods.set(method.offset, methodType.fun);
				} else { // no override
					((ClassTypeNode) entry.type).allMethods.add(methodType.fun);
				}
			}

		}

		n.setType(entry.type);

		//rimuovere la hashmap corrente poiche' esco dallo scope
		symTable.remove(nestingLevel--);
		decOffset=prevNLDecOffset; // restores counter for offset of declarations at previous nesting level
		return null;
	}

	@Override
	public Void visitNode(MethodNode n) throws VoidException {
		if (print) printNode(n);

		Map<String, STentry> virtualTable = symTable.get(nestingLevel);
		List<TypeNode> parTypes = new ArrayList<>();
		for (ParNode par : n.parlist) parTypes.add(par.getType());

		if (virtualTable.containsKey(n.id)) { // method override
			var STentryToOverride = virtualTable.get(n.id);

			if (!(STentryToOverride.type instanceof MethodTypeNode)) { // cannot override field with method
				System.out.println("Cannot override field with method id " + n.id + " at line "+ n.getLine());
				stErrors++;
			} else {
				// 1. Virtual Table update
				var type = new MethodTypeNode(new ArrowTypeNode(parTypes,n.retType));
				virtualTable.put(n.id, new STentry(nestingLevel, type, STentryToOverride.offset));

				// 2. Setto offset del metodo
				n.offset = STentryToOverride.offset;
			}
		} else { // no override
			// 1. Virtual Table update
			var type = new MethodTypeNode(new ArrowTypeNode(parTypes, n.retType));
			virtualTable.put(n.id, new STentry(nestingLevel, type, decOffset));

			// 2. Setto offset del metodo
			n.offset = decOffset++;
		}

		// creare una nuova hashmap per il corpo del metodo
		nestingLevel++;
		Map<String, STentry> hmn = new HashMap<>();
		symTable.add(hmn);
		int prevNLDecOffset=decOffset; // stores counter for offset of declarations at previous nesting level
		decOffset=-2;

		int parOffset=1;
		for (ParNode par : n.parlist)
			if (hmn.put(par.id, new STentry(nestingLevel,par.getType(),parOffset++)) != null) {
				System.out.println("Method Par id " + par.id + " at line "+ n.getLine() +" already declared");
				stErrors++;
			}
		for (Node dec : n.declist) visit(dec);
		visit(n.exp);

		n.setType(new MethodTypeNode(new ArrowTypeNode(parTypes, n.retType)));

		//rimuovere la hashmap corrente poiche' esco dallo scope del motodo
		symTable.remove(nestingLevel--);
		decOffset=prevNLDecOffset; // restores counter for offset of declarations at previous nesting level
		return null;
	}

	@Override
	public Void visitNode(ClassCallNode n) throws VoidException {
		if (print) printNode(n);
		STentry entry = stLookup(n.id);
		if (entry == null) {
			System.out.println("Id " + n.id + " at line "+ n.getLine() + " not declared");
			stErrors++;
		} else if (!(entry.type instanceof RefTypeNode)) {
			System.out.println("Id " + n.id + " at line "+ n.getLine() + " not a RefType");
			stErrors++;
		} else {
			n.entry = entry;
			var className = ((RefTypeNode) entry.type).id;
			var methodEntry = classTable.get(className).get(n.methodId);

			if (methodEntry == null) {
				System.out.println("Method Id " + n.methodId + " at line "+ n.getLine() + " not declared");
				stErrors++;
			} else {
				n.methodEntry = methodEntry;
			}

			n.nl = nestingLevel;
		}
		for (Node arg : n.arglist) visit(arg);
		return null;
	}

	@Override
	public Void visitNode(NewNode n) throws VoidException {
		if (print) printNode(n);

		if (!classTable.containsKey(n.id)) {
			System.out.println("Class id " + n.id + " at line "+ n.getLine() + " not in Class Table");
			stErrors++;
		} else {
			n.entry = symTable.get(0).get(n.id);
			n.nl = nestingLevel;
		}
		for (Node arg : n.arglist) visit(arg);
		return null;
	}

	@Override
	public Void visitNode(EmptyNode n) throws VoidException {
		if (print) printNode(n);
		return null;
	}
}
