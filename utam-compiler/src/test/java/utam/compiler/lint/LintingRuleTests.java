/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: MIT
 * For full license text, see the LICENSE file in the repo root
 * or https://opensource.org/licenses/MIT
 */
package utam.compiler.lint;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static utam.compiler.lint.LintingConfigJson.DEFAULT_LINTING_CONFIG;

import java.util.List;
import org.testng.annotations.Test;
import utam.compiler.grammar.DeserializerUtilities;
import utam.compiler.helpers.TranslationContext;
import utam.core.declarative.lint.LintingConfig;
import utam.core.declarative.lint.LintingContext;
import utam.core.declarative.lint.LintingError;

/**
 * Test linting functionality
 *
 * @author elizaveta.ivanova
 * @since 242
 */
public class LintingRuleTests {

  private static List<LintingError> test(String[] jsonFiles) {
    LintingConfig linting = DEFAULT_LINTING_CONFIG;
    LintingContext context = linting.start();
    for (String jsonFile : jsonFiles) {
      TranslationContext translationContext = new DeserializerUtilities("test/" + jsonFile)
          .getContextWithPath(jsonFile);
      linting.lint(context, translationContext.getLintingObject());
    }
    linting.finish(context);
    linting.writeReport(context, null);
    return context.getErrors();
  }

  private static List<LintingError> test(String jsonFile) {
    String[] files = new String[]{jsonFile};
    return test(files);
  }

  @Test
  public void testDuplicateSelectorsInsideOneFile() {
    List<LintingError> errors = test("lint/rules/defaultConfig");
    assertThat(errors, hasSize(5));
    LintingError error = errors.get(0);
    assertThat(error.getFullMessage(),
        equalTo("lint rule ULR01 failure in page object test/lint/rules/defaultConfig: "
            + "warning 2001: duplicate selector \".two\" for the elements \"three\" and \"two\"; "
            + "remove duplicate elements: \"two\" or \"three\""));
    assertThat(error.getRuleId(), equalTo("ULR01"));
    assertThat(error.getFixSuggestion(),
        equalTo("remove duplicate elements: \"two\" or \"three\""));
    assertThat(error.getSourceLine(), equalTo(21));
    error = errors.get(1);
    assertThat(error.getFullMessage(),
        equalTo("lint rule ULR01 failure in page object test/lint/rules/defaultConfig: "
            + "warning 2001: duplicate selector \":scope > *:first-child\" for the elements \"container2\" and \"container1\"; "
            + "remove duplicate elements: \"container1\" or \"container2\""));
  }

  @Test
  public void testTwoContainersWithSameEmptySelector() {
    List<LintingError> errors = test("lint/rules/twoContainers");
    assertThat(errors, hasSize(1));
    assertThat(errors.get(0).getFullMessage(),
        equalTo("lint rule ULR01 failure in page object test/lint/rules/twoContainers: "
            + "warning 2001: duplicate selector \":scope > *:first-child\" for the elements \"container2\" and \"container1\"; "
            + "remove duplicate elements: \"container1\" or \"container2\""));
  }

  @Test
  public void testListCanHaveSameSelector() {
    List<LintingError> errors = test("lint/rules/listSelector");
    assertThat(errors, hasSize(1));
    assertThat(errors.get(0).getFullMessage(),
        equalTo("lint rule ULR01 failure in page object test/lint/rules/listSelector: "
            + "warning 2001: duplicate selector \"css\" for the elements \"list2\" and \"list1\"; "
            + "remove duplicate elements: \"list1\" or \"list2\""));
  }

  @Test
  public void testRequiredDescription() {
    List<LintingError> errors = test("lint/rules/defaultConfig");
    assertThat(errors, hasSize(5));
    LintingError error = errors.get(2);
    assertThat(error.getFullMessage(),
        equalTo("lint rule ULR02 failure in page object test/lint/rules/defaultConfig: "
            + "warning 2002: root description is missing; "
            + "add \"description\" property at the root"));
    assertThat(error.getRuleId(), equalTo("ULR02"));
    assertThat(error.getFixSuggestion(), equalTo("add \"description\" property at the root"));
    assertThat(error.getSourceLine(), equalTo(1));
    error = errors.get(3);
    assertThat(error.getFullMessage(),
        equalTo("lint rule ULR04 failure in page object test/lint/rules/defaultConfig: "
            + "warning 2003: method \"nodescription\" does not have description; "
            + "add \"description\" property to the method \"nodescription\""));
    assertThat(error.getRuleId(), equalTo("ULR04"));
    assertThat(error.getFixSuggestion(),
        equalTo("add \"description\" property to the method \"nodescription\""));
    assertThat(error.getSourceLine(), equalTo(47));
    error = errors.get(4);
    assertThat(error.getFullMessage(),
        equalTo("lint rule ULR05 failure in page object test/lint/rules/defaultConfig: "
            + "warning 2004: only root shadow boundary is allowed, please create another page object for the element \"three\"; "
            + "remove \"shadow\" under element \"three\" and create separate page object for its content"));
    assertThat(error.getRuleId(), equalTo("ULR05"));
    assertThat(error.getFixSuggestion(), equalTo(
        "remove \"shadow\" under element \"three\" and create separate page object for its content"));
    assertThat(error.getSourceLine(), equalTo(25));
  }

  @Test
  public void testElementDescriptionCanBeEmpty() {
    List<LintingError> errors = test("lint/rules/elementDescription");
    assertThat(errors, hasSize(0));
  }

