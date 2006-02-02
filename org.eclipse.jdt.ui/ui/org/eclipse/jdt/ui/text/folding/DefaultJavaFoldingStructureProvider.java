/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.text.folding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.projection.IProjectionListener;
import org.eclipse.jface.text.source.projection.IProjectionPosition;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.jface.text.source.projection.ProjectionViewer;

import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.text.DocumentCharacterIterator;


/**
 * Updates the projection model of a class file or compilation unit.
 * <p>
 * Clients may instantiate or subclass. Subclasses must make sure to always call the superclass'
 * code when overriding methods that are marked with "subclasses may extend".
 * </p>
 * 
 * @since 3.0 (internal)
 * @since 3.2 (API)
 */
public class DefaultJavaFoldingStructureProvider implements IJavaFoldingStructureProvider, IJavaFoldingStructureProviderExtension {
	/**
	 * A context that contains the information needed to compute the folding structure of an
	 * {@link ICompilationUnit} or an {@link IClassFile}. Computed folding regions are collected
	 * via
	 * {@linkplain #addProjectionRange(DefaultJavaFoldingStructureProvider.JavaProjectionAnnotation, Position) addProjectionRange}.
	 */
	protected final class FoldingStructureComputationContext {
		private final ProjectionAnnotationModel fModel;
		private final IDocument fDocument;
		private final boolean fAllowCollapsing;

		private IType fFirstType;
		private boolean fHasHeaderComment;
		private LinkedHashMap fMap= new LinkedHashMap();

		FoldingStructureComputationContext(IDocument document, ProjectionAnnotationModel model, boolean allowCollapsing) {
			Assert.isNotNull(document);
			Assert.isNotNull(model);
			fDocument= document;
			fModel= model;
			fAllowCollapsing= allowCollapsing;
		}
		
		void setFirstType(IType type) {
			if (hasFirstType())
				throw new IllegalStateException();
			fFirstType= type;
		}
		
		boolean hasFirstType() {
			return fFirstType != null;
		}
		
		IType getFirstType() {
			return fFirstType;
		}

		boolean hasHeaderComment() {
			return fHasHeaderComment;
		}

		void setHasHeaderComment() {
			fHasHeaderComment= true;
		}
		
		/**
		 * Returns <code>true</code> if newly created folding regions may be collapsed,
		 * <code>false</code> if not. This is usually <code>false</code> when updating the
		 * folding structure while typing; it may be <code>true</code> when computing or restoring
		 * the initial folding structure.
		 * 
		 * @return <code>true</code> if newly created folding regions may be collapsed,
		 *         <code>false</code> if not
		 */
		public boolean allowCollapsing() {
			return fAllowCollapsing;
		}

		/**
		 * Returns the document which contains the code being folded.
		 * 
		 * @return the document which contains the code being folded
		 */
		IDocument getDocument() {
			return fDocument;
		}

		ProjectionAnnotationModel getModel() {
			return fModel;
		}
		
		/**
		 * Adds a projection (folding) region to this context. The created annotation / position
		 * pair will be added to the {@link ProjectionAnnotationModel} of the
		 * {@link ProjectionViewer} of the editor.
		 * 
		 * @param annotation the annotation to add
		 * @param position the corresponding position
		 */
		public void addProjectionRange(JavaProjectionAnnotation annotation, Position position) {
			fMap.put(annotation, position);
		}

		/**
		 * Returns <code>true</code> if header comments should be collapsed.
		 * 
		 * @return <code>true</code> if header comments should be collapsed
		 */
		public boolean collapseHeaderComments() {
			return fAllowCollapsing && fCollapseHeaderComments;
		}

		/**
		 * Returns <code>true</code> if import containers should be collapsed.
		 * 
		 * @return <code>true</code> if import containers should be collapsed
		 */
		public boolean collapseImportContainer() {
			return fAllowCollapsing && fCollapseImportContainer;
		}

		/**
		 * Returns <code>true</code> if inner types should be collapsed.
		 * 
		 * @return <code>true</code> if inner types should be collapsed
		 */
		public boolean collapseInnerTypes() {
			return fAllowCollapsing && fCollapseInnerTypes;
		}

		/**
		 * Returns <code>true</code> if javadoc comments should be collapsed.
		 * 
		 * @return <code>true</code> if javadoc comments should be collapsed
		 */
		public boolean collapseJavadoc() {
			return fAllowCollapsing && fCollapseJavadoc;
		}

		/**
		 * Returns <code>true</code> if methods should be collapsed.
		 * 
		 * @return <code>true</code> if methods should be collapsed
		 */
		public boolean collapseMembers() {
			return fAllowCollapsing && fCollapseMembers;
		}
	}
	
	/**
	 * A {@link ProjectionAnnotation} for java code.
	 */
	protected static final class JavaProjectionAnnotation extends ProjectionAnnotation {

		private IJavaElement fJavaElement;
		private boolean fIsComment;

		/**
		 * Creates a new projection annotation.
		 * 
		 * @param isCollapsed <code>true</code> to set the initial state to collapsed,
		 *        <code>false</code> to set it to expanded
		 * @param element the java element this annotation refers to
		 * @param isComment <code>true</code> for a foldable comment, <code>false</code> for a
		 *        foldable code element
		 */
		public JavaProjectionAnnotation(boolean isCollapsed, IJavaElement element, boolean isComment) {
			super(isCollapsed);
			fJavaElement= element;
			fIsComment= isComment;
		}

