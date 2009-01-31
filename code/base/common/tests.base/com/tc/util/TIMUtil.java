/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import com.tc.bundles.BundleSpec;

import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

/**
 * Terracotta Integration Module Util This should be the only source where the TIM names and versions are defined. Check
 * content of integration-modules.properties
 */
public class TIMUtil {
  public static final String      COMMONS_COLLECTIONS_3_1;
  public static final String      SUREFIRE_2_3;
  public static final String      MODULES_COMMON;
  public static final String      JETTY_6_1;

  public static final String      TOMCAT_5_0;
  public static final String      TOMCAT_5_5;
  public static final String      TOMCAT_6_0;

  public static final String      JBOSS_3_2;
  public static final String      JBOSS_4_0;
  public static final String      JBOSS_4_2;

  public static final String      WEBLOGIC_9;
  public static final String      WEBLOGIC_10;

  public static final String      WASCE_1_0;

  public static final String      GLASSFISH_V1;
  public static final String      GLASSFISH_V2;

  private static final Properties modules = new Properties();

  static {
    try {
      modules.load(TIMUtil.class.getResourceAsStream("integration-modules.properties"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    COMMONS_COLLECTIONS_3_1 = lookup(".*commons-collections-3.1");
    SUREFIRE_2_3 = lookup(".*surefire-2.3");
    MODULES_COMMON = lookup("modules-common");
    JETTY_6_1 = lookup("tim-jetty-6.1");
    TOMCAT_5_0 = lookup("tim-tomcat-5.0");
    TOMCAT_5_5 = lookup("tim-tomcat-5.5");
    TOMCAT_6_0 = lookup("tim-tomcat-6.0");
    JBOSS_3_2 = lookup("tim-jboss-3.2");
    JBOSS_4_0 = lookup("tim-jboss-4.0");
    JBOSS_4_2 = lookup("tim-jboss-4.2");
    WEBLOGIC_9 = lookup("tim-weblogic-9");
    WEBLOGIC_10 = lookup("tim-weblogic-10");
    WASCE_1_0 = lookup("tim-wasce-1.0");
    GLASSFISH_V1 = lookup("tim-glassfish-v1");
    GLASSFISH_V2 = lookup("tim-glassfish-v2");
  }

  private TIMUtil() {
    // singleton
  }

  private static String lookup(String pattern) {
    String name = searchModuleName(pattern);
    if (name == null) { throw new RuntimeException("Can't find module with pattern: [" + pattern + "]"); }
    return name;
  }

  /**
   * @param pattern: java regular expression
   */
  public static String searchModuleName(String pattern) {
    if (modules.containsKey(pattern)) { return pattern; }
    String name = null;
    for (Iterator it = modules.keySet().iterator(); it.hasNext();) {
      String moduleName = (String) it.next();
      if (moduleName.matches(pattern)) {
        name = moduleName;
        break;
      }
    }
    return name;
  }

  public static String getVersion(String moduleName) {
    String spec = modules.getProperty(moduleName);
    BundleSpec bundleSpec = BundleSpec.newInstance(spec);
    return bundleSpec.getVersion();
  }

  public static String getGroupId(String moduleName) {
    String spec = modules.getProperty(moduleName);
    BundleSpec bundleSpec = BundleSpec.newInstance(spec);
    return bundleSpec.getGroupId();
  }

  public static BundleSpec getBundleSpec(String moduleName) {
    String spec = modules.getProperty(moduleName);
    return BundleSpec.newInstance(spec);
  }

  public static Set getModuleNames() {
    return modules.keySet();
  }
}
