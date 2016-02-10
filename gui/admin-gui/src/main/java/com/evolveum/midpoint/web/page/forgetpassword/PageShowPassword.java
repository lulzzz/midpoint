package com.evolveum.midpoint.web.page.forgetpassword;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.application.PageDescriptor;

@PageDescriptor(url = "/resetpasswordsuccess")
public class PageShowPassword extends PageBase{

	
	PageBase page = (PageBase) getPage();
	
	public PageShowPassword() {
				
		 //System.out.println("onload:"+getSession().getAttribute("pwdReset"));
		add(new Label("pass", getSession().getAttribute("pwdReset")));
		getSession().removeAttribute("pwdReset");
		
		success(getString("PageShowPassword.success"));
	    add(getFeedbackPanel());
	}
	/*	@Override
		protected IModel<String> createPageTitleModel() {
			return new Model<String>("");
		}*/
		
		


	}

