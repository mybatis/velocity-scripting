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

import org.apache.ibatis.builder.IncompleteElementException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.parsing.GenericTokenParser;
import org.apache.ibatis.parsing.TokenHandler;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.scripting.ScriptingException;
import org.apache.ibatis.session.Configuration;

public class IncludeHandlerParser implements TokenHandler {

  private final Configuration configuration;
  private final MapperBuilderAssistant builderAssistant;

  public IncludeHandlerParser(Configuration configuration, MapperBuilderAssistant builderAssistant) {
    this.configuration = configuration;
    this.builderAssistant = builderAssistant;
  }

  public String parse(String content) {
    GenericTokenParser parser = new GenericTokenParser("@include", "}", this);
    return parser.parse(content);
  }

  @Override
  public String handleToken(String content) {
    if (content == null) {
      throw new ScriptingException("Unresolved @include{} reference");
    }
    int start = content.indexOf("{");
    if (start == -1) {
      throw new ScriptingException("Syntax error at @include{} tag. Expected '{'");
    }
    if (start == content.length() - 1) {
      throw new ScriptingException("Unresolved @include{} reference");
    }
    String refid = content.substring(start+1).trim();
    refid = builderAssistant.applyCurrentNamespace(refid, true);
    try {
      XNode includeNode = configuration.getSqlFragments().get(refid);
      if (includeNode == null) {
        String nsrefid = builderAssistant.applyCurrentNamespace(refid, true);
        includeNode = configuration.getSqlFragments().get(nsrefid);
        if (includeNode == null) {
          throw new IncompleteElementException("Could not find SQL statement to include with refid '" + refid + "'");
        }
      }
      return parse(includeNode.getStringBody(""));
    } catch (IllegalArgumentException e) {
      throw new IncompleteElementException("Could not find SQL statement to include with refid '" + refid + "'", e);
    }
  }
}
