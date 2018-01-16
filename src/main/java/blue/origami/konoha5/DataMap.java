package blue.origami.konoha5;

import blue.origami.util.StringCombinator;

public interface DataMap extends Cloneable, StringCombinator {
	public Object getf(int key, Object def);

	public void setf(int key, Object value);

	public DataMap clone();

	public static DummyDataMap Null = new DummyDataMap();

	static class DummyDataMap implements DataMap {

		@Override
		public Object getf(int key, Object def) {
			return def;
		}

		@Override
		public void setf(int key, Object value) {

		}

		@Override
		public DataMap clone() {
			return this;
		}

		@Override
		public void strOut(StringBuilder sb) {
			sb.append("{}");
		}

	}
}
