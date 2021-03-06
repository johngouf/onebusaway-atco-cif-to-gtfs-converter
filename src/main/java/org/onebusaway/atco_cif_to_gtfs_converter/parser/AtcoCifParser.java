/**
 * Copyright (C) 2012 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onebusaway.atco_cif_to_gtfs_converter.parser;

import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jhlabs.map.proj.CoordinateSystemToCoordinateSystem;
import com.jhlabs.map.proj.Projection;
import com.jhlabs.map.proj.ProjectionException;
import com.jhlabs.map.proj.ProjectionFactory;
import java.util.*;

public class AtcoCifParser {

    private static Logger _log = LoggerFactory.getLogger(AtcoCifParser.class);
    private static final byte[] UTF8_BOM = {(byte) 0xef, (byte) 0xbb, (byte) 0xbf};
    private static Map<String, AtcoCifElement.Type> _typesByKey = new HashMap<String, AtcoCifElement.Type>();
    private List<JourneyTimePointElement> elements = new ArrayList<JourneyTimePointElement>();

    static {
        _typesByKey.put("QS", AtcoCifElement.Type.JOURNEY_HEADER);
        _typesByKey.put("QE", AtcoCifElement.Type.JOURNEY_DATE_RUNNING);
        _typesByKey.put("QO", AtcoCifElement.Type.JOURNEY_ORIGIN);
        _typesByKey.put("QI", AtcoCifElement.Type.JOURNEY_INTERMEDIATE);
        _typesByKey.put("QT", AtcoCifElement.Type.JOURNEY_DESTINATION);
        _typesByKey.put("QL", AtcoCifElement.Type.LOCATION);
        _typesByKey.put("QB", AtcoCifElement.Type.ADDITIONAL_LOCATION);
        _typesByKey.put("QV", AtcoCifElement.Type.VEHICLE_TYPE);
        _typesByKey.put("QC", AtcoCifElement.Type.UNKNOWN);
        _typesByKey.put("QP", AtcoCifElement.Type.OPERATOR);
        _typesByKey.put("QQ", AtcoCifElement.Type.UNKNOWN);
        _typesByKey.put("QJ", AtcoCifElement.Type.UNKNOWN);
        _typesByKey.put("QD", AtcoCifElement.Type.ROUTE_DESCRIPTION);
        _typesByKey.put("QY", AtcoCifElement.Type.UNKNOWN);
        _typesByKey.put("ZM", AtcoCifElement.Type.UNKNOWN);
        _typesByKey.put("ZS", AtcoCifElement.Type.UNKNOWN);
    }
    private static final String _fromProjectionSpec = "+proj=tmerc +lat_0=49 +lon_0=-2 +k=0.9996012717 +x_0=400000 "
            + "+y_0=-100000 +ellps=airy +towgs84=446.448,-125.157,542.060,0.1502,0.2470,0.8421,-20.4894 +datum=OSGB36  +units=m +no_defs";
    private static final Projection _fromProjection = ProjectionFactory.fromPROJ4Specification(_fromProjectionSpec.split(" "));
    private static final String _toProjectionSpec = "+proj=latlong +ellps=WGS84 +datum=WGS84 +no_defs";
    private static final Projection _toProjection = ProjectionFactory.fromPROJ4Specification(_toProjectionSpec.split(" "));
    private File _currentPath = null;
    private int _currentLineNumber = 0;
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

        BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(path), "UTF-8"));
        _currentPath = path;
        _currentJourney = null;
        _currentLine = null;
        _currentLineNumber = 0;
        _currentLineCharactersConsumed = 0;

        handler.startDocument();

        while ((_currentLine = reader.readLine()) != null) {

            _currentLineCharactersConsumed = 0;
            _currentLineNumber++;
            if (_currentLineNumber == 1) {
                parseHeader(handler);
            } else {
                parseLine(handler);
            }
        }
        closeCurrentJourneyIfNeeded(null, handler);
        handler.endDocument();
    }

    private void parseHeader(AtcoCifContentHandler handler) {
        /**
         * Check for and strip the UTF-8 BOM
         */
        try {
            String prefix = peek(1);
            if (prefix.length() == 1
                    && Arrays.equals(prefix.getBytes("UTF-8"), UTF8_BOM)) {
                pop(1);
            }
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalStateException(ex);
        }

        String start = pop(8);
        if (!start.equals("ATCO-CIF")) {
            StringBuilder b = new StringBuilder();
            for (byte singleByte : start.getBytes()) {
                String hb = Integer.toHexString(0xff & singleByte);
                if (hb.length() < 2) {
                    hb += " ";
                }
                b.append(hb).append(" ");
            }
            throw new AtcoCifException(
                    "Excepted feed header to start with ATCO-CIF.  Instead, found \""
                    + start + "\" (" + b.toString() + ")");
        }
    }

    private void parseLine(AtcoCifContentHandler handler) {
        String typeValue = pop(2);
        AtcoCifElement.Type type = _typesByKey.get(typeValue);
        if (type == null) {
            throw new AtcoCifException("uknown record type: " + typeValue
                    + " at line " + _currentLineNumber);
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
            case ROUTE_DESCRIPTION:
                parseRouteDescription(handler);
                break;
            case OPERATOR:
                parseOperator(handler);
                break;
            case UNKNOWN:
                break;
            default:
                throw new AtcoCifException("unhandled record type: " + type);
        }

    }

    private void parseJourneyHeader(AtcoCifContentHandler handler) {
        JourneyHeaderElement element = element(new JourneyHeaderElement());

        String transactionType = pop(1);
        element.setOperatorId(pop(4));
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
        element.setRouteDirection(pop(1));

        closeCurrentJourneyIfNeeded(element, handler);
        _currentJourney = element;
        handler.startElement(element);
    }

    private void parseJourneyDateRunning(AtcoCifContentHandler handler) {
        JourneyDateRunningElement element = element(new JourneyDateRunningElement());
        element.setStartDate(serviceDate(pop(8)));
        element.setEndDate(serviceDate(pop(8)));
        element.setOperationCode(integer(pop(1)));
        if (_currentJourney == null) {
            throw new AtcoCifException("journey timepoint without header at line "
                    + _currentLineNumber);
        }
        _currentJourney.getCalendarModifications().add(element);
        fireElement(element, handler);

    }

    private void parseJourneyOrigin(AtcoCifContentHandler handler) {
        JourneyOriginElement element = element(new JourneyOriginElement());
        element.setLocationId(pop(12));
        String departString = pop(4);
        element.setDepartureString(departString);
        elements.add(element);
//        element.setDepartureTime(time(departTime));
//        pushTimepointElement(element, handler);
    }

    private void parseJourneyIntermediate(AtcoCifContentHandler handler) {
        JourneyIntermediateElement element = element(new JourneyIntermediateElement());
        element.setLocationId(pop(12));
        String arriveString = pop(4);
        String departString = pop(4);
        element.setDepartureString(departString);
        element.setArrivalString(arriveString);
        elements.add(element);
    }

    private void parseJourneyDestination(AtcoCifContentHandler handler) {
        JourneyDestinationElement element = element(new JourneyDestinationElement());
        element.setLocationId(pop(12));
        String arriveString = pop(4);
        element.setArrivalString(arriveString);
        elements.add(element);
        validateJourney(handler);
    }

    private void pushTimepointElement(JourneyTimePointElement element,
            AtcoCifContentHandler handler) {
        if (_currentJourney == null) {
            throw new AtcoCifException("journey timepoint without header at line "
                    + _currentLineNumber);
        }
        element.setHeader(_currentJourney);
        _currentJourney.getTimePoints().add(element);
        fireElement(element, handler);
    }

    private void parseLocation(AtcoCifContentHandler handler) {
        LocationElement element = element(new LocationElement());
        String transactionType = pop(1);
        element.setLocationId(pop(12));
        element.setName(pop(48));
        fireElement(element, handler);
    }

    private void parseAdditionalLocation(AtcoCifContentHandler handler) {
        AdditionalLocationElement element = element(new AdditionalLocationElement());
        String transactionType = pop(1);
        element.setLocationId(pop(12));

        String xValue = pop(8);
        String yValue = pop(8);
        Point2D.Double location = getLocation(xValue, yValue, true);
        if (location != null) {
            element.setLat(location.y);
            element.setLon(location.x);
        }
        fireElement(element, handler);
    }

    private Point2D.Double getLocation(String xValue, String yValue,
            boolean canStripSuffix) {

        if (xValue.isEmpty() && yValue.isEmpty()) {
            return null;
        }

        Point2D.Double from = null;

        try {
            double x = Long.parseLong(xValue);
            double y = Long.parseLong(yValue);
            from = new Point2D.Double(x, y);
        } catch (NumberFormatException ex) {
            throw new AtcoCifException("error parsing additional location: x="
                    + xValue + " y=" + yValue + " line=" + _currentLineNumber);
        }

        try {
            Point2D.Double result = new Point2D.Double();
            CoordinateSystemToCoordinateSystem.transform(_fromProjection,
                    _toProjection, from, result);
            return result;
        } catch (ProjectionException ex) {
            _log.warn("error projecting additional location: x=" + xValue + " y="
                    + yValue + " line=" + _currentLineNumber);
            return null;
        }
    }

    private void parseVehicleType(AtcoCifContentHandler handler) {
        VehicleTypeElement element = element(new VehicleTypeElement());
        pop(1);
        element.setId(pop(8));
        element.setDescription(pop(24));
        fireElement(element, handler);
    }

    private void parseRouteDescription(AtcoCifContentHandler handler) {
        RouteDescriptionElement element = element(new RouteDescriptionElement());
        pop(1);
        element.setOperatorId(pop(4));
        element.setRouteNumber(pop(4));
        element.setRouteDirection(pop(1));
        element.setRouteDescription(pop(68));
        fireElement(element, handler);
    }

    private void parseOperator(AtcoCifContentHandler handler) {
        OperatorElement element = element(new OperatorElement());
        pop(1);
        element.setOperatorId(pop(4));
        element.setShortFormName(pop(24));
        element.setLegalName(pop(48));
        element.setEnquiryPhone(pop(12));
        element.setContactPhone(pop(12));
        fireElement(element, handler);
    }

    private <T extends AtcoCifElement> T element(T element) {
        element.setPath(_currentPath);
        element.setLineNumber(_currentLineNumber);
        return element;
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
                    + "\" at line " + _currentLineNumber, e);
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
            throw new AtcoCifException("expected line " + _currentLineNumber
                    + " to have length of at least "
                    + (_currentLineCharactersConsumed + count) + " but only found "
                    + (_currentLineCharactersConsumed + _currentLine.length()));
        }
        String value = _currentLine.substring(0, count);
        _currentLine = _currentLine.substring(count);
        _currentLineCharactersConsumed += count;
        return value.trim();
    }

    private String peek(int count) {
        count = Math.min(count, _currentLine.length());
        return _currentLine.substring(0, count);
    }

    private void validateJourney(AtcoCifContentHandler handler) {
        List<JourneyTimePointElement> validated = new ArrayList<JourneyTimePointElement>();

        JourneyOriginElement origin = (JourneyOriginElement) elements.get(0);
        JourneyDestinationElement destination = (JourneyDestinationElement) elements.get(elements.size() - 1);

        int startTimeOrig = Integer.parseInt(origin.getDepartureString());

        validated.add(origin);

        for (int i = 1; i < elements.size() - 1; i++) {
            JourneyIntermediateElement intermElement = (JourneyIntermediateElement) elements.get(i);
            int arriveTime = Integer.parseInt(intermElement.getArrivalString());
            int departTime = Integer.parseInt(intermElement.getDepartureString());

            if (arriveTime == 0 && departTime == 0) {
                JourneyTimePointElement prevElement = validated.get(validated.size() - 1);

                if (prevElement instanceof JourneyOriginElement) {
                    if (validBefore00(startTimeOrig)) {
                        validated.add(intermElement);
                    }
                } else {
                    JourneyIntermediateElement prevInterElement = (JourneyIntermediateElement) prevElement;
                    int prevTime = Integer.parseInt(prevInterElement.getDepartureString());
                    if (validBefore00(prevTime)) {
                        validated.add(intermElement);
                    }
                }
            } else if (arriveTime == 0) {
                if (!validAfter00(departTime)) {
                    intermElement.setArrivalString(intermElement.getDepartureString());
                    validated.add(intermElement);
                } 
            } else if (departTime == 0) {
                if (!validBefore00(arriveTime)) {
                    intermElement.setDepartureString(intermElement.getArrivalString());
                    validated.add(intermElement);
                } 
            } else {
                validated.add(intermElement);
            }
        }
        
        origin.setDepartureTime(time(origin.getDepartureString()));
        pushTimepointElement(origin, handler);
        
        for(int i = 1; i < validated.size(); i++)
        {
          JourneyIntermediateElement intermElement = (JourneyIntermediateElement) validated.get(i);
          intermElement.setArrivalTime(time(intermElement.getArrivalString()));
          intermElement.setDepartureTime(time(intermElement.getDepartureString()));
          pushTimepointElement(intermElement, handler);
        }
        
        destination.setArrivalTime(time(destination.getArrivalString()));
        pushTimepointElement(destination, handler);

        elements = new ArrayList<JourneyTimePointElement>();
    }

    private static boolean validBefore00(int time) {
        if (time <= 2359 && time >= 2200) {
            return true;
        }
        return false;
    }

    private static boolean validAfter00(int time) {
        if (time <= 200) {
            return true;
        }
        return false;
    }
}