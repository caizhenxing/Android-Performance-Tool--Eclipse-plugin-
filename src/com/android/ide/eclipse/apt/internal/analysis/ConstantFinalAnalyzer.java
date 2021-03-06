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
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

/**
 * This analyzer checks if there are possible constant declarations that are not final
 * following the guideline:<br/>
 * @see <a href="http://developer.android.com/guide/practices/design/performance.html#use_final">Declare Constants Final</a>
 * @author Mattia Gustarini
 */
public final class ConstantFinalAnalyzer extends SpecificAnalyzer {
	private ClassNode mCurrentClass;
	
	public ConstantFinalAnalyzer() {
		super("Declare Constants Final", "If it is a constant declare it final!");
	}
	
	/* (non-Javadoc)
	 * @see com.android.ide.eclipse.apt.internal.analysis.SpecificAnalyzer#analyzeClass(org.objectweb.asm.tree.ClassNode)
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected Collection<Problem> analyzeClass(final ClassNode classNode) {
		final Collection<Problem> problems = new LinkedList<Problem>();
		if ((classNode.access & Opcodes.ACC_ENUM) == 0) {
			final List<MethodNode> methodNodes = (List<MethodNode>)classNode.methods;
			for (final MethodNode methodNode : methodNodes) {
				if (methodNode.name.equals("<clinit>")) {
					mCurrentClass = classNode;
					final Collection<Problem> foundProblems = analyzeMethod(methodNode);
					problems.addAll(foundProblems);
				}
			}
		}
		return problems;
	}

	/* (non-Javadoc)
	 * @see ch.usi.inf.gustarim.apt.analysis.SpecificAnalyzer#analyzeMethod(org.objectweb.asm.tree.MethodNode)
	 */
	@Override
	protected Collection<Problem> analyzeMethod(final MethodNode methodNode) {
		final LinkedList<Problem> problems = new LinkedList<Problem>();
		final InsnList instructions = methodNode.instructions;
		for (int i = 0; i < instructions.size(); i++) {
			final AbstractInsnNode insnNode = instructions.get(i);
			if (insnNode.getOpcode() == Opcodes.PUTSTATIC) {
				final FieldInsnNode field = (FieldInsnNode)insnNode;
				final String fieldName = field.name;
				if (!isFinal(fieldName)) {
					final Problem problem = new Problem(insnNode);
					problems.add(problem);					
				}
			}
		}
		return problems;
	}
	
	/**
	 * Check if a given field of a class is final or not
	 * @param fieldName Field name of a class
	 * @return true if the field is final, false otherwise
	 */
	@SuppressWarnings("unchecked")
	private boolean isFinal(final String fieldName) {
		boolean isFinal = false;
		final List<FieldNode> fields = (List<FieldNode>)mCurrentClass.fields;
		for (final FieldNode fieldNode : fields) {
			if (fieldNode.name.equals(fieldName)) {
				isFinal = (fieldNode.access & Opcodes.ACC_FINAL) != 0;
				break;
			}
		}
		return isFinal;
	}
}
