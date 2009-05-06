/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.lockmanager.impl;

import com.tc.async.api.EventContext;
import com.tc.management.ClientLockStatManager;
import com.tc.management.L2LockStatsManager;
import com.tc.management.lock.stats.LockStatisticsMessage;
import com.tc.management.lock.stats.LockStatisticsResponseMessageImpl;
import com.tc.net.GroupID;
import com.tc.net.OrderedGroupIDs;
import com.tc.object.session.SessionProvider;
import com.tc.objectserver.api.TestSink;
import com.tc.objectserver.context.LockResponseContext;
import com.tc.objectserver.lockmanager.impl.LockManagerImpl;

public class ClientServerLockStatManagerGlue extends ClientServerLockManagerGlue {
  private ClientLockStatManager clientLockStatManager;
  private L2LockStatsManager    serverLockStatManager;

  public ClientServerLockStatManagerGlue(SessionProvider sessionProvider, TestSink sink) {
    super(sessionProvider, sink, "ClientServerLockStatManagerGlue");
  }

  public void set(ClientLockManagerImpl clmgr, LockManagerImpl slmgr, ClientLockStatManager clientLockStatManager,
                  L2LockStatsManager serverLockStatManager) {
    super.set(clmgr, slmgr);
    this.clientLockStatManager = clientLockStatManager;
    this.serverLockStatManager = serverLockStatManager;
  }

  public void run() {
    while (!stop) {
      EventContext ec = null;
      try {
        ec = sink.take();
      } catch (InterruptedException e) {
        //
      }
      if (ec instanceof LockResponseContext) {
        LockResponseContext lrc = (LockResponseContext) ec;
        if (lrc.isLockAward()) {
          clientLockManager.awardLock(lrc.getNodeID(), sessionProvider.getSessionID(lrc.getNodeID()), lrc.getLockID(),
                                      lrc.getThreadID(), lrc.getLockLevel());
        }
      } else if (ec instanceof LockStatisticsMessage) {
        LockStatisticsMessage lsm = (LockStatisticsMessage) ec;
        if (lsm.isLockStatsEnableDisable()) {
          clientLockStatManager.setLockStatisticsConfig(lsm.getTraceDepth(), lsm.getGatherInterval());
        } else if (lsm.isGatherLockStatistics()) {
          LockDistributionStrategy strategy = new LockDistributionStrategy(
                                                                           new OrderedGroupIDs(
                                                                                               new GroupID[] { GroupID.NULL_ID }));
          clientLockStatManager.requestLockSpecs(GroupID.NULL_ID, strategy);
        }
      } else if (ec instanceof LockStatisticsResponseMessageImpl) {
        LockStatisticsResponseMessageImpl lsrm = (LockStatisticsResponseMessageImpl) ec;
        serverLockStatManager.recordClientStat(lsrm.getSourceNodeID(), lsrm.getStackTraceElements());
      }
    }
  }

}
