package com.sixtyfour.cbmnative.mos6502.generators;

import java.util.List;
import java.util.Map;

import com.sixtyfour.Logger;
import com.sixtyfour.elements.Type;


/**
 * @author EgonOlsen
 *
 */
public class Push
  extends GeneratorBase
{
  @Override
  public String getMnemonic()
  {
    return "PUSH";
  }


  @Override
  public void generateCode(String line, List<String> nCode, List<String> subCode, Map<String, String> name2label)
  {
    Operands ops = new Operands(line, name2label);
    Logger.log(line + " -- " + ops.getTarget());
    Operand target=ops.getTarget();
    if (target.getType()==Type.INTEGER) {
      if (target.isRegister()) {
        nCode.add("LDA #<" + target.getRegisterName());
        nCode.add("LDY #>" + target.getRegisterName());
      } else {
        nCode.add("LDA #<" + target.getAddress());
        nCode.add("LDY #>" + target.getAddress());
      }
      nCode.add("STA TMP_ZP");
      nCode.add("STY TMP_ZP+1");
      nCode.add("JSR PUSHINT");
    } else {
      if (target.isRegister()) {
        nCode.add("LDA #<" + target.getRegisterName());
        nCode.add("LDY #>" + target.getRegisterName());
      } else {
        nCode.add("LDA #<" + target.getAddress());
        nCode.add("LDY #>" + target.getAddress());
      }
      nCode.add("; Real in (A/Y) to FAC");
      nCode.add("JSR $BBA2"); // Real in (A/Y) to FAC
      nCode.add("JSR PUSHREAL");
    }
  }
}
