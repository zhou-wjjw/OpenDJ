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
 *      Copyright 2010 Sun Microsystems, Inc.
 */

package org.opends.sdk.responses;

import com.sun.opends.sdk.util.Function;
import com.sun.opends.sdk.util.Iterables;
import org.opends.sdk.*;

import java.util.Collection;

/**
 * Unmodifiable Search result entry implementation.
 */
class UnmodifiableSearchResultEntryImpl  extends
    AbstractUnmodifiableResponseImpl<SearchResultEntry>
    implements SearchResultEntry
{
  private static final Function<Attribute, Attribute, Void>
      UNMODIFIABLE_ATTRIBUTE_FUNCTION =
      new Function<Attribute, Attribute, Void>()
      {

        public Attribute apply(final Attribute value, final Void p)
        {
          return Attributes.unmodifiableAttribute(value);
        }

      };

  UnmodifiableSearchResultEntryImpl(SearchResultEntry impl) {
    super(impl);
  }

  public boolean addAttribute(Attribute attribute)
      throws UnsupportedOperationException, NullPointerException {
    throw new UnsupportedOperationException();
  }

  public boolean addAttribute(Attribute attribute,
                              Collection<ByteString> duplicateValues)
      throws UnsupportedOperationException, NullPointerException {
    throw new UnsupportedOperationException();
  }

  public SearchResultEntry addAttribute(String attributeDescription,
                                        Object... values)
      throws LocalizedIllegalArgumentException,
      UnsupportedOperationException, NullPointerException {
    throw new UnsupportedOperationException();
  }

  public SearchResultEntry clearAttributes()
      throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  public boolean containsAttribute(Attribute attribute,
                                   Collection<ByteString> missingValues)
      throws NullPointerException {
    return impl.containsAttribute(attribute, missingValues);
  }

  public boolean containsAttribute(String attributeDescription,
                                   Object... values)
      throws LocalizedIllegalArgumentException,
      NullPointerException {
    return impl.containsAttribute(attributeDescription, values);
  }

  public Iterable<Attribute> getAllAttributes() {
    return Iterables.unmodifiable(Iterables.transform(impl
        .getAllAttributes(), UNMODIFIABLE_ATTRIBUTE_FUNCTION));
  }

  public Iterable<Attribute> getAllAttributes(
      AttributeDescription attributeDescription)
      throws NullPointerException {
    return Iterables.unmodifiable(Iterables.transform(impl
        .getAllAttributes(attributeDescription),
        UNMODIFIABLE_ATTRIBUTE_FUNCTION));
  }

  public Iterable<Attribute> getAllAttributes(
      String attributeDescription)
      throws LocalizedIllegalArgumentException,
      NullPointerException {
    return Iterables.unmodifiable(Iterables.transform(impl
        .getAllAttributes(attributeDescription),
        UNMODIFIABLE_ATTRIBUTE_FUNCTION));
  }

  public Attribute getAttribute(
      AttributeDescription attributeDescription)
      throws NullPointerException {
    final Attribute attribute =
        impl.getAttribute(attributeDescription);
    if (attribute != null)
    {
      return Attributes.unmodifiableAttribute(attribute);
    }
    else
    {
      return null;
    }
  }

  public Attribute getAttribute(String attributeDescription)
      throws LocalizedIllegalArgumentException,
      NullPointerException {
    final Attribute attribute =
        impl.getAttribute(attributeDescription);
    if (attribute != null)
    {
      return Attributes.unmodifiableAttribute(attribute);
    }
    else
    {
      return null;
    }
  }

  public int getAttributeCount() {
    return impl.getAttributeCount();
  }

  public DN getName() {
    return impl.getName();
  }

  public boolean removeAttribute(Attribute attribute,
                                 Collection<ByteString> missingValues)
      throws UnsupportedOperationException, NullPointerException {
    throw new UnsupportedOperationException();
  }

  public boolean removeAttribute(
      AttributeDescription attributeDescription)
      throws UnsupportedOperationException, NullPointerException {
    throw new UnsupportedOperationException();
  }

  public SearchResultEntry removeAttribute(String attributeDescription,
                                           Object... values)
      throws LocalizedIllegalArgumentException,
      UnsupportedOperationException, NullPointerException {
    throw new UnsupportedOperationException();
  }

  public boolean replaceAttribute(Attribute attribute)
      throws UnsupportedOperationException, NullPointerException {
    throw new UnsupportedOperationException();
  }

  public SearchResultEntry replaceAttribute(String attributeDescription,
                                            Object... values)
      throws LocalizedIllegalArgumentException,
      UnsupportedOperationException, NullPointerException {
    throw new UnsupportedOperationException();
  }

  public SearchResultEntry setName(DN dn)
      throws UnsupportedOperationException, NullPointerException {
    throw new UnsupportedOperationException();
  }

  public SearchResultEntry setName(String dn)
      throws LocalizedIllegalArgumentException,
      UnsupportedOperationException, NullPointerException {
    throw new UnsupportedOperationException();
  }
}
