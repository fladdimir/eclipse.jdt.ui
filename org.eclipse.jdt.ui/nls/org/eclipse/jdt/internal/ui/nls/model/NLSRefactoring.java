/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.nls.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;

import java.util.Set;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportContainer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.Checks;
import org.eclipse.jdt.internal.core.refactoring.CompositeChange;
import org.eclipse.jdt.internal.core.refactoring.base.IChange;
import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBuffer;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChange;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChangeCreator;
import org.eclipse.jdt.internal.core.refactoring.text.ITextRegion;
import org.eclipse.jdt.internal.core.refactoring.text.SimpleReplaceTextChange;
import org.eclipse.jdt.internal.core.refactoring.text.SimpleTextChange;
import org.eclipse.jdt.internal.formatter.CodeFormatter;
import org.eclipse.jdt.internal.nls.changes.CreateTextFileChange;

public class NLSRefactoring extends Refactoring {
	
	public static final String KEY= "${key}"; //$NON-NLS-1$
	public static final String PROPERTY_FILE_EXT= ".properties"; //$NON-NLS-1$
	private static final String fgLineDelimiter= System.getProperty("line.separator", "\n"); //$NON-NLS-2$ //$NON-NLS-1$

	private String fAccessorClassName= "Messages";  //$NON-NLS-1$
	private ITextBufferChangeCreator fTextBufferChangeCreator;
	
	private boolean fCreateAccessorClass= true;
	private String fProperyFileName= "test"; //simple name //$NON-NLS-1$
	private String fCodePattern;
	private ICompilationUnit fCu;
	private NLSLine[] fLines;
	private NLSSubstitution[] fNlsSubs;
	private IPath fPropertyFilePath;
	private String fAddedImport;
	
	public NLSRefactoring(ITextBufferChangeCreator changeCreator, ICompilationUnit cu){
		Assert.isNotNull(cu);
		fCu= cu;
		Assert.isNotNull(changeCreator, "change creator"); //$NON-NLS-1$
		fTextBufferChangeCreator= changeCreator;
	}
			
	public void setNlsSubstitutions(NLSSubstitution[] subs){
		Assert.isNotNull(subs);
		fNlsSubs= subs;
	}
	
	/**
	 * sets the import to be added
	 * @param decl must be a valid import declaration
	 * otherwise no import declaration will be added
	 * @see JavaConventions#validateImportDeclaration
	 */
	public void setAddedImportDeclaration(String decl){
		if (JavaConventions.validateImportDeclaration(decl).isOK())
			fAddedImport= decl;
		else
			fAddedImport= null;	
	}
	
	/**
	 * no validation is done
	 * @param pattern Example: "Messages.getString(${key})". Must not be <code>null</code>.
	 * should (but does not have to) contain NLSRefactoring.KEY (default value is $key$)
	 * only the first occurrence of this key will be used
	 */
	public void setCodePattern(String pattern){
		Assert.isNotNull(pattern);
		fCodePattern= pattern;
	}
	
	/**
	 * to show the pattern in the ui
	 */
	public String getCodePattern(){
		if (fCodePattern == null)
			return getDefaultCodePattern();
		return fCodePattern;
	}
	
	public String getDefaultCodePattern(){
		return fAccessorClassName + ".getString(" + KEY + ")"; //$NON-NLS-2$ //$NON-NLS-1$
	}
		
	public ICompilationUnit getCu() {
		return fCu;
	}
	
	public String getName() {
		return Messages.getString("NLSrefactoring.NLS_compilation_unit___10")+ fCu.getElementName() + "\""; //$NON-NLS-2$ //$NON-NLS-1$
	}
	
	/**
	 * sets the list of lines
	 * @param List of NLSLines
	 */
	public void setLines(NLSLine[] lines) {
		Assert.isNotNull(lines);
		fLines= lines;
	}
	
