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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.Parser;
import org.apache.commons.cli.PosixParser;

public class AtcoCifToGtfsConverterMain {

  private static final String ARG_AGENCY_ID = "agencyId";

  private static final String ARG_AGENCY_LANG = "agencyLang";

  private static final String ARG_AGENCY_NAME = "agencyName";

  private static final String ARG_AGENCY_PHONE = "agencyPhone";

  private static final String ARG_AGENCY_TIMEZONE = "agencyTimezone";

  private static final String ARG_AGENCY_URL = "agencyUrl";

  public static void main(String[] args) throws ParseException, IOException {
    AtcoCifToGtfsConverterMain m = new AtcoCifToGtfsConverterMain();
    m.run(args);
  }

  public void run(String[] args) throws ParseException, IOException {
    Options options = new Options();
    buildOptions(options);

    Parser parser = new PosixParser();
    CommandLine cli = parser.parse(options, args);
    args = cli.getArgs();

    if (args.length != 2) {
      usage();
      System.exit(-1);
    }

    AtcoCifToGtfsConverter converter = new AtcoCifToGtfsConverter();
    configureConverter(args, cli, converter);

    converter.run();
  }

  private void configureConverter(String[] args, CommandLine cli,
      AtcoCifToGtfsConverter converter) {

    converter.setInputPath(new File(args[0]));
    converter.setOutputPath(new File(args[1]));

    if (cli.hasOption(ARG_AGENCY_ID)) {
      converter.setAgencyId(cli.getOptionValue(ARG_AGENCY_ID));
    }
    if (cli.hasOption(ARG_AGENCY_LANG)) {
      converter.setAgencyLang(cli.getOptionValue(ARG_AGENCY_LANG));
    }
    if (cli.hasOption(ARG_AGENCY_NAME)) {
      converter.setAgencyName(cli.getOptionValue(ARG_AGENCY_NAME));
    }
    if (cli.hasOption(ARG_AGENCY_PHONE)) {
      converter.setAgencyPhone(cli.getOptionValue(ARG_AGENCY_PHONE));
    }
    if (cli.hasOption(ARG_AGENCY_TIMEZONE)) {
      converter.setAgencyTimezone(cli.getOptionValue(ARG_AGENCY_TIMEZONE));
    }
    if (cli.hasOption(ARG_AGENCY_URL)) {
      converter.setAgencyUrl(cli.getOptionValue(ARG_AGENCY_URL));
    }
  }

  protected void buildOptions(Options options) {
    options.addOption(ARG_AGENCY_ID, true, "agency id");
    options.addOption(ARG_AGENCY_LANG, true, "agency lang");
    options.addOption(ARG_AGENCY_NAME, true, "agency name");
    options.addOption(ARG_AGENCY_PHONE, true, "agency phone");
    options.addOption(ARG_AGENCY_TIMEZONE, true, "agency timezone");
    options.addOption(ARG_AGENCY_URL, true, "agency url");
  }

  private void usage() throws IOException {
    InputStream in = getClass().getResourceAsStream("usage.txt");
    BufferedReader reader = new BufferedReader( new InputStreamReader(in));
    String line = null;
    while((line = reader.readLine()) != null){
      System.err.println(line);
    }
    reader.close();
  }
}