		IJavaElement getElement() {
			return fJavaElement;
		}

		void setElement(IJavaElement element) {
			fJavaElement= element;
		}

		boolean isComment() {
			return fIsComment;
		}

		void setIsComment(boolean isComment) {
			fIsComment= isComment;
		}

		/*
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return "JavaProjectionAnnotation:\n" + //$NON-NLS-1$
					"\telement: \t"+ fJavaElement.toString() + "\n" + //$NON-NLS-1$ //$NON-NLS-2$
					"\tcollapsed: \t" + isCollapsed() + "\n" + //$NON-NLS-1$ //$NON-NLS-2$
					"\tcomment: \t" + isComment() + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	

	private static final class Tuple {
		JavaProjectionAnnotation annotation;
		Position position;
		Tuple(JavaProjectionAnnotation annotation, Position position) {
			this.annotation= annotation;
			this.position= position;
		}
	}

	/**
	 * Filter for annotations.
	 */
	private static interface Filter {
		boolean match(JavaProjectionAnnotation annotation);
	}
	
	/**
	 * Matches comments.
	 */
	private static final class CommentFilter implements Filter {
		public boolean match(JavaProjectionAnnotation annotation) {
			if (annotation.isComment() && !annotation.isMarkedDeleted()) {
				return true;
			}
			return false;
		}
	}

	/**
	 * Matches members.
	 */
	private static final class MemberFilter implements Filter {
		public boolean match(JavaProjectionAnnotation annotation) {
			if (!annotation.isComment() && !annotation.isMarkedDeleted()) {
				IJavaElement element= annotation.getElement();
				if (element instanceof IMember) {
					if (element.getElementType() != IJavaElement.TYPE || ((IMember) element).getDeclaringType() != null) {
						return true;
					}
				}
			}
			return false;
		}
	}
	
	/**
	 * Matches java elements contained in a certain set.
	 */
	private static final class JavaElementSetFilter implements Filter {
		private final Set/*<? extends IJavaElement>*/ fSet;
		private final boolean fMatchCollapsed;

		private JavaElementSetFilter(Set/*<? extends IJavaElement>*/ set, boolean matchCollapsed) {
			fSet= set;
			fMatchCollapsed= matchCollapsed;
		}

		public boolean match(JavaProjectionAnnotation annotation) {
			boolean stateMatch= fMatchCollapsed == annotation.isCollapsed();
			if (stateMatch && !annotation.isComment() && !annotation.isMarkedDeleted()) {
				IJavaElement element= annotation.getElement();
				if (fSet.contains(element)) {
					return true;
				}
			}
			return false;
		}
	}

	private class ElementChangedListener implements IElementChangedListener {

		/*
		 * @see org.eclipse.jdt.core.IElementChangedListener#elementChanged(org.eclipse.jdt.core.ElementChangedEvent)
		 */
		public void elementChanged(ElementChangedEvent e) {
			IJavaElementDelta delta= findElement(fInput, e.getDelta());
			if (delta != null && (delta.getFlags() & (IJavaElementDelta.F_CONTENT | IJavaElementDelta.F_CHILDREN)) != 0)
				update();
		}

		private IJavaElementDelta findElement(IJavaElement target, IJavaElementDelta delta) {

			if (delta == null || target == null)
				return null;

			IJavaElement element= delta.getElement();

			if (element.getElementType() > IJavaElement.CLASS_FILE)
				return null;

			if (target.equals(element))
				return delta;

			IJavaElementDelta[] children= delta.getAffectedChildren();

			for (int i= 0; i < children.length; i++) {
				IJavaElementDelta d= findElement(target, children[i]);
				if (d != null)
					return d;
			}

			return null;
		}
	}

	/**
	 * Projection position that will return two foldable regions: one folding away
	 * the region from after the '/**' to the beginning of the content, the other
	 * from after the first content line until after the comment.
	 */
	private static final class CommentPosition extends Position implements IProjectionPosition {
		CommentPosition(int offset, int length) {
			super(offset, length);
		}

		/*
		 * @see org.eclipse.jface.text.source.projection.IProjectionPosition#computeFoldingRegions(org.eclipse.jface.text.IDocument)
		 */
		public IRegion[] computeProjectionRegions(IDocument document) throws BadLocationException {
			DocumentCharacterIterator sequence= new DocumentCharacterIterator(document, offset, offset + length);
			int prefixEnd= 0;
			int contentStart= findFirstContent(sequence, prefixEnd);

			int firstLine= document.getLineOfOffset(offset + prefixEnd);
			int captionLine= document.getLineOfOffset(offset + contentStart);
			int lastLine= document.getLineOfOffset(offset + length);

			Assert.isTrue(firstLine <= captionLine, "first folded line is greater than the caption line"); //$NON-NLS-1$
			Assert.isTrue(captionLine <= lastLine, "caption line is greater than the last folded line"); //$NON-NLS-1$

			IRegion preRegion;
			if (firstLine < captionLine) {
//				preRegion= new Region(offset + prefixEnd, contentStart - prefixEnd);
				int preOffset= document.getLineOffset(firstLine);
				IRegion preEndLineInfo= document.getLineInformation(captionLine);
				int preEnd= preEndLineInfo.getOffset();
				preRegion= new Region(preOffset, preEnd - preOffset);
			} else {
				preRegion= null;
			}

			if (captionLine < lastLine) {
				int postOffset= document.getLineOffset(captionLine + 1);
				IRegion postRegion= new Region(postOffset, offset + length - postOffset);

				if (preRegion == null)
					return new IRegion[] { postRegion };

				return new IRegion[] { preRegion, postRegion };
			}

			if (preRegion != null)
				return new IRegion[] { preRegion };

			return null;
		}