	/**
	 * no validation done here
	 * full path expected
	 * can be null - the default value will be used
	 * to ask what the default value is - use 
	 * getDefaultPropertyFileName to get the file name
	 * getDefaultPropertyPackageName to get the package name
	 */
	public void setPropertyFilePath(IPath path){
		fPropertyFilePath= path;
	}
	
	private IPath getPropertyFilePath() throws JavaModelException{
		if (fPropertyFilePath == null)
			return getDefaultPropertyFilePath();
		return fPropertyFilePath;	
	}
	
	private IPath getDefaultPropertyFilePath() throws JavaModelException{
		IPath cuName= new Path(fCu.getElementName());
		return Refactoring.getResource(fCu).getFullPath()
						  .removeLastSegments(cuName.segmentCount())
						  .append(fProperyFileName + PROPERTY_FILE_EXT);
	}
	
	public String getDefaultPropertyFileName(){
		try{
			return getDefaultPropertyFilePath().lastSegment();
		} catch (JavaModelException e){
			return ""; //$NON-NLS-1$
		}	
	}
	
	/**
	 * returns "" in case of JavaModelException caught during calculation
	 */
	public String getDefaultPropertyPackageName(){
		try{
			IPath path= getDefaultPropertyFilePath();
			IResource res= ResourcesPlugin.getWorkspace().getRoot().findMember(path.removeLastSegments(1));
			IJavaElement je= JavaCore.create(res);
			if (je instanceof IPackageFragment)
				return je.getElementName();
			else	
				return ""; //$NON-NLS-1$
		} catch (JavaModelException e){
			return ""; //$NON-NLS-1$
		}	
	}
	
