/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.jdt.internal.ui.text.correction;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

public class CorrectPackageDeclarationProposal extends CUCorrectionProposal {

	private ProblemPosition fProblemPosition;

	public CorrectPackageDeclarationProposal(ProblemPosition problemPos, int relevance) throws CoreException {
		super(CorrectionMessages.getString("CorrectPackageDeclarationProposal.name"), problemPos.getCompilationUnit(), relevance); //$NON-NLS-1$
		fProblemPosition= problemPos;
	}

	/*
	 * @see CUCorrectionProposal#addEdits(CompilationUnitChange)
	 */
	protected void addEdits(CompilationUnitChange change) throws CoreException {
		ICompilationUnit cu= change.getCompilationUnit();
		
		IPackageFragment parentPack= (IPackageFragment) cu.getParent();
		IPackageDeclaration[] decls= cu.getPackageDeclarations();
		
		if (parentPack.isDefaultPackage() && decls.length > 0) {
			for (int i= 0; i < decls.length; i++) {
				ISourceRange range= decls[i].getSourceRange();
				change.addTextEdit(CorrectionMessages.getString("CorrectPackageDeclarationProposal.removeedit.label"), SimpleTextEdit.createDelete(range.getOffset(), range.getLength())); //$NON-NLS-1$
			}
			return;
		}
		if (!parentPack.isDefaultPackage() && decls.length == 0) {
			String lineDelim= StubUtility.getLineDelimiterUsed(cu);
			String str= "package " + parentPack.getElementName() + ";" + lineDelim + lineDelim; //$NON-NLS-1$ //$NON-NLS-2$
			change.addTextEdit(CorrectionMessages.getString("CorrectPackageDeclarationProposal.addedit.label"), SimpleTextEdit.createInsert(0, str)); //$NON-NLS-1$
			return;
		}
		
		ProblemPosition pos= fProblemPosition;
		change.addTextEdit(CorrectionMessages.getString("CorrectPackageDeclarationProposal.changenameedit.label"), SimpleTextEdit.createReplace(pos.getOffset(), pos.getLength(), parentPack.getElementName())); //$NON-NLS-1$
	}
	
	/*
	 * @see ICompletionProposal#getDisplayString()
	 */
	public String getDisplayString() {
		ICompilationUnit cu= fProblemPosition.getCompilationUnit();
		IPackageFragment parentPack= (IPackageFragment) cu.getParent();
		try {
			IPackageDeclaration[] decls= cu.getPackageDeclarations();		
			if (parentPack.isDefaultPackage() && decls.length > 0) {
				return CorrectionMessages.getString("CorrectPackageDeclarationProposal.remove.description") + decls[0].getElementName() + ";'"; //$NON-NLS-1$ //$NON-NLS-2$
			}
			if (!parentPack.isDefaultPackage() && decls.length == 0) {	
				return (CorrectionMessages.getString("CorrectPackageDeclarationProposal.add.description") + parentPack.getElementName() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		} catch(JavaModelException e) {
			JavaPlugin.log(e);
		}
		return (CorrectionMessages.getString("CorrectPackageDeclarationProposal.change.description") + parentPack.getElementName() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
	}	

	public Image getImage() {
		return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_PACKDECL);
	}

}
