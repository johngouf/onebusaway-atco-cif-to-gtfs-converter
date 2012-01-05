package org.onebusaway.atco_cif_to_gtfs_converter.parser;

public class AtcoCifException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public AtcoCifException(String message) {
    super(message);
  }

  public AtcoCifException(String message, Exception ex) {
    super(message, ex);
  }
}
