/*
 * The Apache Software License, Version 1.1
 *
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights 
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer. 
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:  
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Xalan" and "Apache Software Foundation" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written 
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation and was
 * originally based on software copyright (c) 1999, Lotus
 * Development Corporation., http://www.lotus.com.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
package org.apache.xalan.templates;

import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;

import javax.xml.transform.TransformerConfigurationException;
import org.apache.xpath.XPath;
import org.apache.xml.utils.QName;
import org.apache.xalan.serialize.OutputFormat;
import org.apache.xalan.transformer.TransformerImpl;
import org.apache.xpath.XPathContext;

import org.w3c.dom.Node;
import org.w3c.dom.Element;

import javax.xml.transform.TransformerException;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * Represents a stylesheet that has methods that resolve includes and
 * imports.  It has methods on it that
 * return "composed" properties, which mean that:
 * <ol>
 * <li>Properties that are aggregates, like OutputFormat, will
 * be composed of properties declared in this stylsheet and all
 * included stylesheets.</li>
 * <li>Properties that aren't found, will be searched for first in
 * the includes, and, if none are located, will be searched for in
 * the imports.</li>
 * <li>Properties in that are not atomic on a stylesheet will
 * have the form getXXXComposed. Some properties, like version and id,
 * are not inherited, and so won't have getXXXComposed methods.</li>
 * </ol>
 * <p>In some cases getXXXComposed methods may calculate the composed
 * values dynamically, while in other cases they may store the composed
 * values.</p>
 */
public class StylesheetComposed extends Stylesheet
{

  /**
   * Uses an XSL stylesheet document.
   * @param parent  The including or importing stylesheet.
   */
  public StylesheetComposed(Stylesheet parent)
  {
    super(parent);
  }

  /**
   * Tell if this can be cast to a StylesheetComposed, meaning, you
   * can ask questions from getXXXComposed functions.
   *
   * NEEDSDOC ($objectName$) @return
   */
  public boolean isAggregatedType()
  {
    return true;
  }

  /** NEEDSDOC Field m_importNumber          */
  private int m_importNumber = -1;

  /** NEEDSDOC Field m_importCountComposed          */
  private int m_importCountComposed;

  /**
   * Recalculate the number of this stylesheet in the global
   * import list.
   * <p>For example, suppose</p>
   * <p>stylesheet A imports stylesheets B and C in that order;</p>
   * <p>stylesheet B imports stylesheet D;</p>
   * <p>stylesheet C imports stylesheet E.</p>
   * <p>Then the order of import precedence (lowest first) is D, B, E, C, A.</p>
   * <p>If this were stylesheet C, then the importsComposed list
   * would be E, B, D (highest first).</p>
   */
  void recomposeImports()
  {

    m_importNumber = getStylesheetRoot().getImportNumber(this);

    StylesheetRoot root = getStylesheetRoot();
    int globalImportCount = root.getGlobalImportCount();

    m_importCountComposed = (globalImportCount - m_importNumber) - 1;
  }

  /**
   * Get a stylesheet from the "import" list.
   * @see <a href="http://www.w3.org/TR/xslt#import">import in XSLT Specification</a>
   *
   * NEEDSDOC @param i
   *
   * NEEDSDOC ($objectName$) @return
   *
   * @throws ArrayIndexOutOfBoundsException
   */
  public StylesheetComposed getImportComposed(int i)
          throws ArrayIndexOutOfBoundsException
  {

    StylesheetRoot root = getStylesheetRoot();

    // Get the stylesheet that is offset past this stylesheet.
    // Thus, if the index of this stylesheet is 3, an argument 
    // to getImportComposed of 0 will return the 4th stylesheet 
    // in the global import list.
    return root.getGlobalImport(1 + m_importNumber + i);
  }

  /**
   * Get the number of imported stylesheets.
   * @see <a href="http://www.w3.org/TR/xslt#import">import in XSLT Specification</a>
   *
   * NEEDSDOC ($objectName$) @return
   */
  public int getImportCountComposed()
  {
    return m_importCountComposed;
  }

  /**
   * The combined list of includes.
   */
  private transient Vector m_includesComposed;

  /**
   * Recompose the value of the composed include list.
   *
   * NEEDSDOC @param including
   */
  void recomposeIncludes(Stylesheet including)
  {

    int n = including.getIncludeCount();

    if (n > 0)
    {
      if (null == m_includesComposed)
        m_includesComposed = new Vector();

      for (int i = 0; i < n; i++)
      {
        Stylesheet included = including.getInclude(i);

        m_includesComposed.addElement(included);
        recomposeIncludes(included);
      }
    }
  }

