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

package org.opends.ldap.responses;



import org.opends.ldap.controls.Control;



/**
 * A Search Result Reference represents an area not yet explored during
 * a Search operation.
 */
public interface SearchResultReference extends
    Response<SearchResultReference>
{

  /**
   * {@inheritDoc}
   */
  SearchResultReference addControl(Control control)
      throws UnsupportedOperationException, NullPointerException;



  /**
   * {@inheritDoc}
   */
  SearchResultReference clearControls()
      throws UnsupportedOperationException;



  /**
   * {@inheritDoc}
   */
  Control getControl(String oid) throws NullPointerException;



  /**
   * {@inheritDoc}
   */
  Iterable<Control> getControls();



  /**
   * {@inheritDoc}
   */
  boolean hasControls();



  /**
   * {@inheritDoc}
   */
  Control removeControl(String oid)
      throws UnsupportedOperationException, NullPointerException;



  /**
   * {@inheritDoc}
   */
  String toString();



  /**
   * {@inheritDoc}
   */
  StringBuilder toString(StringBuilder builder)
      throws NullPointerException;



  /**
   * Adds the provided continuation reference URI to this search result
   * reference.
   *
   * @param uri
   *          The continuation reference URI to be added.
   * @return This search result reference.
   * @throws UnsupportedOperationException
   *           If this search result reference does not permit
   *           continuation reference URI to be added.
   * @throws NullPointerException
   *           If {@code uri} was {@code null}.
   */
  SearchResultReference addURI(String uri)
      throws UnsupportedOperationException, NullPointerException;



  /**
   * Removes all the continuation reference URIs included with this
   * search result reference.
   *
   * @return This search result reference.
   * @throws UnsupportedOperationException
   *           If this search result reference does not permit
   *           continuation reference URIs to be removed.
   */
  SearchResultReference clearURIs()
      throws UnsupportedOperationException;



  /**
   * Returns an {@code Iterable} containing the continuation reference
   * URIs included with this search result reference. The returned
   * {@code Iterable} may be used to remove continuation reference URIs
   * if permitted by this search result reference.
   *
   * @return An {@code Iterable} containing the continuation reference
   *         URIs.
   */
  Iterable<String> getURIs();



  /**
   * Returns the number of continuation reference URIs in this search
   * result reference.
   *
   * @return The number of continuation reference URIs.
   */
  int getURICount();



  /**
   * Indicates whether or not this search result reference has any
   * continuation reference URIs.
   *
   * @return {@code true} if this search result reference has any
   *         continuation reference URIs, otherwise {@code false}.
   */
  boolean hasURIs();

}