		/**
		 * Finds the offset of the first identifier part within <code>content</code>.
		 * Returns 0 if none is found.
		 *
		 * @param content the content to search
		 * @return the first index of a unicode identifier part, or zero if none can
		 *         be found
		 */
		private int findFirstContent(final CharSequence content, int prefixEnd) {
			int lenght= content.length();
			for (int i= prefixEnd; i < lenght; i++) {
				if (Character.isUnicodeIdentifierPart(content.charAt(i)))
					return i;
			}
			return 0;
		}

//		/**
//		 * Finds the offset of the first identifier part within <code>content</code>.
//		 * Returns 0 if none is found.
//		 *
//		 * @param content the content to search
//		 * @return the first index of a unicode identifier part, or zero if none can
//		 *         be found
//		 */
//		private int findPrefixEnd(final CharSequence content) {
//			// return the index after the leading '/*' or '/**'
//			int len= content.length();
//			int i= 0;
//			while (i < len && isWhiteSpace(content.charAt(i)))
//				i++;
//			if (len >= i + 2 && content.charAt(i) == '/' && content.charAt(i + 1) == '*')
//				if (len >= i + 3 && content.charAt(i + 2) == '*')
//					return i + 3;
//				else
//					return i + 2;
//			else
//				return i;
//		}
//
//		private boolean isWhiteSpace(char c) {
//			return c == ' ' || c == '\t';
//		}

		/*
		 * @see org.eclipse.jface.text.source.projection.IProjectionPosition#computeCaptionOffset(org.eclipse.jface.text.IDocument)
		 */
		public int computeCaptionOffset(IDocument document) {
//			return 0;
			DocumentCharacterIterator sequence= new DocumentCharacterIterator(document, offset, offset + length);
			return findFirstContent(sequence, 0);
		}
	}

	/**
	 * Projection position that will return two foldable regions: one folding away
	 * the lines before the one containing the simple name of the java element, one
	 * folding away any lines after the caption.
	 */
	private static final class JavaElementPosition extends Position implements IProjectionPosition {

		private IMember fMember;

		public JavaElementPosition(int offset, int length, IMember member) {
			super(offset, length);
			Assert.isNotNull(member);
			fMember= member;
		}
		
		public void setMember(IMember member) {
			Assert.isNotNull(member);
			fMember= member;
		}
		
		/*
		 * @see org.eclipse.jface.text.source.projection.IProjectionPosition#computeFoldingRegions(org.eclipse.jface.text.IDocument)
		 */
		public IRegion[] computeProjectionRegions(IDocument document) throws BadLocationException {
			int nameStart= offset;
			try {
				/* The member's name range may not be correct. However,
				 * reconciling would trigger another element delta which would
				 * lead to reentrant situations. Therefore, we optimistically
				 * assume that the name range is correct, but double check the
				 * received lines below. */
				ISourceRange nameRange= fMember.getNameRange();
				if (nameRange != null)
					nameStart= nameRange.getOffset();

			} catch (JavaModelException e) {
				// ignore and use default
			}

			int firstLine= document.getLineOfOffset(offset);
			int captionLine= document.getLineOfOffset(nameStart);
			int lastLine= document.getLineOfOffset(offset + length);

			/* see comment above - adjust the caption line to be inside the
			 * entire folded region, and rely on later element deltas to correct
			 * the name range. */
			if (captionLine < firstLine)
				captionLine= firstLine;
			if (captionLine > lastLine)
				captionLine= lastLine;

			IRegion preRegion;
			if (firstLine < captionLine) {
				int preOffset= document.getLineOffset(firstLine);
				IRegion preEndLineInfo= document.getLineInformation(captionLine);
				int preEnd= preEndLineInfo.getOffset();
				preRegion= new Region(preOffset, preEnd - preOffset);
			} else {
				preRegion= null;
			}

			if (captionLine < lastLine) {
				int postOffset= document.getLineOffset(captionLine + 1);
				IRegion postRegion= new Region(postOffset, offset + length - postOffset);

				if (preRegion == null)
					return new IRegion[] { postRegion };

				return new IRegion[] { preRegion, postRegion };
			}

			if (preRegion != null)
				return new IRegion[] { preRegion };

			return null;
		}

		/*
		 * @see org.eclipse.jface.text.source.projection.IProjectionPosition#computeCaptionOffset(org.eclipse.jface.text.IDocument)
		 */
		public int computeCaptionOffset(IDocument document) throws BadLocationException {
			int nameStart= offset;
			try {
				// need a reconcile here?
				ISourceRange nameRange= fMember.getNameRange();
				if (nameRange != null)
					nameStart= nameRange.getOffset();
			} catch (JavaModelException e) {
				// ignore and use default
			}

			return nameStart - offset;
		}

	}
	