  /**
   * Get an "xsl:include" property.
   * @see <a href="http://www.w3.org/TR/xslt#include">include in XSLT Specification</a>
   *
   * NEEDSDOC @param i
   *
   * NEEDSDOC ($objectName$) @return
   *
   * @throws ArrayIndexOutOfBoundsException
   */
  public Stylesheet getIncludeComposed(int i)
          throws ArrayIndexOutOfBoundsException
  {

    if (null == m_includesComposed)
      throw new ArrayIndexOutOfBoundsException();

    return (Stylesheet) m_includesComposed.elementAt(i);
  }

  /**
   * Get the number of included stylesheets.
   * @see <a href="http://www.w3.org/TR/xslt#import">import in XSLT Specification</a>
   *
   * NEEDSDOC ($objectName$) @return
   */
  public int getIncludeCountComposed()
  {
    return (null != m_includesComposed) ? m_includesComposed.size() : 0;
  }

  /**
   * Table of DecimalFormatSymbols, keyed by QName.
   */
  private transient Hashtable m_decimalFormatSymbols;

  /**
   * Given a valid element decimal-format name, return the
   * decimalFormatSymbols with that name.
   * <p>It is an error to declare either the default decimal-format or
   * a decimal-format with a given name more than once (even with
   * different import precedence), unless it is declared every
   * time with the same value for all attributes (taking into
   * account any default values).</p>
   * <p>Which means, as far as I can tell, the decimal-format
   * properties are not additive.</p>
   * @return null if name is not found.
   */
  void recomposeDecimalFormats()
  {

    m_decimalFormatSymbols = new Hashtable();

    // Loop for this stylesheet and all stylesheets included or of lower 
    // import precidence.
    int nImports = getImportCountComposed();

    for (int i = -1; i < nImports; i++)
    {
      StylesheetComposed stylesheet = (i < 0) ? this : getImportComposed(i);

      // Does this stylesheet contain it?
      int nDFPs = stylesheet.getDecimalFormatCount();

      for (int dfpIndex = 0; dfpIndex < nDFPs; dfpIndex++)
      {
        DecimalFormatProperties dfp = stylesheet.getDecimalFormat(dfpIndex);

        m_decimalFormatSymbols.put(dfp.getName(),
                                   dfp.getDecimalFormatSymbols());
      }

      // Do the included stylesheets contain it?
      int nIncludes = stylesheet.getIncludeCountComposed();

      for (int k = 0; k < nIncludes; k++)
      {
        Stylesheet included = stylesheet.getIncludeComposed(k);

        nDFPs = included.getDecimalFormatCount();

        for (int dfpIndex = 0; dfpIndex < nDFPs; dfpIndex++)
        {
          DecimalFormatProperties dfp = included.getDecimalFormat(dfpIndex);

          m_decimalFormatSymbols.put(dfp.getName(),
                                     dfp.getDecimalFormatSymbols());
        }
      }
    }
  }

  /**
   * Given a valid element decimal-format name, return the
   * decimalFormatSymbols with that name.
   * <p>It is an error to declare either the default decimal-format or
   * a decimal-format with a given name more than once (even with
   * different import precedence), unless it is declared every
   * time with the same value for all attributes (taking into
   * account any default values).</p>
   * <p>Which means, as far as I can tell, the decimal-format
   * properties are not additive.</p>
   *
   * NEEDSDOC @param name
   * @return null if name is not found.
   */
  public DecimalFormatSymbols getDecimalFormatComposed(QName name)
  {
    return (DecimalFormatSymbols) m_decimalFormatSymbols.get(name);
  }

  /**
   * A list of properties that specify how to do space
   * stripping. This uses the same exact mechanism as Templates.
   */
  private WhitespaceList m_whiteSpaceInfoList;

  /**
   * Compile a lookup table for WhiteSpaceInfo elements, which are built
   * from xsl:strip-space and xsl:preserve space information.
   * @return null if node is not matched.
   *
   * @throws TransformerException
   */
  void recomposeWhiteSpaceInfo() throws TransformerException
  {

    int nIncludes = getIncludeCountComposed();

    for (int k = -1; k < nIncludes; k++)
    {
      Stylesheet included = (-1 == k) ? this : getIncludeComposed(k);
      int n = included.getStripSpaceCount();

      for (int i = 0; i < n; i++)
      {
        if (null == m_whiteSpaceInfoList)
          m_whiteSpaceInfoList = new WhitespaceList(this);

        m_whiteSpaceInfoList.setTemplate(included.getStripSpace(i));
      }

      n = included.getPreserveSpaceCount();

      for (int i = 0; i < n; i++)
      {
        if (null == m_whiteSpaceInfoList)
          m_whiteSpaceInfoList = new WhitespaceList(this);

        m_whiteSpaceInfoList.setTemplate(included.getPreserveSpace(i));
      }
    }
  }

