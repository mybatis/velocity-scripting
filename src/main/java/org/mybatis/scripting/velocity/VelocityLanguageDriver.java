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

import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.session.Configuration;

/**
 * The {@link LanguageDriver} using Velocity.
 * <p>
 * This class rename from {@code Driver}.
 * </p>
 *
 * @since 2.1.0
 * @author Kazuki Shimizu
 */
public class VelocityLanguageDriver implements LanguageDriver {

  /**
   * Default constructor.
   */
  public VelocityLanguageDriver() {
    this(VelocityLanguageDriverConfig.newInstance());
  }

  /**
   * Constructor.
   *
   * @param driverConfig
   *          a language driver configuration
   */
  public VelocityLanguageDriver(VelocityLanguageDriverConfig driverConfig) {
    VelocityFacade.initialize(driverConfig);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ParameterHandler createParameterHandler(MappedStatement mappedStatement, Object parameterObject,
      BoundSql boundSql) {
    return new DefaultParameterHandler(mappedStatement, parameterObject, boundSql);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SqlSource createSqlSource(Configuration configuration, XNode script, Class<?> parameterTypeClass) {
    return new SQLScriptSource(configuration, script.getNode().getTextContent(),
        parameterTypeClass == null ? Object.class : parameterTypeClass);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SqlSource createSqlSource(Configuration configuration, String script, Class<?> parameterTypeClass) {
    return new SQLScriptSource(configuration, script, parameterTypeClass == null ? Object.class : parameterTypeClass);
  }

}
