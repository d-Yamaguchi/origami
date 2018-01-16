package blue.origami.transpiler;

import java.util.ArrayList;
import java.util.List;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.code.ApplyCode;
import blue.origami.transpiler.code.BinaryCode;
import blue.origami.transpiler.code.CastCode;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.CodeBuilder;
import blue.origami.transpiler.code.ExprCode;
import blue.origami.transpiler.code.FuncCode;
import blue.origami.transpiler.code.MultiCode;
import blue.origami.transpiler.code.NameCode;
import blue.origami.transpiler.type.FuncTy;
import blue.origami.transpiler.type.Ty;

public abstract class Generator implements CodeBuilder {
	protected boolean isVerbose = false;

	public void setVerbose(boolean debug) {
		this.isVerbose = debug;
	}

	public boolean isVerbose() {
		return this.isVerbose;
	}

	public abstract void init();

	protected abstract void setup();

	protected abstract Object wrapUp();

	public abstract void emit(TEnv env, Code code);

	public abstract CodeTemplate newConstTemplate(TEnv env, String lname, Ty ret);

	public abstract void defineConst(Transpiler env, boolean isPublic, String name, Ty type, Code expr);

	public String safeName(String name) {
		if (name.indexOf("->") > 0) {
			return "conv";
		}
		return name;
	}

	public abstract CodeTemplate newFuncTemplate(TEnv env, String lname, Ty returnType, Ty... paramTypes);

	public abstract void defineFunction(TEnv env, boolean isPublic, String name, String[] paramNames, Ty[] paramTypes,
			Ty returnType, Code code);

	protected ArrayList<TFunction> funcList = null;

	public void addFunction(String name, TFunction f) {
		if (f.isPublic) {
			if (this.funcList == null) {
				this.funcList = new ArrayList<>(0);
			}
			this.funcList.add(f);
		}
	}

	protected ArrayList<Tree<?>> exampleList = null;

	public void addExample(String name, Tree<?> t) {
		if (this.exampleList == null) {
			this.exampleList = new ArrayList<>(0);
		}
		this.exampleList.add(t);
	}

	public Code emitHeader(TEnv env, Code code) {
		if (this.funcList != null) {
			for (TFunction f : this.funcList) {
				if (f.isExpired()) {
					continue;
				}
				f.generate(env);
			}
			this.funcList = null;
		}
		if (code.isEmpty() && this.exampleList != null) {
			ArrayList<Code> asserts = new ArrayList<>();
			for (Tree<?> t : this.exampleList) {
				Code body = env.parseCode(env, t).asType(env, Ty.tBool);
				asserts.add(new ExprCode("assert", body));
			}
			code = new MultiCode(asserts.toArray(new Code[asserts.size()])).asType(env, Ty.tVoid);
			this.exampleList = null;
		}
		return code;
	}

	public Template genTestBinFunc(TEnv env, String op, Code right) {
		Transpiler tr = env.getTranspiler();
		Ty ty = right.getType();
		String[] names = { "a", "b" };
		Ty[] params = { ty, ty };
		Code name = new NameCode("a", 0, ty, 0);
		MultiCode body = new MultiCode(new ExprCode("assert", new BinaryCode(op, name, new NameCode("b", 1, ty, 0))),
				name);
		return tr.defineFunction("test" + op + ty, names, params, ty, body);
	}

	public Template genFuncConvFunc(TEnv env, FuncTy fromTy, FuncTy toTy) {
		Transpiler tr = env.getTranspiler();
		String[] names = { "_f" };
		Ty[] params = { fromTy };

		Ty[] fromTypes = fromTy.getParamTypes();
		Ty[] toTypes = toTy.getParamTypes();
		String[] fnames = TArrays.names(toTypes.length);
		List<Code> l = new ArrayList<>();
		l.add(new NameCode("_f"));
		for (int c = 0; c < toTy.getParamSize(); c++) {
			l.add(new CastCode(fromTypes[c], new NameCode(String.valueOf((char) c))));
		}
		Code body = new CastCode(toTy.getReturnType(), new ApplyCode(l));
		body = new FuncCode(fnames, fromTypes, toTy.getReturnType(), body);
		return tr.defineFunction("(" + fromTy + ")->(" + toTy + ")", names, params, toTy, body);
	}

}