  /**
   * Check to see if the caller should bother with check for
   * whitespace nodes.
   *
   * NEEDSDOC ($objectName$) @return
   */
  public boolean shouldCheckWhitespace()
  {

    if (null != m_whiteSpaceInfoList)
      return true;

    int n = getImportCountComposed();

    for (int i = 0; i < n; i++)
    {
      StylesheetComposed imported = getImportComposed(i);

      if (null != imported.m_whiteSpaceInfoList)
        return true;
    }

    return false;
  }

  /**
   * Get information about whether or not an element should strip whitespace.
   * @see <a href="http://www.w3.org/TR/xslt#strip">strip in XSLT Specification</a>
   *
   * NEEDSDOC @param support
   * NEEDSDOC @param targetElement
   *
   * NEEDSDOC ($objectName$) @return
   *
   * @throws TransformerException
   */
  public WhiteSpaceInfo getWhiteSpaceInfo(
          XPathContext support, Element targetElement) throws TransformerException
  {

    if (null != m_whiteSpaceInfoList)
      return (WhiteSpaceInfo) m_whiteSpaceInfoList.getTemplate(support,
              targetElement, null, false);
    else
      return null;
  }

  /**
   * A list of all key declarations visible from this stylesheet and all
   * lesser stylesheets.
   */
  private transient Vector m_keyDecls;

  /**
   * Recompose the key decls from this stylesheet and
   * all stylesheets within lesser import precedence.
   */
  void recomposeKeys()
  {

    m_keyDecls = new Vector();

    // Loop for this stylesheet and all stylesheets included or of lower 
    // import precidence.
    int nImports = getImportCountComposed();

    for (int i = -1; i < nImports; i++)
    {
      StylesheetComposed stylesheet = (i < 0) ? this : getImportComposed(i);

      // Does this stylesheet contain it?
      int nKeys = stylesheet.getKeyCount();

      for (int keyIndex = 0; keyIndex < nKeys; keyIndex++)
      {
        KeyDeclaration keyDecl = stylesheet.getKey(keyIndex);

        m_keyDecls.addElement(keyDecl);
      }

      // Do the included stylesheets contain it?
      int nIncludes = stylesheet.getIncludeCountComposed();

      for (int k = 0; k < nIncludes; k++)
      {
        Stylesheet included = stylesheet.getIncludeComposed(k);

        nKeys = included.getKeyCount();

        for (int keyIndex = 0; keyIndex < nKeys; keyIndex++)
        {
          KeyDeclaration keyDecl = included.getKey(keyIndex);

          m_keyDecls.addElement(keyDecl);
        }
      }
    }
  }

  /**
   * Get the composed "xsl:key" properties.
   * @see <a href="http://www.w3.org/TR/xslt#key">key in XSLT Specification</a>
   *
   * NEEDSDOC ($objectName$) @return
   */
  public Vector getKeysComposed()
  {
    return m_keyDecls;
  }

  /**
   * Composed set of all included and imported attribute set properties.
   * Each entry is a vector of ElemAttributeSet objects.
   * <p>Note: Should this go on the StylesheetRoot class instead?</p>
   */
  private transient Hashtable m_attrSets;

  /**
   * Recompose the attribute-set decls from this stylesheet and
   * all stylesheets within import precedence.
   */
  void recomposeAttributeSets()
  {

    m_attrSets = new Hashtable();

    // Loop for this stylesheet and all stylesheets included or of lower 
    // import precidence.
    int nImports = getImportCountComposed();

    for (int i = -1; i < nImports; i++)
    {
      StylesheetComposed stylesheet = (i < 0) ? this : getImportComposed(i);

      // Does this stylesheet contain it?
      int nAS = stylesheet.getAttributeSetCount();

      for (int asIndex = 0; asIndex < nAS; asIndex++)
      {
        ElemAttributeSet attrSet = stylesheet.getAttributeSet(asIndex);
        Vector attrSetList = (Vector) m_attrSets.get(attrSet.getName());

        if (null == attrSetList)
        {
          attrSetList = new Vector();

          m_attrSets.put(attrSet.getName(), attrSetList);
        }

        attrSetList.addElement(attrSet);
      }

      // Do the included stylesheets contain it?
      int nIncludes = stylesheet.getIncludeCountComposed();

      for (int k = 0; k < nIncludes; k++)
      {
        Stylesheet included = stylesheet.getIncludeComposed(k);

        nAS = included.getAttributeSetCount();

        for (int asIndex = 0; asIndex < nAS; asIndex++)
        {
          ElemAttributeSet attrSet = included.getAttributeSet(asIndex);
          Vector attrSetList = (Vector) m_attrSets.get(attrSet.getName());

          if (null == attrSetList)
          {
            attrSetList = new Vector();

            m_attrSets.put(attrSet.getName(), attrSetList);
          }

          attrSetList.addElement(attrSet);
        }
      }
    }
  }

