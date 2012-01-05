package org.onebusaway.atco_cif_to_gtfs_converter.parser;

public class JourneyIntermediateElement extends JourneyTimePointElement {

  private int arrivalTime;

  private int departureTime;

  public JourneyIntermediateElement() {
    super(AtcoCifElement.Type.JOURNEY_INTERMEDIATE);
  }

  public int getArrivalTime() {
    return arrivalTime;
  }

  public void setArrivalTime(int arrivalTime) {
    this.arrivalTime = arrivalTime;
  }

  public int getDepartureTime() {
    return departureTime;
  }

  public void setDepartureTime(int departureTime) {
    this.departureTime = departureTime;
  }

  @Override
  public int getArrivalTimeInSeconds() {
    return arrivalTime * 60;
  }

  @Override
  public int getDepartureTimeInSeconds() {
    return departureTime * 60;
  }
}
