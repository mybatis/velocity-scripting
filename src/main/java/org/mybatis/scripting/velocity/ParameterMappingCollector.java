/*
 *    Copyright 2012-2025 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.mybatis.scripting.velocity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.property.PropertyTokenizer;
import org.apache.ibatis.session.Configuration;

public class ParameterMappingCollector {

  private final ParameterMapping[] parameterMappingSources;
  private final List<ParameterMapping> parameterMappings = new ArrayList<>();
  private final Map<String, Object> context;
  private final Configuration configuration;
  private final MetaObject metaParameters;

  private String itemKey;

  public ParameterMappingCollector(ParameterMapping[] newParameterMappingSources, Map<String, Object> newContext,
      Configuration newConfiguration) {
    this.parameterMappingSources = newParameterMappingSources;
    this.context = newContext;
    this.configuration = newConfiguration;
    this.metaParameters = configuration.newMetaObject(newContext);
  }

  public void setItemKey(String value) {
    this.itemKey = value;
  }

  public String getItemKey() {
    return this.itemKey;
  }

  public String g(int mapping) {
    ParameterMapping parameterMapping = this.parameterMappingSources[mapping];
    this.parameterMappings.add(mappingWithValue(parameterMapping));
    return "?";
  }

  public List<ParameterMapping> getParameterMappings() {
    return this.parameterMappings;
  }

  private ParameterMapping mappingWithValue(ParameterMapping source) {
    String property = source.getProperty();
    ParameterMapping.Builder builder = new ParameterMapping.Builder(this.configuration, property, source.getJavaType());
    builder.expression(source.getExpression()).jdbcType(source.getJdbcType()).jdbcTypeName(source.getJdbcTypeName())
        .mode(source.getMode()).numericScale(source.getNumericScale()).resultMapId(source.getResultMapId())
        .typeHandler(source.getTypeHandler());

    PropertyTokenizer propertyTokenizer = new PropertyTokenizer(property);
    Object parameterObject = context.get(SQLScriptSource.PARAMETER_OBJECT_KEY);
    if (!ParameterMode.OUT.equals(source.getMode())) {
      if (metaParameters.hasGetter(propertyTokenizer.getName())) {
        builder.value(metaParameters.getValue(property));
      } else if (parameterObject == null) {
        builder.value(null);
      } else if (configuration.getTypeHandlerRegistry().hasTypeHandler(parameterObject.getClass())) {
        builder.value(parameterObject);
      } else {
        MetaObject metaObject = configuration.newMetaObject(parameterObject);
        builder.value(metaObject.getValue(property));
      }
    }
    return builder.build();
  }

}