	/**
	 * @see Refactoring#checkActivation(IProgressMonitor)
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		if (! fCu.exists())
			return RefactoringStatus.createFatalErrorStatus(fCu.getElementName() + Messages.getString("NLSrefactoring._does_not_exist_in_the_model_15")); //$NON-NLS-1$
		
		if (fCu.isReadOnly())
			return RefactoringStatus.createFatalErrorStatus(fCu.getElementName() + Messages.getString("NLSrefactoring._is_read_only_16"));	 //$NON-NLS-1$
		
		if (NLSHolder.create(fCu).getSubstitutions().length == 0)	
			return RefactoringStatus.createFatalErrorStatus(Messages.getString("NLSrefactoring.No_strings_found_to_externalize_1")); //$NON-NLS-1$
		
		return new RefactoringStatus();
	}
	
	/**
	 * @see Refactoring#checkInput(IProgressMonitor)
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask(Messages.getString("NLSrefactoring.checking_17"), 5); //$NON-NLS-1$
			RefactoringStatus result= new RefactoringStatus();
			result.merge(checkIfAnythingToDo());
			if (result.hasFatalError())	
				return result;
			result.merge(checkCodePattern());
			pm.worked(1);
			result.merge(checkForDuplicateKeys());
			pm.worked(1);
			result.merge(checkForKeysAlreadyDefined());
			pm.worked(1);
			result.merge(checkKeys());
			pm.worked(1);
			if (!propertyFileExists() && willModifyPropertyFile())
				result.addInfo(Messages.getString("NLSrefactoring.Propfile") + getPropertyFilePath() + Messages.getString("NLSrefactoring.will_be_created")); //$NON-NLS-2$ //$NON-NLS-1$
			pm.worked(1);	
			return result;
		} finally {
			pm.done();
		}	
	}
	
	//should stop checking if fatal error
	private RefactoringStatus checkIfAnythingToDo() throws JavaModelException{
		if (willCreateAccessorClass())
			return null;
		if (willModifyPropertyFile())
			return null;
		if (willModifySource())
			return null;	
			
		RefactoringStatus result= new RefactoringStatus();
		result.addFatalError(Messages.getString("NLSrefactoring.Nothing_to_do_2")); //$NON-NLS-1$
		return result;
	}
	
	private boolean propertyFileExists() throws JavaModelException{
		return Checks.resourceExists(getPropertyFilePath());
	}
	
	private RefactoringStatus checkCodePattern(){
		String pattern= getCodePattern();
		RefactoringStatus result= new RefactoringStatus();
		if ("".equals(pattern.trim())) //$NON-NLS-1$
			result.addError(Messages.getString("NLSrefactoring.Code_pattern_is_empty_19")); //$NON-NLS-1$
		if (pattern.indexOf(KEY) == -1)
			result.addWarning(Messages.getString("NLSrefactoring.Code_pattern_does_not_contain__20") + KEY); //$NON-NLS-1$
		if (pattern.indexOf(KEY) != pattern.lastIndexOf(KEY))	
			result.addWarning(Messages.getString("NLSrefactoring.Only_the_first_occurrence_of__21") + KEY + Messages.getString("NLSrefactoring._will_be_substituted_22")); //$NON-NLS-2$ //$NON-NLS-1$
		return result;	
	}

	private RefactoringStatus checkForKeysAlreadyDefined() throws JavaModelException {
		if (! propertyFileExists())
			return null;
		RefactoringStatus result= new RefactoringStatus();
		PropertyResourceBundle bundle= getPropertyBundle();
		if (bundle == null)
			return null;
		for (int i= 0; i< fNlsSubs.length; i++){
			String s= getBundleString(bundle, fNlsSubs[i].key);
			if (s != null){
				if (! hasSameValue(s, fNlsSubs[i]))
					result.addFatalError(Messages.getString("NLSrefactoring.Key__23") + fNlsSubs[i].key + Messages.getString("NLSrefactoring._already_exists_in_the_resource_bundle._Value___24") + s //$NON-NLS-2$ //$NON-NLS-1$
									+ Messages.getString("NLSrefactoring.different") //$NON-NLS-1$
									+ removeQuotes(fNlsSubs[i].value.getValue())
									+ Messages.getString("NLSrefactoring.on_first_page"));  //$NON-NLS-1$
				else{
					fNlsSubs[i].putToPropertyFile= false;
					result.addWarning(Messages.getString("NLSrefactoring.Key") + fNlsSubs[i].key + Messages.getString("NLSrefactoring.already_in_bundle") + s  //$NON-NLS-2$ //$NON-NLS-1$
									 + Messages.getString("NLSrefactoring.same_first_page")); //$NON-NLS-1$
				}	
			}
		}
		return result;
	}
	
	private boolean hasSameValue(String val, NLSSubstitution sub){
		return (val.equals(removeQuotes(sub.value.getValue())));
	}
	
	/**
	 * returns <code>null</code> if not defined
	 */
	private String getBundleString(PropertyResourceBundle bundle, String key){
		try{
			return bundle.getString(key);
		} catch (MissingResourceException e){
			//XXX very inefficient
			return null;	
		}
	}
	
	private PropertyResourceBundle getPropertyBundle() throws JavaModelException{
		InputStream is= getPropertyFileInputStream();
		if (is == null)
			return null;
		try{
			PropertyResourceBundle result= new PropertyResourceBundle(is);
			return result;
		} catch (IOException e1){	
			return null;
		}finally {
			try{
				is.close();
			} catch (IOException e){
				throw new JavaModelException(e, IJavaModelStatusConstants.IO_EXCEPTION);
			}
		}	
	}
	
	private InputStream getPropertyFileInputStream() throws JavaModelException{
		IFile file= ResourcesPlugin.getWorkspace().getRoot().getFile(getPropertyFilePath());
		
		try{
			return file.getContents();
		} catch(CoreException e){
			throw new JavaModelException(e, IJavaModelStatusConstants.CORE_EXCEPTION);
		}
	}
	
	private RefactoringStatus checkForDuplicateKeys() {
		Map map= new HashMap();//String (key) -> Set of NLSSubstitution
		for (int i= 0; i < fNlsSubs.length; i++) {
			NLSSubstitution sub= fNlsSubs[i];
			String key= sub.key;
			if (!map.containsKey(key)){
			 	map.put(key, new HashSet());
			}
			((Set)map.get(key)).add(sub);		
		}
		
		RefactoringStatus result= new RefactoringStatus();
		for (Iterator iter= map.keySet().iterator(); iter.hasNext();) {
			Set subs= (Set)map.get((String) iter.next());
			result.merge(checkForDuplicateKeys(subs));
		}
		return result;
	}
	
