package org.opends.sdk.schema.matchingrules;

import org.testng.annotations.DataProvider;
import org.opends.sdk.ConditionResult;
import org.opends.sdk.schema.Schema;
import org.opends.sdk.schema.MatchingRule;

import static org.opends.server.schema.SchemaConstants.EMR_CASE_IGNORE_IA5_OID;

/**
 * Test the CaseExactIA5EqualityMatchingRule.
 */
public class CaseIgnoreIA5EqualityMatchingRuleTest extends
    MatchingRuleTest
{

  /**
   * {@inheritDoc}
   */
  @Override
  @DataProvider(name="matchingRuleInvalidAttributeValues")
  public Object[][] createMatchingRuleInvalidAttributeValues()
  {
    return new Object[][] {
        {"12345678\uFFFD"},
    };
  }


  /**
   * {@inheritDoc}
   */
  @Override
  @DataProvider(name="matchingrules")
  public Object[][] createMatchingRuleTest()
  {
    return new Object[][] {
        {"12345678", "12345678", ConditionResult.TRUE},
        {"ABC45678", "ABC45678", ConditionResult.TRUE},
        {"ABC45678", "abc45678", ConditionResult.TRUE},
    };
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected MatchingRule getRule()
  {
    return Schema.getCoreSchema().getMatchingRule(EMR_CASE_IGNORE_IA5_OID);
  }

}
