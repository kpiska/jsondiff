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

import com.google.common.collect.ImmutableSortedMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonNull;
import com.intellij.diff.actions.CompareFilesAction;
import com.intellij.diff.contents.DiffContent;
import com.intellij.diff.contents.DocumentContentImpl;
import com.intellij.diff.requests.DiffRequest;
import com.intellij.diff.requests.ErrorDiffRequest;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ParameterizedTypeImpl;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static com.intellij.diff.DiffRequestFactoryImpl.getContentTitle;
import static com.intellij.diff.DiffRequestFactoryImpl.getTitle;
import static com.intellij.vcsUtil.VcsUtil.getFilePath;

public class JsonDiffAction extends CompareFilesAction {

  @Override
  public void update(@NotNull AnActionEvent e) {
    super.update(e);

    VirtualFile[] files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);

    String text = (files != null && files.length == 1) ? "Compare JSON File With..." : "Compare JSON Files";

    e.getPresentation().setText(text);
  }

  @Override
  protected boolean isAvailable(@NotNull AnActionEvent e) {
    return super.isAvailable(e)
      && Stream.of(Objects.requireNonNull(e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY))).allMatch(this::isJsonVirtualFile);
  }

  @Override
  protected DiffRequest getDiffRequest(@NotNull AnActionEvent e) {
    VirtualFile[] data = e.getRequiredData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
    VirtualFile left = data[0];
    VirtualFile right;

    if (data.length < 2) {
      right = getOtherFile(e.getProject(), data[0]);

      if (right == null || !hasContent(right) || !isJsonVirtualFile(right)) {
        return new ErrorDiffRequest("Problem with second JSON file");
      }

      if (!data[0].isValid()) {
        return new ErrorDiffRequest("Problem with first JSON file"); // getOtherFile() shows dialog that can invalidate this file
      }
    } else {
      right = data[1];
    }

    try {
      return new SimpleDiffRequest(mainTitle(left, right), content(left), content(right), contentTitle(left), contentTitle(right));
    } catch (IOException | RuntimeException exception) {
      exception.printStackTrace();
      return new ErrorDiffRequest("Problem with some of JSON files");
    }
  }

  private Map<String, Object> deepSort(Map<?, ?> map) {
    ImmutableSortedMap.Builder<String, Object> builder = ImmutableSortedMap.naturalOrder();

    map.forEach((k, v) -> {
      if (v instanceof Map) {
        builder.put((String) k, deepSort((Map) v));
      } else {
        builder.put((String) k, v == null ? JsonNull.INSTANCE : v);
      }
    });
    return builder.build();
  }

  private DiffContent content(VirtualFile file) throws IOException {
    Map<String, Object> sortedJsonMap = deepSort(new Gson().fromJson(
      new String(file.contentsToByteArray(), file.getCharset()),
      new ParameterizedTypeImpl(Map.class, String.class, Object.class)
    ));
    String prettyJsonString = new GsonBuilder().setPrettyPrinting().serializeNulls().create().toJson(sortedJsonMap);
    return new DocumentContentImpl(new DocumentImpl(prettyJsonString));
  }

  private String contentTitle(VirtualFile file) {
    return getContentTitle(getFilePath(file));
  }

  private String mainTitle(VirtualFile file1, VirtualFile file2) {
    FilePath path1 = file1 != null ? getFilePath(file1) : null;
    FilePath path2 = file2 != null ? getFilePath(file2) : null;

    return getTitle(path1, path2, " vs ");
  }

  private boolean isJsonVirtualFile(VirtualFile file) {
    return "json".compareToIgnoreCase(file.getExtension()) == 0;
  }

  private VirtualFile getOtherFile(Project project, VirtualFile file) {
    FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, true, true, false);

    return FileChooser.chooseFile(descriptor, project, file);
  }
}
