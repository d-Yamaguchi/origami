package blue.origami.transpiler.code;

import java.util.ArrayList;
import java.util.List;

import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.Template;
import blue.origami.transpiler.code.CastCode.BoxCastCode;
import blue.origami.transpiler.code.CastCode.FuncCastCode;
import blue.origami.transpiler.code.CastCode.UnboxCastCode;
import blue.origami.transpiler.type.FuncTy;
import blue.origami.transpiler.type.Ty;
import blue.origami.transpiler.type.VarDomain;
import blue.origami.transpiler.type.VarLogger;
import blue.origami.transpiler.type.VarTy;
import blue.origami.util.ODebug;
import blue.origami.util.StringCombinator;

public class ExprCode extends CodeN implements CallCode {

	protected String name;
	private Template tp;

	public ExprCode(String name, Code... args) {
		super(args);
		this.name = name;
		this.tp = null;
	}

	public ExprCode(Template tp, Code... args) {
		super(tp.getReturnType(), args);
		this.name = tp.getName();
		this.setTemplate(tp);
	}

	public void setTemplate(Template tp) {
		this.tp = tp;
	}

	@Override
	public Template getTemplate() {
		assert (this.tp != null);
		return this.tp;
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.pushCall(env, this);
	}

	@Override
	public Code asType(TEnv env, Ty ret) {
		if (this.isUntyped()) {
			// final Code[] params = this.args;
			List<Template> l = new ArrayList<>(8);
			env.findList(this.name, Template.class, l,
					(tt) -> !tt.isExpired() && tt.getParamSize() == this.args.length);
			if (l.size() == 0) {
				this.typeArgs(env, l);
				return this.asUnfound(env, l);
			}
			if (l.size() == 1) {
				return this.asMatched(env, l.get(0).update(env, this.args), ret);
			}
			this.typeArgs(env, l);
			Template selected = l.get(0);
			int mapCost = this.checkMapCost(env, ret, selected);
			ODebug.trace("cost=%d,%s", mapCost, selected);
			for (int i = 1; i < l.size(); i++) {
				if (mapCost > 0) {
					Template next = l.get(i);
					int nextCost = this.checkMapCost(env, ret, next);
					ODebug.trace("nextcost=%d,%s", nextCost, next);
					if (nextCost < mapCost) {
						mapCost = nextCost;
						selected = next;
					}
				}
			}
			if (mapCost >= this.maxCost()) {
				return this.asMismatched(env, l);
			}
			return this.asMatched(env, selected.update(env, this.args), ret);
		}
		return super.castType(env, ret);
	}

	public int maxCost() {
		return CastCode.BADCONV;
	}

	protected Code asUnfound(TEnv env, List<Template> l) {
		env.findList(this.name, Template.class, l, (tt) -> !tt.isExpired());
		throw new ErrorCode(this, TFmt.undefined_SSS, this.name, this.msgArgs(), this.msgHint(l));
	}

	protected Code asMismatched(TEnv env, List<Template> l) {
		throw new ErrorCode(this, TFmt.mismatched_SSS, this.name, this.msgArgs(), this.msgHint(l));
	}

	protected void typeArgs(TEnv env, List<Template> l) {
		for (int i = 0; i < this.args.length; i++) {
			Ty pt = this.getCommonParamType(l, i);
			// ODebug.trace("common[%d] %s", i, pt);
			this.args[i] = this.args[i].asType(env, pt);
			// ODebug.trace("typed[%d] %s %s", i, this.args[i],
			// this.args[i].getType());
		}
	}

	private Ty getCommonParamType(List<Template> l, int n) {
		// Ty ty = l.get(0).getParamTypes()[n];
		// ODebug.trace("DD %s", l);
		// for (int i = 1; i < l.size(); i++) {
		// if (!ty.eq(l.get(i).getParamTypes()[n])) {
		return Ty.tUntyped();
		// }
		// }
		// return ty;
	}

	private String msgArgs() {
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		StringCombinator.joins(sb, this.args, ", ", p -> p.getType());
		sb.append(")");
		return sb.toString();
	}

