package org.onebusaway.atco_cif_to_gtfs_converter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.onebusaway.atco_cif_to_gtfs_converter.parser.AtcoCifContentHandler;
import org.onebusaway.atco_cif_to_gtfs_converter.parser.AtcoCifElement;
import org.onebusaway.atco_cif_to_gtfs_converter.parser.AtcoCifException;
import org.onebusaway.atco_cif_to_gtfs_converter.parser.AtcoCifParser;
import org.onebusaway.atco_cif_to_gtfs_converter.parser.AdditionalLocationElement;
import org.onebusaway.atco_cif_to_gtfs_converter.parser.LocationElement;
import org.onebusaway.atco_cif_to_gtfs_converter.parser.JourneyHeaderElement;

public class AtcoCifToGtfsConverter {

  private File _inputPath;

  private File _outputPath;

  private Map<String, JourneyHeaderElement> _journeysById = new HashMap<String, JourneyHeaderElement>();

  private Map<String, LocationElement> _busLocationById = new HashMap<String, LocationElement>();

  private Map<String, AdditionalLocationElement> _busAdditionalLocationById = new HashMap<String, AdditionalLocationElement>();

  public void setInputPath(File inputPath) {
    _inputPath = inputPath;
  }

  public void setOutputPath(File outputPath) {
    _outputPath = outputPath;
  }

  public void run() throws IOException {
    List<File> paths = new ArrayList<File>();
    getApplicableFiles(_inputPath, paths);

    AtcoCifParser parser = new AtcoCifParser();
    HandlerImpl handler = new HandlerImpl();
    for (File path : paths) {
      parser.parse(path, handler);
    }
  }

  private void getApplicableFiles(File path, List<File> applicableFiles) {
    if (path.isDirectory()) {
      for (File subPath : path.listFiles()) {
        getApplicableFiles(subPath, applicableFiles);
      }
    } else if (path.getName().toLowerCase().endsWith(".cif")) {
      applicableFiles.add(path);
    }
  }

  private class HandlerImpl implements AtcoCifContentHandler {

    @Override
    public void startDocument() {
    }

    @Override
    public void endDocument() {

    }

    @Override
    public void startElement(AtcoCifElement element) {
      if (element instanceof JourneyHeaderElement) {
        JourneyHeaderElement journey = (JourneyHeaderElement) element;
        JourneyHeaderElement existing = _journeysById.put(
            journey.getJourneyIdentifier(), journey);
        if (existing != null) {
          throw new AtcoCifException("duplicate journey id "
              + journey.getJourneyIdentifier());
        }
      } else if (element instanceof LocationElement) {
        LocationElement location = (LocationElement) element;
        LocationElement existing = _busLocationById.put(
            location.getLocationId(), location);
        if (existing != null) {
          throw new AtcoCifException("duplicate location id "
              + location.getLocationId());
        }
      } else if (element instanceof AdditionalLocationElement) {
        AdditionalLocationElement location = (AdditionalLocationElement) element;
        AdditionalLocationElement existing = _busAdditionalLocationById.put(
            location.getLocationId(), location);
        if (existing != null) {
          throw new AtcoCifException("duplicate additional location id "
              + location.getLocationId());
        }
      }
    }

    @Override
    public void endElement(AtcoCifElement element) {

    }
  }
}
