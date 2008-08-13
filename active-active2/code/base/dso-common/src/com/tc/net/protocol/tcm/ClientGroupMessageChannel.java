/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.net.groups.NodeID;

public interface ClientGroupMessageChannel extends ClientMessageChannel {
  
  public ClientMessageChannel[] getChannels();
  
  public NodeID[] getServerGroupIDs();
  
  public ClientMessageChannel getChannel(NodeID id);
  
  public void broadcast(final TCMessageImpl message);
  
}