	/**
	 * all elements in the parameter must be NLSSubstitutions with
	 * the same key
	 */
	private RefactoringStatus checkForDuplicateKeys(Set subs){
		if (subs.size() <= 1)
			return null;
		
		NLSSubstitution[] toTranslate= getEntriesToTranslate(subs);
		if (toTranslate.length <= 1)
			return null;
		
		for (int i= 0; i < toTranslate.length; i++) {
			toTranslate[i].putToPropertyFile= (i == 0);
		}

		String value= removeQuotes(toTranslate[0].value.getValue());
		for (int i= 0; i < toTranslate.length; i++) {
			NLSSubstitution each= toTranslate[i];
			if (! hasSameValue(value, each))
				return RefactoringStatus.createFatalErrorStatus(Messages.getString("NLSrefactoring.Key_1") + each.key + Messages.getString("NLSrefactoring.duplicated_5")); //$NON-NLS-2$ //$NON-NLS-1$
		}
		return RefactoringStatus.createWarningStatus(Messages.getString("NLSrefactoring.Key_1") + toTranslate[0].key + Messages.getString("NLSrefactoring.reused_7") + value) ; //$NON-NLS-2$ //$NON-NLS-1$	
	}
	
	private static NLSSubstitution[] getEntriesToTranslate(Set subs){
		List result= new ArrayList(subs.size());
		for (Iterator iter= subs.iterator(); iter.hasNext();) {
			NLSSubstitution each= (NLSSubstitution) iter.next();
			if (each.task == NLSSubstitution.TRANSLATE)
				result.add(each);
		}
		return (NLSSubstitution[]) result.toArray(new NLSSubstitution[result.size()]);
	}
		
