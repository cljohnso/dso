/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.object.ClientObjectManager;
import com.tc.object.LiteralValues;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.bytecode.Manageable;

public class LockIdFactory {

  private final ClientObjectManager objectManager;

  public LockIdFactory(ClientObjectManager objectManager) {
    this.objectManager = objectManager;
  }

  public LockID generateLockIdentifier(final Object obj) {
    if (obj instanceof LockID) {
      return (LockID) obj;
    } else if (obj instanceof Long) {
      return generateLockIdentifier(((Long) obj).longValue());
    } else if (obj instanceof String) {
      return generateLockIdentifier((String) obj);
    } else {
      final TCObject tco = lookupExistingOrNull(obj);
      if (tco != null) {
        if (tco.autoLockingDisabled()) {
          return UnclusteredLockID.UNCLUSTERED_LOCK_ID;
        } else {
          return new DsoLockID(tco.getObjectID());
        }
      } else if (isLiteralAutolock(obj)) {
        try {
          return new DsoLiteralLockID(obj);
        } catch (final IllegalArgumentException e) {
          return UnclusteredLockID.UNCLUSTERED_LOCK_ID;
        }
      } else {
        return UnclusteredLockID.UNCLUSTERED_LOCK_ID;
      }
    }
  }

  private TCObject lookupExistingOrNull(Object obj) {
    return objectManager.lookupExistingOrNull(obj);
  }

  public LockID generateLockIdentifier(final long l) {
    return new LongLockID(l);
  }

  public LockID generateLockIdentifier(final String str) {
    return new StringLockID(str);
  }

  public boolean isLiteralAutolock(final Object o) {
    if (o instanceof Manageable) { return false; }
    return (!(o instanceof Class)) && (!(o instanceof ObjectID)) && LiteralValues.isLiteralInstance(o);
  }
}
