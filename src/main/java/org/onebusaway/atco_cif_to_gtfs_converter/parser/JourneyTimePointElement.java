package org.onebusaway.atco_cif_to_gtfs_converter.parser;

public abstract class JourneyTimePointElement extends AtcoCifElement implements
    JourneyChildElement {

  private JourneyHeaderElement header;

  private String locationId;

  public JourneyTimePointElement(AtcoCifElement.Type type) {
    super(type);
  }

  public JourneyHeaderElement getHeader() {
    return header;
  }

  public void setHeader(JourneyHeaderElement header) {
    this.header = header;
  }

  public String getLocationId() {
    return locationId;
  }

  public void setLocationId(String locationId) {
    this.locationId = locationId;
  }

  public abstract int getArrivalTimeInSeconds();
  
  public abstract int getDepartureTimeInSeconds();
}
