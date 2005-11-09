/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.ui.actions;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;

import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.NewProjectAction;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.PixelConverter;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;

/**
 * <p>Abstract base classed used for the open wizard actions.</p>
 * 
 * <p>
 * Note: This class is for internal use only. Clients should not use this class.
 * </p>
 * @since 3.2
 */
public abstract class AbstractOpenWizardAction extends Action {
	
	private Shell fShell;
	private IStructuredSelection fSelection;
	
	/**
	 * Creates the action.
	 */
	protected AbstractOpenWizardAction() {
		fShell= null;
		fSelection= null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	public void run() {
		Shell shell= getShell();
		if (!doCreateProjectFirstOnEmptyWorkspace(shell)) {
			return;
		}
		try {
			Wizard wizard= createWizard();
			if (wizard instanceof IWorkbenchWizard) {
				((IWorkbenchWizard)wizard).init(PlatformUI.getWorkbench(), getSelection());
			}
			
			WizardDialog dialog= new WizardDialog(shell, wizard);
			if (shell != null) {
				PixelConverter converter= new PixelConverter(shell);
				dialog.setMinimumPageSize(converter.convertWidthInCharsToPixels(70), converter.convertHeightInCharsToPixels(20));
			}
			dialog.create();
			int res= dialog.open();
			
			notifyResult(res == Window.OK);
		} catch (CoreException e) {
			String title= NewWizardMessages.AbstractOpenWizardAction_createerror_title; 
			String message= NewWizardMessages.AbstractOpenWizardAction_createerror_message; 
			ExceptionHandler.handle(e, shell, title, message);
		}
	}
	
	/**
	 * Creates and configures the wizard. This method should only be called once.
	 * @return returns the created wizard.
	 * @throws CoreException exception is thrown when the creation was not successful.
	 */
	abstract protected Wizard createWizard() throws CoreException;
	
	/**
	 * Returns the configured selection. If no selection has been configured using {@link #setSelection(IStructuredSelection)}},
	 * the currently selected element of the active workbench is returned.
	 * @return the configured selection
	 */
	protected IStructuredSelection getSelection() {
		if (fSelection == null) {
			return evaluateCurrentSelection();
		}
		return fSelection;
	}
			
	private IStructuredSelection evaluateCurrentSelection() {
		IWorkbenchWindow window= JavaPlugin.getActiveWorkbenchWindow();
		if (window != null) {
			ISelection selection= window.getSelectionService().getSelection();
			if (selection instanceof IStructuredSelection) {
				return (IStructuredSelection) selection;
			}
		}
		return StructuredSelection.EMPTY;
	}
	
	/**
	 * Configures the selection to be used as initial selection of the wizard. 
	 * @param selection the selection to be set or <code>null</code> to use the selection of the active workbench window
	 */
	public void setSelection(IStructuredSelection selection) {
		fSelection= selection;
	}
	
	/**
	 * Returns the configured selection. If no shell has been configured using {@link #setShell(Shell)}},
	 * <code>null</code> is returned.
	 * @return the configured shell
	 */
	protected Shell getShell() {
		if (fShell == null) {
			return JavaPlugin.getActiveWorkbenchShell();
		}
		return fShell;
	}
	
	/**
	 * Configures the shell to be used as parent shell by the wizard.
	 * @param shell the shell to be set or <code>null</code> to use the shell of the active workbench window
	 */
	public void setShell(Shell shell) {
		fShell= shell;
	}
	
	/**
	 * Opens the new project dialog if the workspace is empty. This method is called on {@link #run()}.
	 * @param shell the shell to use
	 * @return returns <code>true</code> when a project has been created, or <code>false</code> when the
	 * new project has been canceled.
	 */
	protected boolean doCreateProjectFirstOnEmptyWorkspace(Shell shell) {
		IWorkspaceRoot workspaceRoot= ResourcesPlugin.getWorkspace().getRoot();
		if (workspaceRoot.getProjects().length == 0) {
			String title= NewWizardMessages.AbstractOpenWizardAction_noproject_title; 
			String message= NewWizardMessages.AbstractOpenWizardAction_noproject_message; 
			if (MessageDialog.openQuestion(shell, title, message)) {
				new NewProjectAction().run();
				return workspaceRoot.getProjects().length != 0;
			}
			return false;
		}
		return true;
	}
	
	
}
