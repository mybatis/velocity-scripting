/*
 * Copyright 2012 MyBatis.org.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mybatis.scripting.velocity;

import java.io.Serializable;

/**
 * Paranoiac small and fast forward only list
 */
public final class FastLinkedList<E extends Serializable> implements Serializable {

  private Node first = null;

  private Node last = null;

  public FastLinkedList() {
    last = first;
  }

  public boolean isEmpty() {
    return first == null;
  }

  public Node start() {
    return first;
  }

  public void add(E e) {
    final Node n = new Node(e);
    if (first == null) {
      first = n;
      last = first;
    }
    else {
      last.next = n;
      last = n;
    }
  }

  public final class Node {

    final E data;
    Node next;

    public Node(E data) {
      this.data = data;
    }

    public boolean hasNext() {
      return next != null;
    }

  }

}
