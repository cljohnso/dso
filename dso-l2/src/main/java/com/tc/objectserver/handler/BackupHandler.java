/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.async.api.EventHandlerException;

import java.util.concurrent.Callable;

/**
 * @author tim
 */
public class BackupHandler extends AbstractEventHandler {
  @Override
  public void handleEvent(final EventContext context) throws EventHandlerException {
    if (context instanceof Callable) {
      try {
        ((Callable<Void>)context).call();
      } catch (Exception e) {
        throw new EventHandlerException(e);
      }
    } else {
      throw new EventHandlerException("Unknown event context type " + context.getClass());
    }
  }
}
