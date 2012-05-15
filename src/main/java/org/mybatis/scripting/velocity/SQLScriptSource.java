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

import java.util.HashMap;
import java.util.Map;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.session.Configuration;

public class SQLScriptSource implements SqlSource {

  protected static final String PARAMETER_OBJECT_KEY = "_parameter";
  protected static final String DATABASE_ID_KEY = "_databaseId";
  protected static final String MAPPING_COLLECTOR_KEY = "_pmc";

  private static int templateIndex = 0;

  private final ParameterMapping[] parameterMappingSources;
  private final Object compiledScript;
  private final Configuration configuration;

  public SQLScriptSource(Configuration configuration, String script, Class<?> parameterTypeClass) {
    this.configuration = configuration;
    ParameterMappingSourceParser mappingParser = new ParameterMappingSourceParser(configuration, script, parameterTypeClass);
    parameterMappingSources = mappingParser.getParameterMappingSources();
    script = mappingParser.getSql();
    compiledScript = VelocityFacade.compile(script, "velocity-template-" + (++templateIndex));
  }

  @Override
  public BoundSql getBoundSql(Object parameterObject) {

    final Map<String, Object> context = new HashMap<String, Object>();
    final ParameterMappingCollector pmc = new ParameterMappingCollector(parameterMappingSources, context, configuration);

    context.put(DATABASE_ID_KEY, configuration.getDatabaseId());
    context.put(PARAMETER_OBJECT_KEY, parameterObject);
    context.put(MAPPING_COLLECTOR_KEY, pmc);

    final String sql = VelocityFacade.apply(compiledScript, context);
    BoundSql boundSql = new BoundSql(configuration, sql, pmc.getParameterMappings(), parameterObject);
    for (Map.Entry<String, Object> entry : context.entrySet()) {
      boundSql.setAdditionalParameter(entry.getKey(), entry.getValue());
    }

    return boundSql;

  }

}
