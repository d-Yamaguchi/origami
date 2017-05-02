/***********************************************************************
 * Copyright 2017 Kimio Kuramitsu and ORIGAMI project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***********************************************************************/

package blue.origami.nezcc;

import java.util.ArrayList;
import java.util.List;

import blue.nez.ast.Symbol;
import blue.nez.parser.ParserContext.SymbolReset;
import blue.nez.parser.ParserGrammar;
import blue.nez.parser.ParserGrammar.MemoPoint;
import blue.nez.peg.Expression;
import blue.nez.peg.ExpressionVisitor;
import blue.nez.peg.NonEmpty;
import blue.nez.peg.Production;
import blue.nez.peg.Stateful;
import blue.nez.peg.Typestate;
import blue.nez.peg.expression.PAnd;
import blue.nez.peg.expression.PAny;
import blue.nez.peg.expression.PByte;
import blue.nez.peg.expression.PByteSet;
import blue.nez.peg.expression.PChoice;
import blue.nez.peg.expression.PDetree;
import blue.nez.peg.expression.PDispatch;
import blue.nez.peg.expression.PEmpty;
import blue.nez.peg.expression.PFail;
import blue.nez.peg.expression.PIfCondition;
import blue.nez.peg.expression.PLinkTree;
import blue.nez.peg.expression.PNonTerminal;
import blue.nez.peg.expression.PNot;
import blue.nez.peg.expression.POnCondition;
import blue.nez.peg.expression.POption;
import blue.nez.peg.expression.PPair;
import blue.nez.peg.expression.PRepeat;
import blue.nez.peg.expression.PRepetition;
import blue.nez.peg.expression.PScan;
import blue.nez.peg.expression.PSymbolAction;
import blue.nez.peg.expression.PSymbolPredicate;
import blue.nez.peg.expression.PSymbolScope;
import blue.nez.peg.expression.PTag;
import blue.nez.peg.expression.PTrap;
import blue.nez.peg.expression.PTree;
import blue.nez.peg.expression.PValue;

class ParserGeneratorVisitor<C> extends ExpressionVisitor<C, AbstractParserGenerator<C>> {
	final static boolean Function = true;
	final static boolean Inline = false;

	private ArrayList<Expression> waitingList = new ArrayList<>();

	public void start(ParserGrammar g, AbstractParserGenerator<C> pg) {
		Production p = g.getStartProduction();
		pg.declConst("int", "MEMOSIZE", "" + g.getMemoPointSize());
		pg.log("memosize: %d", g.getMemoPointSize());
		boolean isStateful = Stateful.isStateful(p);
		pg.log("stateful: %s", isStateful);
		pg.initGrammarProperty(g.isBinary(), isStateful);
		int c = 0;
		this.waitingList.add(p.getExpression());
		for (int i = 0; i < this.waitingList.size(); i++) {
			Expression e = this.waitingList.get(i);
			String funcName = pg.getFuncName(e);
			MemoPoint memoPoint = null;
			if (e instanceof PNonTerminal) {
				memoPoint = g.getMemoPoint(((PNonTerminal) e).getUniqueName());
				e = ((PNonTerminal) e).getExpression();
			}
			if (!pg.isDefinedSection(funcName)) {
				final Expression target = e;
				final MemoPoint memo = memoPoint;
				SourceSection prev = pg.openSection(funcName);
				pg.setCurrentFuncName(funcName);
				pg.declFunc(pg.s("Tmatched"), funcName, "px", () -> {
					return this.match(target, pg, memo);
				});
				pg.closeSection(prev);
			}
			c++;
		}
		pg.log("funcsize: %d", c);
	}

	public C match(Expression e, AbstractParserGenerator<C> pg, MemoPoint m) {
		if (m == null) {
			return this.match(e, pg, false);
		}
		boolean withTree = Typestate.compute(e) == Typestate.Tree;
		String funcName = "memo" + (withTree ? "3" : "1");
		this.makeMetaMemoFunc(pg, funcName, (withTree ? "3" : "1"));
		return pg.emitFunc(funcName, pg.V("px"), pg.vInt(m.id), this.getInnerFunction(e, pg));
	}

