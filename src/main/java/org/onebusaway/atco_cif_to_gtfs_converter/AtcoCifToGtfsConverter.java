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

  private File _inputPath;

  private File _outputPath;

  private String _agencyId = "1";

  private String _agencyName = "Agency Name";

  private String _agencyTimezone = "Europe/London";

  private String _agencyLang = "en";

  private String _agencyUrl = "http://agency.gov/";

  private String _agencyPhone = "";

  private Map<String, JourneyHeaderElement> _journeysById = new HashMap<String, JourneyHeaderElement>();

  private Map<String, LocationElement> _locationById = new HashMap<String, LocationElement>();

  private Map<String, AdditionalLocationElement> _additionalLocationById = new HashMap<String, AdditionalLocationElement>();

  private Map<String, VehicleTypeElement> _vehicleTypesById = new HashMap<String, VehicleTypeElement>();

  private Map<String, String> _serviceDateModificationSuffixByKey = new HashMap<String, String>();

  private GtfsRelationalDaoImpl _dao = new GtfsRelationalDaoImpl();

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
      _log.info("parsing file: " + path);
      parser.parse(path, handler);
    }

    constructGtfs();
    writeGtfs();
  }

  private void constructGtfs() {
    constructAgency();
    constructStops();
    constructTrips();
  }

  private void constructAgency() {
    Agency agency = new Agency();
    agency.setId(_agencyId);
    agency.setName(_agencyName);
    agency.setUrl(_agencyUrl);
    agency.setLang(_agencyLang);
    agency.setTimezone(_agencyTimezone);
    agency.setPhone(_agencyPhone);
    _dao.saveEntity(agency);
  }

  private void constructStops() {
    for (Map.Entry<String, LocationElement> entry : _locationById.entrySet()) {
      String locationId = entry.getKey();
      LocationElement location = entry.getValue();
      AdditionalLocationElement additionalLocation = _additionalLocationById.get(locationId);
      if (additionalLocation == null) {
        throw new AtcoCifException("found location with id=" + locationId
            + " but no additional location information found");
      }

      Stop stop = new Stop();
      stop.setId(id(locationId));
      stop.setName(location.getName());
      stop.setLat(additionalLocation.getLat());
      stop.setLon(additionalLocation.getLon());
      _dao.saveEntity(stop);
    }
  }

  private void constructTrips() {
    for (JourneyHeaderElement journey : _journeysById.values()) {
      Trip trip = new Trip();
      trip.setId(id(journey.getJourneyIdentifier()));
      trip.setRoute(getRouteForJourney(journey));
      trip.setServiceId(getServiceIdForJourney(journey));
      constructTimepoints(journey, trip);
      _dao.saveEntity(trip);
    }
  }

  private Route getRouteForJourney(JourneyHeaderElement journey) {
    AgencyAndId routeId = id(journey.getRouteIdentifier());
    Route route = _dao.getRouteForId(routeId);
    if (route == null) {
      route = new Route();
      route.setAgency(_dao.getAgencyForId(_agencyId));
      route.setId(routeId);
      route.setShortName(routeId.getId());
      route.setType(getRouteTypeForJourney(journey));
      _dao.saveEntity(route);
    }
    return route;
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
    } else {
      throw new AtcoCifException("unhandled vehicle type description: " + desc);
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
    for (JourneyTimePointElement timePoint : journey.getTimePoints()) {
      AgencyAndId stopId = id(timePoint.getLocationId());
      Stop stop = _dao.getStopForId(stopId);
      if (stop == null) {
        throw new AtcoCifException("no stop found with id " + stopId.getId());
      }
      StopTime stopTime = new StopTime();
      stopTime.setTrip(trip);
      stopTime.setStop(stop);
      stopTime.setArrivalTime(timePoint.getArrivalTimeInSeconds());
      stopTime.setStopSequence(_dao.getAllStopTimes().size());
      _dao.saveEntity(stopTime);
    }
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
    if (path.isDirectory()) {
      for (File subPath : path.listFiles()) {
        getApplicableFiles(subPath, applicableFiles);
      }
    } else if (path.getName().toLowerCase().endsWith(".cif")) {
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
        JourneyHeaderElement existing = _journeysById.put(
            journey.getJourneyIdentifier(), journey);
        if (existing != null) {
          throw new AtcoCifException("duplicate journey id "
              + journey.getJourneyIdentifier());
        }
      } else if (element instanceof LocationElement) {
        LocationElement location = (LocationElement) element;
        LocationElement existing = _locationById.put(location.getLocationId(),
            location);
        // if (existing != null) {
        // throw new AtcoCifException("duplicate location id "
        // + location.getLocationId());
        // }
      } else if (element instanceof AdditionalLocationElement) {
        AdditionalLocationElement location = (AdditionalLocationElement) element;
        AdditionalLocationElement existing = _additionalLocationById.put(
            location.getLocationId(), location);
        // if (existing != null) {
        // throw new AtcoCifException("duplicate additional location id "
        // + location.getLocationId());
        // }
      } else if (element instanceof VehicleTypeElement) {
        VehicleTypeElement vehicle = (VehicleTypeElement) element;
        _vehicleTypesById.put(vehicle.getId(), vehicle);
      }
    }

    @Override
    public void endElement(AtcoCifElement element) {

    }
  }
}
