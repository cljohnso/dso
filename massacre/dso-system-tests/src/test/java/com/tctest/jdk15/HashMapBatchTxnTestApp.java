/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.jdk15;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;
import com.tc.util.runtime.Vm;
import com.tctest.builtin.CyclicBarrier;
import com.tctest.builtin.HashMap;
import com.tctest.runner.AbstractTransparentApp;

import java.util.Map;

public class HashMapBatchTxnTestApp extends AbstractTransparentApp {
  int                             BATCHSIZE    = (Vm.isIBM()) ? 400 : 1000;
  int                             BATCHES      = 80;

  private final CyclicBarrier     barrier;
  private final Map<Integer, Map> hashmap_root = new HashMap<Integer, Map>();

  public HashMapBatchTxnTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
  }

  public void run() {
    try {
      testHashMapBatchTxn();
    } catch (Throwable t) {
      notifyError(t);
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {

    String testClass = HashMapBatchTxnTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    config.addIncludePattern(testClass + "$*", false, false, true);

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    spec.addRoot("barrier", "barrier");
    spec.addRoot("hashmap_root", "hashmap_root");
  }

  private void testHashMapBatchTxn() throws Exception {
    int index = barrier.await();

    for (int batch = 0; batch < BATCHES; batch += 2) {
      if (index == 0) {
        synchronized (hashmap_root) {
          System.out.println("XXX Batching(client=0) " + batch);
          int id = BATCHSIZE * batch;
          for (int i = 0; i < BATCHSIZE; ++i) {
            Map<Integer, Integer> submap = generateNewHashMap(id, 10 + (id % 10));
            hashmap_root.put(Integer.valueOf(id), submap);
            ++id;
          }
        }
      }
      if (index == 1) {
        synchronized (hashmap_root) {
          System.out.println("XXX Batching(client=1) " + (batch + 1));
          int id = BATCHSIZE * batch + BATCHSIZE;
          for (int i = 0; i < BATCHSIZE; ++i) {
            Map<Integer, Integer> submap = generateNewHashMap(id, 10 + (id % 10));
            hashmap_root.put(Integer.valueOf(id), submap);
            ++id;
          }
        }
      }
      ThreadUtil.reallySleep(20);
    }

    barrier.await();

    /* verification */
    System.out.println("XXX starting verification");
    for (int batch = 0; batch < BATCHES; ++batch) {
      System.out.println("XXX verifying batch " + batch);
      synchronized (hashmap_root) {
        for (int i = 0; i < BATCHSIZE; ++i) {
          Map<Integer, Integer> submap = hashmap_root.get(Integer.valueOf(batch * BATCHSIZE + i));
          Assert.assertTrue("Sub-HashMap(" + (batch * BATCHSIZE + i) + ") size is " + submap.size() + " but expect "
                            + (10 + (i % 10)), submap.size() == (10 + (i % 10)));
        }
      }
      ThreadUtil.reallySleep(20);
    }
    System.out.println("XXX verification done");

  }

  Map generateNewHashMap(int startIndex, int size) {
    Map<Integer, Integer> map = new HashMap<Integer, Integer>();

    for (int i = startIndex; i < (startIndex + size); ++i) {
      map.put(Integer.valueOf(i), Integer.valueOf(i));
    }

    return (map);
  }

}