	void makeMetaMemoFunc(AbstractParserGenerator<C> pg, String funcName, String suffix) {
		if (!pg.isDefinedSymbol(funcName)) {
			SourceSection sec = pg.openSection(pg.RuntimeLibrary);
			pg.defineFunction(funcName);
			pg.declFunc(pg.s("Tmatched"), funcName, "px", "memoPoint", "f", () -> {
				C lookup = pg.emitFunc("lookupMemo" + suffix, pg.V("px"), pg.V("memoPoint"));
				C block = pg.beginBlock();
				block = pg.emitStmt(block, pg.emitVarDecl("pos", pg.emitGetter("px.pos")));
				ArrayList<C> cases = new ArrayList<>();
				cases.add(pg.emitFail());
				cases.add(pg.emitSucc());
				cases.add(
						pg.emitFunc("storeMemo", pg.V("px"), pg.V("memoPoint"), pg.V("pos"), pg.emitApply(pg.V("f"))));
				block = pg.emitStmt(block, pg.emitDispatch(lookup, cases));
				return pg.endBlock(block);
			});
			pg.closeSection(sec);
		}
	}

	public C match(Expression e, AbstractParserGenerator<C> px, boolean asFunction) {
		if (asFunction) {
			String funcName = px.getFuncName(e);
			this.waitingList.add(e);
			px.addFunctionDependency(px.getCurrentFuncName(), funcName);
			return px.emitNonTerminal(funcName);
		} else {
			return e.visit(this, px);
		}
	}

	public C match(Expression e, AbstractParserGenerator<C> pg) {
		C inline = null;
		if (e instanceof PRepetition) {
			int stacks = pg.varStacks(POS, e.get(0));
			inline = this.emitInlineMany(stacks, e, pg);
		}
		if (e instanceof PNot) {
			int stacks = pg.varStacks(POS, e.get(0));
			inline = this.emitInlineNot(stacks, e, pg);
		}
		if (e instanceof PLinkTree) {
			inline = this.emitInlineLink((PLinkTree) e, pg);
		}
		if (e instanceof POption) {
			int stacks = pg.varStacks(POS, e.get(0));
			inline = this.emitInlineOption(stacks, e, pg);
		}
		if (e instanceof PAnd) {
			inline = this.emitInlineAnd(POS, e, pg);
		}
		if (inline != null) {
			return inline;
		}
		return this.match(e, pg, !this.isInline(e));
	}

	private boolean isInline(Expression e) {
		if (e instanceof PByte || e instanceof PByteSet || e instanceof PAny || e instanceof PTree
				|| e instanceof PPair) {
			return true;
		}
		if (e instanceof PTag || e instanceof PValue || e instanceof PEmpty || e instanceof PFail) {
			return true;
		}
		return false;
	}

	@Override
	public C visitNonTerminal(PNonTerminal e, AbstractParserGenerator<C> pg) {
		String funcName = pg.getFuncName(e);
		this.waitingList.add(e);
		pg.addFunctionDependency(pg.getCurrentFuncName(), funcName);
		return pg.emitNonTerminal(funcName);
	}

	@Override
	public C visitEmpty(PEmpty e, AbstractParserGenerator<C> pg) {
		return pg.emitSucc();
	}

	@Override
	public C visitFail(PFail e, AbstractParserGenerator<C> pg) {
		return pg.emitFail();
	}

	@Override
	public C visitByte(PByte e, AbstractParserGenerator<C> pg) {
		return pg.emitMatchByte(e.byteChar());
	}

	@Override
	public C visitByteSet(PByteSet e, AbstractParserGenerator<C> pg) {
		return pg.emitMatchByteSet(e.byteSet());
	}

	@Override
	public C visitAny(PAny e, AbstractParserGenerator<C> pg) {
		return pg.emitMatchAny();
	}

	@Override
	public C visitPair(PPair e, AbstractParserGenerator<C> pg) {
		if (pg.useMultiBytes()) {
			ArrayList<Integer> l = new ArrayList<>();
			Expression remain = Expression.extractMultiBytes(e, l);
			if (l.size() > 2) {
				byte[] text = Expression.toMultiBytes(l);
				C match = pg.matchBytes(text);
				if (!(remain instanceof PEmpty)) {
					match = pg.emitAnd(match, this.match(remain, pg));
				}
				return match;
			}
		}
		C pe1 = this.match(e.get(0), pg);
		C pe2 = this.match(e.get(1), pg);
		return pg.emitAnd(pe1, pe2);
	}

	@Override
	public C visitChoice(PChoice e, AbstractParserGenerator<C> pg) {
		int stacks = pg.varStacks(POS, e);
		C first = this.match(e.get(0), pg);
		for (int i = 1; i < e.size(); i++) {
			C second = pg.emitAnd(this.emitBacktrack(pg, stacks), this.match(e.get(i), pg));
			first = pg.emitOr(first, second);
		}
		return this.emitVarDecl(pg, stacks, pg.emitReturn(first));
	}

