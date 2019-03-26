/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.gui.impl.component.prism;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemWrapperOld;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ItemVisibilityHandlerOld;

/**
 * @author lazyman
 */
public class PrismPropertyColumnPanel<IW extends ItemWrapperOld> extends PrismPropertyPanel<IW> {
	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(PrismPropertyColumnPanel.class);

    public PrismPropertyColumnPanel(String id, IModel<IW> model, Form form, ItemVisibilityHandlerOld visibilityHandler) {
		super(id, model, form, visibilityHandler);
	}
    
    protected WebMarkupContainer getHeader(String idComponent) {
    	return new WebMarkupContainer(idComponent);
    }
    
    protected String getItemCssClass() {
    	return " prism-value ";
    }
    
    @Override
    public boolean isVisible(ItemVisibilityHandlerOld visibilityHandler) {
    	return true;
    }
    
    protected String getButtonsCssClass() {
        return " col-xs-2 p-0 ";
    }
}