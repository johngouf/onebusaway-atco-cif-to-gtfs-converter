package org.onebusaway.atco_cif_to_gtfs_converter.parser;

public class JourneyOriginElement extends JourneyElement {

  private int departureTime;

  public JourneyOriginElement() {
    super(AtcoCifElement.Type.JOURNEY_ORIGIN);
  }

  public int getDepartureTime() {
    return departureTime;
  }

  public void setDepartureTime(int departureTime) {
    this.departureTime = departureTime;
  }
}
