/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License").  You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the license at
 * trunk/opends/resource/legal-notices/OpenDS.LICENSE
 * or https://OpenDS.dev.java.net/OpenDS.LICENSE.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at
 * trunk/opends/resource/legal-notices/OpenDS.LICENSE.  If applicable,
 * add the following below this CDDL HEADER, with the fields enclosed
 * by brackets "[]" replaced with your own identifying information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *
 *      Copyright 2009 Sun Microsystems, Inc.
 */
package org.opends.sdk.schema;



import static org.opends.messages.SchemaMessages.WARN_ATTR_SYNTAX_BIT_STRING_INVALID_BIT;
import static org.opends.messages.SchemaMessages.WARN_ATTR_SYNTAX_BIT_STRING_NOT_QUOTED;
import static org.opends.messages.SchemaMessages.WARN_ATTR_SYNTAX_BIT_STRING_TOO_SHORT;

import org.opends.messages.Message;
import org.opends.sdk.DecodeException;
import org.opends.sdk.util.ByteSequence;
import org.opends.sdk.util.ByteString;



/**
 * This class defines the bitStringMatch matching rule defined in X.520
 * and referenced in RFC 2252.
 */
final class BitStringEqualityMatchingRuleImpl extends
    AbstractMatchingRuleImpl
{
  public ByteString normalizeAttributeValue(Schema schema,
      ByteSequence value) throws DecodeException
  {
    final String valueString = value.toString().toUpperCase();

    final int length = valueString.length();
    if (length < 3)
    {
      final Message message =
          WARN_ATTR_SYNTAX_BIT_STRING_TOO_SHORT.get(value.toString());
      throw new DecodeException(message);
    }

    if (valueString.charAt(0) != '\''
        || valueString.charAt(length - 2) != '\''
        || valueString.charAt(length - 1) != 'B')
    {
      final Message message =
          WARN_ATTR_SYNTAX_BIT_STRING_NOT_QUOTED.get(value.toString());
      throw new DecodeException(message);
    }

    for (int i = 1; i < length - 2; i++)
    {
      switch (valueString.charAt(i))
      {
      case '0':
      case '1':
        // These characters are fine.
        break;
      default:
        final Message message =
            WARN_ATTR_SYNTAX_BIT_STRING_INVALID_BIT.get(value
                .toString(), String.valueOf(valueString.charAt(i)));
        throw new DecodeException(message);
      }
    }

    return ByteString.valueOf(valueString);
  }
}
