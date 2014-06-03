/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management;

import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.management.ManagementRequestID;
import com.tc.object.management.RemoteCallDescriptor;
import com.tc.object.management.RemoteCallHolder;
import com.tc.object.management.ResponseHolder;
import com.tc.object.management.TCManagementSerializationException;
import com.tc.object.msg.AbstractManagementMessage;
import com.tc.object.msg.InvokeRegisteredServiceMessage;
import com.tc.object.msg.InvokeRegisteredServiceResponseMessage;
import com.tc.object.msg.ListRegisteredServicesMessage;
import com.tc.object.msg.ListRegisteredServicesResponseMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.NoSuchChannelException;
import com.tc.objectserver.handler.ServerManagementHandler;

import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 */
public class RemoteManagement {

  private final DSOChannelManager channelManager;
  private final ServerManagementHandler serverManagementHandler;

  public RemoteManagement(DSOChannelManager channelManager, ServerManagementHandler serverManagementHandler) {
    this.channelManager = channelManager;
    this.serverManagementHandler = serverManagementHandler;

/*
    Timer t  = new Timer("test-timer", true);
    t.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        try {
          checkNewManagementApi();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }, 15000, 5000);

    registerEventListener(new ServerManagementHandler.EventListener() {
      @Override
      public ClassLoader getClassLoader() {
        return this.getClass().getClassLoader();
      }

      @Override
      public void onEvent(Serializable event) {
        System.out.println("EVENT: " + event);
      }
    });
*/
  }

  public void registerEventListener(ManagementEventListener listener) {
    serverManagementHandler.registerEventListener(listener);
  }

  public void unregisterEventListener(ManagementEventListener listener) {
    serverManagementHandler.unregisterEventListener(listener);
  }

  public Set<NodeID> getAllClientIDs() {
    return channelManager.getAllClientIDs();
  }

  public Set<RemoteCallDescriptor> listRegisteredServices(NodeID node, long timeout, TimeUnit unit) throws RemoteManagementException {
    ListRegisteredServicesMessage message;
    try {
      message = (ListRegisteredServicesMessage)channelManager.getActiveChannel(node)
          .createMessage(TCMessageType.LIST_REGISTERED_SERVICES_MESSAGE);
    } catch (NoSuchChannelException nsce) {
      throw new RemoteManagementException("Error listing registered management services", nsce);
    }

    final AtomicReference<Set<RemoteCallDescriptor>> remoteCallDescriptors = new AtomicReference<Set<RemoteCallDescriptor>>();

    final CountDownLatch latch = new CountDownLatch(1);
    serverManagementHandler.registerResponseListener(message.getManagementRequestID(), new ManagementResponseListener() {
      @Override
      public void onResponse(AbstractManagementMessage mgmtMessage) {
        ListRegisteredServicesResponseMessage response = (ListRegisteredServicesResponseMessage)mgmtMessage;

        remoteCallDescriptors.set(response.getRemoteCallDescriptors());

        serverManagementHandler.unregisterResponseListener(mgmtMessage.getManagementRequestID());
        latch.countDown();
      }
    });
    message.send();

    try {
      if (!latch.await(timeout, unit)) {
        serverManagementHandler.unregisterResponseListener(message.getManagementRequestID());
        throw new RemoteManagementException("Timed out while waiting for listRegisteredServices() response");
      }
    } catch (InterruptedException ie) {
      serverManagementHandler.unregisterResponseListener(message.getManagementRequestID());
      throw new RemoteManagementException("Interrupted while waiting for listRegisteredServices() response", ie);
    }

    return remoteCallDescriptors.get();
  }


