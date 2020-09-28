/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences.cleanup;

import java.util.Map;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;

import org.eclipse.jdt.internal.ui.fix.AbstractCleanUp;
import org.eclipse.jdt.internal.ui.fix.AddOrRemoveAllCleanUp;
import org.eclipse.jdt.internal.ui.fix.BitwiseConditionalExpressionCleanup;
import org.eclipse.jdt.internal.ui.fix.ControlStatementsCleanUp;
import org.eclipse.jdt.internal.ui.fix.ConvertLoopCleanUp;
import org.eclipse.jdt.internal.ui.fix.ExpressionsCleanUp;
import org.eclipse.jdt.internal.ui.fix.LambdaExpressionAndMethodRefCleanUp;
import org.eclipse.jdt.internal.ui.fix.LambdaExpressionsCleanUp;
import org.eclipse.jdt.internal.ui.fix.NumberSuffixCleanUp;
import org.eclipse.jdt.internal.ui.fix.SwitchExpressionsCleanUp;
import org.eclipse.jdt.internal.ui.fix.VarCleanUp;
import org.eclipse.jdt.internal.ui.fix.VariableDeclarationCleanUp;

public final class CodeStyleTabPage extends AbstractCleanUpTabPage {
	public static final String ID= "org.eclipse.jdt.ui.cleanup.tabpage.code_style"; //$NON-NLS-1$

	@Override
	protected AbstractCleanUp[] createPreviewCleanUps(Map<String, String> values) {
		return new AbstractCleanUp[] {
				new ControlStatementsCleanUp(values),
				new ConvertLoopCleanUp(values),
				new AddOrRemoveAllCleanUp(values),
				new ExpressionsCleanUp(values),
				new NumberSuffixCleanUp(values),
				new BitwiseConditionalExpressionCleanup(values),
				new VariableDeclarationCleanUp(values),
				new VarCleanUp(values),
				new SwitchExpressionsCleanUp(values),
				new LambdaExpressionsCleanUp(values),
				new LambdaExpressionAndMethodRefCleanUp(values)
		};
	}

