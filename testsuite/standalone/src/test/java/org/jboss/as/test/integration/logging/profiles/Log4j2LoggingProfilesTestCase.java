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

package org.jboss.as.test.integration.logging.profiles;

import java.io.FilePermission;
import java.security.Permission;
import java.util.PropertyPermission;

import org.jboss.as.test.integration.logging.Log4j2ServiceActivator;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.runner.RunWith;
import org.wildfly.core.testrunner.ServerSetup;
import org.wildfly.core.testrunner.WildflyTestRunner;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RunWith(WildflyTestRunner.class)
@ServerSetup(AbstractLoggingProfilesTestCase.LoggingProfilesTestCaseSetup.class)
public class Log4j2LoggingProfilesTestCase extends AbstractLoggingProfilesTestCase {

    public Log4j2LoggingProfilesTestCase() {
        super(Log4j2ServiceActivator.class, 2);
    }

    @Override
    protected void processDeployment(final JavaArchive deployment) {
        final Permission[] permissions = {
                // The getClassLoader permissions is required for the org.apache.logging.log4j.util.ProviderUtil.
                new RuntimePermission("getClassLoader"),
                // The FilePermissions is also for the org.apache.logging.log4j.util.ProviderUtil as it needs to read the JAR
                // for the service loader.
                new FilePermission("<<ALL FILES>>", "read"),
                // Required for the EnvironmentPropertySource System.getenv().
                new RuntimePermission("getenv.*"),
                // Required for the SystemPropertiesPropertySource System.getProperties().
                new PropertyPermission("*", "read,write"),
        };
        addPermissions(deployment, permissions);
    }
}
