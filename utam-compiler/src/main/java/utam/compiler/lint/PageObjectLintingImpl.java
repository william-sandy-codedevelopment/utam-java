/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: MIT
 * For full license text, see the LICENSE file in the repo root
 * or https://opensource.org/licenses/MIT
 */
package utam.compiler.lint;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import utam.core.declarative.lint.PageObjectLinting;
import utam.core.declarative.representation.TypeProvider;
import utam.core.element.Locator;

/**
 * Object with an information needed for linting of a single PO
 *
 * @author elizaveta.ivanova
 * @since 242
 */
public class PageObjectLintingImpl implements PageObjectLinting {

  private final String name;
  private final String filePath;
  private final String type;
  private final List<ElementLinting> elements = new ArrayList<>();
  private final List<MethodLinting> methods = new ArrayList<>();
  private final Set<String> shadowRoots = new HashSet<>();
  private final File fileScanner;
  private RootLinting rootContext;


  public PageObjectLintingImpl(String name, String pageObjectFilePath, TypeProvider type) {
    this.name = name;
    String dir = System.getProperty("user.dir");
    // file path should be relative to project root for SARIF
    // for unit tests path is dummy hence condition
    this.filePath =
        pageObjectFilePath.contains(dir) ? pageObjectFilePath.substring(dir.length() + 1)
            : pageObjectFilePath;
    this.type = type.getFullName();
    this.fileScanner = pageObjectFilePath.contains(dir) ? new File(pageObjectFilePath) : null;
  }

  static boolean isCustomElement(ElementLinting element) {
    return Stream
        .of(Element.LINTING_BASIC_TYPE, Element.LINTING_CONTAINER_TYPE, Element.LINTING_FRAME_TYPE)
        .noneMatch(type -> type.equals(element.getTypeFullName()));
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getJsonFilePath() {
    return filePath;
  }

  @Override
  public int findCodeLine(FileSearchContext context, String line) {
    if (fileScanner == null) {
      return 1;
    }
    return context.find(fileScanner, line);
  }

  @Override
  public List<MethodLinting> getMethods() {
    return methods;
  }

  @Override
  public void setElement(ElementLinting element) {
    elements.add(element);
  }

  @Override
  public void setMethod(MethodLinting method) {
    methods.add(method);
  }

  @Override
  public void setShadowBoundary(String elementName) {
    shadowRoots.add(elementName);
  }

  @Override
  public Set<String> getShadowBoundaries() {
    return shadowRoots;
  }

  @Override
  public String getTypeFullName() {
    return type;
  }

  @Override
  public RootLinting getRootContext() {
    return rootContext;
  }

  @Override
  public void setRootContext(RootLinting context) {
    this.rootContext = context;
  }

  @Override
  public List<ElementLinting> getElements() {
    return elements;
  }

  /**
   * helper class to remember element selector information
   *
   * @author elizaveta.ivanova
   * @since 246
   */
  public static class ElementSelector {

    private final Object objectValue;
    private final String stringValue;
    private final boolean isList;

    public ElementSelector(Locator locator, boolean isList) {
      this.objectValue = locator.getValue();
      this.stringValue = locator.getStringValue();
      this.isList = isList;
    }
  }

  /**
   * helper class to check element's scope (parent and shadow root)
   *
   * @author elizaveta.ivanova
   * @since 246
   */
  public static class ElementScope {

    private final String parentName;
    private final boolean isInsideShadow;

    public ElementScope(String parentName, boolean isInsideShadow) {
      this.parentName = parentName;
      this.isInsideShadow = isInsideShadow;
    }
  }

  /**
   * Element information for linting
   *
   * @author elizaveta.ivanova
   * @since 242
   */
  public static class Element implements ElementLinting {

    public static final String LINTING_BASIC_TYPE = "basic";
    public static final String LINTING_CONTAINER_TYPE = "container";
    public static final String LINTING_FRAME_TYPE = "frame";

    private final ElementSelector selector;
    private final String type;
    final ElementScope scope;
    private final String name;

    public Element(String name, String type, ElementSelector selector, ElementScope scope) {
      this.type = type;
      this.selector = selector;
      this.scope = scope;
      this.name = name;
    }

    @Override
    public String getLocator() {
      return selector.stringValue;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public String getTypeFullName() {
      return type;
    }

    @Override
    public boolean isSameScope(ElementLinting element) {
      ElementScope firstScope = this.scope;
      ElementScope secondScope = ((Element) element).scope;
      if (firstScope == null) {
        return secondScope == null;
      }
      if (secondScope == null || firstScope.isInsideShadow != secondScope.isInsideShadow) {
        return false;
      }
      if (firstScope.parentName == null) {
        return secondScope.parentName == null;
      }
      return firstScope.parentName.equals(secondScope.parentName);
    }

    @Override
    public boolean isSameLocator(ElementLinting element) {
      ElementSelector first = this.selector;
      ElementSelector second = ((Element) element).selector;
      if (first == null ^ second == null) {
        return false;
      }
      if (first.objectValue.equals(second.objectValue)) {
        // duplicates allowed if one is a list, but not both
        return first.isList == second.isList;
      }
      return false;
    }
  }

  /**
   * Method information for linting
   *
   * @author elizaveta.ivanova
   * @since 242
   */
  public static class Method implements MethodLinting {

    private final boolean hasDescription;
    private final String name;

    public Method(String methodName, boolean hasDescription) {
      this.hasDescription = hasDescription;
      this.name = methodName;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public boolean hasDescription() {
      return hasDescription;
    }
  }

  /**
   * Root information for linting
   *
   * @author elizaveta.ivanova
   * @since 242
   */
  public static class Root implements RootLinting {

    private final boolean hasRootDescription;
    private final boolean hasAuthor;
    private final Element element;

    public Root(boolean hasRootDescription, boolean hasAuthor, ElementSelector selector) {
      this.hasRootDescription = hasRootDescription;
      this.hasAuthor = hasAuthor;
      this.element = selector == null ? null : new Element("root", null, selector, null);
    }

    @Override
    public boolean hasDescription() {
      return hasRootDescription;
    }

    @Override
    public boolean hasAuthor() {
      return hasAuthor;
    }

    @Override
    public ElementLinting getRootElement() {
      return element;
    }

    @Override
    public boolean isRoot() {
      return this.element != null;
    }
  }
}
