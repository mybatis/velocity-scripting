/*
 *    Copyright 2012-2019 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.mybatis.scripting.velocity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Properties;

import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.session.Configuration;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class InDirectiveTest {

  private static VelocityContext ctxt;
  private static VelocityEngine velocity;

  @BeforeAll
  static void setUpClass() {
    Properties p = new Properties();
    p.setProperty(RuntimeConstants.CUSTOM_DIRECTIVES, InDirective.class.getName());
    velocity = new VelocityEngine();
    velocity.setProperty("runtime.log", "target/velocity.log");
    velocity.init(p);
    ctxt = new VelocityContext();
    ctxt.put(SQLScriptSource.MAPPING_COLLECTOR_KEY,
        new ParameterMappingCollector(new ParameterMapping[] {}, new HashMap<>(), new Configuration()));
    StringWriter writer = new StringWriter();
    velocity.evaluate(ctxt, writer, "WARM", "1+1");
  }

  @Test
  void ensureInClauseHasEmpty() {
    StringWriter w = new StringWriter();
    ctxt.put("list", Collections.emptyList());
    velocity.evaluate(ctxt, w, "TEST", "#in($list $id 'id')?#end");
    String result = w.toString();
    assertEquals("((id NOT IN ( NULL )))", result);
  }

  @Test
  void ensureInClauseHasOne() {
    StringWriter w = new StringWriter();
    ctxt.put("list", Collections.singletonList("?"));
    velocity.evaluate(ctxt, w, "TEST", "#in($list $id 'id')?#end");
    String result = w.toString();
    assertEquals(1, result.split("\\?").length - 1);
    assertEquals(1, result.split("IN").length - 1);
  }

  @Test
  void ensureInClauseHasTwo() {
    StringWriter w = new StringWriter();
    ctxt.put("list", Arrays.asList("?", "?"));
    velocity.evaluate(ctxt, w, "TEST", "#in($list $id 'id')?#end");
    String result = w.toString();
    assertEquals(2, result.split("\\?").length - 1);
  }

  @Test
  void ensureInClauseHasOneThousand() {
    StringWriter w = new StringWriter();
    String[] arr = new String[1000];
    Arrays.fill(arr, "?");
    ctxt.put("list", Arrays.asList(arr));
    velocity.evaluate(ctxt, w, "TEST", "#in($list $id 'id')?#end");
    String result = w.toString();
    assertEquals(1000, result.split("\\?").length - 1);
    assertEquals(0, result.split("OR").length - 1);
  }

  @Test
  void ensureInClauseHasOneThousandAndOne() {
    StringWriter w = new StringWriter();
    String[] arr = new String[1001];
    Arrays.fill(arr, "?");
    ctxt.put("list", Arrays.asList(arr));
    velocity.evaluate(ctxt, w, "TEST", "#in($list $id 'id')?#end");
    String result = w.toString();
    assertEquals(1001, result.split("\\?").length - 1);
    assertEquals(1, result.split("OR").length - 1);
  }

  @Test
  void ensureInClauseHasTwoThousand() {
    StringWriter w = new StringWriter();
    String[] arr = new String[2000];
    Arrays.fill(arr, "?");
    ctxt.put("list", Arrays.asList(arr));
    velocity.evaluate(ctxt, w, "TEST", "#in($list $id 'id')?#end");
    String result = w.toString();
    assertEquals(2000, result.split("\\?").length - 1);
    assertEquals(1, result.split("OR").length - 1);
  }

  @Test
  void ensureInClauseHasTwoThousandAndOne() {
    StringWriter w = new StringWriter();
    String[] arr = new String[2001];
    Arrays.fill(arr, "?");
    ctxt.put("list", Arrays.asList(arr));
    velocity.evaluate(ctxt, w, "TEST", "#in($list $id 'id')?#end");
    String result = w.toString();
    assertEquals(2001, result.split("\\?").length - 1);
    assertEquals(2, result.split("OR").length - 1);
  }

  @Test
  void ensureInClauseHasThreeThousandAndOne() {
    StringWriter w = new StringWriter();
    String[] arr = new String[3001];
    Arrays.fill(arr, "?");
    ctxt.put("list", Arrays.asList(arr));
    velocity.evaluate(ctxt, w, "TEST", "#in($list $id 'id')?#end");
    String result = w.toString();
    assertEquals(3001, result.split("\\?").length - 1);
    assertEquals(3, result.split("OR").length - 1);
  }

}
