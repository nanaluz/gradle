/*
 * Copyright 2012 the original author or authors.
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

package org.gradle.api.internal.artifacts.repositories.resolver

import org.gradle.api.Action
import org.gradle.api.internal.artifacts.DefaultModuleIdentifier
import org.gradle.api.internal.artifacts.DefaultModuleVersionIdentifier
import org.gradle.api.resources.MissingResourceException
import org.gradle.api.resources.ResourceException
import org.gradle.internal.UncheckedException
import org.gradle.internal.component.model.DefaultIvyArtifactName
import org.gradle.internal.resolve.result.DefaultResourceAwareResolveResult
import org.gradle.internal.resource.ExternalResourceName
import org.gradle.internal.resource.local.FileStore
import org.gradle.internal.resource.local.LocallyAvailableExternalResource
import org.gradle.internal.resource.transfer.CacheAwareExternalResourceAccessor
import org.gradle.internal.resource.transport.ExternalResourceRepository
import org.xml.sax.SAXParseException
import spock.lang.Specification

class MavenVersionListerTest extends Specification {
    def repo = Mock(ExternalResourceRepository)
    def module = new DefaultModuleIdentifier("org.acme", "testproject")
    def result = new DefaultResourceAwareResolveResult()
    def moduleVersion = new DefaultModuleVersionIdentifier(module, "1.0")
    def artifact = new DefaultIvyArtifactName("testproject", "jar", "jar")

    def resourceAccessor = Mock(CacheAwareExternalResourceAccessor)
    def fileStore = Mock(FileStore)
    def pattern = pattern("testRepo/" + MavenPattern.M2_PATTERN)
    def metaDataResource = new ExternalResourceName('testRepo/org/acme/testproject/maven-metadata.xml')

    final MavenVersionLister lister = new MavenVersionLister(resourceAccessor, fileStore)

    def "visit parses maven-metadata.xml"() {
        LocallyAvailableExternalResource resource = Mock()

        when:
        def versions = []
        def versionList = lister.newVisitor(module, versions, result)
        versionList.visit(pattern, artifact)

        then:
        versions == ['1.1', '1.2']
        result.attempted == [metaDataResource.toString()]

        and:
        1 * resourceAccessor.getResource(metaDataResource, _, null) >> resource
        1 * resource.withContent(_) >> { Action action -> action.execute(new ByteArrayInputStream("""
<metadata>
    <versioning>
        <versions>
            <version>1.1</version>
            <version>1.2</version>
        </versions>
    </versioning>
</metadata>""".bytes))
        }
        1 * resource.close()
        0 * resourceAccessor._
        0 * resource._
    }

    def "visit builds union of versions"() {
        LocallyAvailableExternalResource resource1 = Mock()
        LocallyAvailableExternalResource resource2 = Mock()
        def pattern1 = pattern("prefix1/" + MavenPattern.M2_PATTERN)
        def pattern2 = pattern("prefix2/" + MavenPattern.M2_PATTERN)
        def location1 = new ExternalResourceName('prefix1/org/acme/testproject/maven-metadata.xml')
        def location2 = new ExternalResourceName('prefix2/org/acme/testproject/maven-metadata.xml')

        when:
        def versions = []
        def versionList = lister.newVisitor(module, versions, result)
        versionList.visit(pattern1, artifact)
        versionList.visit(pattern2, artifact)

        then:
        versions == ['1.1', '1.2', '1.2', '1.3']
        result.attempted == [location1.toString(), location2.toString()]

        and:
        1 * resourceAccessor.getResource(location1, _, null) >> resource1
        1 * resource1.withContent(_) >> { Action action -> action.execute(new ByteArrayInputStream("""
<metadata>
    <versioning>
        <versions>
            <version>1.1</version>
            <version>1.2</version>
        </versions>
    </versioning>
</metadata>""".bytes))
        }
        1 * resourceAccessor.getResource(location2, _, null) >> resource2
        1 * resource2.withContent(_) >> { Action action -> action.execute(new ByteArrayInputStream("""
<metadata>
    <versioning>
        <versions>
            <version>1.2</version>
            <version>1.3</version>
        </versions>
    </versioning>
</metadata>""".bytes))
        }
    }

    def "visit ignores duplicate patterns"() {
        LocallyAvailableExternalResource resource = Mock()

        when:
        def versions = []
        def versionList = lister.newVisitor(module, versions, result)
        versionList.visit(pattern, artifact)
        versionList.visit(pattern, artifact)

        then:
        versions == ['1.1', '1.2']
        result.attempted == [metaDataResource.toString()]

        and:
        1 * resourceAccessor.getResource(metaDataResource, _, null) >> resource
        1 * resource.withContent(_) >> { Action action -> action.execute(new ByteArrayInputStream("""
<metadata>
    <versioning>
        <versions>
            <version>1.1</version>
            <version>1.2</version>
        </versions>
    </versioning>
</metadata>""".bytes))
        }
        1 * resource.close()
        0 * resourceAccessor._
        0 * resource._
    }

    def "visit throws MissingResourceException when maven-metadata not available"() {
        when:
        def versionList = lister.newVisitor(module, [], result)
        versionList.visit(pattern, artifact)

        then:
        MissingResourceException e = thrown()
        e.message == "Maven meta-data not available at $metaDataResource"

        and:
        result.attempted == [metaDataResource.toString()]

        and:
        1 * resourceAccessor.getResource(metaDataResource, _, null) >> null
        0 * resourceAccessor._
    }

    def "visit throws ResourceException when maven-metadata cannot be parsed"() {
        LocallyAvailableExternalResource resource = Mock()

        when:
        def versionList = lister.newVisitor(module, [], result)
        versionList.visit(pattern, artifact)

        then:
        ResourceException e = thrown()
        e.message == "Unable to load Maven meta-data from $metaDataResource."
        e.cause instanceof UncheckedException
        e.cause.cause instanceof SAXParseException

        and:
        result.attempted == [metaDataResource.toString()]

        and:
        1 * resource.close()
        1 * resourceAccessor.getResource(metaDataResource, _, null) >> resource;
        1 * resource.withContent(_) >> { Action action -> action.execute(new ByteArrayInputStream("yo".bytes)) }
        0 * resourceAccessor._
    }

    def "visit throws ResourceException when maven-metadata cannot be loaded"() {
        def failure = new RuntimeException()

        when:
        def versionList = lister.newVisitor(module, [], result)
        versionList.visit(pattern, artifact)

        then:
        ResourceException e = thrown()
        e.message == "Unable to load Maven meta-data from $metaDataResource."
        e.cause == failure

        and:
        result.attempted == [metaDataResource.toString()]

        and:
        1 * resourceAccessor.getResource(metaDataResource, _, null) >> { throw failure }
        0 * resourceAccessor._
    }

    def pattern(String pattern) {
        return new M2ResourcePattern(pattern)
    }
}
