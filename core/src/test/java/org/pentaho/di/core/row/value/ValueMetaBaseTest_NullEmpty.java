/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/


package org.pentaho.di.core.row.value;

import org.junit.Test;
import org.pentaho.di.core.Const;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ValueMetaBaseTest_NullEmpty {

  /**
   * By default, converting null value to a string value will yield a null value.
   * This is the expected behavior in current and past versions.
   */
  @Test
  public void convertDataFromStringWithDefaults() throws Exception {
    System.setProperty( Const.KETTLE_EMPTY_STRING_DIFFERS_FROM_NULL, "N" );
    System.setProperty( Const.KETTLE_DO_NOT_NORMALIZE_NULL_STRING_TO_EMPTY, "N" );

    ValueMetaBase out = new ValueMetaString();
    ValueMetaBase value = new ValueMetaString();

    Object data = out.convertDataFromString( null, value, null, null, 0 );
    assertNull( data );
  }

  /**
   * When KETTLE_EMPTY_STRING_DIFFERS_FROM_NULL is set to "Y" whe start getting unexpected results, see PDI-18440.
   * This flag should have no effect in data conversions, only when comparing values.
   */
  @Test
  public void convertDataFromStringWithEmptyDiffersFromNull() throws Exception {
    System.setProperty( Const.KETTLE_EMPTY_STRING_DIFFERS_FROM_NULL, "Y" );
    System.setProperty( Const.KETTLE_DO_NOT_NORMALIZE_NULL_STRING_TO_EMPTY, "N" );

    ValueMetaBase out = new ValueMetaString();
    ValueMetaBase value = new ValueMetaString();

    Object data = out.convertDataFromString( null, value, null, null, 0 );
    assertEquals( "", data );
  }

  /**
   * The new KETTLE_DO_NOT_NORMALIZE_NULL_STRING_TO_EMPTY flag fixes PDI-18440 resetting the behavior to what is expected.
   */
  @Test
  public void convertDataFromStringWithEmptyDiffersFromNullAndDoNotNormalize() throws Exception {
    System.setProperty( Const.KETTLE_EMPTY_STRING_DIFFERS_FROM_NULL, "Y" );
    System.setProperty( Const.KETTLE_DO_NOT_NORMALIZE_NULL_STRING_TO_EMPTY, "Y" );

    ValueMetaBase out = new ValueMetaString();
    ValueMetaBase value = new ValueMetaString();

    Object data = out.convertDataFromString( null, value, null, null, 0 );
    assertNull( data );
  }
}