	private RefactoringStatus checkKeys() {
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < fNlsSubs.length; i++)
			checkKey(fNlsSubs[i].key, result, NLSHolder.UNWANTED_STRINGS);
		return result;
	}	
	
	private void checkKey(String key, RefactoringStatus result, String[] unwantedStrings){
		if (key == null)
			result.addFatalError(Messages.getString("NLSrefactoring.Key_must_not_be_null_26")); //$NON-NLS-1$
		if ("".equals(key.trim())) //$NON-NLS-1$
			result.addFatalError(Messages.getString("NLSrefactoring.Key_must_not_be_empty_28")); //$NON-NLS-1$
		
		//feature in resource bundle - does not work properly if keys have ":"
		for (int i= 0; i < unwantedStrings.length; i++){
			if (key.indexOf(unwantedStrings[i]) != -1)
				result.addError(Messages.getString("NLSrefactoring.Key__30") + key + Messages.getString("NLSrefactoring._should_not_contain_31") + "'" + unwantedStrings[i] + "'"); //$NON-NLS-4$ //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$
		}
	}
	
	private boolean willCreateAccessorClass() throws JavaModelException{
		if (!fCreateAccessorClass)
			return false;
		if (NLSSubstitution.countItems(fNlsSubs, NLSSubstitution.TRANSLATE) == 0)
			return false;
		if (getPackage().getCompilationUnit(getAccessorCUName()).exists())
			return false;
		if (typeNameExistsInPackage(getPackage(), fAccessorClassName))
			return false;
		return (! Checks.resourceExists(getAccessorCUPath()));
	}
	
	private boolean willModifySource(){
		if (NLSSubstitution.countItems(fNlsSubs, NLSSubstitution.SKIP) != fNlsSubs.length)
			return true;
		if (willAddImportDeclaration())
			return true;
		return false;		
	}
	
	private boolean willModifyPropertyFile(){
		return NLSSubstitution.countItems(fNlsSubs, NLSSubstitution.TRANSLATE) > 0;
	}
	
	private boolean willAddImportDeclaration(){
		if (fAddedImport == null)
			return false;
		if ("".equals(fAddedImport.trim())) //$NON-NLS-1$
			return false;	
		if (getCu().getImport(fAddedImport).exists())
			return false;
		if (NLSSubstitution.countItems(fNlsSubs, NLSSubstitution.TRANSLATE) == 0)	
			return false;
		return true;
		//XXX could	avoid creating the import if already imported on demand
	}
	
	// --- changes
	
	/**
	 * @see IRefactoring#createChange(IProgressMonitor)
	 */
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask("", 3); //$NON-NLS-1$
			CompositeChange builder= new CompositeChange();
			
			if (willModifySource())
				builder.addChange(createSourceModification());
			pm.worked(1);
			
			if (willModifyPropertyFile())
				builder.addChange(createPropertyFile());
			pm.worked(1);
			
			if (willCreateAccessorClass())
				builder.addChange(createAccessorCU());
			pm.worked(1);
			
			return builder;
		} finally {
			pm.done();
		}	
	}
	

	//---- modified source files
			
	private IChange createSourceModification() throws JavaModelException{
		ITextBufferChange builder= fTextBufferChangeCreator.create(Messages.getString("NLSrefactoring.nls_33"), fCu); //$NON-NLS-1$
		for (int i= 0; i < fNlsSubs.length; i++){
			addNLS(fNlsSubs[i], builder);
		}
		if (willAddImportDeclaration())
			addImportDeclaration(builder);
		return builder;
	}
	
	private void addImportDeclaration(ITextBufferChange builder) throws JavaModelException{
		IImportContainer importContainer= getCu().getImportContainer();
		int start;
		if (!importContainer.exists()){
			String packName= ((IPackageFragment)getCu().getParent()).getElementName();
			IPackageDeclaration packageDecl= getCu().getPackageDeclaration(packName);
			if (!packageDecl.exists())
				start= 0;
			else{
				ISourceRange sr= packageDecl.getSourceRange();
				start= sr.getOffset() + sr.getLength() - 1;
			}	
		} else{
			ISourceRange sr= importContainer.getSourceRange();
			start= sr.getOffset() + sr.getLength() - 1;
		}	
			
		String newImportText= fgLineDelimiter + "import " + fAddedImport + ";"; //$NON-NLS-2$ //$NON-NLS-1$
		builder.addInsert(Messages.getString("NLSrefactoring.add_import_declaration") + fAddedImport, start + 1, newImportText); //$NON-NLS-1$
	}
	
	private void addNLS(NLSSubstitution sub, ITextBufferChange builder){
		ITextRegion position= sub.value.getPosition();
		String resourceGetter= createResourceGetter(sub.key);
		String text= Messages.getString("NLSrefactoring.extrenalize_string__34") + sub.value.getValue(); //$NON-NLS-1$
		if (sub.task == NLSSubstitution.TRANSLATE)
			builder.addReplace(text, position.getOffset(), position.getLength(), resourceGetter);
		if (sub.task != NLSSubstitution.SKIP)
			builder.addSimpleTextChange(createAddTagChange(sub.value));
	}
	
	//XXX extremelly inefficient way to do it
	private NLSLine findLine(NLSElement element){
		for(int i= 0; i < fLines.length; i++){
			NLSElement[] lineElements= fLines[i].getElements();
			for (int j= 0; j < lineElements.length; j++){
				if (lineElements[j].equals(element))
					return fLines[i];
			}		
		}
		return null;
	}
	
	private int computeIndexInLine(NLSElement element, NLSLine line){
		for (int i= 0; i < line.size(); i++){
			if (line.get(i).equals(element))
				return i;
		};
		Assert.isTrue(false, "element not found in line"); //$NON-NLS-1$
		return -1;
	}
	
	private int computeTagIndex(NLSElement element){
		NLSLine line= findLine(element);
		Assert.isNotNull(line, "line not found for:" + element); //$NON-NLS-1$
		return computeIndexInLine(element, line) + 1; //tags are 1 based
	}
		
	private String createTagText(NLSElement element) {
		return " " + NLSElement.createTagText(computeTagIndex(element)); //$NON-NLS-1$
	}
	
	private SimpleReplaceTextChange createAddTagChange(NLSElement element){
		int offset= element.getPosition().getOffset(); //to be changed
		String text = createTagText(element);
		String name= Messages.getString("NLSrefactoring.add_tag___38")+ text + Messages.getString("NLSrefactoring._for_string___39") + element.getValue(); //$NON-NLS-2$ //$NON-NLS-1$
		int length= 0;
		return new SimpleReplaceTextChange(name, offset, length, text){
			protected SimpleTextChange[] adjust(ITextBuffer buffer) {
				int lineEndOffset= getLineEndOffset(buffer, getOffset());
				if (lineEndOffset != -1)
					setOffset(lineEndOffset);
				return null;	
			}
			private int getLineEndOffset(ITextBuffer buffer, int offset){
				int line= buffer.getLineOfOffset(offset);
				if (line != -1){
					ITextRegion info= buffer.getLineInformation(line);
					return info.getOffset() + info.getLength();
				} 
				return -1;
			};
		};
	}
			
	private String createResourceGetter(String key){
		//we just replace the first occurrence of KEY in the pattern
		StringBuffer buff= new StringBuffer(fCodePattern);
		int i= fCodePattern.indexOf(KEY);
		if (i != -1)
			buff.replace(i, i + KEY.length(), "\"" + key + "\""); //$NON-NLS-2$ //$NON-NLS-1$
		return buff.toString();
	}

	//---- resource bundle file
	
	private IChange createPropertyFile() throws JavaModelException{
		CreateTextFileChange change= new CreateTextFileChange(getPropertyFilePath(), createPropertyFileSource());
		if (propertyFileExists())
			change.setName(Messages.getString("NLSrefactoring.Append_to_property_file") + getPropertyFilePath()); //$NON-NLS-1$
		return change;	
	}
	
	private String createPropertyFileSource() throws JavaModelException{
		StringBuffer sb= new StringBuffer();
		sb.append(getOldPropertyFileSource());
		if (needsLineDelimiter(sb))
			sb.append(fgLineDelimiter);
		for (int i= 0; i < fNlsSubs.length; i++){
			if (fNlsSubs[i].task == NLSSubstitution.TRANSLATE){
				if (fNlsSubs[i].putToPropertyFile)		
					sb.append(createEntry(fNlsSubs[i].value, fNlsSubs[i].key));
			}	
		}	
		return sb.toString();
	}
	
	//heuristic only
	private static boolean needsLineDelimiter(StringBuffer sb){
		if (sb.length() == 0)
			return false;
		String s= sb.toString();
		int lastDelimiter= s.lastIndexOf(fgLineDelimiter);
		if (lastDelimiter == -1)
			return true;
		if ("".equals(s.substring(lastDelimiter).trim())) //$NON-NLS-1$
			return false;
		return true;	
	}
	
	private String getOldPropertyFileSource() throws JavaModelException{
		if (! propertyFileExists())
			return ""; //$NON-NLS-1$
		
		//must read the whole contents - don't want to lose comments etc.
		InputStream is= getPropertyFileInputStream();
		String s= Utils.readString(is);
		return s == null ? "": s; //$NON-NLS-1$
	}
	
	private StringBuffer createEntry(NLSElement element, String key){
		StringBuffer sb= new StringBuffer();
		sb.append(key)
		  .append("=") //$NON-NLS-1$
		  .append(convertToPropertyValue(removeQuotes(element.getValue())))
		  .append(fgLineDelimiter);
		return sb;
	}
	
	/*
	 * see 21.6.7 of the spec
	 */
	private static String convertToPropertyValue(String v){
		int firstNonWhiteSpace=findFirstNonWhiteSpace(v);
		if (firstNonWhiteSpace == 0)
			return v;	
		return escapeEachChar(v.substring(0, firstNonWhiteSpace), '\\') + v.substring(firstNonWhiteSpace);
	}
	
	private static String escapeEachChar(String s, char escapeChar){
		char[] chars= new char[s.length() * 2];
		
		for (int i= 0; i < s.length(); i++){
			chars[2*i]= escapeChar;
			chars[2*i + 1]= s.charAt(i);
		}
		return new String(chars);
	}
	
	/**
	 * returns the length if only whitespaces
	 */
	private static int findFirstNonWhiteSpace(String s){
		for (int i= 0; i < s.length(); i++){
			if (! Character.isWhitespace(s.charAt(i)))
				return i;
		}		
		return s.length();
	}
	
	public static String removeQuotes(String s){
			Assert.isTrue(s.startsWith("\"") && s.endsWith("\"")); //$NON-NLS-2$ //$NON-NLS-1$
			return s.substring(1, s.length() - 1);
	} 

	// ------------ accessor class creation
	
	private IChange createAccessorCU() throws JavaModelException{
		return new CreateTextFileChange(getAccessorCUPath(), createAccessorCUSource());	
	} 
	
	private IPackageFragment getPackage(){
		 return (IPackageFragment)fCu.getParent();
	}
		
	//XXX code dupilicated from TypeRefactoring
	private static boolean typeNameExistsInPackage(IPackageFragment pack, String name) throws JavaModelException{
		Assert.isTrue(pack.exists(), "package must exist"); //$NON-NLS-1$
		Assert.isTrue(!pack.isReadOnly(), "package must not be read-only"); //$NON-NLS-1$
		/*
		 * ICompilationUnit.getType expects simple name
		 */  
		if (name.indexOf(".") != -1) //$NON-NLS-1$
			name= name.substring(0, name.indexOf(".")); //$NON-NLS-1$
		ICompilationUnit[] cus= pack.getCompilationUnits();
		for (int i= 0; i < cus.length; i++){
			if (cus[i].getType(name).exists())
				return true;
		}
		return false;
	}
	
	public void setCreateAccessorClass(boolean create){
		fCreateAccessorClass= create;
	}
	
	public boolean getCreateAccessorClass(){
		return fCreateAccessorClass;
	}
	
	public String getAccessorClassName(){
		return fAccessorClassName;
	}
	
	public void setAccessorClassName(String name){
		fAccessorClassName= name;
		Assert.isNotNull(name);
	}
	
	private String getAccessorCUName(){
		return fAccessorClassName + ".java"; //$NON-NLS-1$
	}
	
	private IPath getAccessorCUPath() throws JavaModelException{
		IPath cuName= new Path(fCu.getElementName());
		return Refactoring.getResource(fCu).getFullPath()
						  .removeLastSegments(cuName.segmentCount())
						  .append(getAccessorCUName());
	}
	
	//--bundle class source creation
	private String createAccessorCUSource() throws JavaModelException{
		StringBuffer buff= new StringBuffer();
		buff.append(createHeader())
			.append(createPackageDeclaration())
			.append(fgLineDelimiter)
			.append(createImports())
			.append(fgLineDelimiter)
			.append(createClass());
		return new CodeFormatter(null).format(buff.toString());
	}
	
	private StringBuffer createHeader(){
		 StringBuffer buff= new StringBuffer();
		 buff.append("/*").append(fgLineDelimiter) //$NON-NLS-1$
		 	 .append(" * (c) Copyright IBM Corp. 2000, 2001.").append(fgLineDelimiter) //$NON-NLS-1$
		 	 .append(" * All Rights Reserved.").append(fgLineDelimiter) //$NON-NLS-1$
			 .append(" */").append(fgLineDelimiter); //$NON-NLS-1$
		 return buff;
	}
	
	private StringBuffer createPackageDeclaration(){
		IPackageFragment pack= getPackage();
		if (pack.isDefaultPackage())
			return new StringBuffer();

		StringBuffer buff= new StringBuffer();
		buff.append("package ") //$NON-NLS-1$
			.append(pack.getElementName())
			.append(";") //$NON-NLS-1$
			.append(fgLineDelimiter);
		return buff;
	}
	
	private StringBuffer createImports(){
		StringBuffer buff= new StringBuffer();
		buff.append("import java.util.MissingResourceException;").append(fgLineDelimiter) //$NON-NLS-1$
			.append("import java.util.ResourceBundle;").append(fgLineDelimiter); //$NON-NLS-1$
		return buff;
	}
	
	private StringBuffer createClass() throws JavaModelException{
		String ld= fgLineDelimiter; //want shorter name
		StringBuffer b= new StringBuffer();
		//XXX should the class be public?
		b.append("public class ").append(fAccessorClassName).append(" {").append(ld) //$NON-NLS-2$ //$NON-NLS-1$
		 .append(ld)
		 .append("\tprivate static final String RESOURCE_BUNDLE= \"") //$NON-NLS-1$
		 .append(getResourceBundleName()).append("\";").append(NLSElement.createTagText(1)).append(ld) //$NON-NLS-1$
		 .append(ld)
		 .append("\tprivate static ResourceBundle fgResourceBundle= ResourceBundle.getBundle(RESOURCE_BUNDLE);") //$NON-NLS-1$
		 .append(ld)
		 .append(ld)
		 .append(createConstructor())
		 .append(ld)
		 .append(createGetStringMethod())
		 .append("}").append(ld); //$NON-NLS-1$
		return b;
	}
	
	private StringBuffer createConstructor(){
		String ld= fgLineDelimiter; //want shorter name
		StringBuffer b= new StringBuffer();
		b.append("\tprivate ").append(fAccessorClassName).append("() {").append(ld) //$NON-NLS-2$ //$NON-NLS-1$
		 .append("\t}").append(ld); //$NON-NLS-1$
		return b;
	}
	
	private StringBuffer createGetStringMethod(){
		String ld= fgLineDelimiter; //want shorter name
		StringBuffer b= new StringBuffer();
		b.append("\tpublic static String getString(String key) {").append(ld) //$NON-NLS-1$
		 .append("\t\ttry {").append(ld) //$NON-NLS-1$
		 .append("\t\t\treturn fgResourceBundle.getString(key);").append(ld) //$NON-NLS-1$
		 .append("\t\t} catch (MissingResourceException e) {").append(ld) //$NON-NLS-1$
		 .append("\t\t\treturn '!' + key + '!';").append(ld) //$NON-NLS-1$
		 .append("\t\t}").append(ld) //$NON-NLS-1$
		 .append("\t}").append(ld); //$NON-NLS-1$
		return b;
	}
	
	//together with the .properties extension
	private String getPropertyFileName() throws JavaModelException{
		return getPropertyFilePath().lastSegment();
	}
	
	//extension removed
	private String getPropertyFileSimpleName() throws JavaModelException{
		String fileName= getPropertyFileName();
		return fileName.substring(0, fileName.indexOf(PROPERTY_FILE_EXT));
	}
	
	private String getResourceBundleName() throws JavaModelException{
		//remove filename.properties
		IResource res= ResourcesPlugin.getWorkspace().getRoot().findMember(getPropertyFilePath().removeLastSegments(1));
		if (res != null && res.exists()){
			IJavaElement el= JavaCore.create(res);
			if (el instanceof IPackageFragment){
				IPackageFragment p= (IPackageFragment)el;
				if (p.isDefaultPackage())
					return getPropertyFileSimpleName();
				return p.getElementName() + "." + getPropertyFileSimpleName(); //$NON-NLS-1$
			}
		}
		//XXX can we get here?
		IPackageFragment pack= getPackage();
		if (pack.isDefaultPackage())
			return fProperyFileName;
		return pack.getElementName() + "." + fProperyFileName; //$NON-NLS-1$
	}
}