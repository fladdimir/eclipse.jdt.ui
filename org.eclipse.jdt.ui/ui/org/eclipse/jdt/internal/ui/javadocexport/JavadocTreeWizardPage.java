/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javadocexport;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.jdt.launching.JavaRuntime;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeItem;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaElementSorter;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.jarpackager.CheckboxTreeAndListGroup;
import org.eclipse.jdt.internal.ui.preferences.JavadocPreferencePage;
import org.eclipse.jdt.internal.ui.preferences.PreferencePageSupport;
import org.eclipse.jdt.internal.ui.util.SWTUtil;

public class JavadocTreeWizardPage extends JavadocWizardPage {

	private CheckboxTreeAndListGroup fInputGroup;

	private Text fDestinationText;
	private Text fJavadocCommandText;
	private Text fDocletText;
	private Text fDocletTypeText;
	private Button fStandardButton;
	private Button fDestinationBrowserButton;
	private Button fCustomButton;
	private Button fPrivateVisibility;
	private Button fProtectedVisibility;
	private Button fPackageVisibility;
	private Button fPublicVisibility;
	private Label fDocletLabel;
	private Label fDocletTypeLabel;
	private Label fDestinationLabel;
	private CLabel fDescriptionLabel;
	
	private String fVisibilitySelection;

	private JavadocOptionsManager fStore;

	private StatusInfo fJavadocStatus;
	private StatusInfo fDestinationStatus;
	private StatusInfo fDocletStatus;
	private StatusInfo fTreeStatus;
	private StatusInfo fPreferenceStatus;
	private StatusInfo fWizardStatus;

	private final int PREFERENCESTATUS= 0;
	private final int CUSTOMSTATUS= 1;
	private final int STANDARDSTATUS= 2;
	private final int TREESTATUS= 3;
	private final int JAVADOCSTATUS= 4;

	/**
	 * Constructor for JavadocTreeWizardPage.
	 * @param pageName
	 */
	protected JavadocTreeWizardPage(String pageName, JavadocOptionsManager store) {
		super(pageName);
		setDescription(JavadocExportMessages.getString("JavadocTreeWizardPage.javadoctreewizardpage.description")); //$NON-NLS-1$

		fStore= store;

		// Status variables
		fJavadocStatus= new StatusInfo();
		fDestinationStatus= new StatusInfo();
		fDocletStatus= new StatusInfo();
		fTreeStatus= new StatusInfo();
		fPreferenceStatus= new StatusInfo();
		fWizardStatus= store.getWizardStatus();
	}

