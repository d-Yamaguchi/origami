package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.MultiCode;

public class MultiExpr implements ParseRule {

	@Override
	public Code apply(TEnv env, Tree<?> t) {
		Code[] nodes = new Code[t.size()];
		int last = t.size();
		for (int i = 0; i < last; i++) {
			nodes[i] = env.parseCode(env, t.get(i));
		}
		return new MultiCode(nodes);
	}
}
