/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.builtin.ArrayList;
import com.tctest.builtin.CyclicBarrier;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.List;

public class SubtypeInstrumentTestApp extends AbstractErrorCatchingTransparentApp {
  private final CyclicBarrier barrier;
  private final SubClass      root = new SubClass();

  public SubtypeInstrumentTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, com.tc.object.config.DSOClientConfigHelper config) {
    String testClass = SubtypeInstrumentTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    spec.addRoot("barrier", "barrier");
    spec.addRoot("root", "root");

    String baseClass = BaseClass.class.getName();
    String subClass = SubClass.class.getName();

    config.addIncludePattern(baseClass + "+");

    // THIS IS INTENTIONALLY COMMENTED OUT TO MAKE SURE
    // THE TEST STILL PASSES. SEE CDV-144
    // config.addIncludePattern(subClass);

    config.addWriteAutolock("* " + baseClass + "*.*(..)");
    config.addWriteAutolock("* " + subClass + "*.*(..)");

  }

  @Override
  protected void runTest() throws Throwable {
    if (barrier.await() == 0) {
      root.add("one");
      root.add("two");
    }

    barrier.await();
    Assert.assertEquals(2, root.getSize());

  }

  static class BaseClass {
    protected List list = new ArrayList();

    public void add(Object o) {
      synchronized (list) {
        list.add(o);
      }
    }

    public int getSize() {
      synchronized (list) {
        return list.size();
      }
    }

  }

  static class SubClass extends BaseClass {
    public Object getFirst() {
      synchronized (list) {
        return list.get(0);
      }
    }
  }

}
