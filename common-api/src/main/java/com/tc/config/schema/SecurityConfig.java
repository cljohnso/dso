/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema;

public interface SecurityConfig extends Config {
  boolean isEnabled();
  String getKeyStorePath();
  String getTrustStorePath();
}
