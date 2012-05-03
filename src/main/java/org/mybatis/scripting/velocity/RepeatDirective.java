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

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import org.apache.velocity.context.ChainedInternalContextAdapter;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.exception.TemplateInitException;
import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.directive.Scope;
import org.apache.velocity.runtime.directive.StopCommand;
import org.apache.velocity.runtime.parser.ParserTreeConstants;
import org.apache.velocity.runtime.parser.node.ASTReference;
import org.apache.velocity.runtime.parser.node.ASTStringLiteral;
import org.apache.velocity.runtime.parser.node.Node;
import org.apache.velocity.util.introspection.Info;

/**
 * #repeat($collection $item SEP OPEN CLOSE).
 */
public class RepeatDirective extends Directive {

  /** Immutable fields */
  private String var;
  private String open = "";
  private String close = "";
  private String separator = "";
  protected Info uberInfo;

  @Override
  public String getName() {
    return "repeat";
  }

  @Override
  public void init(RuntimeServices rs, InternalContextAdapter context, Node node) throws TemplateInitException {
    super.init(rs, context, node);
    final int n = node.jjtGetNumChildren() - 1;
    for (int i = 1; i < n; i++) {
      Node child = node.jjtGetChild(i);
      if (i == 1) {
        if (child.getType() == ParserTreeConstants.JJTREFERENCE) {
          var = ((ASTReference) child).getRootString();
        } else {
          throw new TemplateInitException("Syntax error", getTemplateName(), getLine(), getColumn());
        }
      } else if (child.getType() == ParserTreeConstants.JJTSTRINGLITERAL) {
        String value = (String) ((ASTStringLiteral)child).value(context);
        switch (i) {
          case 2:
            separator = value;
            break;
          case 3:
            open = value;
            break;
          case 4:
            close = value;
            break;
        }
      } else {
        throw new TemplateInitException("Syntax error", getTemplateName(), getLine(), getColumn());
      }
    }
    uberInfo = new Info(this.getTemplateName(), getLine(), getColumn());
  }

  @Override
  public boolean render(InternalContextAdapter context, Writer writer, Node node)
      throws IOException, ResourceNotFoundException, ParseErrorException, MethodInvocationException {

    writer.append(open);

    Object listObject = node.jjtGetChild(0).value(context);

    if (listObject == null) {
      return false;
    }

    Iterator i = null;

    try {
      i = rsvc.getUberspect().getIterator(listObject, uberInfo);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception ee) {
      String msg = "Error getting iterator for #repeat at " + uberInfo;
      rsvc.getLog().error(msg, ee);
      throw new VelocityException(msg, ee);
    }

    if (i == null) {
      throw new VelocityException("Invalid collection");
    }

    int counter = 0;
    boolean maxNbrLoopsExceeded = false;
    Object o = context.get(var);

    ParameterMappingCollector collector = (ParameterMappingCollector) context.get(SQLScriptSource.MAPPING_COLLECTOR_KEY);
    String savedItemKey = collector.getItemKey();
    collector.setItemKey(var);
    RepeatScope foreach = new RepeatScope(this, context.get(getName()), var);
    context.put(getName(), foreach);

    NullHolderContext nullHolderContext = null;

    while (!maxNbrLoopsExceeded && i.hasNext()) {
      Object value = i.next();
      put(context, var, value);
      foreach.index++;
      foreach.hasNext = i.hasNext();

      try {
        if (value == null) {
          if (nullHolderContext == null) {
            nullHolderContext = new NullHolderContext(var, context);
          }
          node.jjtGetChild(node.jjtGetNumChildren()-1).render(nullHolderContext, writer);
        } else {
          node.jjtGetChild(node.jjtGetNumChildren()-1).render(context, writer);
        }
      } catch (StopCommand stop) {
        if (stop.isFor(this)) {
          break;
        } else {
          clean(context, o, collector, savedItemKey);
          throw stop;
        }
      }

      if (i.hasNext()) {
        writer.append(separator);
      }

      counter++;
      maxNbrLoopsExceeded = counter >= 1000;
    }
    writer.append(close);
    clean(context, o, collector, savedItemKey);
    return true;

  }

  protected void clean(InternalContextAdapter context,
      Object o, ParameterMappingCollector collector, String savedItemKey) {
    if (o != null) {
      context.put(var, o);
    } else {
      context.remove(var);
    }
    collector.setItemKey(savedItemKey);
    postRender(context);
  }

  protected void put(InternalContextAdapter context, String key, Object value) {
    context.put(key, value);
  }

  @Override
  public int getType() {
    return BLOCK;
  }

  public static class RepeatScope extends Scope {

    protected int index = -1;
    protected boolean hasNext = false;
    protected final String var;

    public RepeatScope(Object owner, Object replaces, String var) {
      super(owner, replaces);
      this.var = var;
    }

    public int getIndex() {
      return index;
    }

    public int getCount() {
      return index + 1;
    }

    public boolean hasNext() {
      return getHasNext();
    }

    public boolean getHasNext() {
      return hasNext;
    }

    public boolean isFirst() {
      return index < 1;
    }

    public boolean getFirst() {
      return isFirst();
    }

    public boolean isLast() {
      return !hasNext;
    }

    public boolean getLast() {
      return isLast();
    }

    public String getVar() {
      return var;
    }

  }

  protected static class NullHolderContext extends ChainedInternalContextAdapter {

    private String loopVariableKey = "";
    private boolean active = true;

    private NullHolderContext(String key, InternalContextAdapter context) {
      super(context);
      if (key != null) {
        loopVariableKey = key;
      }
    }

    @Override
    public Object get(String key) throws MethodInvocationException {
      return (active && loopVariableKey.equals(key))
          ? null
          : super.get(key);
    }

    @Override
    public Object put(String key, Object value) {
      if (loopVariableKey.equals(key) && (value == null)) {
        active = true;
      }

      return super.put(key, value);
    }

    @Override
    public Object localPut(final String key, final Object value) {
      return put(key, value);
    }

    @Override
    public Object remove(Object key) {
      if (loopVariableKey.equals(key)) {
        active = false;
      }
      return super.remove(key);
    }
  }

  @Override
  public boolean isScopeProvided() {
    return true;
  }

}