	@Override
	public C visitDispatch(PDispatch e, AbstractParserGenerator<C> pg) {
		List<C> exprs = new ArrayList<>(e.size() + 1);
		exprs.add(pg.emitFail());
		for (int i = 0; i < e.size(); i++) {
			exprs.add(this.patch(e.get(i), pg));
		}
		return pg.emitDispatch(pg.emitJumpIndex(e.indexMap), exprs);
	}

	private C patch(Expression e, AbstractParserGenerator<C> px) {
		if (e instanceof PPair) {
			Expression first = e.get(0);
			if (first instanceof PAny || first instanceof PByte || first instanceof PByteSet) {
				return px.emitAnd(px.emitMove(1), this.match(e.get(1), px));
			}
		}
		return this.match(e, px);
	}

	final static int POS = 1;
	final static int TREE = 1 << 1;
	final static int STATE = 1 << 2;
	final static int CNT = 1 << 3;
	final static int EMPTY = 1 << 4;

	@Override
	public C visitOption(POption e, AbstractParserGenerator<C> pg) {
		int stacks = pg.varStacks(POS, e.get(0));
		C inline = this.emitInlineOption(stacks, e, pg);
		if (inline == null) {
			return this.emitOption(pg, stacks, this.match(e.get(0), pg));
		}
		return inline;
	}

	C emitInlineOption(int stacks, Expression e, AbstractParserGenerator<C> pg) {
		C inline = pg.matchOption(e);
		if (inline != null) {
			return inline;
		}
		C innerFunc = this.getInnerFunction(e.get(0), pg);
		if (innerFunc != null) {
			return this.emitOptionFunc(pg, stacks, innerFunc);
		}
		return null;
	}

	@Override
	public C visitRepetition(PRepetition e, AbstractParserGenerator<C> pg) {
		int stacks = pg.varStacks(POS, e.get(0));
		if (e.isOneMore()) {
			stacks |= CNT;
		}
		if (!NonEmpty.isAlwaysConsumed(e.get(0))) {
			stacks |= EMPTY;
		}
		C inline = this.emitInlineMany(stacks, e, pg);
		if (inline == null) {
			C cond = this.match(e.get(0), pg);
			return this.emitMany(pg, pg.getCurrentFuncName(), stacks, cond);
		}
		return inline;
	}

	C emitInlineMany(int stacks, Expression e, AbstractParserGenerator<C> pg) {
		C inline = pg.matchMany(e);
		if (inline != null) {
			return inline;
		}
		C innerFunc = this.getInnerFunction(e.get(0), pg);
		if (innerFunc != null) {
			return this.emitManyFunc(pg, stacks, innerFunc);
		}
		return null;
	}

	@Override
	public C visitAnd(PAnd e, AbstractParserGenerator<C> pg) {
		C inline = this.emitInlineAnd(POS, e, pg);
		if (inline == null) {
			return this.emitAnd(pg, POS, this.match(e.get(0), pg));
		}
		return inline;
	}

	private C emitInlineAnd(int stacks, Expression e, AbstractParserGenerator<C> pg) {
		C inline = pg.matchAnd(e);
		if (inline != null) {
			return inline;
		}
		C innerFunc = this.getInnerFunction(e.get(0), pg);
		if (innerFunc != null) {
			return this.emitAndFunc(pg, stacks, innerFunc);
		}
		return null;
	}

	@Override
	public C visitNot(PNot e, AbstractParserGenerator<C> pg) {
		int stacks = pg.varStacks(POS, e.get(0));
		C inline = this.emitInlineNot(stacks, e, pg);
		if (inline == null) {
			return this.emitNot(pg, stacks, this.match(e.get(0), pg));
		}
		return inline;
	}

	C emitInlineNot(int stacks, Expression e, AbstractParserGenerator<C> pg) {
		C inline = pg.matchNot(e);
		if (inline != null) {
			return inline;
		}
		C innerFunc = this.getInnerFunction(e.get(0), pg);
		if (innerFunc != null) {
			return this.emitNotFunc(pg, stacks, innerFunc);
		}
		return null;
	}

	private C getInnerFunction(Expression inner, AbstractParserGenerator<C> pg) {
		if (pg.supportedLambdaFunction() && this.isInline(inner)) {
			return pg.emitParserLambda(this.match(inner, pg, false));
		}
		this.match(inner, pg, true);
		return pg.emitFuncRef(pg.getFuncName(inner));
	}

