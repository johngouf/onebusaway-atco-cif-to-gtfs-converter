package org.onebusaway.atco_cif_to_gtfs_converter.parser;

import java.util.ArrayList;
import java.util.List;

import org.onebusaway.gtfs.model.calendar.ServiceDate;

public class JourneyHeaderElement extends AtcoCifElement {

  public enum VehicleType {
    TRAIN
  }

  private String journeyIdentifier;

  private ServiceDate startDate;

  private ServiceDate endDate;

  private int monday;

  private int tuesday;

  private int wednesday;

  private int thursday;

  private int friday;

  private int saturday;

  private int sunday;

  private String routeIdentifier;

  private VehicleType vehicleType;

  private List<JourneyElement> elements = new ArrayList<JourneyElement>();

  public JourneyHeaderElement() {
    super(Type.JOURNEY_HEADER);
  }

  public String getJourneyIdentifier() {
    return journeyIdentifier;
  }

  public void setJourneyIdentifier(String journeyIdentifier) {
    this.journeyIdentifier = journeyIdentifier;
  }

  public ServiceDate getStartDate() {
    return startDate;
  }

  public void setStartDate(ServiceDate startDate) {
    this.startDate = startDate;
  }

  public ServiceDate getEndDate() {
    return endDate;
  }

  public void setEndDate(ServiceDate endDate) {
    this.endDate = endDate;
  }

  public int getMonday() {
    return monday;
  }

  public void setMonday(int monday) {
    this.monday = monday;
  }

  public int getTuesday() {
    return tuesday;
  }

  public void setTuesday(int tuesday) {
    this.tuesday = tuesday;
  }

  public int getWednesday() {
    return wednesday;
  }

  public void setWednesday(int wednesday) {
    this.wednesday = wednesday;
  }

  public int getThursday() {
    return thursday;
  }

  public void setThursday(int thursday) {
    this.thursday = thursday;
  }

  public int getFriday() {
    return friday;
  }

  public void setFriday(int friday) {
    this.friday = friday;
  }

  public int getSaturday() {
    return saturday;
  }

  public void setSaturday(int saturday) {
    this.saturday = saturday;
  }

  public int getSunday() {
    return sunday;
  }

  public void setSunday(int sunday) {
    this.sunday = sunday;
  }

  public String getRouteIdentifier() {
    return routeIdentifier;
  }

  public void setRouteIdentifier(String routeIdentifier) {
    this.routeIdentifier = routeIdentifier;
  }

  public VehicleType getVehicleType() {
    return vehicleType;
  }

  public void setVehicleType(VehicleType vehicleType) {
    this.vehicleType = vehicleType;
  }

  public List<JourneyElement> getElements() {
    return elements;
  }

  public void setElements(List<JourneyElement> elements) {
    this.elements = elements;
  }
}
