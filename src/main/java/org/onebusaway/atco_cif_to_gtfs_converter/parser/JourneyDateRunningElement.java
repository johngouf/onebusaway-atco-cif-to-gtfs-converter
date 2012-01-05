package org.onebusaway.atco_cif_to_gtfs_converter.parser;

import org.onebusaway.gtfs.model.calendar.ServiceDate;

public class JourneyDateRunningElement extends AtcoCifElement implements
    JourneyChildElement, Comparable<JourneyDateRunningElement> {

  private ServiceDate startDate;

  private ServiceDate endDate;

  private int operationCode;

  public JourneyDateRunningElement() {
    super(Type.JOURNEY_DATE_RUNNING);
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

  public int getOperationCode() {
    return operationCode;
  }

  public void setOperationCode(int operationCode) {
    this.operationCode = operationCode;
  }

  @Override
  public int compareTo(JourneyDateRunningElement o) {
    int c = this.startDate.compareTo(o.startDate);
    if (c != 0)
      return c;
    c = this.endDate.compareTo(o.endDate);
    if (c != 0)
      return c;
    return o.operationCode - this.operationCode;
  }
}