	private String msgHint(List<Template> l) {
		StringBuilder sb = new StringBuilder();
		StringCombinator.joins(sb, l, ", ", tp -> tp.getName() + ": " + tp.getFuncType());
		if (sb.length() == 0) {
			return "";
		}
		return " \t" + TFmt.hint + " " + sb;
	}

	private int checkMapCost(TEnv env, Ty ret, Template tp) {
		int mapCost = 0;
		VarDomain dom = null;
		VarLogger logs = new VarLogger();
		Ty[] p = tp.getParamTypes();
		Ty codeRet = tp.getReturnType();
		if (tp.isGeneric()) {
			dom = new VarDomain(p.length + 1);
			Ty[] gp = new Ty[p.length];
			for (int i = 0; i < p.length; i++) {
				gp[i] = p[i].dupVarType(dom);
			}
			p = gp;
			codeRet = codeRet.dupVarType(dom);
		}
		for (int i = 0; i < this.args.length; i++) {
			mapCost += env.mapCost(env, this.args[i].getType(), p[i], logs);
			// ODebug.trace("mapCost=%d %s %s", mapCost, this.args[i].getType(),
			// p[i]);
			if (mapCost >= this.maxCost()) {
				return mapCost;
			}
		}
		if (ret.isSpecific()) {
			mapCost += env.mapCost(env, codeRet, ret, logs);
		}
		if (dom != null) {
			mapCost += dom.mapCost();
		}
		logs.abort();
		return mapCost;
	}

	private Code asMatched(TEnv env, Template tp, Ty t) {
		Ty[] p = tp.getParamTypes();
		Ty ret = tp.getReturnType();
		if (tp.isGeneric()) {
			VarDomain dom = new VarDomain(p.length + 1);
			Ty[] gp = new Ty[p.length];
			for (int i = 0; i < p.length; i++) {
				gp[i] = p[i].dupVarType(dom);
			}
			ret = ret.dupVarType(dom);
			for (int i = 0; i < this.args.length; i++) {
				this.args[i] = this.args[i].asType(env, gp[i]);
				if (p[i] instanceof VarTy) {
					ODebug.trace("must upcast %s => %s", gp[i], gp[i]);
					this.args[i] = new BoxCastCode(gp[i], this.args[i]);
				}
				if (p[i] instanceof FuncTy && p[i].hasVar()) {
					Ty anyTy = p[i].dupVarType(null); // AnyRef
					Template conv = env.findTypeMap(env, gp[i], anyTy);
					ODebug.trace("must funccast %s => %s :: %s", gp[i], anyTy, conv);
					this.args[i] = new FuncCastCode(anyTy, conv, this.args[i]);
				}
			}
			this.setTemplate(tp);
			this.setType(ret);
			Code result = this;
			if (tp.getReturnType() instanceof VarTy) {
				ODebug.trace("must downcast %s => %s", tp.getReturnType(), ret);
				result = new UnboxCastCode(ret, result);
			}
			if (tp.getReturnType() instanceof FuncTy && tp.getReturnType().hasVar()) {
				Ty anyTy = tp.getReturnType().dupVarType(null); // AnyRef
				Template conv = env.findTypeMap(env, ret, anyTy);
				ODebug.trace("must funccast %s => %s :: %s", anyTy, ret, conv);
				result = new FuncCastCode(ret, conv, result);
			}
			return result.castType(env, t);
		} else {
			for (int i = 0; i < this.args.length; i++) {
				this.args[i] = this.args[i].asType(env, p[i]);
			}
			this.setTemplate(tp);
			this.setType(ret);
			return this.castType(env, t);
		}
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append("(");
		sb.append(this.name);
		if (this.args.length > 0) {
			sb.append(" ");
		}
		StringCombinator.joins(sb, this.args, " ");
		sb.append(")");
	}

	public static ExprCode option(String name, Code... args) {
		return new OptionExprCode(name, args);
	}

}

class OptionExprCode extends ExprCode implements CallCode {

	OptionExprCode(String name, Code... code) {
		super(name, code);
	}

	@Override
	public int maxCost() {
		return CastCode.CAST;
	}

	@Override
	protected Code asUnfound(TEnv env, List<Template> l) {
		return this.args[0];
	}

	@Override
	protected Code asMismatched(TEnv env, List<Template> l) {
		return this.args[0];
	}

}
