/*
 * Copyright (c) 2021, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: MIT
 * For full license text, see the LICENSE file in the repo root
 * or https://opensource.org/licenses/MIT
 */
package utam.compiler.guardrails;

import java.util.List;
import utam.compiler.helpers.ElementContext;
import utam.compiler.helpers.ElementContext.Basic;
import utam.compiler.helpers.ElementContext.Root;
import utam.core.declarative.representation.TypeProvider;
import utam.core.element.Locator;

/**
 * utilities for guardrails validations
 *
 * @author elizaveta.ivanova
 * @since 236
 */
abstract class ValidationUtilities {

  private static final List<String> PROHIBITED_SELECTORS = List.of(
      "[value",
      "[title");

  static boolean isSameSelector(Locator underTest, Locator testAgainst) {
    // root can have null as selector
    if (underTest == null || testAgainst == null) {
      return false;
    }
    if (underTest.getStringValue().isEmpty() || testAgainst.getStringValue()
        .isEmpty()) { // for container
      return false;
    }
    return testAgainst.equals(underTest);
  }

  static boolean hasHardcodedText(Locator underTest) {
    // root can have null as selector
    if (underTest == null) {
      return false;
    }
    String selector = underTest.getStringValue();
    if (PROHIBITED_SELECTORS.stream().anyMatch(selector::contains)) {
      return !selector.contains("%");
    }
    return false;
  }

  static ValidationError getValidationError(ElementContext first, ElementContext second) {
    if (first.isSelfElement() || first.isDocumentElement() || first instanceof ElementContext.Container) {
      return null;
    }
    if (first.isRootElement()) {
      return validateRootElement((ElementContext.Root) first, second);
    }
    if (first.isCustomElement()) {
      return validateCustomElement(first, second);
    }
    return validateBasicElement(first, second);
  }

  private static ValidationError validateCustomElement(ElementContext customElement,
      ElementContext element) {
    Locator customElementLocator = customElement.getSelector();
    // this statement should be before next because declared root element is also HTML element
    if (element.isRootElement()) {
      if (customElement.getType().isSameType(((Root) element).getEnclosingPageObjectType())) {
        return null;
      }
      if (isSameSelector(customElementLocator, element.getSelector())) {
        return ValidationError.DUPLICATE_WITH_ROOT_SELECTOR;
      }
      return null;
    }
    if (element instanceof Basic && isSameSelector(customElementLocator, element.getSelector())) {
      return ValidationError.COMPONENT_AND_ELEMENT_DUPLICATE_SELECTOR;
    }
    // if selector same but type is different - it's error
    if (element.isCustomElement()
        && !customElement.getType().isSameType(element.getType())
        && isSameSelector(customElementLocator, element.getSelector())) {
      return ValidationError.COMPONENTS_WITH_SAME_SELECTOR_BUT_DIFFERENT_TYPES;
    }
    return null;
  }

  private static ValidationError validateBasicElement(ElementContext basicElement, ElementContext element) {
    Locator basicElementLocator = basicElement.getSelector();
    if (element.isRootElement() && isSameSelector(basicElementLocator, element.getSelector())) {
      return ValidationError.DUPLICATE_WITH_ROOT_SELECTOR;
    }
    if (element.isCustomElement() && isSameSelector(basicElementLocator, element.getSelector())) {
      return ValidationError.COMPONENT_AND_ELEMENT_DUPLICATE_SELECTOR;
    }
    return null;
  }

  private static boolean isSameEnclosingType(TypeProvider enclosingPageObjectType,
      ElementContext element) {
    if (element.isCustomElement() && element.getType().isSameType(enclosingPageObjectType)) {
      return true;
    }
    if (element.isRootElement()) {
      return enclosingPageObjectType.isSameType(((Root) element).getEnclosingPageObjectType());
    }
    return false;
  }

  private static ValidationError validateRootElement(ElementContext.Root rootElement,
      ElementContext element) {
    TypeProvider enclosingPageObjectType = rootElement.getEnclosingPageObjectType();
    Locator rootElementLocator = rootElement.getSelector();
    if (isSameEnclosingType(enclosingPageObjectType, element)) {
      return null;
    }
    if (isSameSelector(rootElementLocator, element.getSelector())) {
      return ValidationError.DUPLICATE_WITH_ROOT_SELECTOR;
    }
    return null;
  }
}
