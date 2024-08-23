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
package com.tc.object;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/**
 * @author steve
 */
public class WeakObjectReference extends WeakReference {
  private final ObjectID id;

  public WeakObjectReference(ObjectID id, Object referent, ReferenceQueue q) {
    super(referent, q);
    if (referent == null) { throw new AssertionError("null reference"); }
    this.id = id;
  }

  public ObjectID getObjectID() {
    return id;
  }

  public String getObjectType() {
    return null;
  }
}