	/**
	 * Internal projection listener.
	 */
	private final class ProjectionListener implements IProjectionListener {
		private ProjectionViewer fViewer;

		/**
		 * Registers the listener with the viewer.
		 * 
		 * @param viewer the viewer to register a listener with
		 */
		public ProjectionListener(ProjectionViewer viewer) {
			Assert.isLegal(viewer != null);
			fViewer= viewer;
			fViewer.addProjectionListener(this);
		}
		
		/**
		 * Disposes of this listener and removes the projection listener from the viewer.
		 */
		public void dispose() {
			if (fViewer != null) {
				fViewer.removeProjectionListener(this);
				fViewer= null;
			}
		}
		
		/*
		 * @see org.eclipse.jface.text.source.projection.IProjectionListener#projectionEnabled()
		 */
		public void projectionEnabled() {
			handleProjectionEnabled();
		}

		/*
		 * @see org.eclipse.jface.text.source.projection.IProjectionListener#projectionDisabled()
		 */
		public void projectionDisabled() {
			handleProjectionDisabled();
		}
	}
	
	/* context and listeners */
	private ITextEditor fEditor;
	private ProjectionListener fProjectionListener;
	private IJavaElement fInput;
	private IElementChangedListener fElementListener;

	/* preferences */
	private boolean fCollapseJavadoc= false;
	private boolean fCollapseImportContainer= true;
	private boolean fCollapseInnerTypes= true;
	private boolean fCollapseMembers= false;
	private boolean fCollapseHeaderComments= true;

	/* filters */
	/** Member filter, matches nested members (but not top-level types). */
	private final Filter fMemberFilter = new MemberFilter();
	/** Comment filter, matches comments. */
	private final Filter fCommentFilter = new CommentFilter();

