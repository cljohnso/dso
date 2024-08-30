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
package com.terracotta.toolkit.object.serialization;

public class UserSuppliedClassLoader extends ClassLoader {

  private final ClassLoader userLoader;
  private final ClassLoader toolkitLoader;

  public UserSuppliedClassLoader(ClassLoader userLoader, ClassLoader toolkitLoader) {
    this.userLoader = userLoader;
    this.toolkitLoader = toolkitLoader;
  }

  @Override
  public Class<?> loadClass(String name) throws ClassNotFoundException {
    try {
      return userLoader.loadClass(name);
    } catch (ClassNotFoundException cnfe) {
      //
    }

    return toolkitLoader.loadClass(name);
  }

}
