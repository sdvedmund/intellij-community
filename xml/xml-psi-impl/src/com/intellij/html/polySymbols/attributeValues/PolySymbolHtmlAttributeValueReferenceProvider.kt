package com.intellij.html.polySymbols.attributeValues

import com.intellij.html.polySymbols.attributes.PolySymbolAttributeDescriptor
import com.intellij.polySymbols.html.HTML_ATTRIBUTE_VALUES
import com.intellij.polySymbols.PolySymbol
import com.intellij.polySymbols.html.PolySymbolHtmlAttributeValue
import com.intellij.polySymbols.html.PolySymbolHtmlAttributeValue.Type
import com.intellij.polySymbols.query.PolySymbolsQueryExecutorFactory
import com.intellij.polySymbols.references.PsiPolySymbolReferenceProvider
import com.intellij.polySymbols.utils.asSingleSymbol
import com.intellij.polySymbols.utils.hasOnlyExtensions
import com.intellij.psi.util.parentOfType
import com.intellij.psi.util.startOffset
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.util.asSafely

class PolySymbolHtmlAttributeValueReferenceProvider : PsiPolySymbolReferenceProvider<XmlAttributeValue> {
  override fun getReferencedSymbolNameOffset(psiElement: XmlAttributeValue): Int =
    psiElement.valueTextRange.startOffset - psiElement.startOffset

  override fun getReferencedSymbol(psiElement: XmlAttributeValue): PolySymbol? {
    val attribute = psiElement.parentOfType<XmlAttribute>()
    val attributeDescriptor = attribute?.descriptor?.asSafely<PolySymbolAttributeDescriptor>() ?: return null
    val type = attributeDescriptor.symbol.attributeValue
                 ?.takeIf { it.kind == null || it.kind == PolySymbolHtmlAttributeValue.Kind.PLAIN }
                 ?.type?.takeIf { it == Type.ENUM || it == Type.SYMBOL }
               ?: return null
    val name = psiElement.value.takeIf { it.isNotEmpty() } ?: return null
    val queryExecutor = PolySymbolsQueryExecutorFactory.create(psiElement)

    return if (type == Type.ENUM)
      if (queryExecutor.runCodeCompletionQuery(HTML_ATTRIBUTE_VALUES, "", 0)
          .filter { !it.completeAfterInsert }
          .none { it.name == name })
        null
      else
        queryExecutor
          .runNameMatchQuery(HTML_ATTRIBUTE_VALUES, name)
          .takeIf {
            it.isNotEmpty()
            && !it.hasOnlyExtensions()
          }
          ?.asSingleSymbol()
    else
      queryExecutor
        .also { it.keepUnresolvedTopLevelReferences = true }
        .runNameMatchQuery(HTML_ATTRIBUTE_VALUES, name)
        .takeIf {
          it.isNotEmpty()
          && !it.hasOnlyExtensions()
        }
        ?.asSingleSymbol()
      ?: PsiPolySymbolReferenceProvider.unresolvedSymbol(HTML_ATTRIBUTE_VALUES, name)
  }
}