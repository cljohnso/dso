/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.terracotta.toolkit;

import org.terracotta.toolkit.internal.feature.NonStopInternalFeature;

import com.terracotta.toolkit.feature.EnabledToolkitFeature;
import com.terracotta.toolkit.nonstop.NonStopContextImpl;

public class NonStopInternalFeatureImpl extends EnabledToolkitFeature implements NonStopInternalFeature {
  private final NonStopContextImpl nonStopContext;

  public NonStopInternalFeatureImpl(NonStopContextImpl nonStopContext) {
    this.nonStopContext = nonStopContext;
  }

  @Override
  public void enableForCurrentThread(boolean enable) {
    nonStopContext.enableForCurrentThread(enable);
  }

}
