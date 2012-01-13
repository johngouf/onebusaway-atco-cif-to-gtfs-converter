package org.onebusaway.atco_cif_to_gtfs_converter.parser;

public class OperatorElement extends AtcoCifElement {

  private String operatorId;

  private String shortFormName;

  private String legalName;

  private String enquiryPhone;

  private String contactPhone;

  public OperatorElement() {
    super(Type.OPERATOR);
  }

  public String getOperatorId() {
    return operatorId;
  }

  public void setOperatorId(String operatorId) {
    this.operatorId = operatorId;
  }

  public String getShortFormName() {
    return shortFormName;
  }

  public void setShortFormName(String shortFormName) {
    this.shortFormName = shortFormName;
  }

  public String getLegalName() {
    return legalName;
  }

  public void setLegalName(String legalName) {
    this.legalName = legalName;
  }

  public String getEnquiryPhone() {
    return enquiryPhone;
  }

  public void setEnquiryPhone(String enquiryPhone) {
    this.enquiryPhone = enquiryPhone;
  }

  public String getContactPhone() {
    return contactPhone;
  }

  public void setContactPhone(String contactPhone) {
    this.contactPhone = contactPhone;
  }
}