  @Test
  public void testAuthorCantBeEmpty() {
    List<LintingError> errors = test("lint/rules/rootNoAuthor");
    assertThat(errors, hasSize(1));
    LintingError error = errors.get(0);
    assertThat(error.getFullMessage(),
        equalTo("lint rule ULR03 failure in page object test/lint/rules/rootNoAuthor: "
            + "warning 2005: property \"author\" is missing in the root description; "
            + "add \"author\" property to the root description"));
    assertThat(error.getRuleId(), equalTo("ULR03"));
    assertThat(error.getSourceLine(), equalTo(2));
  }

  @Test
  public void testShadowBoundaryRule() {
    List<LintingError> errors = test("lint/rules/defaultConfig");
    assertThat(errors, hasSize(5));
    LintingError error = errors.get(4);
    assertThat(error.getFullMessage(),
        equalTo("lint rule ULR05 failure in page object test/lint/rules/defaultConfig: "
            + "warning 2004: only root shadow boundary is allowed, please create another page object for the element \"three\"; "
            + "remove \"shadow\" under element \"three\" and create separate page object for its content"));
    assertThat(error.getRuleId(), equalTo("ULR05"));
    assertThat(error.getFixSuggestion(), equalTo(
        "remove \"shadow\" under element \"three\" and create separate page object for its content"));
    assertThat(error.getSourceLine(), equalTo(25));
  }

  @Test
  public void testRulesAppliedToInterface() {
    List<LintingError> errors = test("lint/rules/interface");
    assertThat(errors, hasSize(2));
  }

  @Test
  public void testDuplicateRootSelectors() {
    String[] files = new String[]{
        "lint/rootSelectors/one",
        "lint/rootSelectors/two",
        "lint/rootSelectors/three"
    };
    List<LintingError> errors = test(files);
    assertThat(errors, hasSize(3));
    LintingError error = errors.get(0);
    assertThat(error.getFullMessage(),
        equalTo("lint rule ULR06 failure in page object test/lint/rootSelectors/one: "
            + "warning 3001: same root selector \"root-selector\" is used as a root selector in the page object test/lint/rootSelectors/two; "
            + "remove one of the page objects with same root selector: \"test/lint/rootSelectors/one\" or \"test/lint/rootSelectors/two\""));
    assertThat(error.getRuleId(), equalTo("ULR06"));
    assertThat(error.getSourceLine(), equalTo(9));
    assertThat(errors.get(1).getFullMessage(),
        equalTo("lint rule ULR06 failure in page object test/lint/rootSelectors/one: "
            + "warning 3001: same root selector \"root-selector\" is used as a root selector in the page object test/lint/rootSelectors/three; "
            + "remove one of the page objects with same root selector: \"test/lint/rootSelectors/one\" or \"test/lint/rootSelectors/three\""));
    assertThat(errors.get(2).getFullMessage(),
        equalTo("lint rule ULR06 failure in page object test/lint/rootSelectors/two: "
            + "warning 3001: same root selector \"root-selector\" is used as a root selector in the page object test/lint/rootSelectors/three; "
            + "remove one of the page objects with same root selector: \"test/lint/rootSelectors/two\" or \"test/lint/rootSelectors/three\""));
  }

  @Test
  public void testElementTypeSameAsRoot() {
    String[] files = new String[]{
        "lint/rootType/root",
        "lint/rootType/elements"
    };
    List<LintingError> errors = test(files);
    assertThat(errors, hasSize(1));
    assertThat(errors.get(0).getFullMessage(),
        equalTo("lint rule ULR07 failure in page object test/lint/rootType/elements: "
            + "warning 3002: element \"customDifferentType\" should have type \"test.lint.roottype.Root\" because it uses its root selector; "
            + "change the element \"customDifferentType\" type to the type of the page object \"test.lint.roottype.Root\""));
  }

  @Test
  public void testElementsOfSameTypeProduceNoError() {
    String[] files = new String[]{
        "lint/elementTypes/customType",
        "lint/elementTypes/anotherCustomType"
    };
    List<LintingError> errors = test(files);
    assertThat(errors, hasSize(0));
  }

  @Test
  public void testBasicAndCustomTypesViolation() {
    String[] files = new String[]{
        "lint/elementTypes/basicType",
        "lint/elementTypes/customType"
    };
    List<LintingError> errors = test(files);
    assertThat(errors, hasSize(1));
    assertThat(errors.get(0).getFullMessage(),
        equalTo("lint rule ULR08 failure in page object test/lint/elementTypes/customType: warning 3003: "
            + "custom selector \"my-test-custom\" of the element \"test\" is used for an element \"basic\" in the page object test/lint/elementTypes/customType, but has a different type \"my.test.Custom\"; "
            + "change the element \"test\" type to the same type as the element \"basic\" in page object \"test/lint/elementTypes/customType\""));
  }


  @Test
  public void testContainerAndCustomType() {
    String[] files = new String[]{
        "lint/elementTypes/customType",
        "lint/elementTypes/containerType"
    };
    List<LintingError> errors = test(files);
    assertThat(errors, hasSize(1));
    assertThat(errors.get(0).getFullMessage(),
        equalTo("lint rule ULR08 failure in page object test/lint/elementTypes/containerType: warning 3003: "
            + "custom selector \"my-test-custom\" of the element \"container\" is used for an element \"test\" in the page object test/lint/elementTypes/containerType, but has a different type \"container\"; "
            + "change the element \"container\" type to the same type as the element \"test\" in page object \"test/lint/elementTypes/containerType\""));

  }
}
