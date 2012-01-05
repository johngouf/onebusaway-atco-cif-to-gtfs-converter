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

public class AtcoCifElement {

  public enum Type {
    JOURNEY_DATE_RUNNING, JOURNEY_HEADER, JOURNEY_ORIGIN, JOURNEY_INTERMEDIATE, JOURNEY_DESTINATION, LOCATION, ADDITIONAL_LOCATION, VEHICLE_TYPE
  }

  private final Type type;

  public AtcoCifElement(Type type) {
    this.type = type;
  }

  public Type getType() {
    return type;
  }
}
