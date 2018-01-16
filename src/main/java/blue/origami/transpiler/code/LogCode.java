package blue.origami.transpiler.code;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TLog;
import blue.origami.util.StringCombinator;

public class LogCode extends Code1 {
	protected TLog log;

	public LogCode(TLog log, Code inner) {
		super(AutoType, inner);
		this.log = log;
	}

	public TLog getLog() {
		return this.log;
	}

	@Override
	public Code setSource(Tree<?> t) {
		this.inner.setSource(t);
		this.log.setSource(t);
		return this;
	}

	@Override
	public void strOut(StringBuilder sb) {
		StringCombinator.append(sb, this.getInner());
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.pushLog(env, this);
	}

}