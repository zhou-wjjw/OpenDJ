package org.opends.sdk.extensions;

import java.io.IOException;

import org.opends.sdk.ResultCode;
import org.opends.sdk.asn1.ASN1;
import org.opends.sdk.asn1.ASN1Writer;
import org.opends.sdk.spi.AbstractExtendedResult;
import org.opends.sdk.util.ByteString;
import org.opends.sdk.util.ByteStringBuilder;

/**
 * This class implements the password modify extended operation response
 * defined in RFC 3062.  It includes support for requiring the user's current
 * password as well as for generating a new password if none was provided.
 */
public class PasswordModifyResult
    extends AbstractExtendedResult<PasswordModifyResult>
{
  private ByteString genPassword;

  public PasswordModifyResult(ResultCode resultCode)
  {
    super(resultCode);
  }

  /**
   * Get the newly generated password.
   *
   * @return The password generated by the server or <code>null</code> if
   *         it is not available.
   */
  public ByteString getGenPassword()
  {
    return genPassword;
  }

  public PasswordModifyResult setGenPassword(ByteString genPassword)
  {
    this.genPassword = genPassword;
    return this;
  }

  public ByteString getResponseValue()
  {
    if(genPassword != null)
    {
      ByteStringBuilder buffer = new ByteStringBuilder();
      ASN1Writer writer = ASN1.getWriter(buffer);

      try
      {
        writer.writeStartSequence();
        writer.writeOctetString(
            PasswordModifyRequest.TYPE_PASSWORD_MODIFY_GENERATED_PASSWORD,
            genPassword);
        writer.writeEndSequence();
      }
      catch (IOException ioe)
      {
        // This should never happen unless there is a bug somewhere.
        throw new RuntimeException(ioe);
      }

      return buffer.toByteString();
    }
    return null;
  }

  @Override
  public String toString()
  {
    StringBuilder builder = new StringBuilder();
    builder.append("PasswordModifyExtendedResponse(resultCode=");
    builder.append(getResultCode());
    builder.append(", matchedDN=");
    builder.append(getMatchedDN());
    builder.append(", diagnosticMessage=");
    builder.append(getDiagnosticMessage());
    builder.append(", referrals=");
    builder.append(getReferralURIs());
    if(genPassword != null)
    {
      builder.append(", genPassword=");
      builder.append(genPassword);
    }
    builder.append(", controls=");
    builder.append(getControls());
    builder.append(")");
    return builder.toString();
  }
}
