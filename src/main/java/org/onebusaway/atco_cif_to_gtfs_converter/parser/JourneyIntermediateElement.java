/**
 * Copyright (C) 2012 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onebusaway.atco_cif_to_gtfs_converter.parser;

public class JourneyIntermediateElement extends JourneyTimePointElement {

  private int arrivalTime;
  private String arrivalString;

  private int departureTime;
  private String departureString;

  public JourneyIntermediateElement() {
    super(AtcoCifElement.Type.JOURNEY_INTERMEDIATE);
  }

  @Override
  public int getArrivalTime() {
    return arrivalTime;
  }

  public void setArrivalTime(int arrivalTime) {
    this.arrivalTime = arrivalTime;
  }

  @Override
  public int getDepartureTime() {
    return departureTime;
  }

  public void setDepartureTime(int departureTime) {
    this.departureTime = departureTime;
  }

    public String getArrivalString() {
        return arrivalString;
    }

    public void setArrivalString(String arrivalString) {
        this.arrivalString = arrivalString;
    }

    public String getDepartureString() {
        return departureString;
    }

    public void setDepartureString(String departureString) {
        this.departureString = departureString;
    }
    
}