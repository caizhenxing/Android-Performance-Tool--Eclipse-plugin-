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

import java.io.IOException;
import java.util.Collection;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.objectweb.asm.tree.ClassNode;

import com.android.ide.eclipse.apt.internal.analysis.SpecificAnalyzer.Problem;

/**
 * This class performs all the analysis in order to cover the guidelines 
 * suggested to obtain good performances in an Android application.
 * @author Mattia Gustarini
 *
 */
public final class Analyzer {
	private final static String SRC_FOLDER = "src";
	private final static String JAVA_FILE = ".java";
	/**
	 * Array storing a single instance of each implemented analyzer
	 */
	private final static SpecificAnalyzer[] sAnalyzers = {
		new VirtualInterfaceAnalyzer(),
		new InnerClassAnalyzer(),
		new FloatAnalyzer(),
		new InternalGetSetAnalyzer(),
		new EnumAnalyzer(),
		new StaticVirtualAnalyzer(),
		new ConstantFinalAnalyzer(),
		new EnhancedForLoopAnalyzer()
	};
	
	/**
	 * Start the analysis of the whole project using all the implemented specific analyzers
	 * @throws IOException 
	 */
	public static void analyseProject(final IProject project, final Collection<ClassNode> classNodes) throws IOException {
		//removes all the markers present in the project from previous analysis
		deleteAllMarkers(project);
		//for each class found in the project it performs a complete analysis
		for (final ClassNode classNode : classNodes) {
			final IResource problemResource = getProblemResource(project, classNode);
			if (problemResource != null) {
				//each analyzer is used to analyze the current class node
				for (final SpecificAnalyzer analyzer : sAnalyzers) {
					analyzer.analyse(classNode);
					//after the analysis the found problems are retrieved from the analyzer
					final Collection<Problem> problems = analyzer.getProblems();
					if (!problems.isEmpty()) {
						for (final Problem problem : problems) {
							//the markers are added to the resource where the problem is located
							problem.setMarker(problemResource);
						}
					}
				}
			}
		}
	}
	
	/**
	 * Retrieves the project resource (java source code) for a given class node
	 * @param project The current Android project
	 * @param classNode The current class node from the current Android project
	 * @return The resource related to the current class node
	 */
	private static IResource getProblemResource(final IProject project, final ClassNode classNode) {
		//get the path of the resource by looking to the full class name
		final IPath resourcePath = getResourcePath(classNode.name);
		final IFolder srcFolder = project.getFolder(SRC_FOLDER);
		IResource resource = null;
		if (srcFolder.exists()) {
			resource = srcFolder.findMember(resourcePath);
		}
		return resource;
	}
	
	/**
	 * Retrieves the source path of a class
	 * @param resourcePath The full name of a class encoded as a String
	 * @return The IPath representation of the full class name
	 */
	private static IPath getResourcePath(final String resourcePath) {
		String resourceName = resourcePath;
		final int dollar = resourcePath.lastIndexOf('$');
		if (dollar > -1) {
			resourceName = resourcePath.substring(0, dollar);
		}
		resourceName = resourceName + JAVA_FILE;
		final Path path = new Path(resourceName);
		return path;
	}
	
	/**
	 * Deletes all the markers present in the project
	 * @param resource
	 */
	private static void deleteAllMarkers(final IResource resource) {
		try {
			resource.deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
		} catch (final CoreException e) {
			deleteAllMarkers(resource);
			//retry
			//I know can lead to a loop...
		}
	}
}