	@Override
	public C visitTree(PTree e, AbstractParserGenerator<C> pg) {
		C pe = pg.emitAnd(this.match(e.get(0), pg), pg.endTree(e.endShift, e.tag, e.value));
		if (e.folding) {
			return pg.emitAnd(pg.foldTree(e.beginShift, e.label), pe);
		} else {
			return pg.emitAnd(pg.beginTree(e.beginShift), pe);
		}
	}

	@Override
	public C visitDetree(PDetree e, AbstractParserGenerator<C> pg) {
		C main = pg.emitAnd(this.match(e.get(0), pg), this.emitBacktrack(pg, TREE));
		return this.emitVarDecl(pg, TREE, pg.emitReturn(main));
	}

	C emitInlineDetree(PDetree e, AbstractParserGenerator<C> pg) {
		C innerFunc = this.getInnerFunction(e.get(0), pg);
		if (innerFunc != null) {
			String funcName = /* pg.useFunc */("detree" + TREE);
			this.makeMetaDetreeFunc(pg, funcName);
			return pg.emitFunc(funcName, pg.V("px"), innerFunc);
		}
		return null;
	}

	void makeMetaDetreeFunc(AbstractParserGenerator<C> pg, String funcName) {
		this.makeBacktrackFunc(pg, TREE);
		if (!pg.isDefinedSymbol(funcName)) {
			SourceSection sec = pg.openSection(pg.RuntimeLibrary);
			pg.defineFunction(funcName);
			pg.declFunc(pg.s("bool"), funcName, "px", "f", () -> {
				C main = pg.emitAnd(pg.emitApply(pg.V("f")), this.emitBacktrack(pg, TREE));
				return this.emitVarDecl(pg, TREE, pg.emitReturn(main));
			});
			pg.closeSection(sec);
		}
	}

	@Override
	public C visitLinkTree(PLinkTree e, AbstractParserGenerator<C> pg) {
		C inline = this.emitInlineLink(e, pg);
		if (inline == null) {
			C main = pg.emitAnd(this.match(e.get(0), pg), pg.backLink(e.label));
			return this.emitVarDecl(pg, TREE, pg.emitReturn(main));
		}
		return inline;
	}

	C emitInlineLink(PLinkTree e, AbstractParserGenerator<C> pg) {
		C innerFunc = this.getInnerFunction(e.get(0), pg);
		if (innerFunc != null) {
			String funcName = /* pg.useFunc */("link" + TREE);
			this.makeMetaLinkFunc(pg, funcName, e.label);
			return pg.emitFunc(funcName, pg.V("px"), pg.vLabel(e.label), innerFunc);
		}
		return null;
	}

	void makeMetaLinkFunc(AbstractParserGenerator<C> pg, String funcName, Symbol label) {
		if (!pg.isDefinedSymbol(funcName)) {
			SourceSection sec = pg.openSection(pg.RuntimeLibrary);
			pg.defineFunction(funcName);
			pg.declFunc(pg.s("bool"), funcName, "px", "label", "f", () -> {
				C main = pg.emitAnd(pg.emitApply(pg.V("f")), pg.backLink(label));
				return this.emitVarDecl(pg, TREE, pg.emitReturn(main));
			});
			pg.closeSection(sec);
		}
	}

	@Override
	public C visitTag(PTag e, AbstractParserGenerator<C> pg) {
		return pg.tagTree(e.tag);
	}

	@Override
	public C visitReplace(PValue e, AbstractParserGenerator<C> pg) {
		return pg.valueTree(e.value);
	}

	@Override
	public C visitSymbolScope(PSymbolScope e, AbstractParserGenerator<C> pg) {
		C main = pg.emitAnd(this.match(e.get(0), pg), this.emitBacktrack(pg, STATE));
		if (e.label != null) {
			main = pg.emitAnd(pg.callAction(new SymbolReset(), e.label, null), main);
		}
		return this.emitVarDecl(pg, STATE, pg.emitReturn(main));
	}

	@Override
	public C visitSymbolAction(PSymbolAction e, AbstractParserGenerator<C> pg) {
		if (e.isEmpty()) {
			return pg.callAction(e.action, e.label, e.thunk);
		} else {
			C main = pg.emitAnd(this.match(e.get(0), pg), pg.callAction(e.action, e.label, e.thunk));
			return this.emitVarDecl(pg, POS, pg.emitReturn(main));
		}
	}

