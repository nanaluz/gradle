/*
 * Copyright 2016 the original author or authors.
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

package org.gradle.integtests.tooling.r212

import org.gradle.integtests.tooling.fixture.TargetGradleVersion
import org.gradle.integtests.tooling.fixture.ToolingApiSpecification
import org.gradle.integtests.tooling.fixture.ToolingApiVersion
import org.gradle.integtests.tooling.fixture.ToolingModelTestTrait
import org.gradle.tooling.model.eclipse.HierarchicalEclipseProject
import org.gradle.util.UsesNativeServices

@ToolingApiVersion('>=2.12')
@TargetGradleVersion(">=1.3")
@UsesNativeServices
class ToolingModelSerializationCrossVersionSpec extends ToolingApiSpecification implements ToolingModelTestTrait {
    def "check that the eclipse model TAPI proxy is serializable"() {
        projectDir.file('build.gradle').text = '''
description = 'this is a project'
apply plugin: 'java'
apply plugin: 'eclipse'
'''
        projectDir.file('settings.gradle').text = 'rootProject.name = \"test project\"'

        when:
        loadToolingModel(HierarchicalEclipseProject)

        then:
        noExceptionThrown()

        when:
        loadEclipseProjectModel()

        then:
        noExceptionThrown()
    }
}
