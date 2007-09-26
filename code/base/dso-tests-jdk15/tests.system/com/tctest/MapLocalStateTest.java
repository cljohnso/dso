/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

public class MapLocalStateTest extends TransparentTestBase {

  public static final int NODE_COUNT = 2;
    
  public MapLocalStateTest() {
    disableAllUntil("2007-10-05");
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return MapLocalStateTestApp.class;
  }

}
