/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.process;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class HeartBeatServer {
  public static final String PULSE               = "PULSE";
  public static final String KILL                = "KILL";
  public static final String IS_APP_SERVER_ALIVE = "IS_APP_SERVER_ALIVE";
  public static final String IM_ALIVE            = "IM_ALIVE";
  public static final int    PULSE_INTERVAL      = 15 * 1000;

  private static final List  heartBeatThreads    = Collections.synchronizedList(new ArrayList());

  private ListenThread       listenThread;

  public HeartBeatServer() {
    //
  }

  public synchronized void start() {
    listenThread = new ListenThread(this);
    listenThread.setDaemon(true);
    listenThread.start();
  }

  public synchronized void shutdown() {
    try {
      listenThread.shutdown();
      listenThread.join();
    } catch (InterruptedException ignored) {
      // nop
    }
    sendKillSignalToChildren();
  }

  public synchronized void sendKillSignalToChildren() {
    for (Iterator it = heartBeatThreads.iterator(); it.hasNext();) {
      HeartBeatThread hb = (HeartBeatThread) it.next();
      hb.sendKillSignal();
    }
    heartBeatThreads.clear();
  }

  public synchronized boolean anyAppServerAlive() {
    boolean alive = false;
    for (Iterator it = heartBeatThreads.iterator(); it.hasNext();) {
      HeartBeatThread hb = (HeartBeatThread) it.next();
      alive = alive || hb.pingAppServer();
    }
    return alive;
  }

  public synchronized int listeningPort() {
    if (!listenThread.isAlive()) throw new IllegalStateException("Heartbeat server has not started");
    return listenThread.listeningPort();
  }

  public void removeDeadClient(HeartBeatThread thread) {
    heartBeatThreads.remove(thread);
  }

  private static class ListenThread extends Thread {
    private ServerSocket    serverSocket;
    private int             listeningPort = -1;
    private boolean         isShutdown    = false;
    private HeartBeatServer server;

    public ListenThread(HeartBeatServer server) {
      this.server = server;
    }

    public void shutdown() {
      try {
        isShutdown = true;
        serverSocket.close();
      } catch (IOException ignored) {
        // nop
      }
    }

    public void run() {
      try {
        synchronized (this) {
          isShutdown = false;
          serverSocket = new ServerSocket(0);
          listeningPort = serverSocket.getLocalPort();
          this.notifyAll();
        }
        System.out.println("Heartbeat server is online...");
        Socket clientSocket;
        while ((clientSocket = serverSocket.accept()) != null) {
          System.out.println("Heartbeat server got new client...");
          HeartBeatThread hb = new HeartBeatThread(server, clientSocket);
          heartBeatThreads.add(hb);
          hb.start();
        }
      } catch (Exception e) {
        if (isShutdown) {
          System.out.println("Heartbeat server is shutdown");
        } else {
          throw new RuntimeException(e);
        }
      }
    }

    public int listeningPort() {
      synchronized (this) {
        while (listeningPort == -1) {
          try {
            this.wait(5000);
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
        }
      }
      return listeningPort;
    }
  }

  private static class HeartBeatThread extends Thread {
    private Socket          socket;
    private BufferedReader  in;
    private PrintWriter     out;
    private HeartBeatServer server;

    public HeartBeatThread(HeartBeatServer server, Socket s) {
      this.server = server;
      socket = s;
      try {
        socket.setSoTimeout(PULSE_INTERVAL);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    public void run() {
      try {
        while (true) {
          out.println(PULSE);
          Thread.sleep(PULSE_INTERVAL);
        }
      } catch (Exception e) {
        server.removeDeadClient(this);
      }
    }

    public void sendKillSignal() {
      try {
        out.println(KILL);
        socket.close();
      } catch (Exception e) {
        // ignored
      }
    }

    public boolean pingAppServer() {
      boolean alive = false;
      try {
        out.println(IS_APP_SERVER_ALIVE);
        String reply = in.readLine();
        if (reply != null && IM_ALIVE.equals(reply)) {
          alive = true;
        }
      } catch (Exception e) {
        // ignore - dead anyway
      }
      return alive;
    }

  }

}
