package org.onebusaway.atco_cif_to_gtfs_converter.parser;

public class AtcoCifElement {

  public enum Type {
    JOURNEY_HEADER, JOURNEY_ORIGIN, JOURNEY_INTERMEDIATE, JOURNEY_DESTINATION, LOCATION, ADDITIONAL_LOCATION, VEHICLE_TYPE
  }

  private final Type type;

  public AtcoCifElement(Type type) {
    this.type = type;
  }

  public Type getType() {
    return type;
  }
}
