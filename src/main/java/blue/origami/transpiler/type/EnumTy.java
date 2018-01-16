package blue.origami.transpiler.type;

//enum Season = Spring|Summer|Fall|Winter
//enum Mutable

public class EnumTy extends Ty {

	String name;
	String[] names;

	public boolean isBool() {
		return this.names.length <= 2;
	}

	public boolean valueOf(String name) {
		if (this.names.length == 0) {
			return (name.equals(this.name));
		}
		return (name.equals(this.names[0]));
	}

	public int valueOf2(String name) {
		for (int i = 0; i < this.names.length; i++) {
			if (this.names[i].equals(name)) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public boolean acceptTy(boolean sub, Ty codeTy, VarLogger logs) {
		if (codeTy instanceof EnumTy) {
			return ((EnumTy) codeTy).name.equals(this.name);
		}
		return false;
	}

	@Override
	public boolean isDynamic() {
		return false;
	}

	@Override
	public Ty staticTy() {
		return this;
	}

	@Override
	public boolean hasVar() {
		return false;
	}

	@Override
	public <C> C mapType(TypeMap<C> codeType) {
		return Ty.tInt.mapType(codeType);
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append(this.name);
	}

}
