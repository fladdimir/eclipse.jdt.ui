package org.eclipse.jdt.internal.ui.refactoring;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.ParameterInfo;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.structure.ModifyParametersRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.util.JdtFlags;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class ModifyParametersInputPage extends UserInputWizardPage {

	public static final String PAGE_NAME= "ModifyParametersInputPage"; //$NON-NLS-1$
	private Label fSignaturePreview;
	
	public ModifyParametersInputPage() {
		super(PAGE_NAME, true);
		setMessage(RefactoringMessages.getString("ModifyParametersInputPage.new_order")); //$NON-NLS-1$
	}
	
	/*
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout((new GridLayout()));
		
		createVisibilityControl(composite);
		createParameterTableComposite(composite);
		
		Label label= new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		label.setLayoutData((new GridData(GridData.FILL_HORIZONTAL)));
		
		fSignaturePreview= new Label(composite, SWT.WRAP);
		GridData gl= new GridData(GridData.FILL_BOTH);
		gl.widthHint= convertWidthInCharsToPixels(50);
		fSignaturePreview.setLayoutData(gl);
		update(false);
		
		setControl(composite);
		WorkbenchHelp.setHelp(composite, IJavaHelpContextIds.MODIFY_PARAMETERS_WIZARD_PAGE);
	}
	
	private void createVisibilityControl(Composite parent) {
		try {
			List allowedVisibilities= convertToIntegerList(getModifyParametersRefactoring().getAvailableVisibilities());
			if (allowedVisibilities.size() == 1)
				return;
			
			Composite composite= new Composite(parent, SWT.NONE);
			composite.setLayoutData((new GridData(GridData.FILL_HORIZONTAL)));
			GridLayout layout= new GridLayout();
			layout.numColumns= 2; layout.marginWidth= 0;
			composite.setLayout(layout);
			
			
			Label label= new Label(composite, SWT.NONE);
			label.setText("Access modifier:");
			
			Composite group= new Composite(composite, SWT.NONE);
			group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			layout= new GridLayout();
			layout.numColumns= 4; layout.marginWidth= 0;
			group.setLayout(layout);
			
			String[] labels= new String[] {
				"&public",
				"pro&tected",
				"defa&ult",
				"pri&vate"
			};
			Integer[] data= new Integer[] {
						new Integer(JdtFlags.VISIBILITY_CODE_PUBLIC),
						new Integer(JdtFlags.VISIBILITY_CODE_PROTECTED),
						new Integer(JdtFlags.VISIBILITY_CODE_PACKAGE),
						new Integer(JdtFlags.VISIBILITY_CODE_PRIVATE)};
			Integer initialVisibility= new Integer(getModifyParametersRefactoring().getVisibility());
			for (int i= 0; i < labels.length; i++) {
				Button radio= new Button(group, SWT.RADIO);
				Integer visibilityCode= data[i];
				radio.setText(labels[i]);
				radio.setData(visibilityCode);
				radio.setSelection(visibilityCode.equals(initialVisibility));
				radio.setEnabled(allowedVisibilities.contains(visibilityCode));
				radio.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent event) {
						getModifyParametersRefactoring().setVisibility(((Integer)event.widget.getData()).intValue());
						if (((Button)event.widget).getSelection())
							update(true);
					}
				});
			}
			label.setLayoutData((new GridData()));
			group.setLayoutData((new GridData(GridData.FILL_HORIZONTAL)));
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
			return;
		}
	}
	private static List convertToIntegerList(int[] array) {
		List result= new ArrayList(array.length);
		for (int i= 0; i < array.length; i++) {
			result.add(new Integer(array[i]));
		}
		return result;
	}
	
	private void createParameterTableComposite(Composite composite) {
		String labelText= RefactoringMessages.getString("ModifyParametersInputPage.parameters");
		ChangeParametersControl cp= new ChangeParametersControl(composite, SWT.NONE, labelText, new ParameterListChangeListener() {
			public void parameterChanged(ParameterInfo parameter) {
				update(true);
			}
			public void parameterListChanged() {
				update(true);
			}
		}, true, true);
		cp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
		cp.setInput(getModifyParametersRefactoring().getParameterInfos());
	}

	private ModifyParametersRefactoring getModifyParametersRefactoring(){
		return	(ModifyParametersRefactoring)getRefactoring();
	}

	private void update(boolean displayErrorMessage){
		updateStatus(displayErrorMessage);
		updateSignaturePreview();
	}

	private void updateStatus(boolean displayErrorMessage) {
		try{
			RefactoringStatus nameCheck= getModifyParametersRefactoring().checkParameters();
			if (nameCheck.hasFatalError()){
				if (displayErrorMessage)
					setErrorMessage(nameCheck.getFirstMessage(RefactoringStatus.FATAL));
				setPageComplete(false);
			} else {
				setErrorMessage(null);	
				setPageComplete(true);
			}	
		} catch (JavaModelException e){
			setErrorMessage("Internal Error. Please see log for details.");			setPageComplete(false);
			JavaPlugin.log(e);
		};
	}

	private void updateSignaturePreview() {
		try{
			fSignaturePreview.setText(RefactoringMessages.getString("ModifyParametersInputPage.method_Signature_Preview") + getModifyParametersRefactoring().getMethodSignaturePreview()); //$NON-NLS-1$
		} catch (JavaModelException e){
			ExceptionHandler.handle(e, RefactoringMessages.getString("ModifyParamatersInputPage.modify_Parameters"), RefactoringMessages.getString("ModifyParametersInputPage.exception")); //$NON-NLS-2$ //$NON-NLS-1$
		}	
	}	
}	