	@Override
	public C visitSymbolPredicate(PSymbolPredicate e, AbstractParserGenerator<C> pg) {
		if (e.isEmpty()) {
			return pg.callPredicate(e.pred, e.label, e.thunk);
		} else {
			C main = pg.emitAnd(this.match(e.get(0), pg), pg.callPredicate(e.pred, e.label, e.thunk));
			return this.emitVarDecl(pg, POS, pg.emitReturn(main));
		}
	}

	@Override
	public C visitIf(PIfCondition e, AbstractParserGenerator<C> pg) {
		return pg.emitSucc();
	}

	@Override
	public C visitOn(POnCondition e, AbstractParserGenerator<C> pg) {
		return pg.emitSucc();
	}

	@Override
	public C visitScan(PScan e, AbstractParserGenerator<C> pg) {
		return pg.emitSucc();
	}

	@Override
	public C visitRepeat(PRepeat e, AbstractParserGenerator<C> pg) {
		return pg.emitSucc();
	}

	@Override
	public C visitTrap(PTrap e, AbstractParserGenerator<C> pg) {
		return pg.emitSucc();
	}

	// ---
	private final static String RuntimeLibrary = null;

	C emitVarDecl(AbstractParserGenerator<C> pg, int stacks, C returnExpr) {
		C block = pg.beginBlock();
		for (String n : pg.getStackNames(stacks)) {
			block = pg.emitStmt(block, pg.emitVarDecl(n, pg.emitGetter(n)));
		}
		block = pg.emitStmt(block, returnExpr);
		return pg.endBlock(block);
	}

	C emitUpdate(AbstractParserGenerator<C> pg, int stacks) {
		C block = pg.beginBlock();
		for (String n : pg.getStackNames(stacks)) {
			block = pg.emitStmt(block, pg.emitAssign(n, pg.emitGetter(n)));
		}
		return pg.endBlock(block);
	}

	String[] joins(String s, String[] a) {
		if (a.length == 0) {
			return new String[] { s };
		}
		String[] b = new String[a.length + 1];
		b[0] = s;
		System.arraycopy(a, 0, b, 1, a.length);
		return b;
	}

	C emitBacktrack(AbstractParserGenerator<C> pg, int stacks) {
		String funcName = pg.s("back") + (stacks & ~CNT);
		String[] args = pg.getStackNames(stacks);
		ArrayList<C> params = new ArrayList<>();
		params.add(pg.V("px"));
		for (String a : args) {
			params.add(pg.V(a));
		}
		return pg.emitFunc(funcName, params);
	}

	void makeBacktrackFunc(AbstractParserGenerator<C> pg, int stacks) {
		String funcName = pg.s("back") + (stacks & ~CNT);
		if (!pg.isDefinedSymbol(funcName)) {
			pg.defineSymbol(funcName, pg.localName(funcName));
			SourceSection prev = pg.openSection(RuntimeLibrary);
			String[] args = pg.getStackNames(stacks);
			pg.declFunc(pg.s("bool"), funcName, this.joins("px", args), () -> {
				C block = pg.beginBlock();
				for (String a : args) {
					block = pg.emitStmt(block, pg.emitSetter(a, pg.V(a)));
				}
				block = pg.emitStmt(block, pg.emitReturn(pg.emitSucc()));
				return pg.endBlock(block);
			});
			pg.closeSection(prev);
		}
	}

	C emitChoice2(AbstractParserGenerator<C> pg, int stacks, C pe, C pe2) {
		this.makeBacktrackFunc(pg, stacks);
		String funcName = pg.s("choice") + (stacks & ~CNT);
		if (!pg.isDefinedSymbol(funcName)) {
			pg.defineSymbol(funcName, pg.localName(funcName));
			SourceSection prev = pg.openSection(RuntimeLibrary);
			pg.declFunc(pg.s("bool"), funcName, "px", "f", "f2", () -> {
				C first = pg.emitApply(pg.V("f"));
				C second = pg.emitAnd(this.emitBacktrack(pg, stacks), pg.emitApply(pg.V("f2")));
				C result = this.emitVarDecl(pg, stacks, pg.emitReturn(pg.emitOr(first, second)));
				return result;
			});
			pg.closeSection(prev);
		}
		return pg.emitFunc(funcName, pg.V("px"), pe, pe2);
	}