	/**
	 * Creates a new folding provider. It must be
	 * {@link #install(ITextEditor, ProjectionViewer) installed} on an editor/viewer pair before it
	 * can be used, and {@link #uninstall() uninstalled} when not used any longer.
	 * <p>
	 * The projection state may be reset by calling {@link #initialize()}.
	 * </p>
	 */
	public DefaultJavaFoldingStructureProvider() {
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Subclasses may extend.
	 * </p>
	 * 
	 * @param editor {@inheritDoc}
	 * @param viewer {@inheritDoc}
	 */
	public void install(ITextEditor editor, ProjectionViewer viewer) {
		Assert.isLegal(editor != null);
		Assert.isLegal(viewer != null);

		internalUninstall();
		
		if (editor instanceof JavaEditor) {
			fEditor= editor;
			fProjectionListener= new ProjectionListener(viewer);
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Subclasses may extend.
	 * </p>
	 */
	public void uninstall() {
		internalUninstall();
	}
	
	/**
	 * Internal implementation of {@link #uninstall()}.
	 */
	private void internalUninstall() {
		if (isInstalled()) {
			handleProjectionDisabled();
			fProjectionListener.dispose();
			fProjectionListener= null;
			fEditor= null;
		}
	}

	/**
	 * Returns <code>true</code> if the provider is installed, <code>false</code> otherwise.
	 * 
	 * @return <code>true</code> if the provider is installed, <code>false</code> otherwise
	 */
	protected final boolean isInstalled() {
		return fEditor != null;
	}

	/**
	 * Called whenever projection is enabled, for example when the viewer issues a
	 * {@link IProjectionListener#projectionEnabled() projectionEnabled} message. When the provider
	 * is already enabled when this method is called, it is first
	 * {@link #handleProjectionDisabled() disabled}.
	 * <p>
	 * Subclasses may extend.
	 * </p>
	 */
	protected void handleProjectionEnabled() {
		// http://home.ott.oti.com/teams/wswb/anon/out/vms/index.html
		// projectionEnabled messages are not always paired with projectionDisabled
		// i.e. multiple enabled messages may be sent out.
		// we have to make sure that we disable first when getting an enable
		// message.
		handleProjectionDisabled();

		if (fEditor instanceof JavaEditor) {
			initialize();
			fElementListener= new ElementChangedListener();
			JavaCore.addElementChangedListener(fElementListener);
		}
	}

	/**
	 * Called whenever projection is disabled, for example when the provider is
	 * {@link #uninstall() uninstalled}, when the viewer issues a
	 * {@link IProjectionListener#projectionDisabled() projectionDisabled} message and before
	 * {@link #handleProjectionEnabled() enabling} the provider. Implementations must be prepared to
	 * handle multiple calls to this method even if the provider is already disabled.
	 * <p>
	 * Subclasses may extend.
	 * </p>
	 */
	protected void handleProjectionDisabled() {
		if (fElementListener != null) {
			JavaCore.removeElementChangedListener(fElementListener);
			fElementListener= null;
		}
	}

	/*
	 * @see org.eclipse.jdt.ui.text.folding.IJavaFoldingStructureProvider#initialize()
	 */
	public final void initialize() {
		FoldingStructureComputationContext ctx= createInitialContext();
		if (ctx == null)
			return;
		
		computeFoldingStructure(ctx);
		Map additions= ctx.fMap;
		/*
		 *  Minimize the events being sent out - as this happens in the
		 *  UI thread merge everything into one call.
		 */
		List removals= new LinkedList();
		Iterator existing= ctx.getModel().getAnnotationIterator();
		while (existing.hasNext())
			removals.add(existing.next());
		ctx.getModel().replaceAnnotations((Annotation[]) removals.toArray(new Annotation[removals.size()]), additions);
	}

	private FoldingStructureComputationContext createInitialContext() {
		initializePreferences();
		fInput= getInputElement();
		if (fInput == null)
			return null;
		
		return createContext(true);
	}

	private FoldingStructureComputationContext createContext(boolean allowCollapse) {
		if (!isInstalled())
			return null;
		ProjectionAnnotationModel model= getModel();
		if (model == null)
			return null;
		IDocument doc= getDocument();
		if (doc == null)
			return null;
		
		return new FoldingStructureComputationContext(doc, model, allowCollapse);
	}
	
	private IJavaElement getInputElement() {
		if (fEditor == null)
			return null;
		
		IJavaElement javaElement= EditorUtility.getEditorInputJavaElement(fEditor, false);
		if (javaElement instanceof ICompilationUnit) {
			ICompilationUnit unit= (ICompilationUnit) javaElement;
			try {
				JavaModelUtil.reconcile(unit);
			} catch (JavaModelException x) {
				JavaPlugin.log(x);
			}
		}
		
		return javaElement;
	}

	private void initializePreferences() {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		fCollapseInnerTypes= store.getBoolean(PreferenceConstants.EDITOR_FOLDING_INNERTYPES);
		fCollapseImportContainer= store.getBoolean(PreferenceConstants.EDITOR_FOLDING_IMPORTS);
		fCollapseJavadoc= store.getBoolean(PreferenceConstants.EDITOR_FOLDING_JAVADOC);
		fCollapseMembers= store.getBoolean(PreferenceConstants.EDITOR_FOLDING_METHODS);
		fCollapseHeaderComments= store.getBoolean(PreferenceConstants.EDITOR_FOLDING_HEADERS);
	}

	private void update() {
		FoldingStructureComputationContext ctx= createContext(false);
		if (ctx == null)
			return;

		Map additions= new HashMap();
		List deletions= new ArrayList();
		List updates= new ArrayList();
		
		computeFoldingStructure(ctx);
		Map updated= ctx.fMap;
		Map previous= computeCurrentStructure(ctx);


		Iterator e= updated.keySet().iterator();
		while (e.hasNext()) {
			JavaProjectionAnnotation newAnnotation= (JavaProjectionAnnotation) e.next();
			IJavaElement element= newAnnotation.getElement();
			Position newPosition= (Position) updated.get(newAnnotation);

			List annotations= (List) previous.get(element);
			if (annotations == null) {

				additions.put(newAnnotation, newPosition);

			} else {
				Iterator x= annotations.iterator();
				boolean matched= false;
				while (x.hasNext()) {
					Tuple tuple= (Tuple) x.next();
					JavaProjectionAnnotation existingAnnotation= tuple.annotation;
					Position existingPosition= tuple.position;
					if (newAnnotation.isComment() == existingAnnotation.isComment()) {
						if (existingPosition != null && (!newPosition.equals(existingPosition))) {
							existingPosition.setOffset(newPosition.getOffset());
							existingPosition.setLength(newPosition.getLength());
							updates.add(existingAnnotation);
						}
						matched= true;
						x.remove();
						break;
					}
				}
				if (!matched)
					additions.put(newAnnotation, newPosition);

				if (annotations.isEmpty())
					previous.remove(element);
			}
		}

		e= previous.values().iterator();
		while (e.hasNext()) {
			List list= (List) e.next();
			int size= list.size();
			for (int i= 0; i < size; i++)
				deletions.add(((Tuple) list.get(i)).annotation);
		}

		match(deletions, additions, updates, ctx);

		Annotation[] removals= new Annotation[deletions.size()];
		deletions.toArray(removals);
		Annotation[] changes= new Annotation[updates.size()];
		updates.toArray(changes);
		ctx.getModel().modifyAnnotations(removals, additions, changes);
	}
	
	private void computeFoldingStructure(FoldingStructureComputationContext ctx) {
		IParent parent= (IParent) fInput;
		try {
			computeFoldingStructure(parent.getChildren(), ctx);
		} catch (JavaModelException x) {
		}
	}

	private void computeFoldingStructure(IJavaElement[] elements, FoldingStructureComputationContext ctx) throws JavaModelException {
		for (int i= 0; i < elements.length; i++) {
			IJavaElement element= elements[i];

			computeFoldingStructure(element, ctx);

			if (element instanceof IParent) {
				IParent parent= (IParent) element;
				computeFoldingStructure(parent.getChildren(), ctx);
			}
		}
	}

	/**
	 * Computes the folding structure for a given {@link IJavaElement java element}. Computed
	 * projection annotations are
	 * {@link DefaultJavaFoldingStructureProvider.FoldingStructureComputationContext#addProjectionRange(DefaultJavaFoldingStructureProvider.JavaProjectionAnnotation, Position) added}
	 * to the computation context.
	 * <p>
	 * Subclasses may extend or replace. The default implementation creates projection annotations
	 * for the following elements:
	 * <ul>
	 * <li>true members (not for top-level types)</li>
	 * <li>the javadoc comments of any member</li>
	 * <li>header comments (javadoc or multi-line comments appearing before the first type's
	 * javadoc or before the package or import declarations).</li>
	 * </ul>
	 * </p>
	 * 
	 * @param element the java element to compute the folding structure for
	 * @param ctx the computation context
	 */
	protected void computeFoldingStructure(IJavaElement element, FoldingStructureComputationContext ctx) {

		boolean collapse= false;
		boolean collapseCode= true;
		switch (element.getElementType()) {

			case IJavaElement.IMPORT_CONTAINER:
				collapse= ctx.collapseImportContainer();
				break;
			case IJavaElement.TYPE:
				collapseCode= isInnerType((IType) element);
				collapse= ctx.collapseInnerTypes() && collapseCode;
				break;
			case IJavaElement.METHOD:
			case IJavaElement.FIELD:
			case IJavaElement.INITIALIZER:
				collapse= ctx.collapseMembers();
				break;
			default:
				return;
		}

		IRegion[] regions= computeProjectionRanges((ISourceReference) element, ctx);
		if (regions.length > 0) {
			// comments
			for (int i= 0; i < regions.length - 1; i++) {
				IRegion normalized= alignRegion(regions[i], ctx);
				if (normalized != null) {
					Position position= createCommentPosition(normalized);
					if (position != null) {
						boolean commentCollapse;
						if (i == 0 && (regions.length > 2 || ctx.hasHeaderComment()) && element == ctx.getFirstType()) {
							commentCollapse= ctx.collapseHeaderComments();
						} else {
							commentCollapse= ctx.collapseJavadoc();
						}
						ctx.addProjectionRange(new JavaProjectionAnnotation(commentCollapse, element, true), position);
					}
				}
			}
			// code
			if (collapseCode) {
				IRegion normalized= alignRegion(regions[regions.length - 1], ctx);
				if (normalized != null) {
					Position position= element instanceof IMember ? createMemberPosition(normalized, (IMember) element) : createCommentPosition(normalized);
					if (position != null)
						ctx.addProjectionRange(new JavaProjectionAnnotation(collapse, element, false), position);
				}
			}
		}
	}

	/**
	 * Returns <code>true</code> if <code>type</code> is not a top-level type, <code>false</code> if it is.
	 * 
	 * @param type the type to test
	 * @return <code>true</code> if <code>type</code> is an inner type
	 */
	private boolean isInnerType(IType type) {
		return type.getDeclaringType() != null;
	}

	/**
	 * Computes the projection ranges for a given <code>ISourceReference</code>. More than one
	 * range or none at all may be returned. If there are no foldable regions, an empty array is
	 * returned.
	 * <p>
	 * The last region in the returned array (if not empty) describes the region for the java
	 * element that implements the source reference. Any preceding regions describe javadoc comments
	 * of that java element.
	 * </p>
	 * 
	 * @param reference a java element that is a source reference
	 * @param ctx the folding context
	 * @return the regions to be folded
	 */
	protected final IRegion[] computeProjectionRanges(ISourceReference reference, FoldingStructureComputationContext ctx) {
		try {
				ISourceRange range= reference.getSourceRange();

				String contents= reference.getSource();
				if (contents == null)
					return new IRegion[0];

				List regions= new ArrayList();
				if (!ctx.hasFirstType() && reference instanceof IType) {
					ctx.setFirstType((IType) reference);
					IRegion headerComment= computeHeaderComment(ctx);
					if (headerComment != null) {
						regions.add(headerComment);
						ctx.setHasHeaderComment();
					}
				}

				IScanner scanner= ToolFactory.createScanner(true, false, false, false);
				scanner.setSource(contents.toCharArray());
				final int shift= range.getOffset();
				int start= shift;
				while (true) {

					int token= scanner.getNextToken();
					start= shift + scanner.getCurrentTokenStartPosition();

					switch (token) {
						case ITerminalSymbols.TokenNameCOMMENT_JAVADOC:
						case ITerminalSymbols.TokenNameCOMMENT_BLOCK: {
							int end= shift + scanner.getCurrentTokenEndPosition() + 1;
							regions.add(new Region(start, end - start));
						}
						case ITerminalSymbols.TokenNameCOMMENT_LINE:
							continue;
					}

					break;
				}

				regions.add(new Region(start, shift + range.getLength() - start));

				IRegion[] result= new IRegion[regions.size()];
				regions.toArray(result);
				return result;
		} catch (JavaModelException e) {
		} catch (InvalidInputException e) {
		}

		return new IRegion[0];
	}

	private IRegion computeHeaderComment(FoldingStructureComputationContext ctx) throws JavaModelException {
		// search at most up to the first type
		ISourceRange range= ctx.getFirstType().getSourceRange();
		if (range == null)
			return null;
		int start= 0;
		int end= range.getOffset();

		String content;
		try {
			content= ctx.getDocument().get(start, end - start);
		} catch (BadLocationException e) {
			return null; // ignore header comment in that case
		}

		/* code adapted from CommentFormattingStrategy:
		 * scan the header content up to the first type. Once a comment is
		 * found, accumulate any additional comments up to the stop condition.
		 * The stop condition is reaching a package declaration, import container,
		 * or the end of the input.
		 */
		IScanner scanner= ToolFactory.createScanner(true, false, false, false);
		scanner.setSource(content.toCharArray());

		int headerStart= -1;
		int headerEnd= -1;
		try {
			boolean foundComment= false;
			int terminal= scanner.getNextToken();
			while (terminal != ITerminalSymbols.TokenNameEOF && !(terminal == ITerminalSymbols.TokenNameclass || terminal == ITerminalSymbols.TokenNameinterface || terminal == ITerminalSymbols.TokenNameenum || (foundComment && (terminal == ITerminalSymbols.TokenNameimport || terminal == ITerminalSymbols.TokenNamepackage)))) {

				if (terminal == ITerminalSymbols.TokenNameCOMMENT_JAVADOC || terminal == ITerminalSymbols.TokenNameCOMMENT_BLOCK || terminal == ITerminalSymbols.TokenNameCOMMENT_LINE) {
					if (!foundComment)
						headerStart= scanner.getCurrentTokenStartPosition();
					headerEnd= scanner.getCurrentTokenEndPosition();
					foundComment= true;
				}
				terminal= scanner.getNextToken();
			}


		} catch (InvalidInputException ex) {
			return null;
		}

		if (headerEnd != -1) {
			return new Region(headerStart, headerEnd - headerStart);
		}
		return null;
	}
	
	/**
	 * Creates a comment folding position from an
	 * {@link #alignRegion(IRegion, DefaultJavaFoldingStructureProvider.FoldingStructureComputationContext) aligned}
	 * region.
	 * 
	 * @param aligned an aligned region
	 * @return a folding position corresponding to <code>aligned</code>
	 */
	protected final Position createCommentPosition(IRegion aligned) {
		return new CommentPosition(aligned.getOffset(), aligned.getLength());
	}
	
	/**
	 * Creates a folding position that remembers its member from an
	 * {@link #alignRegion(IRegion, DefaultJavaFoldingStructureProvider.FoldingStructureComputationContext) aligned}
	 * region.
	 * 
	 * @param aligned an aligned region
	 * @param member the member to remember
	 * @return a folding position corresponding to <code>aligned</code>
	 */
	protected final Position createMemberPosition(IRegion aligned, IMember member) {
		return new JavaElementPosition(aligned.getOffset(), aligned.getLength(), member);
	}

	/**
	 * Aligns <code>region</code> to start and end at a line offset. The region's start is
	 * decreased to the next line offset, and the end offset increased to the next line start or the
	 * end of the document. <code>null</code> is returned if <code>region</code> is
	 * <code>null</code> itself or does not comprise at least one line delimiter, as a single line
	 * cannot be folded.
	 * 
	 * @param region the region to align, may be <code>null</code>
	 * @param ctx the folding context
	 * @return a region equal or greater than <code>region</code> that is aligned with line
	 *         offsets, <code>null</code> if the region is too small to be foldable (e.g. covers
	 *         only one line)
	 */
	protected final IRegion alignRegion(IRegion region, FoldingStructureComputationContext ctx) {
		if (region == null)
			return null;
		
		IDocument document= ctx.getDocument();
		
		try {
			
			int start= document.getLineOfOffset(region.getOffset());
			int end= document.getLineOfOffset(region.getOffset() + region.getLength());
			if (start >= end)
				return null;
			
			int offset= document.getLineOffset(start);
			int endOffset;
			if (document.getNumberOfLines() > end + 1)
				endOffset= document.getLineOffset(end + 1);
			else
				endOffset= document.getLineOffset(end) + document.getLineLength(end);
			
			return new Region(offset, endOffset - offset);
			
		} catch (BadLocationException x) {
			// concurrent modification
			return null;
		}
	}
	
	private ProjectionAnnotationModel getModel() {
		return (ProjectionAnnotationModel) fEditor.getAdapter(ProjectionAnnotationModel.class);
	}

	private IDocument getDocument() {
		IDocumentProvider provider= fEditor.getDocumentProvider();
		return provider.getDocument(fEditor.getEditorInput());
	}

	/**
	 * Matches deleted annotations to changed or added ones. A deleted
	 * annotation/position tuple that has a matching addition / change
	 * is updated and marked as changed. The matching tuple is not added
	 * (for additions) or marked as deletion instead (for changes). The
	 * result is that more annotations are changed and fewer get
	 * deleted/re-added.
	 */
	private void match(List deletions, Map additions, List changes, FoldingStructureComputationContext ctx) {
		if (deletions.isEmpty() || (additions.isEmpty() && changes.isEmpty()))
			return;

		List newDeletions= new ArrayList();
		List newChanges= new ArrayList();

		Iterator deletionIterator= deletions.iterator();
		while (deletionIterator.hasNext()) {
			JavaProjectionAnnotation deleted= (JavaProjectionAnnotation) deletionIterator.next();
			Position deletedPosition= ctx.getModel().getPosition(deleted);
			if (deletedPosition == null)
				continue;
			
			Tuple deletedTuple= new Tuple(deleted, deletedPosition);

			Tuple match= findMatch(deletedTuple, changes, null, ctx);
			boolean addToDeletions= true; 
			if (match == null) {
				match= findMatch(deletedTuple, additions.keySet(), additions, ctx);
				addToDeletions= false;
			}
			
			if (match != null) {
				IJavaElement element= match.annotation.getElement();
				deleted.setElement(element);
				deletedPosition.setLength(match.position.getLength());
				if (deletedPosition instanceof JavaElementPosition && element instanceof IMember) {
					JavaElementPosition jep= (JavaElementPosition) deletedPosition;
					jep.setMember((IMember) element);
				}

				deletionIterator.remove();
				newChanges.add(deleted);

				if (addToDeletions)
					newDeletions.add(match.annotation);
			}
		}

		deletions.addAll(newDeletions);
		changes.addAll(newChanges);
	}

	/**
	 * Finds a match for <code>tuple</code> in a collection of
	 * annotations. The positions for the
	 * <code>JavaProjectionAnnotation</code> instances in
	 * <code>annotations</code> can be found in the passed
	 * <code>positionMap</code> or <code>fCachedModel</code> if
	 * <code>positionMap</code> is <code>null</code>.
	 * <p>
	 * A tuple is said to match another if their annotations have the
	 * same comment flag and their position offsets are equal.
	 * </p>
	 * <p>
	 * If a match is found, the annotation gets removed from
	 * <code>annotations</code>.
	 * </p>
	 * 
	 * @param tuple the tuple for which we want to find a match
	 * @param annotations collection of
	 *        <code>JavaProjectionAnnotation</code>
	 * @param positionMap a <code>Map&lt;Annotation, Position&gt;</code>
	 *        or <code>null</code>
	 * @return a matching tuple or <code>null</code> for no match
	 */
	private Tuple findMatch(Tuple tuple, Collection annotations, Map positionMap, FoldingStructureComputationContext ctx) {
		Iterator it= annotations.iterator();
		while (it.hasNext()) {
			JavaProjectionAnnotation annotation= (JavaProjectionAnnotation) it.next();
			if (tuple.annotation.isComment() == annotation.isComment()) {
				Position position= positionMap == null ? ctx.getModel().getPosition(annotation) : (Position) positionMap.get(annotation);
				if (position == null)
					continue;

				if (tuple.position.getOffset() == position.getOffset()) {
					it.remove();
					return new Tuple(annotation, position);
				}
			}
		}
		
		return null;
	}

	private Map computeCurrentStructure(FoldingStructureComputationContext ctx) {
		Map map= new HashMap();
		ProjectionAnnotationModel model= ctx.getModel();
		Iterator e= model.getAnnotationIterator();
		while (e.hasNext()) {
			Object annotation= e.next();
			if (annotation instanceof JavaProjectionAnnotation) {
				JavaProjectionAnnotation java= (JavaProjectionAnnotation) annotation;
				Position position= model.getPosition(java);
				Assert.isNotNull(position);
				List list= (List) map.get(java.getElement());
				if (list == null) {
					list= new ArrayList(2);
					map.put(java.getElement(), list);
				}
				list.add(new Tuple(java, position));
			}
		}

		Comparator comparator= new Comparator() {
			public int compare(Object o1, Object o2) {
				return ((Tuple) o1).position.getOffset() - ((Tuple) o2).position.getOffset();
			}
		};
		for (Iterator it= map.values().iterator(); it.hasNext();) {
			List list= (List) it.next();
			Collections.sort(list, comparator);
		}
		return map;
	}
	
	/*
	 * @see IJavaFoldingStructureProviderExtension#collapseMembers()
	 * @since 3.2
	 */
	public final void collapseMembers() {
		modifyFiltered(fMemberFilter, false);
	}
	
	/*
	 * @see IJavaFoldingStructureProviderExtension#collapseComments()
	 * @since 3.2
	 */
	public final void collapseComments() {
		modifyFiltered(fCommentFilter, false);
	}

	/*
	 * @see org.eclipse.jdt.ui.text.folding.IJavaFoldingStructureProviderExtension#collapseElements(org.eclipse.jdt.core.IJavaElement[])
	 */
	public final void collapseElements(IJavaElement[] elements) {
		Set set= new HashSet(Arrays.asList(elements));
		modifyFiltered(new JavaElementSetFilter(set, false), false);
	}

	/*
	 * @see org.eclipse.jdt.ui.text.folding.IJavaFoldingStructureProviderExtension#expandElements(org.eclipse.jdt.core.IJavaElement[])
	 */
	public final void expandElements(IJavaElement[] elements) {
		Set set= new HashSet(Arrays.asList(elements));
		modifyFiltered(new JavaElementSetFilter(set, true), true);
	}

	/**
	 * Collapses or expands all annotations matched by the passed filter.
	 * 
	 * @param filter the filter to use to select which annotations to collapse
	 * @param expand <code>true</code> to expand the matched annotations, <code>false</code> to
	 *        collapse them
	 */
	private void modifyFiltered(Filter filter, boolean expand) {
		if (!isInstalled())
			return;

		ProjectionAnnotationModel model= getModel();
		if (model == null)
			return;
		
		List modified= new ArrayList();
		Iterator iter= model.getAnnotationIterator();
		while (iter.hasNext()) {
			Object annotation= iter.next();
			if (annotation instanceof JavaProjectionAnnotation) {
				JavaProjectionAnnotation java= (JavaProjectionAnnotation) annotation;
				
				if (expand == java.isCollapsed() && filter.match(java)) {
					if (expand)
						java.markExpanded();
					else
						java.markCollapsed();
					modified.add(java);
				}

			}
		}
		
		model.modifyAnnotations(null, null, (Annotation[]) modified.toArray(new Annotation[modified.size()]));
	}
}
