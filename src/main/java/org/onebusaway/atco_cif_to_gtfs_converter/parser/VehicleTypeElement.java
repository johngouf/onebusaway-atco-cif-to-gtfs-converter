package org.onebusaway.atco_cif_to_gtfs_converter.parser;

public class VehicleTypeElement extends AtcoCifElement {

  private String id;

  private String description;

  public VehicleTypeElement() {
    super(Type.VEHICLE_TYPE);
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}
