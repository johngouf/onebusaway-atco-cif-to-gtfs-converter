package org.onebusaway.atco_cif_to_gtfs_converter.parser;

import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.onebusaway.gtfs.model.calendar.ServiceDate;

import com.jhlabs.map.proj.Projection;
import com.jhlabs.map.proj.ProjectionFactory;

public class AtcoCifParser {

  private static Map<String, AtcoCifElement.Type> _typesByKey = new HashMap<String, AtcoCifElement.Type>();

  static {
    _typesByKey.put("QS", AtcoCifElement.Type.JOURNEY_HEADER);
    _typesByKey.put("QE", AtcoCifElement.Type.JOURNEY_DATE_RUNNING);
    _typesByKey.put("QO", AtcoCifElement.Type.JOURNEY_ORIGIN);
    _typesByKey.put("QI", AtcoCifElement.Type.JOURNEY_INTERMEDIATE);
    _typesByKey.put("QT", AtcoCifElement.Type.JOURNEY_DESTINATION);
    _typesByKey.put("QL", AtcoCifElement.Type.LOCATION);
    _typesByKey.put("QB", AtcoCifElement.Type.ADDITIONAL_LOCATION);
    _typesByKey.put("QV", AtcoCifElement.Type.VEHICLE_TYPE);
  }

  private static final String _fromProjectionSpec = "+proj=tmerc +lat_0=49 +lon_0=-2 +k=0.9996012717 +x_0=400000 "
      + "+y_0=-100000 +ellps=airy +datum=OSGB36  +units=m +no_defs";
  // +towgs84=446.448,-125.157,542.060,0.1502,0.2470,0.8421,-20.4894
  private static final Projection _fromProjection = ProjectionFactory.fromPROJ4Specification(_fromProjectionSpec.split(" "));

  // private static final String _toProjectionSpec =
  // "+proj=longlat +ellps=WGS84 +datum=WGS84 +no_defs";

  // private static final Projection _toProjection =
  // ProjectionFactory.fromPROJ4Specification(_toProjectionSpec.split(" "));

  private int _currentLineCount = 0;

  private String _currentLine;

  private int _currentLineCharactersConsumed;

  private JourneyHeaderElement _currentJourney = null;

  private Date _maxServiceDate;

  public AtcoCifParser() {
    Calendar c = Calendar.getInstance();
    c.add(Calendar.YEAR, 2);
    _maxServiceDate = c.getTime();
  }

  public void parse(File path, AtcoCifContentHandler handler)
      throws IOException {

    BufferedReader reader = new BufferedReader(new FileReader(path));
    _currentJourney = null;
    _currentLine = null;
    _currentLineCount = 0;
    _currentLineCharactersConsumed = 0;

    handler.startDocument();

    while ((_currentLine = reader.readLine()) != null) {
      _currentLineCharactersConsumed = 0;
      _currentLineCount++;
      if (_currentLineCount == 1) {
        parseHeader(handler);
      } else {
        parseLine(handler);
      }
    }
    closeCurrentJourneyIfNeeded(null, handler);
    handler.endDocument();
  }

  private void parseHeader(AtcoCifContentHandler handler) {
    String start = pop(8);
    if (!start.equals("ATCO-CIF")) {
      throw new AtcoCifException("Excepted feed header to start with ATCO-CIF");
    }
  }

  private void parseLine(AtcoCifContentHandler handler) {
    String typeValue = pop(2);
    AtcoCifElement.Type type = _typesByKey.get(typeValue);
    if (type == null) {
      throw new AtcoCifException("uknown record type: " + typeValue
          + " at line " + _currentLineCount);
    }
    switch (type) {
      case JOURNEY_HEADER:
        parseJourneyHeader(handler);
        break;
      case JOURNEY_DATE_RUNNING:
        parseJourneyDateRunning(handler);
        break;
      case JOURNEY_ORIGIN:
        parseJourneyOrigin(handler);
        break;
      case JOURNEY_INTERMEDIATE:
        parseJourneyIntermediate(handler);
        break;
      case JOURNEY_DESTINATION:
        parseJourneyDestination(handler);
        break;
      case LOCATION:
        parseLocation(handler);
        break;
      case ADDITIONAL_LOCATION:
        parseAdditionalLocation(handler);
        break;
      case VEHICLE_TYPE:
        parseVehicleType(handler);
        break;
      default:
        throw new AtcoCifException("unhandled record type: " + type);
    }

  }

  private void parseJourneyHeader(AtcoCifContentHandler handler) {
    JourneyHeaderElement element = new JourneyHeaderElement();

    String transactionType = pop(1);
    String operator = pop(4);
    element.setJourneyIdentifier(pop(6));
    element.setStartDate(serviceDate(pop(8)));
    element.setEndDate(serviceDate(pop(8)));
    element.setMonday(integer(pop(1)));
    element.setTuesday(integer(pop(1)));
    element.setWednesday(integer(pop(1)));
    element.setThursday(integer(pop(1)));
    element.setFriday(integer(pop(1)));
    element.setSaturday(integer(pop(1)));
    element.setSunday(integer(pop(1)));

    String schoolTermTime = pop(1);
    String bankHolidays = pop(1);
    element.setRouteIdentifier(pop(4));
    String runningBoard = pop(6);

    element.setVehicleType(pop(8));

    String registrationNumber = pop(8);
    String routeDirection = pop(1);

    closeCurrentJourneyIfNeeded(element, handler);
    _currentJourney = element;
    handler.startElement(element);
  }