	/*
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);

		final Composite composite= new Composite(parent, SWT.NONE);
		final GridLayout layout= new GridLayout();
		layout.numColumns= 6;
		composite.setLayout(layout);

		createJavadocCommandSet(composite);
		createInputGroup(composite);
		createVisibilitySet(composite);
		createOptionsSet(composite);

		setControl(composite);
		Dialog.applyDialogFont(composite);
		WorkbenchHelp.setHelp(composite, IJavaHelpContextIds.JAVADOC_TREE_PAGE);
	}
	
	protected void createJavadocCommandSet(Composite composite) {
		
		final int numColumns= 2;
		
		GridLayout layout= createGridLayout(numColumns);
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		Composite group = new Composite(composite, SWT.NONE);
		group.setLayoutData(createGridData(GridData.FILL_HORIZONTAL, 6, 0));
		group.setLayout(layout);

		createLabel(group, SWT.NONE, JavadocExportMessages.getString("JavadocTreeWizardPage.javadoccommand.label"), createGridData(GridData.HORIZONTAL_ALIGN_BEGINNING, numColumns, 0)); //$NON-NLS-1$
		fJavadocCommandText= createText(group, SWT.READ_ONLY | SWT.SINGLE | SWT.BORDER, null, createGridData(GridData.FILL_HORIZONTAL, numColumns - 1, 0));

		fJavadocCommandText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				doValidation(JAVADOCSTATUS);
			}
		});

		final Button javadocCommandBrowserButton= createButton(group, SWT.PUSH, JavadocExportMessages.getString("JavadocTreeWizardPage.javadoccommand.button.label"), createGridData(GridData.HORIZONTAL_ALIGN_FILL, 1, 0)); //$NON-NLS-1$
		SWTUtil.setButtonDimensionHint(javadocCommandBrowserButton);

		javadocCommandBrowserButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				JavadocPreferencePage page= new JavadocPreferencePage();
				PreferencePageSupport.showPreferencePage(getShell(), JavadocPreferencePage.ID, page); //$NON-NLS-1$
				fJavadocCommandText.setText(JavadocPreferencePage.getJavaDocCommand());
			}
		});
	}
	

	
	
	protected void createInputGroup(Composite composite) {

		createLabel(composite, SWT.NONE, JavadocExportMessages.getString("JavadocTreeWizardPage.checkboxtreeandlistgroup.label"), createGridData(6)); //$NON-NLS-1$
		Composite c= new Composite(composite, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.numColumns= 1;
		layout.makeColumnsEqualWidth= true;
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		c.setLayout(layout);
		c.setLayoutData(createGridData(GridData.FILL_HORIZONTAL, 6, 0));
		
		ITreeContentProvider treeContentProvider= new JavadocProjectContentProvider();
		ITreeContentProvider listContentProvider= new JavadocMemberContentProvider();
		fInputGroup= new CheckboxTreeAndListGroup(c, this, treeContentProvider, new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT), listContentProvider, new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT), SWT.NONE, convertWidthInCharsToPixels(60), convertHeightInCharsToPixels(10));

		fInputGroup.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent e) {
				doValidation(TREESTATUS);
			}
		});
		fInputGroup.setTreeSorter(new JavaElementSorter());
		
		IJavaElement[] elements= fStore.getInitialElements();
		setTreeChecked(elements);
		if (elements.length > 0) {
			fInputGroup.setTreeSelection(new StructuredSelection(elements[0].getJavaProject()));
		}

		fInputGroup.aboutToOpen();
	}

	private void createVisibilitySet(Composite composite) {

		GridLayout visibilityLayout= createGridLayout(4);
		visibilityLayout.marginHeight= 0;
		visibilityLayout.marginWidth= 0;
		Composite visibilityGroup= new Composite(composite, SWT.NONE);
		visibilityGroup.setLayoutData(createGridData(GridData.FILL_HORIZONTAL, 6, 0));
		visibilityGroup.setLayout(visibilityLayout);

		createLabel(visibilityGroup, SWT.NONE, JavadocExportMessages.getString("JavadocTreeWizardPage.visibilitygroup.label"), createGridData(GridData.FILL_HORIZONTAL, 4, 0)); //$NON-NLS-1$
		fPrivateVisibility= createButton(visibilityGroup, SWT.RADIO, JavadocExportMessages.getString("JavadocTreeWizardPage.privatebutton.label"), createGridData(GridData.FILL_HORIZONTAL, 1, 0)); //$NON-NLS-1$
		fPackageVisibility= createButton(visibilityGroup, SWT.RADIO, JavadocExportMessages.getString("JavadocTreeWizardPage.packagebutton.label"), createGridData(GridData.FILL_HORIZONTAL, 1, 0)); //$NON-NLS-1$
		fProtectedVisibility= createButton(visibilityGroup, SWT.RADIO, JavadocExportMessages.getString("JavadocTreeWizardPage.protectedbutton.label"), createGridData(GridData.FILL_HORIZONTAL, 1, 0)); //$NON-NLS-1$
		fPublicVisibility= createButton(visibilityGroup, SWT.RADIO, JavadocExportMessages.getString("JavadocTreeWizardPage.publicbutton.label"), createGridData(GridData.FILL_HORIZONTAL, 1, 0)); //$NON-NLS-1$

		fDescriptionLabel= new CLabel(visibilityGroup, SWT.LEFT);
		fDescriptionLabel.setLayoutData(createGridData(GridData.FILL_HORIZONTAL, 4, convertWidthInCharsToPixels(3) -  3)); // INDENT of CLabel

		fPrivateVisibility.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (((Button) e.widget).getSelection()) {
					fVisibilitySelection= fStore.PRIVATE;
					fDescriptionLabel.setText(JavadocExportMessages.getString("JavadocTreeWizardPage.privatevisibilitydescription.label")); //$NON-NLS-1$
				}
			}
		});
		fPackageVisibility.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (((Button) e.widget).getSelection()) {
					fVisibilitySelection= fStore.PACKAGE;
					fDescriptionLabel.setText(JavadocExportMessages.getString("JavadocTreeWizardPage.packagevisibledescription.label")); //$NON-NLS-1$
				}
			}
		});
		fProtectedVisibility.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (((Button) e.widget).getSelection()) {
					fVisibilitySelection= fStore.PROTECTED;
					fDescriptionLabel.setText(JavadocExportMessages.getString("JavadocTreeWizardPage.protectedvisibilitydescription.label")); //$NON-NLS-1$
				}
			}
		});

		fPublicVisibility.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (((Button) e.widget).getSelection()) {
					fVisibilitySelection= fStore.PUBLIC;
					fDescriptionLabel.setText(JavadocExportMessages.getString("JavadocTreeWizardPage.publicvisibilitydescription.label")); //$NON-NLS-1$
				}
			}
		});

		setVisibilitySettings();

	}

	protected void setVisibilitySettings() {

		fVisibilitySelection= fStore.getAccess();
		fPrivateVisibility.setSelection(fVisibilitySelection.equals(fStore.PRIVATE));
		if (fPrivateVisibility.getSelection())
			fDescriptionLabel.setText(JavadocExportMessages.getString("JavadocTreeWizardPage.privatevisibilitydescription.label")); //$NON-NLS-1$
		//$NON-NLS-1$
		fProtectedVisibility.setSelection(fVisibilitySelection.equals(fStore.PROTECTED));
		if (fProtectedVisibility.getSelection())
			fDescriptionLabel.setText(JavadocExportMessages.getString("JavadocTreeWizardPage.protectedvisibilitydescription.label")); //$NON-NLS-1$
		//$NON-NLS-1$
		fPackageVisibility.setSelection(fVisibilitySelection.equals(fStore.PACKAGE));
		if (fPackageVisibility.getSelection())
			fDescriptionLabel.setText(JavadocExportMessages.getString("JavadocTreeWizardPage.packagevisibledescription.label")); //$NON-NLS-1$
		//$NON-NLS-1$
		fPublicVisibility.setSelection(fVisibilitySelection.equals(fStore.PUBLIC));
		if (fPublicVisibility.getSelection())
			fDescriptionLabel.setText(JavadocExportMessages.getString("JavadocTreeWizardPage.publicvisibilitydescription.label")); //$NON-NLS-1$
		//$NON-NLS-1$
	}

	private void createOptionsSet(Composite composite) {
		
		final int numColumns= 4;

		final GridLayout layout= createGridLayout(numColumns);
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		Composite group= new Composite(composite, SWT.NONE);
		group.setLayoutData(createGridData(GridData.FILL_HORIZONTAL, 6, 0));
		group.setLayout(layout);

		fStandardButton= createButton(group, SWT.RADIO, JavadocExportMessages.getString("JavadocTreeWizardPage.standarddocletbutton.label"), createGridData(GridData.HORIZONTAL_ALIGN_FILL, numColumns, 0)); //$NON-NLS-1$

		fDestinationLabel= createLabel(group, SWT.NONE, JavadocExportMessages.getString("JavadocTreeWizardPage.destinationfield.label"), createGridData(GridData.HORIZONTAL_ALIGN_FILL, 1, convertWidthInCharsToPixels(3))); //$NON-NLS-1$?
		fDestinationText= createText(group, SWT.SINGLE | SWT.BORDER, null, createGridData(GridData.FILL_HORIZONTAL, numColumns - 2, 0));
		((GridData) fDestinationText.getLayoutData()).widthHint= 0;
		fDestinationText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				doValidation(STANDARDSTATUS);
			}
		});

		fDestinationBrowserButton= createButton(group, SWT.PUSH, JavadocExportMessages.getString("JavadocTreeWizardPage.destinationbrowse.label"), createGridData(GridData.HORIZONTAL_ALIGN_END, 1, 0)); //$NON-NLS-1$
		SWTUtil.setButtonDimensionHint(fDestinationBrowserButton);

		//Option to use custom doclet
		fCustomButton= createButton(group, SWT.RADIO, JavadocExportMessages.getString("JavadocTreeWizardPage.customdocletbutton.label"), createGridData(GridData.HORIZONTAL_ALIGN_FILL, numColumns, 0)); //$NON-NLS-1$
		
		//For Entering location of custom doclet
		fDocletTypeLabel= createLabel(group, SWT.NONE, JavadocExportMessages.getString("JavadocTreeWizardPage.docletnamefield.label"), createGridData(GridData.HORIZONTAL_ALIGN_BEGINNING, 1, convertWidthInCharsToPixels(3))); //$NON-NLS-1$
		fDocletTypeText= createText(group, SWT.SINGLE | SWT.BORDER, null, createGridData(GridData.HORIZONTAL_ALIGN_FILL, numColumns - 1, 0));
		((GridData) fDocletTypeText.getLayoutData()).widthHint= 0;
		
		
		fDocletTypeText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				doValidation(CUSTOMSTATUS);
			}

		});

		fDocletLabel= createLabel(group, SWT.NONE, JavadocExportMessages.getString("JavadocTreeWizardPage.docletpathfield.label"), createGridData(GridData.HORIZONTAL_ALIGN_BEGINNING, 1, convertWidthInCharsToPixels(3))); //$NON-NLS-1$
		fDocletText= createText(group, SWT.SINGLE | SWT.BORDER, null, createGridData(GridData.HORIZONTAL_ALIGN_FILL, numColumns - 1, 0));
		((GridData) fDocletText.getLayoutData()).widthHint= 0;
		
		fDocletText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				doValidation(CUSTOMSTATUS);
			}

		});

		//Add Listeners
		fCustomButton.addSelectionListener(new EnableSelectionAdapter(new Control[] { fDocletLabel, fDocletText, fDocletTypeLabel, fDocletTypeText }, new Control[] { fDestinationLabel, fDestinationText, fDestinationBrowserButton }));
		fCustomButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				doValidation(CUSTOMSTATUS);
			}
		});
		fStandardButton.addSelectionListener(new EnableSelectionAdapter(new Control[] { fDestinationLabel, fDestinationText, fDestinationBrowserButton }, new Control[] { fDocletLabel, fDocletText, fDocletTypeLabel, fDocletTypeText }));
		fStandardButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				doValidation(STANDARDSTATUS);
			}
		});
		fDestinationBrowserButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
										String text= handleFolderBrowseButtonPressed(fDestinationText.getText(), fDestinationText.getShell(), JavadocExportMessages.getString("JavadocTreeWizardPage.destinationbrowsedialog.title"), //$NON-NLS-1$
						    		JavadocExportMessages.getString("JavadocTreeWizardPage.destinationbrowsedialog.label")); //$NON-NLS-1$
				fDestinationText.setText(text);
			}
		});

		setOptionSetSettings();
	}

	public boolean getCustom() {
		return fCustomButton.getSelection();
	}

	private void setOptionSetSettings() {

		if (!fStore.isFromStandard()) {
			fCustomButton.setSelection(true);
			fDocletText.setText(fStore.getDocletPath());
			fDocletTypeText.setText(fStore.getDocletName());
			fDestinationText.setText(fStore.getDestination());
			fDestinationText.setEnabled(false);
			fDestinationBrowserButton.setEnabled(false);
			fDestinationLabel.setEnabled(false);
			
		} else {
			fStandardButton.setSelection(true);
			fDestinationText.setText(fStore.getDestination());
			fDocletText.setText(fStore.getDocletPath());
			fDocletTypeText.setText(fStore.getDocletName());
			fDocletText.setEnabled(false);
			fDocletLabel.setEnabled(false);
			fDocletTypeText.setEnabled(false);
			fDocletTypeLabel.setEnabled(false);
		}
		String javadocCommand = JavadocPreferencePage.getJavaDocCommand();
		fJavadocCommandText.setText(javadocCommand);
		fJavadocCommandText.setToolTipText(javadocCommand);
	}

	/**
	 * Receives of list of elements selected by the user and passes them
	 * to the CheckedTree. List can contain multiple projects and elements from
	 * different projects. If the list of seletected elements is empty a default
	 * project is selected.
	 */
	private void setTreeChecked(IJavaElement[] sourceElements) {
		for (int i= 0; i < sourceElements.length; i++) {
			IJavaElement curr= sourceElements[i];
			if (curr instanceof ICompilationUnit) {
				fInputGroup.initialCheckListItem(curr);
			} else if (curr instanceof IPackageFragment) {
				fInputGroup.initialCheckTreeItem(curr);
			} else if (curr instanceof IJavaProject) {
				fInputGroup.initialCheckTreeItem(curr);
			} else if (curr instanceof IPackageFragmentRoot) {
				IPackageFragmentRoot root= (IPackageFragmentRoot) curr;
				if (!root.isArchive())
					fInputGroup.initialCheckTreeItem(curr);
			}
		}
	}

