package blue.origami.nezcc.asm;

import java.io.IOException;
import java.util.List;

import blue.nez.ast.Symbol;
import blue.nez.peg.expression.ByteSet;
import blue.origami.nezcc.AbstractParserGenerator;
import blue.origami.nezcc.Block;
import blue.origami.ocode.OCode;

public class JavaAsmParserGenerator extends AbstractParserGenerator<OCode> {

	@Override
	protected void initSymbols() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void writeHeader() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	protected void writeFooter() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	protected void declStruct(String typeName, String... fields) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void declFuncType(String ret, String typeName, String... params) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void declConst(String typeName, String constName, String literal) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void declProtoType(String ret, String funcName, String[] params) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void declFunc(String ret, String funcName, String[] params, Block<OCode> block) {
		// TODO Auto-generated method stub

	}

	@Override
	protected OCode emitParams(String... params) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode beginBlock() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode emitStmt(OCode block, OCode expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode emitVarDecl(OCode block, boolean mutable, String name, OCode expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode emitIfStmt(OCode block, OCode expr, boolean elseIf, Block<OCode> stmt) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode emitWhileStmt(OCode block, OCode expr, Block<OCode> stmt) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode endBlock(OCode block) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode emitAssign(String name, OCode expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode emitAssign2(OCode left, OCode expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode emitReturn(OCode expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode emitOp(OCode expr, String op, OCode expr2) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode emitCast(String var, OCode expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode emitNull() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode emitArrayIndex(OCode a, OCode index) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode emitNewArray(String type, OCode index) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode emitArrayLength(OCode a) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode emitChar(int uchar) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode emitGetter(OCode self, String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode emitSetter(OCode self, String name, OCode expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode emitFunc(String func, List<OCode> params) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode emitApply(OCode func, List<OCode> params) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode emitNot(OCode pe) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode emitSucc() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode emitFail() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode emitAnd(OCode pe, OCode pe2) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode emitOr(OCode pe, OCode pe2) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode emitIf(OCode pe, OCode pe2, OCode pe3) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode emitDispatch(OCode index, List<OCode> cases) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode emitAsm(String expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode vInt(int shift) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode vIndexMap(byte[] indexMap) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode vString(String s) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode vValue(String s) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode vTag(Symbol s) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode vLabel(Symbol s) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode vThunk(Object s) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode vByteSet(ByteSet bs) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode emitFuncRef(String funcName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode emitParserLambda(OCode match) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OCode V(String name) {
		// TODO Auto-generated method stub
		return null;
	}

}