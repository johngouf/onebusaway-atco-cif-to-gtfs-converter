package org.onebusaway.atco_cif_to_gtfs_converter.parser;

public class JourneyDestinationElement extends JourneyElement {

  private int arrivalTime;

  public JourneyDestinationElement() {
    super(AtcoCifElement.Type.JOURNEY_DESTINATION);
  }

  public int getArrivalTime() {
    return arrivalTime;
  }

  public void setArrivalTime(int arrivalTime) {
    this.arrivalTime = arrivalTime;
  }
}
