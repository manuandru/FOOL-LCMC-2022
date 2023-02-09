package compiler;

import compiler.AST.*;
import compiler.lib.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class TypeRels {

	public static Map<String, String> superType = new HashMap<>();

	// valuta se il tipo "a" e' <= al tipo "b", dove "a" e "b" sono tipi di base: IntTypeNode o BoolTypeNode
	public static boolean isSubtype(TypeNode a, TypeNode b) {
		return isIntTypeAndBoolType(a, b)
				|| checkHierarchy(a, b)
				|| isEmptyTypeAndRefType(a, b)
				|| checkMethodSubtyping(a, b);
	}

	public static TypeNode lowestCommonAncestor(TypeNode a, TypeNode b) {

		if (isSubtype(a, b)) return b;
		if (isSubtype(b, a)) return a;

		if (!(a instanceof RefTypeNode aType)) return null;

		var superClassA = superType.get(aType.id);
		while (superClassA != null) {
			var typeOfSuperA = new RefTypeNode(superClassA);
			if (isSubtype(b, typeOfSuperA)) {
				return typeOfSuperA;
			}

			superClassA = superType.get(superClassA);
		}

		return null;
	}

	/**
	 *  Stesso tipo o a=BoolTypeNode e b=IntTypeNode
 	 */
	private static boolean isIntTypeAndBoolType(TypeNode a, TypeNode b) {
		return ((a instanceof BoolTypeNode) && (b instanceof IntTypeNode))
				|| ((a instanceof BoolTypeNode) && (b instanceof BoolTypeNode))
				|| ((a instanceof IntTypeNode) && (b instanceof IntTypeNode));
	}

	/**
	 *  RefTypeNode e RefTypeNode usando superType (Gerarchia di tipi)
	 */
	private static boolean checkHierarchy(TypeNode a, TypeNode b) {
		if (!(a instanceof RefTypeNode) || !(b instanceof RefTypeNode)) {
			return false;
		}
		var typeA = ((RefTypeNode) a).id;
		var typeB = ((RefTypeNode) b).id;

		if (typeA.equals(typeB)) {
			return true;
		}

		var superTypeA = superType.get(typeA);
		while (superTypeA != null) {
			if (superTypeA.equals(typeB)) {
				return true;
			}
			superTypeA = superType.get(superTypeA);
		}

		return false;
	}

	/**
	 *  EmptyTypeNode sottotipo di qualsiasi RefTypeNode
	 */
	private static boolean isEmptyTypeAndRefType(TypeNode a, TypeNode b) {
		return (a instanceof EmptyTypeNode) && (b instanceof RefTypeNode);
	}

	/**
	 *  ArrowTypeNode subtyping per overriding
	 *  1. Co-varianza sul tipo di ritorno
	 *  2. Contro-varianza sul tipo dei parametri
	 */
	private static boolean checkMethodSubtyping(TypeNode a, TypeNode b) {
		if (!(a instanceof ArrowTypeNode) || !(b instanceof ArrowTypeNode)) {
			return false;
		}
		var typeA = ((ArrowTypeNode) a);
		var typeB = ((ArrowTypeNode) b);

		// Co-varianza sul tipo di ritorno
		if (!isSubtype(typeA.ret, typeB.ret)) {
			return false;
		}

		// Contro-varianza sul tipo dei parametri
		for (var i = 0; i < typeA.parlist.size(); i++) {
			if (!isSubtype(typeB.parlist.get(i), typeA.parlist.get(i))) {
				return false;
			}
		}

		return true;
	}

}
