/**
 *    Copyright 2012-2015 the original author or authors.
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

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.ibatis.builder.BuilderException;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeInstance;
import org.apache.velocity.runtime.parser.node.SimpleNode;
import org.apache.velocity.util.StringUtils;

public class VelocityFacade {

  private static final String ADDITIONAL_CTX_ATTRIBUTES_KEY = "additional.context.attributes";
  private static final String EXTERNAL_PROPERTIES = "mybatis-velocity.properties";
  private static final String DIRECTIVES = TrimDirective.class.getName() 
						    + "," + WhereDirective.class.getName() 
						    + "," + SetDirective.class.getName() 
						    + "," + InDirective.class.getName()
						    + "," + RepeatDirective.class.getName();
  
  private static final RuntimeInstance engine;
  
  /** Contains thread safe objects to be set in the velocity context.*/
  private static final Map<String, Object> additionalCtxAttributes;
  private static final Properties settings;

  static {

	settings = loadPropeties();
    additionalCtxAttributes = Collections.unmodifiableMap(loadAdditionalCtxAttributes());
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
    context.putAll(additionalCtxAttributes);
    ((Template) template).merge(new VelocityContext(context), out);
    return out.toString();
  }

  private static Properties loadPropeties() {
    final Properties props = new Properties();
    // Defaults
    props.setProperty("resource.loader", "class");
    props.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
    
    
    try {
      // External properties
      ClassLoader cl = Thread.currentThread().getContextClassLoader();
      props.load(cl.getResourceAsStream(EXTERNAL_PROPERTIES));
    } catch (Exception ex) {
      // No custom properties
    }
    
    // Append the user defined directives if provided
    String userDirective = StringUtils.nullTrim(props.getProperty("userdirective"));
    if(userDirective == null) {
    	userDirective = DIRECTIVES;
    } else {
    	userDirective += "," + DIRECTIVES;
    }
    props.setProperty("userdirective", userDirective);
    return props;
  }

  private static Map<String, Object> loadAdditionalCtxAttributes() {
    Map<String, Object> attributes = new HashMap<String, Object>();
    String additionalContextAttributes = settings.getProperty(ADDITIONAL_CTX_ATTRIBUTES_KEY);
    if (additionalContextAttributes == null) {
      return attributes;
    }

    try {
      String[] entries = additionalContextAttributes.split(",");
      for (String str : entries) {
        String[] entry = str.trim().split(":");
        attributes.put(entry[0].trim(), Class.forName(entry[1].trim()).newInstance());
      }
    } catch (Exception ex) {
       throw new BuilderException("Error parsing velocity property '" + ADDITIONAL_CTX_ATTRIBUTES_KEY + "'", ex);
    }
    return attributes;
  }
}
