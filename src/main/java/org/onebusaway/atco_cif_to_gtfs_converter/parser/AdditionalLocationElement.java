package org.onebusaway.atco_cif_to_gtfs_converter.parser;

public class AdditionalLocationElement extends AtcoCifElement {

  private String locationId;

  private double lat;

  private double lon;

  public AdditionalLocationElement() {
    super(AtcoCifElement.Type.ADDITIONAL_LOCATION);
  }

  public String getLocationId() {
    return locationId;
  }

  public void setLocationId(String locationId) {
    this.locationId = locationId;
  }

  public double getLat() {
    return lat;
  }

  public void setLat(double lat) {
    this.lat = lat;
  }

  public double getLon() {
    return lon;
  }

  public void setLon(double lon) {
    this.lon = lon;
  }
}
