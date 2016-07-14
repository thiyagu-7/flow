/*
 * Copyright 2000-2016 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.hummingbird.uitest.ui.template;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.annotations.EventHandler;
import com.vaadin.hummingbird.template.model.TemplateModel;
import com.vaadin.ui.Template;

public class ForView extends Template {

    public static class Item {
        private String text;

        public void setText(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }
    }

    public interface Model extends TemplateModel {
        List<Item> getItems();

        void setItems(List<Item> items);
    }

    private int itemCount = 0;

    public ForView() {
        getModel().setItems(new ArrayList<>());
    }

    @Override
    protected Model getModel() {
        return (Model) super.getModel();
    }

    @EventHandler
    private void add() {
        Item item = new Item();
        item.setText("Item " + itemCount++);
        getModel().getItems().add(item);
    }

    @EventHandler
    private void remove() {
        List<Item> items = getModel().getItems();
        if (!items.isEmpty()) {
            items.remove(items.size() - 1);
        }
    }

    @EventHandler
    private void update() {
        List<Item> items = getModel().getItems();
        if (!items.isEmpty()) {
            Item item = items.get(items.size() - 1);
            item.setText(item.getText() + " updated");
        }
    }
}