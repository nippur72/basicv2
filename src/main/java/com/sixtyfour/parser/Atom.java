package com.sixtyfour.parser;

import java.util.List;

import com.sixtyfour.config.CompilerConfig;
import com.sixtyfour.elements.Type;
import com.sixtyfour.parser.cbmnative.CodeContainer;
import com.sixtyfour.system.Machine;

/**
 * Interface for atoms. Every part of a BASIC program will be parsed into an
 * Atom, i.e. terms, variables, constants, commands...are all Atoms.
 */
public interface Atom {

	/**
	 * Returns the Type that this Atom is supposed to return when calling the
	 * eval() method.
	 * 
	 * @return the type
	 */
	Type getType();

	/**
	 * Returns the Type that this Atom is supposed to return when calling the
	 * eval() method. This method ignores mismatching types and returns REAL
	 * instead.
	 * 
	 * @return the type
	 */
	Type getType(boolean ignoreTM);

	/**
	 * Evaluates this Atom in the context of the current machine state.
	 * 
	 * @param machine
	 *            the machine
	 * @return the result of the evaluation. This will be either null, an
	 *         Integer, a Float or a String.
	 */
	Object eval(Machine machine);

	/**
	 * "Evaluates" an Atom to "native" code.
	 * 
	 * @param machine
	 *            the machine
	 * @return the result list
	 */
	List<CodeContainer> evalToCode(CompilerConfig config, Machine machine);

	/**
	 * Returns true, if the Atom represents a Term and false otherwise.
	 * 
	 * @return true, if it's a term
	 */
	boolean isTerm();

	/**
	 * Converts some Atoms into Java-Code. If that's not possible, null has to
	 * be returned
	 * 
	 * @return the code or null
	 */
	String toCode(Machine machine);

	/**
	 * Returns is this atom is safe to be considered constant, i.e. it's known
	 * not to change during execution. If this returns false, that doesn't mean
	 * that the actual atom isn't constant. It just means that we don't know for
	 * sure or maybe just don't care for this particular type.
	 * 
	 * @return is it constant?
	 */
	boolean isConstant();

}
