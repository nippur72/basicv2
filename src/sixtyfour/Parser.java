package sixtyfour;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import sixtyfour.elements.Atom;
import sixtyfour.elements.Constant;
import sixtyfour.elements.Line;
import sixtyfour.elements.Operator;
import sixtyfour.elements.Term;
import sixtyfour.elements.Variable;
import sixtyfour.elements.commands.Command;
import sixtyfour.elements.commands.CommandList;
import sixtyfour.elements.functions.Function;
import sixtyfour.elements.functions.FunctionList;

public class Parser {
	public static Line getLine(String line) {
		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if (!Character.isDigit(c)) {
				return new Line(Integer.parseInt(line.substring(0, i)), line.substring(i).trim());
			}
		}
		throw new RuntimeException("No line number found in: " + line);
	}

	public static String[] getParts(Line line) {
		return line.getLine().split(":");
	}

	public static Command getCommand(String linePart) {
		List<Command> commands = CommandList.getCommands();
		Command com = null;

		for (Command command : commands) {
			if (command.isCommand(linePart)) {
				com = command.clone(linePart);
				break;
			}
		}
		if (com == null) {
			if (linePart.contains("=")) {
				com = CommandList.getLetCommand().clone("LET" + linePart);
			}
		}
		return com;
	}

	public static Variable getVariable(String linePart, Memory memory) {
		int pos = linePart.indexOf('=');
		if (pos == -1) {
			throw new RuntimeException("Missing assignment: " + linePart);
		}
		linePart = linePart.substring(0, pos).trim().toUpperCase(Locale.ENGLISH);
		for (int i = 0; i < linePart.length(); i++) {
			char c = linePart.charAt(i);
			if (!Character.isAlphabetic(c) && ((i > 0) && (!Character.isDigit(c) && c != '%' && c != '$'))) {
				throw new RuntimeException("Invalid variable name: " + linePart);
			}
			if ((c == '%' || c == '$') && i != linePart.length() - 1) {
				throw new RuntimeException("Invalid variable name: " + linePart);
			}
		}
		char c = linePart.charAt(linePart.length() - 1);
		String ret = linePart.substring(0, Math.min(2, linePart.length()));
		if (c == '%' || c == '$') {
			if (!ret.endsWith(Character.toString(c))) {
				ret += c;
			}
		}
		return memory.add(new Variable(ret, (Term) null));
	}

	public static Function getFunction(String linePart, Map<String, Term> termMap, Memory memory) {
		List<Function> functions = FunctionList.getFunctions();
		Function fun = null;

		for (Function function : functions) {
			if (function.isFunction(linePart)) {
				fun = function.clone(linePart);
				int pos = linePart.indexOf('(');
				int pos2 = linePart.indexOf(')');
				if (pos == -1 || pos2 < pos) {
					pos = linePart.indexOf('{');
					pos2 = linePart.indexOf('}');
					if (termMap == null || pos == -1 || pos2 < pos) {
						throw new RuntimeException("Invalid function call: " + linePart);
					} else {
						fun.setTerm(Parser.createTerm(linePart.substring(pos, pos2 + 1), termMap, memory));
					}
				} else {
					fun.setTerm(Parser.getTerm(linePart.substring(pos + 1, pos2), memory));
				}
				break;
			}
		}

		return fun;
	}

	// a*b+d*c = a*b=i1, d*c=i2 => i1+i2
	// a+b*d+c = b*d=i1, ai1=i2 => i2+d
	// a/c*d;

	public static Term getTerm(String term, Memory memory) {
		int pos = term.indexOf('=');
		if (pos != -1) {
			term = term.substring(pos + 1);
		}
		term = addBrackets(term);
		return createTerms(term, new HashMap<String, Term>(), memory);
	}

	public static String addBrackets(String term) {
		int open = 0;
		for (int i = 0; i < term.length(); i++) {
			char c = term.charAt(i);
			if (c == '(') {
				open++;
			} else if (c == ')') {
				open--;
			}
		}
		if (open != 0) {
			throw new RuntimeException("Invalid term: " + term);
		}
		return addBrackets(addBrackets(handleNegations(term), 0), 1);
	}

	private static String handleNegations(String term) {
		term = term.replace(" ", "");
		StringBuilder sb = new StringBuilder();
		boolean wasOp = true;
		for (int i = 0; i < term.length(); i++) {
			char c = term.charAt(i);
			if (c == '-' && wasOp) {
				int end = findEnd(term, i);
				sb.append("(-1*").append(term.substring(i + 1, end)).append(")");
				i = end - 1;
			} else {
				sb.append(c);
			}

			wasOp = Operator.isOperator(c);
		}
		return sb.toString();
	}

	public static String addBrackets(String term, int level) {
		term = term.replace(" ", "");
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < term.length(); i++) {
			char c = term.charAt(i);
			if ((level == 1 && (c == '*' || c == '/')) || (level == 0 && c == '^')) {
				int start = findStart(term, i);
				int end = findEnd(term, i);
				if (start > 0 && term.charAt(start - 1) == '(' && end < term.length() && term.charAt(end) == ')') {
					sb.append(term.substring(0, start)).append(term.substring(start, end));
				} else {
					sb.append(term.substring(0, start)).append('(').append(term.substring(start, end)).append(')');
					i++;
				}
				if (end != term.length()) {
					sb.append(term.substring(end));
				}
				term = sb.toString();
				sb.setLength(0);
			}
		}
		return term;
	}

	private static int findEnd(String term, int pos) {
		int brackets = 0;
		for (int i = pos + 1; i < term.length(); i++) {
			char c = term.charAt(i);
			if (brackets == 0 && Operator.isOperator(c) && (c != '-' || (i < term.length() - 1 && Operator.isOperator(term.charAt(i + 1)) && term.charAt(i + 1) != ')'))
					|| c == ')') {
				return i;
			}
			if (c == '(') {
				brackets++;
			} else if (c == ')' && brackets > 0) {
				brackets--;
			}
		}
		return term.length();
	}

	private static int findStart(String term, int pos) {
		int brackets = 0;
		for (int i = pos - 1; i >= 0; i--) {
			char c = term.charAt(i);
			if (brackets == 0 && (Operator.isOperator(c) && (c != '-' || (i > 0 && !Operator.isOperator(term.charAt(i - 1)) && term.charAt(i - 1) != '('))) || c == '(') {
				return i + 1;
			}
			if (c == ')') {
				brackets++;
			} else if (c == '(' && brackets > 0) {
				brackets--;
			}
		}
		return 0;
	}

	private static Term createTerms(String term, Map<String, Term> termMap, Memory memory) {

		int start = 0;
		boolean open = false;
		for (int i = 0; i < term.length(); i++) {
			char c = term.charAt(i);
			if (c == '(') {
				open = true;
				start = i;
			}
			if (c == ')') {
				if (open) {
					String sub = term.substring(start + 1, i);
					Term res = createTerm(sub, termMap, memory);
					if (res != null) {
						String termKey = null;
						int index = termMap.size();
						if (res.getKey() == null) {
							termKey = "{t" + index + "}";
						} else {
							termKey = res.getKey();
						}
						res.setKey(termKey);
						termMap.put(termKey, res);
						// System.out.println("1: " + term);
						term = term.substring(0, start) + termKey + term.substring(i + 1);
						// System.out.println("2: " + term);
						// System.out.println(res);
					}
					open = false;
					i = -1;
				} else {
					throw new RuntimeException("Parse error in: " + term + "/" + start + "/" + i);
				}
			}
		}

		// System.out.println("F: " + term);
		Term finalTerm = new Term(term);
		termMap.put("final", finalTerm);
		finalTerm = build(finalTerm, termMap, memory);
		finalTerm.setKey("final");
		if (!finalTerm.isComplete()) {
			finalTerm.setOperator(Operator.NOP);
			finalTerm.setRight(new Constant<Integer>(0));
		}
		return finalTerm;
	}

	private static Term createTerm(String term, Map<String, Term> termMap, Memory memory) {
		if (!term.contains("(") && !term.contains(")")) {
			if (isTermPlaceholder(term)) {
				return termMap.get(term);
			}
			Term t = new Term(term);
			t = build(t, termMap, memory);
			if (!t.isComplete()) {
				t.setOperator(Operator.NOP);
				t.setRight(new Constant<Integer>(0));
			}
			return t;
		}
		return null;
	}

	private static Term build(Term t, Map<String, Term> termMap, Memory memory) {
		String exp = t.getExpression();

		// System.out.println("EX: "+exp);
		StringBuilder part = new StringBuilder();
		char lastC = '(';
		for (int i = 0; i < exp.length(); i++) {
			char c = exp.charAt(i);
			boolean isOp = Operator.isOperator(c);
			boolean appended = false;
			if (!isOp || (c == '-' && (lastC == '(' || Operator.isOperator(lastC)))) {
				part.append(c);
				appended = true;
			}
			if (!appended || (i >= exp.length() - 1)) {
				Atom atom = createAtom(part.toString(), termMap, memory);
				part.setLength(0);
				if (t.getLeft() == null) {
					t.setLeft(atom);
				} else if (t.getRight() == null) {
					t.setRight(atom);
				}
			}
			if (isOp && !appended) {
				part.setLength(0);
				if (t.isComplete()) {
					Term nt = new Term(t.getExpression());
					nt.setLeft(t);
					t = nt;
				}
				t.setOperator(new Operator(c));
			}

			lastC = c;
		}
		return t;
	}

	private static Atom createAtom(String part, Map<String, Term> termMap, Memory memory) {
		// Identify commands
		Command command = Parser.getCommand(part);
		if (command != null) {
			return command;
		}

		// Identify functions
		Function function = Parser.getFunction(part, termMap, memory);
		if (function != null) {
			return function;
		}

		// String constants
		if (part.startsWith("\"")) {
			if (part.endsWith("\"")) {
				String ct = part.replaceAll("\"", "");
				Atom str = new Constant<String>(ct);
				return str;
			} else {
				throw new RuntimeException("Unterminated string: " + part);
			}
		}
		if (part.endsWith("\"")) {
			throw new RuntimeException("String not open: " + part);
		}

		// Numbers
		boolean number = true;
		boolean real = false;
		for (int i = 0; i < part.length(); i++) {
			char c = part.charAt(i);
			if (!Character.isDigit(c) && c != '-' && c != '.') {
				number = false;
				break;
			}
			if (c == '.') {
				real = true;
			}
		}

		if (number) {
			if (real) {
				Atom fl = new Constant<Float>(Float.valueOf(part));
				return fl;
			} else {
				Atom in = new Constant<Integer>(Integer.valueOf(part));
				return in;
			}
		}

		// Terms
		if (part.startsWith("{") && part.endsWith("}")) {
			Term t = termMap.get(part);
			if (t == null) {
				throw new RuntimeException("Unknown term: " + part);
			}
			if (!t.isComplete()) {
				t = build(t, termMap, memory);
			}
			return t;
		}

		// Variables
		String var = part.toUpperCase(Locale.ENGLISH);
		Variable vary = memory.getVariable(var);
		if (vary == null) {
			vary = new Variable(var, (Term) null);
			vary = memory.add(vary);
		}
		return vary;
	}

	private static boolean isTermPlaceholder(String txt) {
		return txt.startsWith("{") && txt.indexOf('}') == (txt.length() - 1);
	}
}