  private void parseJourneyDateRunning(AtcoCifContentHandler handler) {
    JourneyDateRunningElement element = new JourneyDateRunningElement();
    element.setStartDate(serviceDate(pop(8)));
    element.setEndDate(serviceDate(pop(8)));
    element.setOperationCode(integer(pop(1)));
    if (_currentJourney == null)
      throw new AtcoCifException("journey timepoint without header at line "
          + _currentLineCount);
    _currentJourney.getCalendarModifications().add(element);
    fireElement(element, handler);

  }

  private void parseJourneyOrigin(AtcoCifContentHandler handler) {
    JourneyOriginElement element = new JourneyOriginElement();
    element.setLocationId(pop(12));
    element.setDepartureTime(time(pop(4)));
    pushTimepointElement(element, handler);
  }

  private void parseJourneyIntermediate(AtcoCifContentHandler handler) {
    JourneyIntermediateElement element = new JourneyIntermediateElement();
    element.setLocationId(pop(12));
    element.setArrivalTime(time(pop(4)));
    element.setDepartureTime(time(pop(4)));
    pushTimepointElement(element, handler);
  }

  private void parseJourneyDestination(AtcoCifContentHandler handler) {
    JourneyDestinationElement element = new JourneyDestinationElement();
    element.setLocationId(pop(12));
    element.setArrivalTime(time(pop(4)));
    pushTimepointElement(element, handler);
  }

  private void pushTimepointElement(JourneyTimePointElement element,
      AtcoCifContentHandler handler) {
    if (_currentJourney == null)
      throw new AtcoCifException("journey timepoint without header at line "
          + _currentLineCount);
    element.setHeader(_currentJourney);
    _currentJourney.getTimePoints().add(element);
    fireElement(element, handler);
  }

  private void parseLocation(AtcoCifContentHandler handler) {
    LocationElement element = new LocationElement();
    String transactionType = pop(1);
    element.setLocationId(pop(12));
    element.setName(pop(48));
    fireElement(element, handler);
  }

  private void parseAdditionalLocation(AtcoCifContentHandler handler) {
    AdditionalLocationElement element = new AdditionalLocationElement();
    String transactionType = pop(1);
    element.setLocationId(pop(12));
    long x = Long.parseLong(pop(8));
    long y = Long.parseLong(pop(8));
    Point2D.Double from = new Point2D.Double(x, y);
    Point2D.Double result = new Point2D.Double();
    result = _fromProjection.inverseTransform(from, result);
    // from = _toProjection.transform(result, from);
    element.setLat(result.y);
    element.setLon(result.x);
    fireElement(element, handler);
  }

  private void parseVehicleType(AtcoCifContentHandler handler) {
    VehicleTypeElement element = new VehicleTypeElement();
    pop(1);
    element.setId(pop(8));
    element.setDescription(pop(24));
    fireElement(element, handler);
  }

  private void fireElement(AtcoCifElement element, AtcoCifContentHandler handler) {
    closeCurrentJourneyIfNeeded(element, handler);
    handler.startElement(element);
    handler.endElement(element);
  }

  private void closeCurrentJourneyIfNeeded(AtcoCifElement element,
      AtcoCifContentHandler handler) {
    if ((element == null || !(element instanceof JourneyChildElement))
        && _currentJourney != null) {
      handler.endElement(_currentJourney);
      _currentJourney = null;
    }
  }

  private ServiceDate serviceDate(String value) {
    try {
      ServiceDate serviceDate = ServiceDate.parseString(value);
      Date date = serviceDate.getAsDate();
      if (date.after(_maxServiceDate)) {
        serviceDate = new ServiceDate(_maxServiceDate);
      }
      return serviceDate;
    } catch (ParseException e) {
      throw new AtcoCifException("error parsing service date \"" + value
          + "\" at line " + _currentLineCount, e);
    }
  }

  private int time(String pop) {
    int hour = integer(pop.substring(0, 2));
    int min = integer(pop.substring(2));
    return hour * 60 + min;
  }

  private int integer(String value) {
    return Integer.parseInt(value);
  }

  private String pop(int count) {
    if (_currentLine.length() < count) {
      throw new AtcoCifException("expected line " + _currentLineCount
          + " to have length of at least "
          + (_currentLineCharactersConsumed + count) + " but only found "
          + (_currentLineCharactersConsumed + _currentLine.length()));
    }
    String value = _currentLine.substring(0, count);
    _currentLine = _currentLine.substring(count);
    _currentLineCharactersConsumed += count;
    return value.trim();
  }
}
