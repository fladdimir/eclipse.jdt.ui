/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.java;

import org.eclipse.jdt.core.dom.CompilationUnit;


/**
 * Interface of an object listening to Java reconciling.
 * 
 * @since 3.0
 */
public interface IJavaReconcilingListener {

	/**
	 * Called before reconciling is started.
	 */
	void aboutToBeReconciled();
	
	/**
	 * Called after reconciling has been finished.
	 * 
	 * @param ast			the compilation unit AST or <code>null</code> if
	 * 							the working copy was consistent or reconciliation has been cancelled
	 * @param cancelled		<code>true</code> iff the reconciliation has been cancelled
	 * @param forced		<code>true</code> iff this reconciliation was forced
	 */
	void reconciled(CompilationUnit ast, boolean cancelled, boolean forced);
}
