package org.opends.sdk.schema.matchingrules;

import org.opends.sdk.schema.Schema;
import org.opends.sdk.schema.MatchingRule;

import static org.opends.server.schema.SchemaConstants.OMR_CASE_EXACT_OID;
import org.testng.annotations.DataProvider;

/**
 * Test the CaseExactOrderingMatchingRule.
 */
public class CaseExactOrderingMatchingRuleTest extends
    OrderingMatchingRuleTest
{

  /**
   * {@inheritDoc}
   */
  @Override
  @DataProvider(name="OrderingMatchingRuleInvalidValues")
  public Object[][] createOrderingMatchingRuleInvalidValues()
  {
    return new Object[][] {
    };
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @DataProvider(name="Orderingmatchingrules")
  public Object[][] createOrderingMatchingRuleTestData()
  {
    return new Object[][] {
        {"12345678", "02345678", 1},
        {"abcdef", "bcdefa", -1},
        {"abcdef", "abcdef", 0},
    };
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected MatchingRule getRule()
  {
    return Schema.getCoreSchema().getMatchingRule(OMR_CASE_EXACT_OID);
  }
}
