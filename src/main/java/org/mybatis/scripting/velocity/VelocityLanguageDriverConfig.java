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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.text.WordUtils;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.factory.DefaultObjectFactory;
import org.apache.ibatis.reflection.property.PropertyTokenizer;
import org.apache.ibatis.reflection.wrapper.DefaultObjectWrapperFactory;
import org.apache.ibatis.scripting.ScriptingException;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.RuntimeInstance;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

/**
 * Configuration class for {@link Driver}.
 *
 * @author Kazuki Shimizu
 * @since 2.1.0
 */
public class VelocityLanguageDriverConfig {

  private static final String PROPERTY_KEY_CONFIG_FILE = "mybatis-velocity.config.file";
  private static final String PROPERTY_KEY_CONFIG_ENCODING = "mybatis-velocity.config.encoding";
  private static final String DEFAULT_PROPERTIES_FILE = "mybatis-velocity.properties";
  private static final String PROPERTY_KEY_ADDITIONAL_CONTEXT_ATTRIBUTE = "additional.context.attributes";
  private static final String[] BUILT_IN_DIRECTIVES = { TrimDirective.class.getName(), WhereDirective.class.getName(),
      SetDirective.class.getName(), InDirective.class.getName(), RepeatDirective.class.getName() };

  private static final Map<Class<?>, Function<String, Object>> TYPE_CONVERTERS;
  static {
    Map<Class<?>, Function<String, Object>> converters = new HashMap<>();
    converters.put(String.class, String::trim);
    converters.put(Charset.class, v -> Charset.forName(v.trim()));
    converters.put(String[].class, v -> Stream.of(v.split(",")).map(String::trim).toArray(String[]::new));
    converters.put(Object.class, v -> v);
    TYPE_CONVERTERS = Collections.unmodifiableMap(converters);
  }

  private static final Log log = LogFactory.getLog(VelocityLanguageDriverConfig.class);

  /**
   * The Velocity settings.
   */
  private final Map<String, String> velocitySettings = new HashMap<>();
  {
    velocitySettings.put(RuntimeConstants.RESOURCE_LOADERS, "class");
    velocitySettings.put(RuntimeConstants.RESOURCE_LOADER + ".class.class", ClasspathResourceLoader.class.getName());
  }

  /**
   * The base directory for reading template resources.
   */
  private String[] userDirectives = {};

  /**
   * The additional context attribute.
   */
  private final Map<String, String> additionalContextAttributes = new HashMap<>();

  /**
   * Get Velocity settings.
   *
   * @return Velocity settings
   */
  public Map<String, String> getVelocitySettings() {
    return velocitySettings;
  }

  /**
   * Get user define directives.
   *
   * @return user define directives.
   * @deprecated Recommend to use the 'velocity-settings.runtime.custom_directives' or 'runtime.custom_directives'
   *             because this method defined for keeping backward compatibility (There is possibility that this method
   *             removed at a future version)
   */
  @Deprecated
  public String[] getUserdirective() {
    return userDirectives;
  }

  /**
   * Set user define directives.
   *
   * @param userDirectives
   *          user define directives
   * @deprecated Recommend to use the 'velocity-settings.runtime.custom_directives' or 'runtime.custom_directives'
   *             because this method defined for keeping backward compatibility (There is possibility that this method
   *             removed at a future version)
   */
  @Deprecated
  public void setUserdirective(String... userDirectives) {
    log.warn(
        "The 'userdirective' has been deprecated since 2.1.0. Please use the 'velocity-settings.runtime.custom_directives' or 'runtime.custom_directives'.");
    this.userDirectives = userDirectives;
  }

  /**
   * Get additional context attributes.
   *
   * @return additional context attributes
   */
  public Map<String, String> getAdditionalContextAttributes() {
    return additionalContextAttributes;
  }

  /**
   * Generate a custom directives string.
   *
   * @return a custom directives string
   */
  public String generateCustomDirectivesString() {
    StringJoiner customDirectivesJoiner = new StringJoiner(",");
    Optional.ofNullable(velocitySettings.get(RuntimeConstants.CUSTOM_DIRECTIVES))
        .ifPresent(customDirectivesJoiner::add);
    Stream.of(userDirectives).forEach(customDirectivesJoiner::add);
    Stream.of(BUILT_IN_DIRECTIVES).forEach(customDirectivesJoiner::add);
    return customDirectivesJoiner.toString();
  }

  /**
   * Create an instance from default properties file. <br>
   * If you want to customize a default {@link RuntimeInstance}, you can configure some property using
   * mybatis-velocity.properties that encoded by UTF-8. Also, you can change the properties file that will read using
   * system property (-Dmybatis-velocity.config.file=... -Dmybatis-velocity.config.encoding=...). <br>
   * Supported properties are as follows:
   * <table border="1">
   * <caption>Supported properties</caption>
   * <tr>
   * <th>Property Key</th>
   * <th>Description</th>
   * <th>Default</th>
   * </tr>
   * <tr>
   * <th colspan="3">Directive configuration</th>
   * </tr>
   * <tr>
   * <td>userdirective</td>
   * <td>The user defined directives (Recommend to use the 'velocity-settings.runtime.custom_directives' property
   * because this property defined for keeping backward compatibility)</td>
   * <td>None(empty)</td>
   * </tr>
   * <tr>
   * <th colspan="3">Additional context attribute configuration</th>
   * </tr>
   * <tr>
   * <td>additional.context.attributes</td>
   * <td>The user defined additional context attribute values(Recommend to use the
   * 'additional-context-attributes.{name}' because this property defined for keeping backward compatibility)</td>
   * <td>None(empty)</td>
   * </tr>
   * <tr>
   * <td>additional-context-attributes.{name}</td>
   * <td>The user defined additional context attributes value(FQCN)</td>
   * <td>-</td>
   * </tr>
   * <tr>
   * <th colspan="3">Velocity settings configuration</th>
   * </tr>
   * <tr>
   * <td>velocity-settings.{name}</td>
   * <td>The settings of Velocity's {@link RuntimeInstance#setProperty(String, Object)}</td>
   * <td>-</td>
   * </tr>
   * <tr>
   * <td>{name}</td>
   * <td>The settings of Velocity's {@link RuntimeInstance#setProperty(String, Object)} (Recommend to use the
   * 'velocity-settings.{name}' because this property defined for keeping backward compatibility)</td>
   * <td>-</td>
   * </tr>
   * </table>
   *
   * @return a configuration instance
   */
  public static VelocityLanguageDriverConfig newInstance() {
    return newInstance(loadDefaultProperties());
  }

