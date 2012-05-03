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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.session.Configuration;

public class ParameterMappingCollector {

  private final ParameterMapping[] parameterMappingSources;
  private final List<ParameterMapping> parameterMappings = new ArrayList<ParameterMapping>();
  private final Map<String, Object> context;
  private final Configuration configuration;

  private int uid = 0;
  private String itemKey;

  public ParameterMappingCollector(ParameterMapping[] parameterMappingSources, Map<String, Object> context, Configuration configuration) {
    this.parameterMappingSources = parameterMappingSources;
    this.context = context;
    this.configuration = configuration;
  }

  public void setItemKey(String itemKey) {
    this.itemKey = itemKey;
  }

  public String getItemKey() {
    return itemKey;
  }

  public String g(int mapping) {
    ParameterMapping parameterMapping = parameterMappingSources[mapping];
    PropertyInfo vi = getPropertyInfo(parameterMapping.getProperty());
    if (vi.isIterable) {
      parameterMapping = itemize(parameterMapping, vi);
      context.put(vi.root, context.get(itemKey));
    }
    parameterMappings.add(parameterMapping);
    return "?";
  }

  public List<ParameterMapping> getParameterMappings() {
    return parameterMappings;
  }

  private ParameterMapping itemize(ParameterMapping source, PropertyInfo var) {
    StringBuilder sb = new StringBuilder().append("_RPTITEM_").append(uid++);
    var.root = sb.toString();
    String propertyName = sb.append(var.path).toString();
    ParameterMapping.Builder builder = new ParameterMapping.Builder(configuration, propertyName, source.getJavaType());
    builder
        .expression(source.getExpression())
        .jdbcType(source.getJdbcType())
        .jdbcTypeName(source.getJdbcTypeName())
        .mode(source.getMode())
        .numericScale(source.getNumericScale())
        .resultMapId(source.getResultMapId())
        .typeHandler(source.getTypeHandler());
    return builder.build();
  }

  private PropertyInfo getPropertyInfo(String name) {
    PropertyInfo i = new PropertyInfo();
    if (name != null) {
      int p = name.indexOf('.');
      if (p == -1) {
        i.root = name;
      } else {
        i.root = name.substring(0, p);
        i.path = name.substring(p);
      }
    }
    i.isIterable = itemKey != null && itemKey.equals(i.root);
    return i;
  }

  private static class PropertyInfo {
    boolean isIterable = false;
    String root = "";
    String path = "";
  }

}
