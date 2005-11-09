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

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.ui.wizards.NewInterfaceWizardPage;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.wizards.NewInterfaceCreationWizard;

/**
 * <p>Action that opens the new interface wizard. The action is not a selection listener, but takes the
 * selection as configured by {@link #setSelection(IStructuredSelection)} or the selection of
 * the active workbench window.</p>
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 *  
 * @since 3.2 
 */
public class OpenNewInterfaceWizardAction extends AbstractOpenWizardAction {
	
	private NewInterfaceWizardPage fPage;

	/**
	 * Creates an instance of the <code>OpenNewInterfaceWizardAction</code>.
	 */
	public OpenNewInterfaceWizardAction() {
		setText(ActionMessages.OpenNewInterfaceWizardAction_text); 
		setDescription(ActionMessages.OpenNewInterfaceWizardAction_description); 
		setToolTipText(ActionMessages.OpenNewInterfaceWizardAction_tooltip); 
		setImageDescriptor(JavaPluginImages.DESC_WIZBAN_NEWINT);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.OPEN_INTERFACE_WIZARD_ACTION);
		setShell(JavaPlugin.getActiveWorkbenchShell());
		
		fPage= null;
	}
	
	/**
	 * Sets a page to be used by the wizard or <code>null</code> to use a page initialized with values
	 * from the current selection (see {@link #getSelection()} and {@link #setSelection(IStructuredSelection)}).
	 * @param page the page to use or <code>null</code>
	 */
	public void setConfiguredWizardPage(NewInterfaceWizardPage page) {
		fPage= page;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.actions.AbstractOpenWizardAction#createWizard()
	 */
	protected final Wizard createWizard() throws CoreException {
		return new NewInterfaceCreationWizard(fPage);
	}
}
