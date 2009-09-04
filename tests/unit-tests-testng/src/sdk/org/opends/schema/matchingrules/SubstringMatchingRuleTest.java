package org.opends.schema.matchingrules;

import org.opends.schema.SchemaTestCase;
import org.opends.schema.SubstringMatchingRule;
import org.opends.schema.MatchingRule;
import org.opends.server.types.ByteString;
import org.opends.server.types.ByteSequence;
import org.opends.types.ConditionResult;
import org.opends.ldap.DecodeException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

import java.util.List;
import java.util.ArrayList;

/**
 * Abstract class for building test for the substring matching rules.
 * This class is intended to be extended by one class for each substring
 * matching rules.
 */
public abstract class SubstringMatchingRuleTest extends SchemaTestCase
{
   /**
   * Generate data for the test of the middle string match.
   *
   * @return the data for the test of the middle string match.
   */
  @DataProvider(name="substringMiddleMatchData")
  public abstract Object[][] createSubstringMiddleMatchData();

  /**
   * Generate data for the test of the initial string match.
   *
   * @return the data for the test of the initial string match.
   */
  @DataProvider(name="substringInitialMatchData")
  public abstract Object[][] createSubstringInitialMatchData();

  /**
   * Generate data for the test of the final string match.
   *
   * @return the data for the test of the final string match.
   */
  @DataProvider(name="substringInitialMatchData")
  public abstract Object[][] createSubstringFinalMatchData();

  /**
   * Generate invalid attribute values for the Matching Rule test.
   *
   * @return the data for the EqualityMatchingRulesInvalidValuestest.
   */
  @DataProvider(name="substringInvalidAttributeValues")
  public abstract Object[][] createMatchingRuleInvalidAttributeValues();

  /**
   * Generate invalid assertion values for the Matching Rule test.
   *
   * @return the data for the EqualityMatchingRulesInvalidValuestest.
   */
  @DataProvider(name="substringInvalidAssertionValues")
  public abstract Object[][] createMatchingRuleInvalidAssertionValues();

  /**
   * Get an instance of the matching rule.
   *
   * @return An instance of the matching rule to test.
   */
  protected abstract SubstringMatchingRule getRule();

  /**
   * Test the normalization and the middle substring match.
   */
  @Test(dataProvider= "substringMiddleMatchData")
  public void middleMatchingRules(
      String value, String[] middleSubs, ConditionResult result) throws Exception
  {
    SubstringMatchingRule rule = getRule();

    // normalize the 2 provided values and check that they are equals
    ByteString normalizedValue =
      rule.normalizeAttributeValue(ByteString.valueOf(value));

    StringBuilder printableMiddleSubs = new StringBuilder();
    List<ByteSequence> middleList =
        new ArrayList<ByteSequence>(middleSubs.length);
    printableMiddleSubs.append("*");
    for (String middleSub : middleSubs) {
      printableMiddleSubs.append(middleSub);
      printableMiddleSubs.append("*");
      middleList.add(ByteString.valueOf(middleSub));
    }

    if (rule.getAssertion(null, middleList, null).matches(normalizedValue)
        != result ||
        rule.getAssertion(ByteString.valueOf(printableMiddleSubs)).matches(
            normalizedValue) != result)
    {
      fail("middle substring matching rule " + rule +
          " does not give expected result (" + result + ") for values : " +
          value + " and " + printableMiddleSubs);
    }
  }

  /**
   * Test the normalization and the initial substring match.
   */
  @Test(dataProvider= "substringInitialMatchData")
  public void initialMatchingRules(
      String value, String initial, ConditionResult result) throws Exception
  {
    SubstringMatchingRule rule = getRule();

    // normalize the 2 provided values and check that they are equals
    ByteString normalizedValue =
      rule.normalizeAttributeValue(ByteString.valueOf(value));

    if (rule.getAssertion(ByteString.valueOf(initial), null, null).matches(
        normalizedValue) != result ||
        rule.getAssertion(ByteString.valueOf(initial+"*")).matches(
            normalizedValue) != result)
    {
      fail("initial substring matching rule " + rule +
          " does not give expected result (" + result + ") for values : " +
          value + " and " + initial);
    }
  }

  /**
   * Test the normalization and the final substring match.
   */
  @Test(dataProvider= "substringFinalMatchData")
  public void finalMatchingRules(
      String value, String finalValue, ConditionResult result) throws Exception
  {
    SubstringMatchingRule rule = getRule();

    // normalize the 2 provided values and check that they are equals
    ByteString normalizedValue =
      rule.normalizeAttributeValue(ByteString.valueOf(value));

    if (rule.getAssertion(null, null, ByteString.valueOf(finalValue)).matches(
        normalizedValue) != result ||
        rule.getAssertion(ByteString.valueOf("*"+finalValue)).matches(
            normalizedValue) != result)
    {
      fail("final substring matching rule " + rule +
          " does not give expected result (" + result + ") for values : " +
          value + " and " + finalValue);
    }
  }

   /**
   * Test that invalid values are rejected.
   */
  @Test(expectedExceptions = DecodeException.class,
      dataProvider= "substringInvalidAttributeValues")
  public void substringInvalidAttributeValues(String value) throws Exception
    {
    // Get the instance of the rule to be tested.
    SubstringMatchingRule rule = getRule();

    rule.normalizeAttributeValue(ByteString.valueOf(value));
  }

  /**
   * Test that invalid values are rejected.
   */
  @Test(expectedExceptions = DecodeException.class,
      dataProvider= "substringInvalidAssertionValues")
  public void matchingRulesInvalidAssertionValues(String subInitial,
                                                  String[] anys,
                                                  String subFinal)
      throws Exception
  {
    // Get the instance of the rule to be tested.
    SubstringMatchingRule rule = getRule();

    List<ByteSequence> anyList =
        new ArrayList<ByteSequence>(anys.length);
    for (String middleSub : anys) {
      anyList.add(ByteString.valueOf(middleSub));
    }
    rule.getAssertion(subInitial == null ? null :
        ByteString.valueOf(subInitial), anyList,
        subFinal == null ? null : ByteString.valueOf(subFinal));
  }

    /**
   * Test that invalid values are rejected.
   */
  @Test(expectedExceptions = DecodeException.class,
      dataProvider= "substringInvalidAssertionValues")
  public void matchingRulesInvalidAssertionValuesString(String subInitial,
                                                  String[] anys,
                                                  String subFinal)
      throws Exception
  {
    // Get the instance of the rule to be tested.
    SubstringMatchingRule rule = getRule();

    StringBuilder assertionString = new StringBuilder();
    if(subInitial != null)
    {
      assertionString.append(subInitial);
    }
    assertionString.append("*");
    for (String middleSub : anys) {
      assertionString.append(middleSub);
      assertionString.append("*");
    }
    if(subFinal != null)
    {
      assertionString.append(subFinal);
    }
    rule.getAssertion(ByteString.valueOf(assertionString.toString()));
  }
}
