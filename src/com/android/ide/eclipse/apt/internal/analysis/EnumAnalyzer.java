/**
 * Copyright (C) Mattia Gustarini
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
 */
package com.android.ide.eclipse.apt.internal.analysis;

import java.util.Collection;
import java.util.LinkedList;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

import com.android.ide.eclipse.apt.internal.crawler.ClassCrawler;

/**
 * This analyzer checks if in the code are present uses of enums
 * following the guideline:<br/>
 * @see <a href="http://developer.android.com/guide/practices/design/performance.html#avoid_enums">Avoid Enums</a>
 * @author Mattia Gustarini
 *
 */
public final class EnumAnalyzer extends SpecificAnalyzer {
	private final LinkedList<String> mEnumClasseNames;
	private boolean mFirstClass;

	public EnumAnalyzer() {
		super("Avoid Enums", "The use of enum is discouraged, because they increase the byte code size and decrease speed!");
		mEnumClasseNames = new LinkedList<String>();
		mFirstClass = true;
	}
	
	/* (non-Javadoc)
	 * @see com.android.ide.eclipse.apt.internal.analysis.SpecificAnalyzer#analyse(org.objectweb.asm.tree.ClassNode)
	 */
	@Override
	public void analyse(final ClassNode classNode) {
		if (mFirstClass) {
			//search for all the Enum classes in the project
			final Collection<ClassNode> classes = ClassCrawler.sClassNodes;
			for (final ClassNode classNode2 : classes) {
				if ((classNode2.access & Opcodes.ACC_ENUM) != 0) {
					mEnumClasseNames.add(classNode2.name);
				}
			}
			mFirstClass = false;
		}
		if ((classNode.access & Opcodes.ACC_ENUM) == 0) {
			super.analyse(classNode);
		}
	}

	/* (non-Javadoc)
	 * @see ch.usi.inf.gustarim.apt.analysis.SpecificAnalyzer#analyzeMethod(org.objectweb.asm.tree.MethodNode)
	 */
	@Override
	protected Collection<Problem> analyzeMethod(MethodNode methodNode) {
		final Collection<Problem> problems = new LinkedList<Problem>();
		final InsnList instructions = methodNode.instructions;
		for (int i = 0; i < instructions.size(); i++) {
			final AbstractInsnNode instruction = instructions.get(i);
			final int op = instruction.getOpcode();
			if (op == Opcodes.GETSTATIC) {
				final FieldInsnNode field = (FieldInsnNode)instruction;
				final String fieldDesc = field.desc;
				final int fieldDescLength = fieldDesc.length();
				if (fieldDescLength > 1) {
					final String type = fieldDesc.substring(1, fieldDesc.length() - 1);
					if (mEnumClasseNames.contains(type)) {
						final Problem problem = new Problem(instruction);
						problems.add(problem);
					}
				}
			}
		}
		return problems;
	}

}
