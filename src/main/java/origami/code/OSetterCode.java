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

package origami.code;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.Optional;

import origami.OEnv;
import origami.asm.OAsm;
import origami.lang.OField;
import origami.lang.OSetter;
import origami.trait.OTypeUtils;
import origami.type.OType;

public class OSetterCode extends OParamCode<OField> implements DynamicInvokable {

	public OSetterCode(OField handled, OType ty, OCode... node) {
		super(handled, ty, node);
	}

	@Override
	public Object eval(OEnv env) throws Throwable {
		Field field = this.getHandled().unwrap(env);
		Object[] values = evalParams(env, nodes);
		if (OTypeUtils.isStatic(field)) {
			field.set(null, values[0]);
			return values[0];
		} else {
			field.set(values[0], values[1]);
			return values[1];
		}
	}

	public OCode expr() {
		return this.nodes.length > 1 ? this.nodes[1] : this.nodes[0];
	}

	public Optional<OCode> recv() {
		return Optional.ofNullable(this.nodes.length > 1 ? this.nodes[0] : null);
	}

	@Override
	public void generate(OAsm gen) {
		gen.pushSetter(this);
	}

	@Override
	public MethodHandle getMethodHandle(OEnv env, MethodHandles.Lookup lookup) throws Throwable {
		return new OSetter(getHandled()).getMethodHandle(env, lookup);
	}

}