	private IPath[] getSourcePath(IJavaProject[] projects) {
		HashSet res= new HashSet();
		//loops through all projects and gets a list if of thier sourpaths
		for (int k= 0; k < projects.length; k++) {
			IJavaProject iJavaProject= projects[k];

			try {
				IPackageFragmentRoot[] roots= iJavaProject.getPackageFragmentRoots();
				for (int i= 0; i < roots.length; i++) {
					IPackageFragmentRoot curr= roots[i];
					if (curr.getKind() == IPackageFragmentRoot.K_SOURCE) {
						IResource resource= curr.getResource();
						if (resource != null) {
							IPath p= resource.getLocation();
							if (p != null) {
								res.add(p);
							}
						}
					}
				}
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
		}
		return (IPath[]) res.toArray(new IPath[res.size()]);
	}

	private IPath[] getClassPath(IJavaProject[] javaProjects) {
		HashSet res= new HashSet();

		for (int j= 0; j < javaProjects.length; j++) {
			IJavaProject curr= javaProjects[j];

			try {
				IPath p= curr.getProject().getLocation();
				if (p == null)
					continue;

				IPath outputLocation= p.append(curr.getOutputLocation());
				String[] classPath= JavaRuntime.computeDefaultRuntimeClassPath(curr);

				for (int i= 0; i < classPath.length; i++) {
					IPath path= new Path(classPath[i]);
					if (!outputLocation.equals(path)) {
						res.add(path);
					}
				}
			} catch (CoreException e) {
				JavaPlugin.log(e);
			}
		}
		return (IPath[]) res.toArray(new IPath[res.size()]);
	}

	/**
	 * Gets a list of elements to generated javadoc for from each project. 
	 * Javadoc can be generated for either a IPackageFragmentRoot or a ICompilationUnit.
	 */
	private IJavaElement[] getSourceElements(IJavaProject[] projects) {
		ArrayList res= new ArrayList();
		try {
			Set allChecked= fInputGroup.getAllCheckedTreeItems();

			Set incompletePackages= new HashSet();
			for (int h= 0; h < projects.length; h++) {
				IJavaProject iJavaProject= projects[h];

				IPackageFragmentRoot[] roots= iJavaProject.getPackageFragmentRoots();
				for (int i= 0; i < roots.length; i++) {
					IPackageFragmentRoot root= roots[i];
					if (root.getKind() == IPackageFragmentRoot.K_SOURCE) {
						IJavaElement[] packs= root.getChildren();
						for (int k= 0; k < packs.length; k++) {
							IJavaElement curr= packs[k];
							if (curr.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
								// default packages are always incomplete
								if (curr.getElementName().length() == 0 || !allChecked.contains(curr) || fInputGroup.isTreeItemGreyChecked(curr)) {
									incompletePackages.add(curr.getElementName());
								}
							}
						}
					}
				}
			}

			Iterator checkedElements= fInputGroup.getAllCheckedListItems();
			while (checkedElements.hasNext()) {
				Object element= checkedElements.next();
				if (element instanceof ICompilationUnit) {
					ICompilationUnit unit= (ICompilationUnit) element;
					if (incompletePackages.contains(unit.getParent().getElementName())) {
						res.add(unit);
					}
				}
			}

			Set addedPackages= new HashSet();

			checkedElements= allChecked.iterator();
			while (checkedElements.hasNext()) {
				Object element= checkedElements.next();
				if (element instanceof IPackageFragment) {
					IPackageFragment fragment= (IPackageFragment) element;
					String name= fragment.getElementName();
					if (!incompletePackages.contains(name) && !addedPackages.contains(name)) {
						res.add(fragment);
						addedPackages.add(name);
					}
				}
			}

		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
		return (IJavaElement[]) res.toArray(new IJavaElement[res.size()]);
	}

	protected void updateStore(IJavaProject[] checkedProjects) {

		if (fCustomButton.getSelection()) {
			fStore.setDocletName(fDocletTypeText.getText());
			fStore.setDocletPath(fDocletText.getText());
			fStore.setFromStandard(false);
		}
		if (fStandardButton.getSelection()) {
			fStore.setFromStandard(true);
			//the destination used in javadoc generation
			fStore.setDestination(fDestinationText.getText());
		}

		fStore.setSourcepath(getSourcePath(checkedProjects));
		fStore.setClasspath(getClassPath(checkedProjects));
		fStore.setAccess(fVisibilitySelection);
		fStore.setSelectedElements(getSourceElements(checkedProjects));
	}

	public IJavaProject[] getCheckedProjects() {
		ArrayList res= new ArrayList();
		TreeItem[] treeItems= fInputGroup.getTree().getItems();
		for (int i= 0; i < treeItems.length; i++) {
			if (treeItems[i].getChecked()) {
				Object curr= treeItems[i].getData();
				if (curr instanceof IJavaProject) {
					res.add(curr);
				}
			}
		}
		return (IJavaProject[]) res.toArray(new IJavaProject[res.size()]);
	}
	
	protected void doValidation(int validate) {

		switch (validate) {
			case PREFERENCESTATUS :
				fPreferenceStatus= new StatusInfo();
				fDocletStatus= new StatusInfo();
				updateStatus(findMostSevereStatus());
				break;
			case CUSTOMSTATUS :

				if (fCustomButton.getSelection()) {
					fDestinationStatus= new StatusInfo();
					fDocletStatus= new StatusInfo();
					String doclet= fDocletTypeText.getText();
					String docletPath= fDocletText.getText();
					if (doclet.length() == 0) {
						fDocletStatus.setError(JavadocExportMessages.getString("JavadocTreeWizardPage.nodocletname.error")); //$NON-NLS-1$

					} else if (JavaConventions.validateJavaTypeName(doclet).matches(IStatus.ERROR)) {
						fDocletStatus.setError(JavadocExportMessages.getString("JavadocTreeWizardPage.invaliddocletname.error")); //$NON-NLS-1$
					} else if ((docletPath.length() == 0) || !validDocletPath(docletPath)) {
						fDocletStatus.setError(JavadocExportMessages.getString("JavadocTreeWizardPage.invaliddocletpath.error")); //$NON-NLS-1$
					}
					updateStatus(findMostSevereStatus());
				}
				break;

			case STANDARDSTATUS :
				if (fStandardButton.getSelection()) {
					fDestinationStatus= new StatusInfo();
					fDocletStatus= new StatusInfo();
					IPath path= new Path(fDestinationText.getText());
					if (Path.ROOT.equals(path) || Path.EMPTY.equals(path)) {
						fDestinationStatus.setError(JavadocExportMessages.getString("JavadocTreeWizardPage.nodestination.error")); //$NON-NLS-1$
					}
					File file= new File(path.toOSString());
					if (!path.isValidPath(path.toOSString()) || file.isFile()) {
						fDestinationStatus.setError(JavadocExportMessages.getString("JavadocTreeWizardPage.invaliddestination.error")); //$NON-NLS-1$
					}
					if ((path.append("package-list").toFile().exists()) || (path.append("index.html").toFile().exists())) //$NON-NLS-1$//$NON-NLS-2$
						fDestinationStatus.setWarning(JavadocExportMessages.getString("JavadocTreeWizardPage.warning.mayoverwritefiles")); //$NON-NLS-1$
					updateStatus(findMostSevereStatus());
				}
				break;

			case TREESTATUS :

				fTreeStatus= new StatusInfo();

				if (fInputGroup.getAllCheckedTreeItems().size() == 0)
					fTreeStatus.setError(JavadocExportMessages.getString("JavadocTreeWizardPage.invalidtreeselection.error")); //$NON-NLS-1$
				updateStatus(findMostSevereStatus());

				break;
				
			case JAVADOCSTATUS:
				fJavadocStatus= new StatusInfo();
				IPath path= new Path(fJavadocCommandText.getText());
				if (!path.toFile().isFile()) {
					fJavadocStatus.setError(JavadocExportMessages.getString("JavadocTreeWizardPage.javadoccommandfile.error"));  //$NON-NLS-1$
				}
				updateStatus(findMostSevereStatus());
				break;
		} //end switch
	}

	private boolean validDocletPath(String docletPath) {
		StringTokenizer tokens= new StringTokenizer(docletPath, ";"); //$NON-NLS-1$
		while (tokens.hasMoreTokens()) {
			File file= new File(tokens.nextToken());
			if (!file.exists())
				return false;
		}
		return true;
	}

	/**
	 * Finds the most severe error (if there is one)
	 */
	private IStatus findMostSevereStatus() {
		return StatusUtil.getMostSevere(new IStatus[] { fJavadocStatus, fPreferenceStatus, fDestinationStatus, fDocletStatus, fTreeStatus, fWizardStatus });
	}

	public void init() {
		updateStatus(new StatusInfo());
	}

	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			doValidation(JAVADOCSTATUS);
			doValidation(STANDARDSTATUS);
			doValidation(CUSTOMSTATUS);
			doValidation(TREESTATUS);
			doValidation(PREFERENCESTATUS);
		}
	}

}
