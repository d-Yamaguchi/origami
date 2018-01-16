package blue.origami.transpiler.code;

import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.type.Ty;

public class ExistFieldCode extends Code1 {

	private String name;

	public ExistFieldCode(Code inner, String name) {
		super(inner);
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	@Override
	public Code asType(TEnv env, Ty ret) {
		if (this.isUntyped()) {
			this.asTypeAt(env, 0, Ty.tData());
			this.setType(Ty.tBool);
		}
		return this.castType(env, ret);
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.pushExistField(env, this);
	}

	@Override
	public void strOut(StringBuilder sb) {
		// TODO Auto-generated method stub

	}

}