  public Future<Object> asyncRemoteCall(final RemoteCallDescriptor remoteCallDescriptor, final ClassLoader classLoader, Object... args) throws RemoteManagementException {
    InvokeRegisteredServiceMessage invokeMessage;
    try {
      invokeMessage = (InvokeRegisteredServiceMessage)channelManager.getActiveChannel(remoteCallDescriptor.getL1Node())
          .createMessage(TCMessageType.INVOKE_REGISTERED_SERVICE_MESSAGE);
    } catch (NoSuchChannelException nsce) {
      throw new RemoteManagementException("Error calling management services", nsce);
    }

    final AtomicReference<Exception> exception = new AtomicReference<Exception>();
    final AtomicReference<Object> response = new AtomicReference<Object>();

    RemoteCallHolder remoteCallHolder = new RemoteCallHolder(remoteCallDescriptor, args);
    invokeMessage.setRemoteCallHolder(remoteCallHolder);

    final AtomicReference<ManagementRequestID> responseManagementRequestIDRef = new AtomicReference<ManagementRequestID>();
    final CountDownLatch latch = new CountDownLatch(1);
    serverManagementHandler.registerResponseListener(invokeMessage.getManagementRequestID(), new ManagementResponseListener() {
      @Override
      public void onResponse(AbstractManagementMessage message) {
        InvokeRegisteredServiceResponseMessage responseMessage = (InvokeRegisteredServiceResponseMessage)message;
        ResponseHolder responseHolder = responseMessage.getResponseHolder();
        try {
          exception.set(responseHolder.getException(classLoader));
          response.set(responseHolder.getResponse(classLoader));
        } catch (ClassNotFoundException cnfe) {
          exception.set(new TCManagementSerializationException("Error deserializing management response", cnfe));
          response.set(null);
        }
        ManagementRequestID managementRequestID = message.getManagementRequestID();
        responseManagementRequestIDRef.set(managementRequestID);
        serverManagementHandler.unregisterResponseListener(managementRequestID);
        latch.countDown();
      }
    });
    final ManagementRequestID sentManagementRequestID = invokeMessage.getManagementRequestID();
    invokeMessage.send();

    return new Future<Object>() {
      private volatile boolean cancelled = false;

      @Override
      public boolean cancel(boolean mayInterruptIfRunning) {
        if (cancelled) {
          return true;
        } else if (!isDone()) {
          cancelled = true;
          serverManagementHandler.unregisterResponseListener(sentManagementRequestID);
          latch.countDown(); // make it done
          return true;
        } else {
          return false;
        }
      }

      @Override
      public boolean isCancelled() {
        return cancelled;
      }

      @Override
      public boolean isDone() {
        return latch.getCount() == 0;
      }

      @Override
      public Object get() throws InterruptedException, ExecutionException {
        latch.await();
        if (isCancelled()) {
          throw new CancellationException("Management remote L1 call on " + remoteCallDescriptor.getL1Node() + " got cancelled");
        }
        if (exception.get() != null) {
          throw new ExecutionException(new RemoteManagementException("Error performing management remote L1 call on " + remoteCallDescriptor.getL1Node(), exception.get()));
        }
        return response.get();
      }

      @Override
      public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (!latch.await(timeout, unit)) {
          throw new TimeoutException("Timed out waiting for management remote L1 call response");
        }
        if (isCancelled()) {
          throw new CancellationException("Management remote L1 call on " + remoteCallDescriptor.getL1Node() + " got cancelled");
        }
        if (exception.get() != null) {
          throw new ExecutionException(new RemoteManagementException("Error performing management remote L1 call on " + remoteCallDescriptor.getL1Node(), exception.get()));
        }
        return response.get();
      }
    };
  }