	C emitOptionFunc(AbstractParserGenerator<C> pg, int stacks, C pe) {
		String funcName = pg.s("option") + (stacks & ~CNT);
		this.makeBacktrackFunc(pg, stacks);
		if (!pg.isDefinedSymbol(funcName)) {
			pg.defineSymbol(funcName, pg.localName(funcName));
			SourceSection prev = pg.openSection(RuntimeLibrary);
			pg.declFunc(pg.s("bool"), funcName, "px", "f", () -> {
				return (this.emitOption(pg, stacks, pg.emitApply(pg.V("f"))));
			});
			pg.closeSection(prev);
		}
		return pg.emitFunc(funcName, pg.V("px"), pe);
	}

	C emitOption(AbstractParserGenerator<C> pg, int stacks, C first) {
		C second = this.emitBacktrack(pg, stacks);
		return this.emitVarDecl(pg, stacks, pg.emitReturn(pg.emitOr(first, second)));
	}

	C emitManyFunc(AbstractParserGenerator<C> pg, int stacks, C pe) {
		String funcName = pg.s("many") + stacks;
		this.makeBacktrackFunc(pg, stacks);
		if (!pg.isDefinedSymbol(funcName)) {
			pg.defineSymbol(funcName, pg.localName(funcName));
			SourceSection prev = pg.openSection(RuntimeLibrary);
			pg.declFunc(pg.s("bool"), funcName, "px", "f", () -> {
				return (this.emitMany(pg, funcName, stacks, pg.emitApply(pg.V("f"))));
			});
			pg.closeSection(prev);
		}
		return pg.emitFunc(funcName, pg.V("px"), pe);
	}

	C emitMany(AbstractParserGenerator<C> pg, String funcName, int stacks, C cond) {
		C back = this.emitBacktrack(pg, stacks);
		if ((stacks & CNT) == CNT) {
			back = pg.emitAnd(pg.checkCountVar(), back);
		}
		if ((stacks & EMPTY) == EMPTY) {
			cond = pg.emitAnd(cond, pg.emitCheckNonEmpty());
		}
		if (pg.isFunctional()) {
			C main = pg.emitIf(cond, pg.emitNonTerminal(funcName), back);
			return this.emitVarDecl(pg, stacks, pg.emitReturn(main));
		} else {
			C block = pg.beginBlock();
			block = pg.emitStmt(block, pg.emitWhileStmt(cond, () -> this.emitUpdate(pg, stacks)));
			block = pg.emitStmt(block, pg.emitReturn(back));
			return this.emitVarDecl(pg, stacks, block);
		}
	}

	protected C emitAndFunc(AbstractParserGenerator<C> pg, int stacks, C pe) {
		String funcName = pg.s("and") + (stacks & ~CNT);
		this.makeBacktrackFunc(pg, POS);
		if (!pg.isDefinedSymbol(funcName)) {
			pg.defineSymbol(funcName, pg.localName(funcName));
			SourceSection prev = pg.openSection(RuntimeLibrary);
			pg.declFunc(pg.s("bool"), funcName, "px", "f", () -> {
				return (this.emitAnd(pg, POS, pg.emitApply(pg.V("f"))));
			});
			pg.closeSection(prev);
		}
		return pg.emitFunc(funcName, pg.V("px"), pe);
	}

	protected C emitAnd(AbstractParserGenerator<C> pg, int stacks, C inner) {
		stacks = POS;
		C main = pg.emitAnd(inner, this.emitBacktrack(pg, stacks));
		return this.emitVarDecl(pg, stacks, pg.emitReturn(main));
	}

	protected C emitNotFunc(AbstractParserGenerator<C> pg, int stacks, C pe) {
		String funcName = pg.s("not") + (stacks & ~CNT);
		this.makeBacktrackFunc(pg, stacks);
		if (!pg.isDefinedSymbol(funcName)) {
			pg.defineSymbol(funcName, pg.localName(funcName));
			SourceSection prev = pg.openSection(RuntimeLibrary);
			pg.declFunc(pg.s("bool"), funcName, "px", "f", () -> {
				return (this.emitNot(pg, stacks, pg.emitApply(pg.V("f"))));
			});
			pg.closeSection(prev);
		}
		return pg.emitFunc(funcName, pg.V("px"), pe);
	}

	protected C emitNot(AbstractParserGenerator<C> pg, int stacks, C inner) {
		C main = pg.emitIf(inner, pg.emitFail(), this.emitBacktrack(pg, stacks));
		return this.emitVarDecl(pg, stacks, pg.emitReturn(main));
	}

}