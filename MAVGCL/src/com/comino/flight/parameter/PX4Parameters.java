/****************************************************************************
 *
 *   Copyright (c) 2016 Eike Mansfeld ecm@gmx.de. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 ****************************************************************************/

package com.comino.flight.parameter;

import java.util.ArrayList;
import java.util.List;

import org.mavlink.messages.lquac.msg_param_request_list;
import org.mavlink.messages.lquac.msg_param_value;

import com.comino.flight.observables.StateProperties;
import com.comino.mav.control.IMAVController;
import com.comino.msp.main.control.listener.IMAVLinkListener;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;


public class PX4Parameters implements IMAVLinkListener {

	private static PX4Parameters px4params = null;

	private BooleanProperty isReadyProperty = new SimpleBooleanProperty();

	private List<ParameterAttributes> parameterList = null;

	private IMAVController control;

	private ParameterFactMetaData metadata = null;

	private int totalCount = 0;

	public static PX4Parameters getInstance(IMAVController control) {
		if(px4params==null)
			px4params = new PX4Parameters(control);
		return px4params;
	}

	public static PX4Parameters getInstance() {
		return px4params;
	}


	private PX4Parameters(IMAVController control) {
		this.control  = control;
		this.control.addMAVLinkListener(this);

		this.metadata = new ParameterFactMetaData("PX4ParameterFactMetaData.xml");
		this.parameterList = new ArrayList<ParameterAttributes>();

		StateProperties.getInstance().getConnectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue observable, Boolean oldValue, Boolean newValue) {
				refreshParameterList();
			}
		});
	}

	public void refreshParameterList() {
		parameterList.clear();
		isReadyProperty().setValue(false);
		msg_param_request_list msg = new msg_param_request_list(255,1);
		msg.target_component = 1;
		msg.target_system = 1;
		control.sendMAVLinkMessage(msg);
	}


	public BooleanProperty isReadyProperty() {
		return isReadyProperty;
	}

	@Override
	public void received(Object _msg) {

		if( _msg instanceof msg_param_value) {
			msg_param_value msg = (msg_param_value)_msg;
			this.totalCount = msg.param_count;

			if(msg.param_id[0]=='_')
				return;

			ParameterAttributes attributes = metadata.getMetaData(msg.getParam_id());
			if(attributes == null)
				attributes = new ParameterAttributes(msg.getParam_id(),"(DefaultGroup)");
			attributes.value = msg.param_value;
			attributes.vtype = msg.param_type;
			parameterList.add(attributes);
			if(parameterList.size() == totalCount)
				isReadyProperty().setValue(true);
		}
	}

	public ParameterFactMetaData getMetaData() {
		return this.metadata;
	}

	public List<ParameterAttributes> getList() {
		return this.parameterList;
	}

}