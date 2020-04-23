/*
 * Copyright 2020 the original author or authors.
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
package org.gradle.internal.component.model;

import com.google.common.collect.ImmutableList;
import org.gradle.internal.component.external.model.ConfigurationBoundExternalDependencyMetadata;
import org.gradle.internal.component.external.model.ModuleDependencyMetadata;

import java.util.List;

public abstract class ConfigurationMetadataHelper {
    public static ImmutableList<ModuleDependencyMetadata> isolate(List<ModuleDependencyMetadata> configDependencies) {
        if (configDependencies == null) {
            return null;
        }
        if (!configDependencies.isEmpty()) {
            ImmutableList.Builder<ModuleDependencyMetadata> dependencies = ImmutableList.builderWithExpectedSize(configDependencies.size());
            for (ModuleDependencyMetadata dependency : configDependencies) {
                if (dependency instanceof ConfigurationBoundExternalDependencyMetadata) {
                    ConfigurationBoundExternalDependencyMetadata md = (ConfigurationBoundExternalDependencyMetadata) dependency;
                    dependencies.add(md.withoutRules());
                } else {
                    dependencies.add(dependency);
                }
            }
            return dependencies.build();
        }
        return ImmutableList.copyOf(configDependencies);
    }
}
