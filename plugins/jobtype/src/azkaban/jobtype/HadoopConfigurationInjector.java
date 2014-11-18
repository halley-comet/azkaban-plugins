/*
 * Copyright 2014 LinkedIn Corp.
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
package azkaban.jobtype;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import org.apache.hadoop.conf.Configuration;
import org.apache.log4j.Logger;

import azkaban.utils.Props;

/**
 * HadoopConfigurationInjector is responsible for inserting links back to the
 * Azkaban UI in configurations. It is assumed that the necessary links have
 * already been loaded into the properties. After writing the necessary links as
 * a xml file as required by Hadoop's configuration, clients may add the links
 * as a default resource using injectLinks() so that they are included in any
 * Configuration constructed
 */
public class HadoopConfigurationInjector {
  private static Logger _logger = Logger.getLogger(HadoopConfigurationInjector.class);
  /**
   * To be called by the forked process to load the generated links
   */
  public static void injectLinks() {
    Configuration.addDefaultResource("azkaban-links.xml");
  }

  public static String getPath(Props jobProps, String workingDir) {
    return new File(workingDir, getDirName(jobProps)).toString();
  }

  /**
   * Write out links to a xml file so that they may be loaded by a client as a
   * configuration resource
   * 
   * @param props
   * @param workingDir
   */
  public static void prepareLinks(Props props, String workingDir) {
    try {
      File file = getLinkFile(props, workingDir);
      Configuration conf = new Configuration(false);
      // These are equivalent to
      // CommonJobProperties.[EXECUTION,WORKFLOW,JOB,JOBEXEC,ATTEMPT]_LINK
      // respectively, but we use literals for backwards compatibility.
      loadProp(props, conf, "azkaban.link.execution.url");
      loadProp(props, conf, "azkaban.link.workflow.url");
      loadProp(props, conf, "azkaban.link.job.url");
      loadProp(props, conf, "azkaban.link.jobexec.url");
      loadProp(props, conf, "azkaban.link.attempt.url");

      loadProp(props, conf, "azkaban.job.outnodes");
      loadProp(props, conf, "azkaban.job.innodes");

      OutputStream xmlOut = new FileOutputStream(file);
      conf.writeXml(xmlOut);
      xmlOut.close();
    } catch (Throwable e) {
      _logger.error("Encountered error while preparing links", e);
    }
  }

  /**
   * Resolve the location of the file containing the link configuration file
   */
  public static File getLinkFile(Props props, String workingDir) {
    File jobDir = new File(workingDir, getDirName(props));
    if (!jobDir.exists()) {
      jobDir.mkdir();
    }

    return new File(jobDir, "azkaban-links.xml");
  }

  /**
   * For classpath reasons, we'll put each link file in a separate directory.
   * This must be called only after the job id has been inserted by the Job
   * 
   * @param workingDir
   * @param jobProps
   * @return
   */
  public static String getDirName(Props jobProps) {
    String dirSuffix = jobProps.get("azkaban.flow.nested.path");

    if ((dirSuffix == null) || (dirSuffix.length() == 0)) {
      dirSuffix = jobProps.get("azkaban.job.id");
      if ((dirSuffix == null) || (dirSuffix.length() == 0)) {
        throw new RuntimeException("azkaban.flow.nested.path and azkaban.job.id were not set");
      }
    }

    return "_link_" + dirSuffix;
  }

  public static void loadProp(Props props, Configuration conf, String name) {
    String prop = props.get(name);
    if (prop != null) {
      conf.set(name, prop);
    }
  }
}
