/*
 * Copyright 2018 Kamil Piska
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
package pl.kpiska.jsondiff;

import com.intellij.diff.contents.DiffContent;
import com.intellij.diff.contents.DocumentContentImpl;
import com.intellij.diff.requests.DiffRequest;
import com.intellij.diff.requests.ErrorDiffRequest;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.TestDataProvider;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.stream.Stream;

public class JsonDiffActionTest extends LightPlatformCodeInsightFixtureTestCase {

  public void testShouldActionBeAvailableWhenOneJsonFile() {
    // given
    JsonDiffAction action = new JsonDiffAction();
    AnActionEvent event = eventWithFiles(action, "valid1a.json");

    // when
    boolean available = action.isAvailable(event);

    // then
    assertTrue(available);
  }

  public void testShouldActionBeAvailableWhenTwoJsonFiles() {
    // given
    JsonDiffAction action = new JsonDiffAction();
    AnActionEvent event = eventWithFiles(action, "valid1a.json", "valid2.JSON");

    // when
    boolean available = action.isAvailable(event);

    // then
    assertTrue(available);
  }

  public void testShouldActionNotBeAvailableWhenThreeOrMoreJsonFiles() {
    // given
    JsonDiffAction action = new JsonDiffAction();
    AnActionEvent event = eventWithFiles(action, "valid1a.json", "valid1b.json", "valid2.JSON");

    // when
    boolean available = action.isAvailable(event);

    // then
    assertFalse(available);
  }

  public void testShouldActionNotBeAvailableWhenZeroFiles() {
    // given
    JsonDiffAction action = new JsonDiffAction();
    AnActionEvent event = eventWithFiles(action);

    // when
    boolean available = action.isAvailable(event);

    // then
    assertFalse(available);
  }

  public void testShouldActionBeNotAvailableWhenAnyNotJsonFile() {
    // given
    JsonDiffAction action = new JsonDiffAction();
    AnActionEvent event = eventWithFiles(action, "not_json.txt", "valid2.JSON");

    // when
    boolean available = action.isAvailable(event);

    // then
    assertFalse(available);
  }

  public void testShouldActionBeNotAvailableWhenDirectoryWithJsonExtension() {
    // given
    JsonDiffAction action = new JsonDiffAction();
    AnActionEvent event = eventWithFiles(action, "directory.json", "valid2.JSON");

    // when
    boolean available = action.isAvailable(event);

    // then
    assertFalse(available);
  }

  public void testShouldJsonWithDifferentFieldsOrderBeEqual() {
    // given
    JsonDiffAction action = new JsonDiffAction();
    AnActionEvent event = eventWithFiles(action, "valid1a.json", "valid1b.json");

    // when
    DiffRequest diffRequest = action.getDiffRequest(event);

    // then
    assertTrue(isEqual(diffRequest));
  }

  public void testShouldDifferentJsonFilesBeNotEqual() {
    // given
    JsonDiffAction action = new JsonDiffAction();
    AnActionEvent event = eventWithFiles(action, "valid1a.json", "valid2.JSON");

    // when
    DiffRequest diffRequest = action.getDiffRequest(event);

    // then
    assertFalse(isEqual(diffRequest));
  }

  public void testShouldReturnErrorDiffWhenInvalidJsonFile() {
    // given
    JsonDiffAction action = new JsonDiffAction();
    AnActionEvent event = eventWithFiles(action, "not_valid.json", "valid2.JSON");

    // when
    DiffRequest diffRequest = action.getDiffRequest(event);

    // then
    assertNotNull(diffRequest);
    assertInstanceOf(diffRequest, ErrorDiffRequest.class);
  }

  private AnActionEvent eventWithFiles(AnAction action, String... fileNames) {
    VirtualFile[] files = Stream.of(fileNames)
      .map(fileName -> LocalFileSystem.getInstance().findFileByIoFile(new File("testData/" + fileName)))
      .toArray(VirtualFile[]::new);

    return new AnActionEvent(
      null,
      new VirtualFilesDataProvider(myFixture.getProject(), files),
      "",
      action.getTemplatePresentation().clone(), ActionManager.getInstance(),
      0
    );
  }

  private boolean isEqual(DiffRequest diffRequest) {
    List<DiffContent> diffContents = ((SimpleDiffRequest) diffRequest).getContents();

    return ((DocumentContentImpl) diffContents.get(0)).getDocument().getText()
      .compareTo(((DocumentContentImpl) diffContents.get(1)).getDocument().getText()) == 0;
  }

  private static final class VirtualFilesDataProvider extends TestDataProvider {
    private final VirtualFile[] files;

    private VirtualFilesDataProvider(@NotNull Project project, VirtualFile[] files) {
      super(project);
      this.files = files;
    }

    @Override
    public Object getData(String dataId) {
      if (CommonDataKeys.VIRTUAL_FILE_ARRAY.is(dataId)) {
        return files;
      }

      return super.getData(dataId);
    }
  }
}