  /**
   * Create an instance from specified properties.
   *
   * @param customProperties
   *          custom configuration properties
   * @return a configuration instance
   * @see #newInstance()
   */
  public static VelocityLanguageDriverConfig newInstance(Properties customProperties) {
    VelocityLanguageDriverConfig config = new VelocityLanguageDriverConfig();
    Properties properties = loadDefaultProperties();
    Optional.ofNullable(customProperties).ifPresent(properties::putAll);
    override(config, properties);
    configureVelocitySettings(config, properties);
    return config;
  }

  /**
   * Create an instance using specified customizer and override using a default properties file.
   *
   * @param customizer
   *          baseline customizer
   * @return a configuration instance
   * @see #newInstance()
   */
  public static VelocityLanguageDriverConfig newInstance(Consumer<VelocityLanguageDriverConfig> customizer) {
    VelocityLanguageDriverConfig config = new VelocityLanguageDriverConfig();
    Properties properties = loadDefaultProperties();
    customizer.accept(config);
    override(config, properties);
    configureVelocitySettings(config, properties);
    return config;
  }

  private static void override(VelocityLanguageDriverConfig config, Properties properties) {
    enableLegacyAdditionalContextAttributes(properties);
    MetaObject metaObject = MetaObject.forObject(config, new DefaultObjectFactory(), new DefaultObjectWrapperFactory(),
        new DefaultReflectorFactory());
    Set<Object> consumedKeys = new HashSet<>();
    properties.forEach((key, value) -> {
      String propertyPath = WordUtils
          .uncapitalize(WordUtils.capitalize(Objects.toString(key), '-').replaceAll("-", ""));
      if (metaObject.hasSetter(propertyPath)) {
        PropertyTokenizer pt = new PropertyTokenizer(propertyPath);
        if (Map.class.isAssignableFrom(metaObject.getGetterType(pt.getName()))) {
          @SuppressWarnings("unchecked")
          Map<String, Object> map = (Map<String, Object>) metaObject.getValue(pt.getName());
          map.put(pt.getChildren(), value);
        } else {
          Optional.ofNullable(value).ifPresent(v -> {
            Object convertedValue = TYPE_CONVERTERS.get(metaObject.getSetterType(propertyPath)).apply(value.toString());
            metaObject.setValue(propertyPath, convertedValue);
          });
        }
        consumedKeys.add(key);
      }
    });
    consumedKeys.forEach(properties::remove);
  }

  private static void enableLegacyAdditionalContextAttributes(Properties properties) {
    String additionalContextAttributes = properties.getProperty(PROPERTY_KEY_ADDITIONAL_CONTEXT_ATTRIBUTE);
    if (Objects.nonNull(additionalContextAttributes)) {
      log.warn(String.format(
          "The '%s' has been deprecated since 2.1.0. Please use the 'additionalContextAttributes.{name}={value}'.",
          PROPERTY_KEY_ADDITIONAL_CONTEXT_ATTRIBUTE));
      Stream.of(additionalContextAttributes.split(",")).forEach(pair -> {
        String[] keyValue = pair.split(":");
        if (keyValue.length != 2) {
          throw new ScriptingException("Invalid additional context property '" + pair + "' on '"
              + PROPERTY_KEY_ADDITIONAL_CONTEXT_ATTRIBUTE + "'. Must be specify by 'key:value' format.");
        }
        properties.setProperty("additional-context-attributes." + keyValue[0].trim(), keyValue[1].trim());
      });
      properties.remove(PROPERTY_KEY_ADDITIONAL_CONTEXT_ATTRIBUTE);
    }
  }

  private static void configureVelocitySettings(VelocityLanguageDriverConfig config, Properties properties) {
    properties.forEach((name, value) -> config.getVelocitySettings().put((String) name, (String) value));
  }

  private static Properties loadDefaultProperties() {
    return loadProperties(System.getProperty(PROPERTY_KEY_CONFIG_FILE, DEFAULT_PROPERTIES_FILE));
  }

  private static Properties loadProperties(String resourcePath) {
    Properties properties = new Properties();
    InputStream in;
    try {
      in = Resources.getResourceAsStream(resourcePath);
    } catch (IOException e) {
      in = null;
    }
    if (in != null) {
      Charset encoding = Optional.ofNullable(System.getProperty(PROPERTY_KEY_CONFIG_ENCODING)).map(Charset::forName)
          .orElse(StandardCharsets.UTF_8);
      try (InputStreamReader inReader = new InputStreamReader(in, encoding);
          BufferedReader bufReader = new BufferedReader(inReader)) {
        properties.load(bufReader);
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
    return properties;
  }

}
