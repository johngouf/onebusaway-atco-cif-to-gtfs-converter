package org.onebusaway.atco_cif_to_gtfs_converter.parser;

public interface AtcoCifContentHandler {

  public void startDocument();

  public void endDocument();

  public void startElement(AtcoCifElement element);

  public void endElement(AtcoCifElement element);
}
