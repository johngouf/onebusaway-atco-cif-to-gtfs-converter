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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.Parser;
import org.apache.commons.cli.PosixParser;

public class AtcoCifToGtfsConverterMain {
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
    converter.setInputPath(new File(args[0]));
    converter.setOutputPath(new File(args[1]));
    converter.run();
  }

  protected void buildOptions(Options options) {

  }

  private void usage() {
    System.err.println("usage: input.cif gtfs_output.zip");
    System.err.println("usage: input_path gtfs_output.zip");
  }
}
