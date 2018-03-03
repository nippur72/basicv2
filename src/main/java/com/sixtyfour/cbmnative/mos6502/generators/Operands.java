package com.sixtyfour.cbmnative.mos6502.generators;

import java.util.Locale;
import java.util.Map;

import com.sixtyfour.elements.Type;

/**
 * @author EgonOlsen
 * 
 */
public class Operands {
	private Operand target;
	private Operand source;

	public Operands(String command, Map<String, String> name2label) {
		int pos = command.indexOf(" ");
		if (pos == -1) {
			throw new RuntimeException("Can't parse command: " + command);
		}

		command = command.substring(pos).trim();

		pos = command.indexOf(",");
		if (pos == -1) {
			String left = command.trim();
			target = new Operand();
			fill(target, left, name2label);
			source = target;
		} else {
			String left = command.substring(0, pos).trim();
			String right = command.substring(pos + 1).trim();

			target = new Operand();
			source = new Operand();

			fill(target, left, name2label);
			fill(source, right, name2label);
		}
	}

	public Operand getTarget() {
		return target;
	}

	public void setTarget(Operand target) {
		this.target = target;
	}

	public Operand getSource() {
		return source;
	}

	public void setSource(Operand source) {
		this.source = source;
	}

	private void fill(Operand op, String txt, Map<String, String> name2label) {
		op.setIndexed(false);
		op.setAddress(null);
		op.setRegister(null);
		op.setArray(false);
		op.setType(getType(txt));

		String shorty = txt;
		int start = shorty.lastIndexOf("{");
		if (start != -1) {
			shorty = shorty.substring(0, start);
		}
		if (shorty.endsWith("[]")) {
			op.setArray(true);
		}

		if (txt.length() == 1 && !Character.isDigit(txt.charAt(0))) {
			op.setRegister(txt.toUpperCase(Locale.ENGLISH));
		} else {
			if (txt.startsWith("(")) {
				op.setIndexed(true);
				if (txt.endsWith("})")) {
					txt = removeBrackets(txt);
					if (op.getType() == Type.STRING && txt.startsWith("#")) {
						txt = "$" + txt.substring(1);
					}
					String label = name2label.get(txt);
					op.setAddress(label == null ? txt : label);
				} else {
					txt = removeBrackets(txt);
					op.setRegister(txt);
				}
			} else {
				txt = removeBrackets(txt);
				if (op.getType() == Type.STRING && txt.startsWith("#")) {
					txt = "$" + txt.substring(1);
				}
				String label = name2label.get(txt);
				op.setAddress(label == null ? txt : label);
			}
		}
	}

	private Type getType(String txt) {
		int end = txt.lastIndexOf("}");
		int start = txt.lastIndexOf("{");
		if (start != -1 && end != -1 && start + 1 != end) {
			String name = txt.substring(0, start);
			Type type = Type.valueOf(txt.substring(start + 1, end));
			// Range check...convert to real if needed
			if (name.startsWith("#")) {
				name = name.substring(1);
				if (type == Type.INTEGER) {
					int num = Integer.parseInt(name);
					if (num < -32768 || num > 32767) {
						name = name + ".0";
						type = Type.REAL;
					}
				}
			}
			return type;
		}
		if (txt.contains(".")) {
			return Type.REAL;
		}
		return Type.INTEGER;
	}

	private String removeBrackets(String txt) {
		txt = txt.replace("(", "").replace(")", "");
		int pos = txt.lastIndexOf("{");
		if (pos != -1) {
			txt = txt.substring(0, pos);
		}
		return txt;
	}

}
