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

package com.comino.flight.widgets.details;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

import com.comino.flight.model.AnalysisDataModel;
import com.comino.flight.model.AnalysisDataModelMetaData;
import com.comino.flight.model.KeyFigureMetaData;
import com.comino.flight.model.service.AnalysisModelService;
import com.comino.flight.observables.StateProperties;
import com.comino.flight.widgets.FadePane;
import com.comino.mav.control.IMAVController;
import com.comino.model.types.MSTYPE;
import com.comino.msp.model.DataModel;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

public class DetailsWidget extends FadePane  {


	private static String[] key_figures_details = {
			"ROLL",
			"PITCH",
			//MSTYPE.MSP_GRSPEED,
			//MSTYPE.MSP_CLIMBRATE,
			//MSTYPE.MSP_AIRSPEED,
			null,
			"HEAD",
			"RGPSNO",
			"RGPSHDOP",
			null,
			"ALTSL",
			"ALTTR",
			"BOTCL",
			null,
			"FLOWQ",
			"LIDAR",
			null,
			"LPOSX",
			"LPOSY",
			"LPOSZ",
			null,
			//MSTYPE.MSP_RC0,
			//MSTYPE.MSP_RC1,
			//MSTYPE.MSP_RC2,
			//MSTYPE.MSP_RC3,
			null,
			"BATC",
			"BATH",
			null,
			//MSTYPE.MSP_ABSPRESSURE,
			null,
			//MSTYPE.MSP_IMUTEMP,
			//MSTYPE.MSP_CPULOAD,
			//MSTYPE.MSP_RSSI,
			//MSTYPE.MSP_TIME_ARMED,
	};

	@FXML
	private GridPane grid;

	private Task<Long> task;
	private AnalysisDataModel model = AnalysisModelService.getInstance().getCurrent();
	private AnalysisDataModelMetaData meta = AnalysisDataModelMetaData.getInstance();

	private List<KeyFigure> figures = null;

	private DecimalFormat f = new DecimalFormat("#0.#");

	public DetailsWidget() {

		DecimalFormatSymbols f_symbols = new DecimalFormatSymbols();
		f_symbols.setNaN("-");
		f.setDecimalFormatSymbols(f_symbols);

		figures = new ArrayList<KeyFigure>();

		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("DetailsWidget.fxml"));
		fxmlLoader.setRoot(this);
		fxmlLoader.setController(this);
		try {
			fxmlLoader.load();
		} catch (IOException exception) {

			throw new RuntimeException(exception);
		}

		task = new Task<Long>() {

			@Override
			protected Long call() throws Exception {
				while(true) {
					LockSupport.parkNanos(333000000L);
					if(isDisabled() || !isVisible()) {
						continue;
					}

					if (isCancelled()) {
						break;
					}

					Platform.runLater(() -> {
						for(KeyFigure figure : figures) {
							figure.setValue(model);
						}
					});
				}
				return model.tms;
			}
		};

	}


	public void setup(IMAVController control) {

		int i=0;
		for(String k : key_figures_details) {
			figures.add(new KeyFigure(grid,k,i));
			i++;
		}

		Thread th = new Thread(task);
		th.setPriority(Thread.MIN_PRIORITY);
		th.setDaemon(true);
		th.start();

		this.disableProperty().bind(StateProperties.getInstance().getConnectedProperty().not());

	}

	private class KeyFigure {
		KeyFigureMetaData kf  = null;
		Label  value = null;

		public KeyFigure(GridPane grid, String k, int row) {
			this.kf = meta.getMetaData(k);
			if(kf==null) {
				grid.add(new Label(),0,row);
			} else {
			Label l1 = new Label(kf.desc1+" :");
			l1.setPrefWidth(120); l1.setPrefHeight(19);
			grid.add(l1, 0, row);
			value = new Label("-"); value.setPrefWidth(60); value.setAlignment(Pos.CENTER_RIGHT);
			grid.add(value, 1, row);
			Label l3 = new Label(" "+kf.uom); l3.setPrefWidth(50);
			grid.add(l3, 2, row);
			}
		}

		public void setValue(AnalysisDataModel model) {
			if(kf!=null) {
				f.applyPattern(kf.mask);
			    value.setText(f.format(model.getValue(kf)));
			}
		}
	}


}