  /**
   * Get a list "xsl:attribute-set" properties that match the qname.
   * @see <a href="http://www.w3.org/TR/xslt#attribute-sets">attribute-sets in XSLT Specification</a>
   *
   * NEEDSDOC @param name
   *
   * NEEDSDOC ($objectName$) @return
   *
   * @throws ArrayIndexOutOfBoundsException
   */
  public Vector getAttributeSetComposed(QName name)
          throws ArrayIndexOutOfBoundsException
  {
    return (Vector) m_attrSets.get(name);
  }
  
  /**
   * Composed set of all variables and params.
   * <p>Note: Should this go on the StylesheetRoot class instead?</p>
   */
  private transient Vector m_variables;

  /**
   * Recompose the attribute-set decls from this stylesheet and
   * all stylesheets within import precedence.
   */
  void recomposeVariables()
  {

    m_variables = new Vector();

    // Loop for this stylesheet and all stylesheets included or of lower 
    // import precidence.
    int nImports = getImportCountComposed();

    for (int i = -1; i < nImports; i++)
    {
      StylesheetComposed stylesheet = (i < 0) ? this : getImportComposed(i);

      // Does this stylesheet contain it?
      int nVariables = stylesheet.getVariableOrParamCount();

      for (int vIndex = 0; vIndex < nVariables; vIndex++)
      {
        ElemVariable elemVar = stylesheet.getVariableOrParam(vIndex);

        // Don't overide higher priority variable        
        if (getVariableOrParamComposed(elemVar.getName()) == null)
          m_variables.addElement(elemVar);
      }

      // Do the included stylesheets contain it?
      int nIncludes = stylesheet.getIncludeCountComposed();

      for (int k = 0; k < nIncludes; k++)
      {
        Stylesheet included = stylesheet.getIncludeComposed(k);

        nVariables = included.getVariableOrParamCount();

        for (int vIndex = 0; vIndex < nVariables; vIndex++)
        {
          ElemVariable elemVar = included.getVariableOrParam(vIndex);

          // Don't overide higher priority variable
          if (getVariableOrParamComposed(elemVar.getName()) == null)
            m_variables.addElement(elemVar);
        }
      }
    }
  }

  /**
   * Get an "xsl:variable" property.
   * @see <a href="http://www.w3.org/TR/xslt#top-level-variables">top-level-variables in XSLT Specification</a>
   *
   * NEEDSDOC @param qname
   *
   * NEEDSDOC ($objectName$) @return
   */
  public ElemVariable getVariableOrParamComposed(QName qname)
  {
    if (null != m_variables)
    {
      int n = m_variables.size();

      for (int i = 0; i < n; i++)
      {
        ElemVariable var = (ElemVariable)m_variables.elementAt(i);
        if(var.getName().equals(qname))
          return var;
      }
    }

    return null;
  }

  /**
   * Get all global "xsl:variable" properties in scope for this stylesheet.
   * @see <a href="http://www.w3.org/TR/xslt#top-level-variables">top-level-variables in XSLT Specification</a>
   *
   * NEEDSDOC ($objectName$) @return
   */
  public Vector getVariablesAndParamsComposed()
  {
    return m_variables;
  }

  /**
   * The "xsl:template" properties.
   */
  private transient TemplateList m_templateList = new TemplateList(this);

  /**
   * NEEDSDOC Method getTemplateListComposed 
   *
   *
   * NEEDSDOC (getTemplateListComposed) @return
   */
  public final TemplateList getTemplateListComposed()
  {
    return m_templateList;
  }

  /**
   * Aggregate the list of templates and included templates into a single list.
   * @see <a href="http://www.w3.org/TR/xslt#section-Defining-Template-Rules">section-Defining-Template-Rules in XSLT Specification</a>
   *
   * @throws TransformerException
   */
  public void recomposeTemplates() throws TransformerException
  {

    int nIncludes = getIncludeCountComposed();

    for (int k = nIncludes - 1; k >= -1; k--)
    {
      Stylesheet included = (-1 == k) ? this : getIncludeComposed(k);
      int n = included.getTemplateCount();

      for (int i = 0; i < n; i++)
      {
        ElemTemplate template = included.getTemplate(i);
        m_templateList.setTemplate(template);
      }
    }

    m_templateList.compose();
  }

