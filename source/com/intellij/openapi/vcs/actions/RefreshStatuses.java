/*
 * Copyright 2000-2005 JetBrains s.r.o.
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
package com.intellij.openapi.vcs.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataConstants;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;

public class RefreshStatuses extends AnAction {
  public void actionPerformed(AnActionEvent e) {
    Project project = (Project)e.getDataContext().getData(DataConstants.PROJECT);
    if (project != null) {
      FileStatusManager.getInstance(project).fileStatusesChanged();
    }
  }

  public void update(AnActionEvent e) {
    super.update(e);
    final Object project = e.getDataContext().getData(DataConstants.PROJECT);
    if (!(project instanceof Project) ||
        ProjectLevelVcsManager.getInstance((Project) project).getAllActiveVcss().length == 0) {
      e.getPresentation().setEnabled(false);
      e.getPresentation().setVisible(false);
    }
  }
}
