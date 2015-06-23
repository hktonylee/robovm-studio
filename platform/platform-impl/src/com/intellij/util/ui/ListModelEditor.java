/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.util.ui;

import com.intellij.ui.*;
import com.intellij.ui.components.JBList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xmlb.XmlSerializerUtil;
import gnu.trove.TObjectObjectProcedure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

public class ListModelEditor<T> extends CollectionModelEditor<T, ListItemEditor<T>> {
  private final CollectionListModel<T> model = new CollectionListModel<T>() {
    @Override
    public void remove(int index) {
      T item = getElementAt(index);
      super.remove(index);
      helper.remove(item);
    }

    @Override
    public void removeAll() {
      super.removeAll();
      helper.clear();
    }
  };

  private final ToolbarDecorator toolbarDecorator;

  private JBList list = new JBList(model);

  public ListModelEditor(@NotNull final ListItemEditor<T> itemEditor) {
    super(itemEditor);

    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    list.setCellRenderer(new MyListCellRenderer());

    toolbarDecorator = ToolbarDecorator.createDecorator(list)
      .setAddAction(new AnActionButtonRunnable() {
        @Override
        public void run(AnActionButton button) {
          if (!model.isEmpty()) {
            T lastItem = model.getElementAt(model.getSize() - 1);
            if (itemEditor.isEmpty(lastItem)) {
              ListScrollingUtil.selectItem(list, ContainerUtil.indexOfIdentity(model.getItems(), lastItem));
              return;
            }
          }

          T item = createElement();
          model.add(item);
          ListScrollingUtil.selectItem(list, ContainerUtil.indexOfIdentity(model.getItems(), item));
        }
      });
        //.setRemoveAction(new AnActionButtonRunnable() {
      //  @Override
      //  public void run(AnActionButton button) {
      //    ListUtil.removeSelectedItems(myEditor.getList());
      //    myEditor.getList().repaint();
      //    myKeymapListener.processCurrentKeymapChanged(currentQuickListsToArray());
      //  }
      //})
  }

  @NotNull
  public T getMutable(@NotNull T item) {
    T mutable = helper.getMutable(item, itemEditor);
    if (mutable != item) {
      model.setElementAt(mutable, ContainerUtil.indexOfIdentity(model.getItems(), item));
    }
    return mutable;
  }

  @NotNull
  public JComponent createComponent() {
    return toolbarDecorator.createPanel();
  }

  @NotNull
  public JBList getList() {
    return list;
  }

  @NotNull
  public CollectionListModel<T> getModel() {
    return model;
  }

  @Nullable
  public T getSelected() {
    //noinspection unchecked
    return (T)list.getSelectedValue();
  }

  public void reset(@NotNull List<T> items) {
    model.replaceAll(items);
    // todo should we really do this?
    //noinspection SSBasedInspection
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        if (!model.isEmpty()) {
          list.setSelectedIndex(0);
        }
      }
    });
  }

  @NotNull
  @Override
  protected List<T> getItems() {
    return model.getItems();
  }

  @NotNull
  public List<T> apply() {
    final List<T> items = getItems();
    if (!helper.hasModifiedItems()) {
      return items;
    }

    helper.process(new TObjectObjectProcedure<T, T>() {
      @Override
      public boolean execute(T newItem, T oldItem) {
        XmlSerializerUtil.copyBean(newItem, oldItem);
        int index = ContainerUtil.indexOfIdentity(items, newItem);
        if (index == -1) {
          LOG.error("Inconsistence model", newItem.toString());
        }
        model.setElementAt(oldItem, index);
        return true;
      }
    });
    helper.clear();

    return getItems();
  }

  private class MyListCellRenderer extends ColoredListCellRenderer {
    @Override
    protected void customizeCellRenderer(JList list, Object value, int index, boolean selected, boolean hasFocus) {
      setBackground(UIUtil.getListBackground(selected));
      //noinspection unchecked
      append((itemEditor.getName(((T)value))));
    }
  }
}
