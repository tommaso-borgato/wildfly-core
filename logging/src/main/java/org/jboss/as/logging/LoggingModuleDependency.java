/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2020 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.logging;

import org.jboss.as.controller.registry.RuntimePackageDependency;

/**
 * Describes a module dependency.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public enum LoggingModuleDependency {
    APACHE_COMMONS_LOGGING("org.apache.commons.logging"),
    LOG4J("org.apache.log4j"),
    LOG4J2("org.apache.logging.log4j.api"),
    LOG4J2_IMPL("org.jboss.logmanager.log4j2", true),
    JBOSS_LOGGING("org.jboss.logging"),
    JUL_TO_SLF4J("org.jboss.logging.jul-to-slf4j-stub"),
    JBOSS_LOG_MANAGER("org.jboss.logmanager"),
    SLF4J("org.slf4j"),
    SLF4J_IMPL("org.slf4j.impl"),
    ;

    private static final RuntimePackageDependency[] RUNTIME_DEPENDENCIES = {
            RuntimePackageDependency.optional(APACHE_COMMONS_LOGGING.moduleName),
            RuntimePackageDependency.optional(LOG4J2.moduleName),
            RuntimePackageDependency.optional(LOG4J2_IMPL.moduleName),
            RuntimePackageDependency.optional(SLF4J.moduleName),
            RuntimePackageDependency.optional(JUL_TO_SLF4J.moduleName),
            RuntimePackageDependency.passive(SLF4J_IMPL.moduleName),
    };

    private final String moduleName;
    private final boolean importServices;

    LoggingModuleDependency(final String moduleName) {
        this(moduleName, false);
    }

    LoggingModuleDependency(final String moduleName, final boolean importServices) {
        this.moduleName = moduleName;
        this.importServices = importServices;
    }

    /**
     * The module name.
     *
     * @return the module name
     */
    public String getModuleName() {
        return moduleName;
    }

    /**
     * Indicates whether or not services should be imported.
     *
     * @return {@code true} if services from the module should be imported
     */
    public boolean isImportServices() {
        return importServices;
    }

    /**
     * An array of the runtime dependencies.
     *
     * @return the runtime dependencies
     */
    public static RuntimePackageDependency[] getRuntimeDependencies() {
        return RUNTIME_DEPENDENCIES;
    }
}
