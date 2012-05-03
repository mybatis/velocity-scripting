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

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;
import java.util.Properties;
import org.apache.ibatis.builder.BuilderException;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeInstance;
import org.apache.velocity.runtime.parser.node.SimpleNode;

public class VelocityFacade {

  private static final RuntimeInstance engine;

  static {
    final Properties settings = new Properties();
    settings.setProperty("userdirective",
        TrimDirective.class.getName()  + "," +
        WhereDirective.class.getName() + "," +
        SetDirective.class.getName()   + "," +
        RepeatDirective.class.getName());
    engine = new RuntimeInstance();
    engine.init(settings);
  }

  public static Object compile(String script, String name) {
    try {
      StringReader reader = new StringReader(script);
      SimpleNode node = engine.parse(reader, name);
      Template template = new Template();
      template.setRuntimeServices(engine);
      template.setData(node);
      template.initDocument();
      return template;
    } catch (Exception ex) {
      throw new BuilderException("Error parsing velocity script '" + name + "'", ex);
    }
  }

  public static String apply(Object template, Map<String, Object> context) {
    final StringWriter out = new StringWriter();
    ((Template)template).merge(new VelocityContext(context), out);
    return out.toString();
  }

}