/*

  Object syncRemoteCall(RemoteCallDescriptor remoteCallDescriptor, final ClassLoader classLoader, Object... args) throws Exception {
    InvokeRegisteredServiceMessage invokeMessage = (InvokeRegisteredServiceMessage)channelManager.getActiveChannel(remoteCallDescriptor.getL1Node())
        .createMessage(TCMessageType.INVOKE_REGISTERED_SERVICE_MESSAGE);

    final AtomicReference<Exception> exception = new AtomicReference<Exception>();
    final AtomicReference<Object> response = new AtomicReference<Object>();

    RemoteCallHolder remoteCallHolder = new RemoteCallHolder(remoteCallDescriptor, args);
    invokeMessage.setRemoteCallHolder(remoteCallHolder);

    final CountDownLatch latch = new CountDownLatch(1);
    serverManagementHandler.registerResponseListener(invokeMessage.getManagementRequestID(), new ServerManagementHandler.ResponseListener() {
      @Override
      public void onResponse(AbstractManagementMessage message) {
        InvokeRegisteredServiceResponseMessage responseMessage = (InvokeRegisteredServiceResponseMessage)message;
        ResponseHolder responseHolder = responseMessage.getResponseHolder();
        try {
          exception.set(responseHolder.getException(classLoader));
          response.set(responseHolder.getResponse(classLoader));
        } catch (ClassNotFoundException cnfe) {
          exception.set(new TCManagementSerializationException("Error deserializing management response", cnfe));
          response.set(null);
        }
        serverManagementHandler.unregisterResponseListener(message.getManagementRequestID());
        latch.countDown();
      }
    });
    invokeMessage.send();
    latch.await();

    if (exception.get() != null) {
      throw new RemoteManagementException("Error performing remote L1 call on " + remoteCallDescriptor.getL1Node(), exception.get());
    }
    return response.get();
  }


  void checkNewManagementApi() throws Exception {
    printAllRegisteredServiceMethods();

    printInvocation("sample.HelloService", "sayHello", "world");
    printInvocation("sample.HelloService", "sayHello", "world");
    printInvocation("sample.HelloService", "sayHello", "world");
    printInvocation("sample.HelloService", "sayHello", "world");

    printInvocations(4, "sample.HelloService", "sayHello", "world");
  }

  void printInvocations(int count, String serviceClassName, String methodName, Object... args) throws Exception {
    Set allClientIDs = channelManager.getAllClientIDs();

    List<Future<Object>> futures = new ArrayList<Future<Object>>();

    for (Object clientID : allClientIDs) {
      final NodeID node = (NodeID)clientID;

      Set<RemoteCallDescriptor> descriptors = listRegisteredServices(node);
      for (RemoteCallDescriptor remoteCallDescriptor : descriptors) {
        String className = remoteCallDescriptor.getServiceID().getClassName();
        if (!className.equals(serviceClassName)) { continue; }
        if (!remoteCallDescriptor.getMethodName().equals(methodName)) { continue; }

        for (int i=0;i<count;i++) {
          System.out.println("INVOKING " + remoteCallDescriptor.getMethodName() + " on " + remoteCallDescriptor.getL1Node());
          Future<Object> f = asyncRemoteCall(remoteCallDescriptor, getClass().getClassLoader(), args);
          futures.add(f);
        }
      }
    }

    for (Future<Object> future : futures) {
      Object rc = future.get();
      System.out.println("INVOCATION RESPONSE: [" + rc + "]");
    }

  }

  void printInvocation(String serviceClassName, String methodName, Object... args) throws Exception {
    Set allClientIDs = channelManager.getAllClientIDs();

    for (Object clientID : allClientIDs) {
      final NodeID node = (NodeID)clientID;

      Set<RemoteCallDescriptor> descriptors = listRegisteredServices(node);
      for (RemoteCallDescriptor remoteCallDescriptor : descriptors) {
        String className = remoteCallDescriptor.getServiceID().getClassName();
        if (!className.equals(serviceClassName)) { continue; }
        if (!remoteCallDescriptor.getMethodName().equals(methodName)) { continue; }

        System.out.println("INVOKING " + remoteCallDescriptor.getMethodName() + " on " + remoteCallDescriptor.getL1Node());
        Object rc = syncRemoteCall(remoteCallDescriptor, getClass().getClassLoader(), args);
        System.out.println("INVOCATION RESPONSE: [" + rc + "]");
      }
    }

  }

  void printAllRegisteredServiceMethods() throws Exception {
    Set allClientIDs = channelManager.getAllClientIDs();

    for (Object clientID : allClientIDs) {
      final NodeID node = (NodeID)clientID;

      Set<RemoteCallDescriptor> descriptors = listRegisteredServices(node);
      for (RemoteCallDescriptor descriptor : descriptors) {
        System.out.println("DISCOVERED " + descriptor);
      }
    }
  }
*/

}