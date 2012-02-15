/**
 * Copyright (C) 2012 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onebusaway.atco_cif_to_gtfs_converter;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.onebusaway.atco_cif_to_gtfs_converter.parser.AdditionalLocationElement;
import org.onebusaway.atco_cif_to_gtfs_converter.parser.AtcoCifContentHandler;
import org.onebusaway.atco_cif_to_gtfs_converter.parser.AtcoCifElement;
import org.onebusaway.atco_cif_to_gtfs_converter.parser.AtcoCifException;
import org.onebusaway.atco_cif_to_gtfs_converter.parser.AtcoCifParser;
import org.onebusaway.atco_cif_to_gtfs_converter.parser.JourneyDateRunningElement;
import org.onebusaway.atco_cif_to_gtfs_converter.parser.JourneyHeaderElement;
import org.onebusaway.atco_cif_to_gtfs_converter.parser.JourneyTimePointElement;
import org.onebusaway.atco_cif_to_gtfs_converter.parser.LocationElement;
import org.onebusaway.atco_cif_to_gtfs_converter.parser.OperatorElement;
import org.onebusaway.atco_cif_to_gtfs_converter.parser.RouteDescriptionElement;
import org.onebusaway.atco_cif_to_gtfs_converter.parser.VehicleTypeElement;
import org.onebusaway.csv_entities.schema.DefaultEntitySchemaFactory;
import org.onebusaway.gtfs.impl.GtfsRelationalDaoImpl;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.ServiceCalendar;
import org.onebusaway.gtfs.model.ServiceCalendarDate;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.gtfs.serialization.GtfsEntitySchemaFactory;
import org.onebusaway.gtfs.serialization.GtfsWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AtcoCifToGtfsConverter {

  private static Logger _log = LoggerFactory.getLogger(AtcoCifParser.class);

  private static final int MINUTES_IN_DAY = 24 * 60;

  private File _inputPath;

  private File _outputPath;

  private String _agencyId = "1";

  private String _agencyName = "Agency Name";

  private String _agencyTimezone = "Europe/London";

  private String _agencyLang = "en";

  private String _agencyUrl = "http://agency.gov/";

  private String _agencyPhone = "";

  private int _vehicleType = -1;

  private Map<AgencyAndId, List<JourneyHeaderElement>> _journeysById = new HashMap<AgencyAndId, List<JourneyHeaderElement>>();

  private Map<String, LocationElement> _locationById = new HashMap<String, LocationElement>();

  private Map<String, AdditionalLocationElement> _additionalLocationById = new HashMap<String, AdditionalLocationElement>();

  private Map<String, VehicleTypeElement> _vehicleTypesById = new HashMap<String, VehicleTypeElement>();

  private Map<AgencyAndId, Map<String, RouteDescriptionElement>> _routeDescriptionsByIdAndDirection = new HashMap<AgencyAndId, Map<String, RouteDescriptionElement>>();

  private Map<String, OperatorElement> _operatorsById = new HashMap<String, OperatorElement>();

  private Map<String, String> _serviceDateModificationSuffixByKey = new HashMap<String, String>();

  private GtfsRelationalDaoImpl _dao = new GtfsRelationalDaoImpl();

  public void setInputPath(File inputPath) {
    _inputPath = inputPath;
  }

  public void setOutputPath(File outputPath) {
    _outputPath = outputPath;
  }

  public void setAgencyId(String agencyId) {
    _agencyId = agencyId;
  }

  public void setAgencyName(String agencyName) {
    _agencyName = agencyName;
  }

  public void setAgencyUrl(String agencyUrl) {
    _agencyUrl = agencyUrl;
  }

  public void setAgencyPhone(String agencyPhone) {
    _agencyPhone = agencyPhone;
  }

  public void setAgencyTimezone(String agencyTimezone) {
    _agencyTimezone = agencyTimezone;
  }

  public void setAgencyLang(String agencyLang) {
    _agencyLang = agencyLang;
  }
  
  public void setVehicleType(int vehicleType) {
    _vehicleType = vehicleType;
  }

  public void run() throws IOException {

    _log.info("Input path: " + _inputPath);
    _log.info("Output path: " + _outputPath);

    List<File> paths = new ArrayList<File>();
    getApplicableFiles(_inputPath, paths);

    if (paths.isEmpty()) {
      _log.error("No applicable input files were found!");
      System.exit(-1);
    }

    AtcoCifParser parser = new AtcoCifParser();
    HandlerImpl handler = new HandlerImpl();
    for (File path : paths) {
      _log.info("parsing file: " + path);
      parser.parse(path, handler);
    }

    constructGtfs();
    writeGtfs();
  }

  private void constructGtfs() {
    constructTrips();
  }

  private void constructTrips() {
    for (List<JourneyHeaderElement> journies : _journeysById.values()) {
      for (int i = 0; i < journies.size(); ++i) {
        JourneyHeaderElement journey = journies.get(i);
        Trip trip = new Trip();
        String id = journey.getOperatorId() + "-"
            + journey.getJourneyIdentifier();
        if (journies.size() > 1) {
          id += "-" + i;
        }
        trip.setId(new AgencyAndId(journey.getOperatorId(), id));
        trip.setRoute(getRouteForJourney(journey));
        trip.setServiceId(getServiceIdForJourney(journey));

        AgencyAndId routeId = trip.getRoute().getId();
        Map<String, RouteDescriptionElement> routeDescriptions = _routeDescriptionsByIdAndDirection.get(routeId);
        if (routeDescriptions != null) {
          RouteDescriptionElement routeDescription = routeDescriptions.get(journey.getRouteDirection());
          if (routeDescription != null) {
            trip.setTripHeadsign(routeDescription.getRouteDescription());
          }
        }

        constructTimepoints(journey, trip);
        _dao.saveEntity(trip);
      }
    }
  }

  private Route getRouteForJourney(JourneyHeaderElement journey) {
    String operatorId = journey.getOperatorId();
    AgencyAndId routeId = new AgencyAndId(operatorId, operatorId + "-"
        + journey.getRouteIdentifier());
    Route route = _dao.getRouteForId(routeId);
    if (route == null) {
      route = new Route();
      route.setAgency(getAgencyForId(operatorId));
      route.setId(routeId);
      route.setShortName(routeId.getId());
      route.setType(getRouteTypeForJourney(journey));
      _dao.saveEntity(route);
    }
    return route;
  }

  private Agency getAgencyForId(String id) {
    Agency agency = _dao.getAgencyForId(id);
    if (agency == null) {
      agency = new Agency();
      agency.setId(id);

      OperatorElement operator = _operatorsById.get(id);
      agency.setTimezone(_agencyTimezone);
      agency.setUrl(_agencyUrl);
      agency.setLang(_agencyLang);

      if (operator != null) {
        agency.setName(operator.getShortFormName());
        agency.setPhone(operator.getEnquiryPhone());
      } else {
        agency.setName(_agencyName);
        agency.setPhone(_agencyPhone);
      }
      _dao.saveEntity(agency);
    }
    return agency;
  }

  private int getRouteTypeForJourney(JourneyHeaderElement journey) {
    VehicleTypeElement vehicleType = _vehicleTypesById.get(journey.getVehicleType());
    if (vehicleType == null) {
      throw new AtcoCifException("unknown vehicle type: " + vehicleType);
    }
    String desc = vehicleType.getDescription().toLowerCase();
    if (desc.equals("bus") || desc.equals("coach")) {
      return 3;
    } else if (desc.equals("heavy rail")) {
      return 2;
    } else if (_vehicleType != -1) {
      return _vehicleType;
    } else {
      throw new AtcoCifException(
          "no defautl VehicleType specified and could not determine GTFS route vehicle type from ATCO-CIF vehicle type description: "
              + desc);
    }
  }

  private AgencyAndId getServiceIdForJourney(JourneyHeaderElement journey) {
    AgencyAndId serviceId = constructServiceIdForJourney(journey);
    ServiceCalendar calendar = _dao.getCalendarForServiceId(serviceId);
    if (calendar == null) {
      calendar = new ServiceCalendar();
      calendar.setServiceId(serviceId);
      calendar.setStartDate(journey.getStartDate());
      calendar.setEndDate(journey.getEndDate());
      calendar.setMonday(journey.getMonday());
      calendar.setTuesday(journey.getTuesday());
      calendar.setWednesday(journey.getWednesday());
      calendar.setThursday(journey.getThursday());
      calendar.setFriday(journey.getFriday());
      calendar.setSaturday(journey.getSaturday());
      calendar.setSunday(journey.getSunday());
      _dao.saveEntity(calendar);

      for (JourneyDateRunningElement modification : journey.getCalendarModifications()) {
        Date startDate = modification.getStartDate().getAsDate();
        Date endDate = modification.getEndDate().getAsDate();

        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTime(startDate);

        int exceptionType = modification.getOperationCode() == 1 ? 1 : 2;

        while (true) {
          Date date = c.getTime();
          if (date.after(endDate))
            break;

          ServiceCalendarDate calendarDate = new ServiceCalendarDate();
          calendarDate.setServiceId(serviceId);
          calendarDate.setDate(new ServiceDate(date));
          calendarDate.setExceptionType(exceptionType);
          _dao.saveEntity(calendarDate);

          c.add(Calendar.DAY_OF_YEAR, 1);
        }
      }
      _dao.clearAllCaches();
    }
    return serviceId;
  }

  private AgencyAndId constructServiceIdForJourney(JourneyHeaderElement journey) {
    StringBuilder b = new StringBuilder();
    b.append(journey.getStartDate().getAsString());
    b.append('-');
    b.append(journey.getEndDate().getAsString());
    b.append('-');
    b.append(journey.getSunday() == 1 ? "S" : "_");
    b.append(journey.getMonday() == 1 ? "M" : "_");
    b.append(journey.getTuesday() == 1 ? "T" : "_");
    b.append(journey.getWednesday() == 1 ? "W" : "_");
    b.append(journey.getThursday() == 1 ? "H" : "_");
    b.append(journey.getFriday() == 1 ? "F" : "_");
    b.append(journey.getSaturday() == 1 ? "S" : "_");
    b.append('-');
    b.append(getServiceDateModificationsSuffix(journey));
    return id(b.toString());
  }

  private String getServiceDateModificationsSuffix(JourneyHeaderElement journey) {
    List<JourneyDateRunningElement> modifications = journey.getCalendarModifications();
    if (modifications.isEmpty()) {
      return "00";
    }

    StringBuilder b = new StringBuilder();
    Collections.sort(modifications);
    for (JourneyDateRunningElement modification : modifications) {
      b.append('|');
      b.append(modification.getStartDate().getAsString());
      b.append('-');
      b.append(modification.getEndDate().getAsString());
      b.append('-');
      b.append(modification.getOperationCode());
    }
    String key = b.toString();
    String suffix = _serviceDateModificationSuffixByKey.get(key);
    if (suffix == null) {
      DecimalFormat format = new DecimalFormat("00");
      suffix = format.format(_serviceDateModificationSuffixByKey.size() + 1);
      _serviceDateModificationSuffixByKey.put(key, suffix);
    }
    return suffix;
  }

  private void constructTimepoints(JourneyHeaderElement journey, Trip trip) {

    normalizeTimes(journey);

    boolean first = true;

    for (JourneyTimePointElement timePoint : journey.getTimePoints()) {
      String stopId = timePoint.getLocationId();
      Stop stop = findStop(stopId);
      if (stop == null) {
        throw new AtcoCifException("no stop found with id " + stopId);
      }
      StopTime stopTime = new StopTime();
      stopTime.setTrip(trip);
      stopTime.setStop(stop);
      if (timePoint.getArrivalTime() != 0 || timePoint.getDepartureTime() != 0
          || first) {
        stopTime.setArrivalTime(timePoint.getArrivalTime() * 60);
        stopTime.setDepartureTime(timePoint.getDepartureTime() * 60);
      }

      stopTime.setStopSequence(_dao.getAllStopTimes().size());
      _dao.saveEntity(stopTime);
      first = false;
    }
  }

  private void normalizeTimes(JourneyHeaderElement journey) {
    List<JourneyTimePointElement> timepoints = journey.getTimePoints();
    if (timepoints.isEmpty()) {
      return;
    }

    int prevDepartureTime = -1;
    int dayOffset = 0;

    for (int i = 0; i < timepoints.size(); ++i) {
      JourneyTimePointElement timepoint = timepoints.get(i);
      int arrivalTime = timepoint.getArrivalTime();
      int departureTime = timepoint.getDepartureTime();

      arrivalTime += dayOffset * MINUTES_IN_DAY;
      while (arrivalTime < prevDepartureTime) {
        arrivalTime += MINUTES_IN_DAY;
        dayOffset++;
      }

      departureTime += dayOffset * MINUTES_IN_DAY;
      while (departureTime < arrivalTime) {
        departureTime += MINUTES_IN_DAY;
        dayOffset++;
      }

      timepoint.setArrivalTime(arrivalTime);
      timepoint.setDepartureTime(departureTime);
      prevDepartureTime = departureTime;
    }
  }

  private Stop findStop(String stopId) {
    LocationElement location = getLocationForId(stopId);
    if (location == null) {
      return null;
    }
    String locationId = location.getLocationId();
    AgencyAndId id = id(locationId);
    Stop stop = _dao.getStopForId(id);
    if (stop == null) {
      AdditionalLocationElement additionalLocation = _additionalLocationById.get(locationId);
      if (additionalLocation == null) {
        throw new AtcoCifException("found location with id=" + locationId
            + " but no additional location information found");
      }

      stop = new Stop();
      stop.setId(id(locationId));
      stop.setName(location.getName());
      stop.setLat(additionalLocation.getLat());
      stop.setLon(additionalLocation.getLon());

      if (additionalLocation.getLat() == 0.0
          || additionalLocation.getLon() == 0.0) {
        _log.info("stop with no location: " + locationId);
      }
      _dao.saveEntity(stop);
    }
    return stop;
  }

  private LocationElement getLocationForId(String stopId) {
    LocationElement location = _locationById.get(stopId);
    /**
     * I've noticed a strange case where a journey references a stop with an id
     * "blahX" when only a stop with id "blah" exists.
     */
    if (location == null) {
      if (stopId.length() > 1) {
        stopId = stopId.substring(0, stopId.length() - 1);
        location = _locationById.get(stopId);
      }
    }
    return location;
  }

  private void writeGtfs() throws IOException {
    GtfsWriter writer = new GtfsWriter();
    writer.setOutputLocation(_outputPath);
    DefaultEntitySchemaFactory schemaFactory = new DefaultEntitySchemaFactory();
    schemaFactory.addFactory(GtfsEntitySchemaFactory.createEntitySchemaFactory());
    writer.setEntitySchemaFactory(schemaFactory);
    writer.run(_dao);
  }

  private void getApplicableFiles(File path, List<File> applicableFiles) {
    _log.info("Scanning path: " + path);
    if (path.isDirectory()) {
      _log.info("Directory found...");
      for (File subPath : path.listFiles()) {
        getApplicableFiles(subPath, applicableFiles);
      }
    } else if (path.getName().toLowerCase().endsWith(".cif")) {
      _log.info("CIF File found!");
      applicableFiles.add(path);
    }
  }

  private AgencyAndId id(String id) {
    return new AgencyAndId(_agencyId, id);
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
        AgencyAndId journeyId = new AgencyAndId(journey.getOperatorId(),
            journey.getJourneyIdentifier());
        List<JourneyHeaderElement> journies = _journeysById.get(journeyId);
        if (journies == null) {
          journies = new ArrayList<JourneyHeaderElement>();
          _journeysById.put(journeyId, journies);
        }
        journies.add(journey);
      } else if (element instanceof LocationElement) {
        LocationElement location = (LocationElement) element;
        _locationById.put(location.getLocationId(), location);
      } else if (element instanceof AdditionalLocationElement) {
        AdditionalLocationElement location = (AdditionalLocationElement) element;
        _additionalLocationById.put(location.getLocationId(), location);
      } else if (element instanceof VehicleTypeElement) {
        VehicleTypeElement vehicle = (VehicleTypeElement) element;
        _vehicleTypesById.put(vehicle.getId(), vehicle);
      } else if (element instanceof RouteDescriptionElement) {
        RouteDescriptionElement route = (RouteDescriptionElement) element;
        AgencyAndId id = new AgencyAndId(route.getOperatorId(),
            route.getRouteNumber());
        Map<String, RouteDescriptionElement> byDirection = _routeDescriptionsByIdAndDirection.get(id);
        if (byDirection == null) {
          byDirection = new HashMap<String, RouteDescriptionElement>();
          _routeDescriptionsByIdAndDirection.put(id, byDirection);
        }
        RouteDescriptionElement existing = byDirection.put(
            route.getRouteDirection(), route);
        if (existing != null) {
          System.out.println(existing);
        }
      } else if (element instanceof OperatorElement) {
        OperatorElement operator = (OperatorElement) element;
        OperatorElement existing = _operatorsById.put(operator.getOperatorId(),
            operator);
        if (existing != null) {
          _log.info("!");
        }
      }

    }

    @Override
    public void endElement(AtcoCifElement element) {

    }
  }
}
