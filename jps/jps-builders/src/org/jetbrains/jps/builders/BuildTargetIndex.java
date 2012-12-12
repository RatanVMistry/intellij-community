/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.jps.builders;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.builders.impl.BuildTargetChunk;
import org.jetbrains.jps.incremental.CompileContext;

import java.util.List;
import java.util.Set;

/**
 * @author nik
 */
public interface BuildTargetIndex extends BuildTargetRegistry {

  List<BuildTargetChunk> getSortedTargetChunks(@NotNull CompileContext context);

  Set<BuildTarget<?>> getDependenciesRecursively(@NotNull BuildTarget<?> target, @NotNull CompileContext context);
}
