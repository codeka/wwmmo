/*
 * Copyright 2011 Google Inc.
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
package au.com.codeka.warworlds.client;

import au.com.codeka.warworlds.client.pages.DashboardPage;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.History;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class WarWorlds implements EntryPoint, ValueChangeHandler<String> {
	/**
	 * This is the entry point method.
	 */
	public void onModuleLoad() {
		History.addValueChangeHandler(this);
		if (History.getToken().isEmpty()) {
			History.newItem("warworlds");
		}

		Navigation.go(new DashboardPage());
	}

	@Override
	public void onValueChange(ValueChangeEvent<String> event) {
		Navigation.go(History.getToken());
	}
}
