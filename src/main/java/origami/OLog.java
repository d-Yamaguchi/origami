/***********************************************************************
 * Copyright 2017 Kimio Kuramitsu and ORIGAMI project
 *  *
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

package origami;

import origami.code.OCode;
import origami.code.OWarningCode;
import origami.nez.ast.Tree;
import origami.rule.OFmt;
import origami.trait.OStringBuilder;

public class OLog {
	public Tree<?> s;
	public final int level;
	public final String format;
	public final Object[] args;
	public OLog next = null;

	public OLog(Tree<?> s, int level, String format, Object... args) {
		this.s = s;
		this.level = level;
		this.format = format;
		this.args = filter(args);
	}

	public static Object[] filter(Object... args) {
		class ArrayFormatter {
			Object[] a;
			String delim;

			ArrayFormatter(Object[] a, String delim) {
				this.a = a;
				this.delim = delim;
			}

			@Override
			public String toString() {
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < a.length; i++) {
					if (i > 0) {
						sb.append(delim);
					}
					Object o = a[i];
					if (o instanceof OCode) {
						o = ((OCode) o).getType();
					}
					sb.append(o);
				}
				return sb.toString();
			}
		}
		for (int i = 0; i < args.length; i++) {
			if (args[i] != null && args[i].getClass().isArray()) {
				args[i] = new ArrayFormatter((Object[]) args[i], ",");
			}
		}
		return args;
	}

	public void setSource(Tree<?> s) {
		if (this.s == null && s != null) {
			// ODebug.trace("pos=%s %s", s.getSourcePosition(), s);
			this.s = s;
		}
	}

	public OLog next() {
		return this.next;
	}

	@Override
	public String toString() {
		String msgType = (level == Error ? OFmt.error : OFmt.warning).toString();
		if (s != null) {
			return s.getSource().formatPositionLine(msgType, s.getSourcePosition(), OStringBuilder.format(format, args));
		}
		return "(unknown source) [" + msgType + "]" + OStringBuilder.format(format, args);
	}

	public final static int Error = 1;
	public final static int Warning = 1 << 1;
	public final static int Notice = 1 << 2;
	public final static int Info = 1 << 3;
	public final static int Syntax = 1 << 4;
	public final static int Type = 1 << 5;

	public final static int TypeInfo = Info | Type;

	static void report(OEnv env, int level, String msg) {
		if ((level & Error) == Error) {
			OConsole.beginColor(OConsole.Red);
			msg = OConsole.bold(msg);
		} else if ((level & Notice) == Notice) {
			OConsole.beginColor(OConsole.Cyan);
		} else {
			OConsole.beginColor(OConsole.Magenta);
		}
		OConsole.println(msg);
		OConsole.endColor();
	}

	public static void reportError(OEnv env, String msg) {
		report(env, Error, msg);
	}

	public static void reportWarning(OEnv env, String msg) {
		report(env, Warning, msg);
	}

	public static void report(OEnv env, OLog log) {
		report(env, log.level, log.toString());
	}

	public static class Messenger {
		private OLog head = null;

		private void addMessage(Tree<?> s, int level, String format, Object[] args) {
			OLog m = new OLog(s, level, format, filter(args));
			if (head == null) {
				head = m;
				return;
			}
			while (head.next != null) {
				head = head.next;
			}
			head.next = m;
		}

		private Object[] filter(Object[] args) {
			return args;
		}

		public void reportError(Tree<?> s, String fmt, Object... args) {
			addMessage(s, Error, fmt, args);
		}

		public void reportWarning(Tree<?> s, String fmt, Object... args) {
			addMessage(s, Warning, fmt, args);
		}

		public void reportNotice(Tree<?> s, String fmt, Object... args) {
			addMessage(s, Notice, fmt, args);
		}

		public OCode newMessageCode(OCode code) {
			if (head == null) {
				return code;
			}
			return new OWarningCode(code, head);
		}

	}

}
