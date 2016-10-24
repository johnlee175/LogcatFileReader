/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package com.johnsoft.ui;

import javax.swing.JTextPane;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BoxView;
import javax.swing.text.ComponentView;
import javax.swing.text.EditorKit;
import javax.swing.text.Element;
import javax.swing.text.IconView;
import javax.swing.text.LabelView;
import javax.swing.text.ParagraphView;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

/**
 * A JTextPane like JTextField, no line wrap
 * @author John Kenrinus Lee
 * @version 2016-10-21
 */
public class JNoWrapTextPane extends JTextPane {
    @Override
    protected EditorKit createDefaultEditorKit() {
        return new NoWrapStyledEditorKit();
    }

    private static final class NoWrapStyledEditorKit extends StyledEditorKit {
        private static final ViewFactory defaultFactory = new NoWrapStyledViewFactory();

        public ViewFactory getViewFactory() {
            return defaultFactory;
        }
    }

    private static final class NoWrapStyledViewFactory implements ViewFactory {
        public View create(Element elem) {
            String kind = elem.getName();
            if (kind != null) {
                if (kind.equals(AbstractDocument.ContentElementName)) {
                    return new LabelView(elem);
                } else if (kind.equals(AbstractDocument.ParagraphElementName)) {
                    return new ParagraphView(elem);
                } else if (kind.equals(AbstractDocument.SectionElementName)) {
                    return new BoxView(elem, View.Y_AXIS);
                } else if (kind.equals(StyleConstants.ComponentElementName)) {
                    return new ComponentView(elem);
                } else if (kind.equals(StyleConstants.IconElementName)) {
                    return new IconView(elem);
                }
            }
            // default to text display
            return new LabelView(elem);
        }
    }

    private static final class NoWrapParagraphView extends ParagraphView {
        public NoWrapParagraphView(Element elem) {
            super(elem);
        }

        public void layout(int width, int height) {
            super.layout(Short.MAX_VALUE, height);
        }

        public float getMinimumSpan(int axis) {
            return super.getPreferredSpan(axis);
        }
    }
}
