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
package com.intellij.psi.impl.compiled;

import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.impl.source.JavaFileElementType;
import com.intellij.psi.stubs.BinaryFileStubBuilder;
import com.intellij.psi.stubs.PsiFileStub;
import com.intellij.psi.stubs.StubElement;
import com.intellij.util.cls.ClsFormatException;

/**
 * @author max
 */
public class ClassFileStubBuilder implements BinaryFileStubBuilder {
  @Override
  public boolean acceptsFile(final VirtualFile file) {
    return true;
  }

  @Override
  public StubElement buildStubTree(final VirtualFile file, final byte[] content, final Project project) {
    try {
      final ClsStubBuilderFactory[] factories = Extensions.getExtensions(ClsStubBuilderFactory.EP_NAME);
      for (ClsStubBuilderFactory factory : factories) {
        if (!factory.isInnerClass(file) && factory.canBeProcessed(file, content)) {
          PsiFileStub stub = factory.buildFileStub(file, content, project);
          if (stub != null) return stub;
        }
      }
      return null;
    }
    catch (ClsFormatException e) {
      return null;
    }
  }

  @Override
  public int getStubVersion() {
    return JavaFileElementType.STUB_VERSION + 6;
  }
}
