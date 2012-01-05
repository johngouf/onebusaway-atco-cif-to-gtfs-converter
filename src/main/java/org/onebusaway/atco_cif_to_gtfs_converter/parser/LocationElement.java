package org.onebusaway.atco_cif_to_gtfs_converter.parser;

public class LocationElement extends AtcoCifElement {

  private String locationId;

  private String name;

  public LocationElement() {
    super(AtcoCifElement.Type.LOCATION);
  }

  public String getLocationId() {
    return locationId;
  }

  public void setLocationId(String locationId) {
    this.locationId = locationId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