  /**
   * For compilation support, we need the option of overwriting
   * (rather than appending to) previous composition.
   * We could phase out the old API in favor of this one, but I'm
   * holding off until we've made up our minds about compilation.
   * ADDED 9/5/2000 to support compilation experiment
   * @see <a href="http://www.w3.org/TR/xslt#section-Defining-Template-Rules">section-Defining-Template-Rules in XSLT Specification</a>
   *
   * NEEDSDOC @param flushFirst
   *
   * @throws TransformerException
   */
  public void recomposeTemplates(boolean flushFirst) throws TransformerException
  {

    if (flushFirst)
      m_templateList = new TemplateList(this);

    recomposeTemplates();
  }

  /**
   * Get an "xsl:template" property by node match. This looks in the imports as
   * well as this stylesheet.
   * @see <a href="http://www.w3.org/TR/xslt#section-Defining-Template-Rules">section-Defining-Template-Rules in XSLT Specification</a>
   *
   * NEEDSDOC @param support
   * NEEDSDOC @param targetNode
   * NEEDSDOC @param mode
   * NEEDSDOC @param quietConflictWarnings
   *
   * NEEDSDOC ($objectName$) @return
   *
   * @throws TransformerException
   */
  public ElemTemplate getTemplateComposed(
          XPathContext support, Node targetNode, QName mode, boolean quietConflictWarnings)
            throws TransformerException
  {
    return m_templateList.getTemplate(support, targetNode, mode,
                                      quietConflictWarnings);
  }

  /**
   * Get an "xsl:template" property. This looks in the imports as
   * well as this stylesheet.
   * @see <a href="http://www.w3.org/TR/xslt#section-Defining-Template-Rules">section-Defining-Template-Rules in XSLT Specification</a>
   *
   * NEEDSDOC @param qname
   *
   * NEEDSDOC ($objectName$) @return
   */
  public ElemTemplate getTemplateComposed(QName qname)
  {
    return m_templateList.getTemplate(qname);
  }

  /**
   * Composed set of all params.
   * <p>Note: Should this go on the StylesheetRoot class instead?</p>
   */
  private transient Hashtable m_namespaceAliasComposed;

  /**
   * Recompose the attribute-set decls from this stylesheet and
   * all stylesheets within import precedence.
   */
  void recomposeNamespaceAliases()
  {

    m_namespaceAliasComposed = new Hashtable();

    // Loop for this stylesheet and all stylesheets included or of lower 
    // import precidence.
    int nImports = getImportCountComposed();

    for (int i = -1; i < nImports; i++)
    {
      StylesheetComposed stylesheet = (i < 0) ? this : getImportComposed(i);

      // Does this stylesheet contain it?
      int nNSA = stylesheet.getNamespaceAliasCount();

      for (int nsaIndex = 0; nsaIndex < nNSA; nsaIndex++)
      {
        NamespaceAlias nsAlias = stylesheet.getNamespaceAlias(nsaIndex);

        m_namespaceAliasComposed.put(nsAlias.getStylesheetPrefix(),
                                     nsAlias.getResultPrefix());
      }

      // Do the included stylesheets contain it?
      int nIncludes = stylesheet.getIncludeCountComposed();

      for (int k = 0; k < nIncludes; k++)
      {
        Stylesheet included = stylesheet.getIncludeComposed(k);

        nNSA = included.getNamespaceAliasCount();

        for (int nsaIndex = 0; nsaIndex < nNSA; nsaIndex++)
        {
          NamespaceAlias nsAlias = included.getNamespaceAlias(nsaIndex);

          m_namespaceAliasComposed.put(nsAlias.getStylesheetPrefix(),
                                       nsAlias.getResultPrefix());
        }
      }
    }
  }

  /**
   * Get the "xsl:namespace-alias" property.
   * Return the alias namespace uri for a given namespace uri if one is found.
   * @see <a href="http://www.w3.org/TR/xslt#literal-result-element">literal-result-element in XSLT Specification</a>
   *
   * NEEDSDOC @param uri
   *
   * NEEDSDOC ($objectName$) @return
   */
  public String getNamespaceAliasComposed(String uri)
  {
    return (String) m_namespaceAliasComposed.get(uri);
  }
}
