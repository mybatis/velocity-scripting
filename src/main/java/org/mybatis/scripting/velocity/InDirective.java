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

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.exception.TemplateInitException;
import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.directive.StopCommand;
import org.apache.velocity.runtime.parser.ParserTreeConstants;
import org.apache.velocity.runtime.parser.node.ASTReference;
import org.apache.velocity.runtime.parser.node.ASTStringLiteral;
import org.apache.velocity.runtime.parser.node.Node;
import org.apache.velocity.util.introspection.Info;

/**
 * #in($collection $item COLUMN).
 * 
 */
public class InDirective extends RepeatDirective {

	/** Immutable fields */
	private String var;
	private String open = "(";
	private String close = ")";
	private String separator = ", ";
	private String column = "";

	@Override
	public String getName() {
		return "in";
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
	            this.column = value;
	            break;
	        }
	      } else {
	        throw new TemplateInitException("Syntax error", getTemplateName(), getLine(), getColumn());
	      }
	    }
	    uberInfo = new Info(this.getTemplateName(), getLine(), getColumn());
	  }
	
	@Override
	public boolean render(InternalContextAdapter context, Writer writer, Node node) throws IOException, ResourceNotFoundException,
			ParseErrorException, MethodInvocationException {
	    Object listObject = node.jjtGetChild(0).value(context);
	    if (listObject == null) {
	      return false;
	    }
		
	    Iterator<?> iterator = null;

	    try {
	      iterator = this.rsvc.getUberspect().getIterator(listObject, uberInfo);
	    } catch (RuntimeException e) {
	      throw e;
	    } catch (Exception ee) {
	      String msg = "Error getting iterator for #in at " + uberInfo;
	      rsvc.getLog().error(msg, ee);
	      throw new VelocityException(msg, ee);
	    }

	    if (iterator == null) {
	      throw new VelocityException("Invalid collection");
	    }
        
	    int counter = 0;
	    Object o = context.get(this.var);

	    ParameterMappingCollector collector = (ParameterMappingCollector) context.get(SQLScriptSource.MAPPING_COLLECTOR_KEY);
	    String savedItemKey = collector.getItemKey();
	    collector.setItemKey(this.var);
	    RepeatScope foreach = new RepeatScope(this, context.get(getName()), var);
	    context.put(getName(), foreach);

	    NullHolderContext nullHolderContext = null;
	    Object value = null;
  	    writer.append(this.open);
 		while (iterator.hasNext()) {
			
		  if(counter % MAX_IN_CLAUSE_SIZE == 0) {
			  writer.append(this.open); // Group begins
			  writer.append(this.column);
			  writer.append(" IN ");
			  writer.append(this.open); // In starts
		  }
		  
	      value = iterator.next();
	      put(context, this.var, value);
	      foreach.index++;
	      foreach.hasNext = iterator.hasNext();

	      try {
	        if (value == null) {
	          if (nullHolderContext == null) {
	            nullHolderContext = new NullHolderContext(this.var, context);
	          }
	          node.jjtGetChild(node.jjtGetNumChildren() - 1).render(nullHolderContext, writer);
	        } else {
	          node.jjtGetChild(node.jjtGetNumChildren() - 1).render(context, writer);
	        }
	      } catch (StopCommand stop) {
	        if (stop.isFor(this)) {
	          break;
	        } else {
	          clean(context, o, collector, savedItemKey);
	          throw stop;
	        }
	      }
	      counter++;

	      
		  if((counter > 0 && counter % MAX_IN_CLAUSE_SIZE == 0) || !iterator.hasNext()) {
			  writer.append(this.close); // In ends
			  writer.append(this.close); // Group ends
			  if(iterator.hasNext()) {
				  writer.append(" OR ");
			  } 
		  } else if(iterator.hasNext()) {
			  writer.append(this.separator);
		  }

		}

		writer.append(this.close);
	    clean(context, o, collector, savedItemKey);
		return true;
	}

	@Override
	public int getType() {
		return BLOCK;
	}

}