	@Override
	protected void doCreatePreferences(Composite composite, int numColumns) {
		Group controlGroup= createGroup(numColumns, composite, CleanUpMessages.CodeStyleTabPage_GroupName_ControlStatments);

		final CheckboxPreference useBlockPref= createCheckboxPref(controlGroup, numColumns, CleanUpMessages.CodeStyleTabPage_CheckboxName_UseBlocks, CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS, CleanUpModifyDialog.FALSE_TRUE);
		intent(controlGroup);
		final RadioPreference useBlockAlwaysPref= createRadioPref(controlGroup, numColumns - 1, CleanUpMessages.CodeStyleTabPage_RadioName_AlwaysUseBlocks, CleanUpConstants.CONTROL_STATMENTS_USE_BLOCKS_ALWAYS, CleanUpModifyDialog.FALSE_TRUE);
		intent(controlGroup);
		final RadioPreference useBlockJDTStylePref= createRadioPref(controlGroup, numColumns - 1, CleanUpMessages.CodeStyleTabPage_RadioName_UseBlocksSpecial, CleanUpConstants.CONTROL_STATMENTS_USE_BLOCKS_NO_FOR_RETURN_AND_THROW, CleanUpModifyDialog.FALSE_TRUE);
		intent(controlGroup);
		final RadioPreference useBlockNeverPref= createRadioPref(controlGroup, numColumns - 1, CleanUpMessages.CodeStyleTabPage_RadioName_NeverUseBlocks, CleanUpConstants.CONTROL_STATMENTS_USE_BLOCKS_NEVER, CleanUpModifyDialog.FALSE_TRUE);
		registerSlavePreference(useBlockPref, new RadioPreference[] {useBlockAlwaysPref, useBlockJDTStylePref, useBlockNeverPref});

		CheckboxPreference convertToSwitchExpressions= createCheckboxPref(controlGroup, numColumns, CleanUpMessages.CodeStyleTabPage_CheckboxName_ConvertToSwitchExpressions, CleanUpConstants.CONTROL_STATEMENTS_CONVERT_TO_SWITCH_EXPRESSIONS, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(convertToSwitchExpressions);

		CheckboxPreference convertLoop= createCheckboxPref(controlGroup, numColumns, CleanUpMessages.CodeStyleTabPage_CheckboxName_ConvertForLoopToEnhanced, CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(convertLoop);
		intent(controlGroup);
		final CheckboxPreference convertLoopOnlyIfLoopVariableUsed= createCheckboxPref(controlGroup, 1, CleanUpMessages.CodeStyleTabPage_CheckboxName_ConvertLoopOnlyIfLoopVarUsed, CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_ONLY_IF_LOOP_VAR_USED, CleanUpModifyDialog.FALSE_TRUE);
		registerSlavePreference(convertLoop, new CheckboxPreference[] {convertLoopOnlyIfLoopVariableUsed});

		final CheckboxPreference addOrRemoveAllPref= createCheckboxPref(controlGroup, numColumns, CleanUpMessages.CodeStyleTabPage_CheckboxName_UseAddAllRemoveAll, CleanUpConstants.CONTROL_STATEMENTS_USE_ADD_REMOVE_ALL,
				CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(addOrRemoveAllPref);

		Group expressionsGroup= createGroup(numColumns, composite, CleanUpMessages.CodeStyleTabPage_GroupName_Expressions);

		final CheckboxPreference useParenthesesPref= createCheckboxPref(expressionsGroup, numColumns, CleanUpMessages.CodeStyleTabPage_CheckboxName_UseParentheses, CleanUpConstants.EXPRESSIONS_USE_PARENTHESES, CleanUpModifyDialog.FALSE_TRUE);
		intent(expressionsGroup);
		final RadioPreference useParenthesesAlwaysPref= createRadioPref(expressionsGroup, 1, CleanUpMessages.CodeStyleTabPage_RadioName_AlwaysUseParantheses, CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_ALWAYS, CleanUpModifyDialog.FALSE_TRUE);
		final RadioPreference useParenthesesNeverPref= createRadioPref(expressionsGroup, 1, CleanUpMessages.CodeStyleTabPage_RadioName_NeverUseParantheses, CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER, CleanUpModifyDialog.FALSE_TRUE);
		registerSlavePreference(useParenthesesPref, new RadioPreference[] {useParenthesesAlwaysPref, useParenthesesNeverPref});

		final CheckboxPreference bitwiseComparison= createCheckboxPref(expressionsGroup, numColumns, CleanUpMessages.CodeStyleTabPage_CheckboxName_CheckSignOfBitwiseOperation, CleanUpConstants.CHECK_SIGN_OF_BITWISE_OPERATION, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(bitwiseComparison);

		Group numberSuffixGroup= createGroup(numColumns, composite, CleanUpMessages.CodeStyleTabPage_GroupName_NumberLiteral);

		final CheckboxPreference numberSuffixPref= createCheckboxPref(numberSuffixGroup, numColumns, CleanUpMessages.CodeStyleTabPage_CheckboxName_NumberSuffix, CleanUpConstants.NUMBER_SUFFIX,
				CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(numberSuffixPref);

		Group variableGroup= createGroup(numColumns, composite, CleanUpMessages.CodeStyleTabPage_GroupName_VariableDeclarations);

		final CheckboxPreference useFinalPref= createCheckboxPref(variableGroup, numColumns, CleanUpMessages.CodeStyleTabPage_CheckboxName_UseFinal, CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL, CleanUpModifyDialog.FALSE_TRUE);
		intent(variableGroup);
		final CheckboxPreference useFinalFieldsPref= createCheckboxPref(variableGroup, 1, CleanUpMessages.CodeStyleTabPage_CheckboxName_UseFinalForFields, CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS, CleanUpModifyDialog.FALSE_TRUE);
		final CheckboxPreference useFinalParametersPref= createCheckboxPref(variableGroup, 1, CleanUpMessages.CodeStyleTabPage_CheckboxName_UseFinalForParameters, CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PARAMETERS, CleanUpModifyDialog.FALSE_TRUE);
		final CheckboxPreference useFinalVariablesPref= createCheckboxPref(variableGroup, 1, CleanUpMessages.CodeStyleTabPage_CheckboxName_UseFinalForLocals, CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES, CleanUpModifyDialog.FALSE_TRUE);
		registerSlavePreference(useFinalPref, new CheckboxPreference[] {useFinalFieldsPref, useFinalParametersPref, useFinalVariablesPref});
		final CheckboxPreference useVarPref= createCheckboxPref(variableGroup, numColumns, CleanUpMessages.CodeStyleTabPage_CheckboxName_UseVar, CleanUpConstants.USE_VAR,
				CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(useVarPref);

		Group functionalInterfacesGroup= createGroup(numColumns, composite, CleanUpMessages.CodeStyleTabPage_GroupName_FunctionalInterfaces);

		CheckboxPreference convertFunctionalInterfaces= createCheckboxPref(functionalInterfacesGroup, numColumns, CleanUpMessages.CodeStyleTabPage_CheckboxName_ConvertFunctionalInterfaces, CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES, CleanUpModifyDialog.FALSE_TRUE);
		intent(functionalInterfacesGroup);
		RadioPreference useLambdaPref= createRadioPref(functionalInterfacesGroup, 1, CleanUpMessages.CodeStyleTabPage_RadioName_UseLambdaWherePossible, CleanUpConstants.USE_LAMBDA, CleanUpModifyDialog.FALSE_TRUE);
		RadioPreference useAnonymousPref= createRadioPref(functionalInterfacesGroup, 1, CleanUpMessages.CodeStyleTabPage_RadioName_UseAnonymous, CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION, CleanUpModifyDialog.FALSE_TRUE);
		registerSlavePreference(convertFunctionalInterfaces, new RadioPreference[] { useLambdaPref, useAnonymousPref });

		CheckboxPreference simplifyLambdaExpressionAndMethodRef= createCheckboxPref(functionalInterfacesGroup, numColumns, CleanUpMessages.CodeStyleTabPage_CheckboxName_SimplifyLambdaExpressionAndMethodRefSyntax, CleanUpConstants.SIMPLIFY_LAMBDA_EXPRESSION_AND_METHOD_REF, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(simplifyLambdaExpressionAndMethodRef);
	}
}
