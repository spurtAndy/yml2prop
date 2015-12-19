package github.com.phuonghuynh.yml2prop;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.*;

/**
 * Read and transfer YAML document into Properties
 *
 * @author phuonghqh
 */
@Mojo(name = "run")
public class Yml2Prop extends AbstractMojo {

  @Parameter(required = true)
  private String sourceYaml;

  @Parameter(required = true)
  private List<String> entries;

  @Parameter(defaultValue = "${basedir}/src/main/resources/application.properties")
  private String destProp;

  /**
   * Run yaml processing
   *
   * @return
   * @throws {@link MojoExecutionException}
   * @throws {@link MojoFailureException}
   */
  @SuppressWarnings("unchecked")
  public void execute() throws MojoExecutionException, MojoFailureException {
    try {
      Yaml yaml = new Yaml();
      Map<String, Object> map = (Map<String, Object>) yaml.load(new FileInputStream(new File(sourceYaml)));
      Properties properties = new Properties() {
        public synchronized Enumeration<Object> keys() {
          return Collections.enumeration(new TreeSet<Object>(super.keySet()));
        }
      };
      for (String entryName : entries) {
        if (StringUtils.isEmpty(entryName)) {
          continue;
        }
        Map<String, Object> entry = (Map<String, Object>) map.get(entryName);
        getLog()
          .info(String.format("Merged entry [%s]", entryName));
        iterateAndProcess(properties, entry, "");
      }
      properties.store(new FileOutputStream(new File(destProp)), "Generated by Yml2Prop plugin.");
    }
    catch (FileNotFoundException e) {
      getLog().error(String.format("File %s not found", sourceYaml));
      throw new MojoExecutionException(String.format("File %s not found", sourceYaml), e);
    }
    catch (IOException e) {
      getLog().error(String.format("File %s can not be written", destProp));
      throw new MojoExecutionException(String.format("File %s can not be written", sourceYaml), e);
    }
  }

  /**
   * Iterate yaml entry and push values into properties
   *
   * @param properties
   * @param ymlEntry
   * @param rootKey
   * @return
   */
  @SuppressWarnings("unchecked")
  private void iterateAndProcess(Properties properties, Map<String, Object> ymlEntry, String rootKey) {
    for (String key : ymlEntry.keySet()) {
      Object value = ymlEntry.get(key);
      if (value instanceof Map) {
        iterateAndProcess(properties, (Map<String, Object>) value, StringUtils.isEmpty(rootKey) ? key : rootKey
          + "." + key);
      }
      else {
        properties.setProperty(StringUtils.isEmpty(rootKey) ? key : rootKey + "." + key, value.toString());
      }
    }
  }
}
