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

import static org.junit.Assert.assertEquals;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Properties;

import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.session.Configuration;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.junit.BeforeClass;
import org.junit.Test;

public class InDirectiveTest {

	static VelocityContext ctxt;
	static VelocityEngine velocity;

	@BeforeClass
	public static void setUpClass() throws Exception {
		Properties p = new Properties();
		p.setProperty("userdirective", InDirective.class.getName());
		velocity = new VelocityEngine();
		velocity.init(p);
		ctxt = new VelocityContext();
		ctxt.put(SQLScriptSource.MAPPING_COLLECTOR_KEY, 
				new ParameterMappingCollector(new ParameterMapping[]{}, new HashMap<String, Object>(), new Configuration()));
		StringWriter writer = new StringWriter();
		velocity.evaluate(ctxt, writer, "WARM", "1+1");
	}

	@Test
	public void ensureInClauseHasOne() throws Exception {
		StringWriter w = new StringWriter();
		ctxt.put("list", Collections.singletonList("?"));
		velocity.evaluate(ctxt, w, "TEST", "#in($list $id 'id')?#end");
		String result = w.toString();
		assertEquals(result.split("\\?").length -1, 1);
		assertEquals(result.split("IN").length -1, 1);
	}

	@Test
	public void ensureInClauseHasTwo() throws Exception {
		StringWriter w = new StringWriter();
		ctxt.put("list", Arrays.asList("?", "?"));
		velocity.evaluate(ctxt, w, "TEST", "#in($list $id 'id')?#end");
		String result = w.toString();
		assertEquals(result.split("\\?").length -1, 2);
	}

	@Test
	public void ensureInClauseHasOneThousand() throws Exception {
		StringWriter w = new StringWriter();
		String[] arr = new String[1000];
		Arrays.fill(arr, "?");
		ctxt.put("list", Arrays.asList(arr));
		velocity.evaluate(ctxt, w, "TEST", "#in($list $id 'id')?#end");
		String result = w.toString();
		assertEquals(result.split("\\?").length -1, 1000);
		assertEquals(result.split("OR").length -1, 0);
	}

	@Test
	public void ensureInClauseHasOneThousandAndOne() throws Exception {
		StringWriter w = new StringWriter();
		String[] arr = new String[1001];
		Arrays.fill(arr, "?");
		ctxt.put("list", Arrays.asList(arr));
		velocity.evaluate(ctxt, w, "TEST", "#in($list $id 'id')?#end");
		String result = w.toString();
		assertEquals(result.split("\\?").length -1, 1001);
		assertEquals(result.split("OR").length -1, 1);
	}

	@Test
	public void ensureInClauseHasTwoThousand() throws Exception {
		StringWriter w = new StringWriter();
		String[] arr = new String[2000];
		Arrays.fill(arr, "?");
		ctxt.put("list", Arrays.asList(arr));
		velocity.evaluate(ctxt, w, "TEST", "#in($list $id 'id')?#end");
		String result = w.toString();
		assertEquals(result.split("\\?").length -1, 2000);
		assertEquals(result.split("OR").length -1, 1);
	}

	@Test
	public void ensureInClauseHasTwoThousandAndOne() throws Exception {
		StringWriter w = new StringWriter();
		String[] arr = new String[2001];
		Arrays.fill(arr, "?");
		ctxt.put("list", Arrays.asList(arr));
		velocity.evaluate(ctxt, w, "TEST", "#in($list $id 'id')?#end");
		String result = w.toString();
		assertEquals(result.split("\\?").length -1, 2001);
		assertEquals(result.split("OR").length -1, 2);
	}

	@Test
	public void ensureInClauseHasThreeThousandAndOne() throws Exception {
		StringWriter w = new StringWriter();
		String[] arr = new String[3001];
		Arrays.fill(arr, "?");
		ctxt.put("list", Arrays.asList(arr));
		velocity.evaluate(ctxt, w, "TEST", "#in($list $id 'id')?#end");
		String result = w.toString();
		assertEquals(result.split("\\?").length -1, 3001);
		assertEquals(result.split("OR").length -1, 3);
	}

}
