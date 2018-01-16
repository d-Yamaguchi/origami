package blue.origami.transpiler.asm;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TArrays;
import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.type.Ty;

public class EmptyAsmCode implements Code {

	Ty type;

	EmptyAsmCode(Ty type) {
		this.type = type;
	}

	@Override
	public Ty getType() {
		return this.type;
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {

	}

	@Override
	public void strOut(StringBuilder sb) {
	}

	@Override
	public Code setSource(Tree<?> t) {
		return this;
	}

	@Override
	public Tree<?> getSource() {
		return null;
	}

	@Override
	public Code[] args() {
		return TArrays.emptyCodes;
	}

}
