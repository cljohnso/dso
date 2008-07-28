/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.runtime;

public class NullThreadIDMap implements ThreadIDMap {

  public void addTCThreadID(long l) {
    //
  }

  public Long getTCThreadID(long l) {
    return null;
  }

}
