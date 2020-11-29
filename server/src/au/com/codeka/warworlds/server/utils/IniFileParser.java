package au.com.codeka.warworlds.server.utils;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import au.com.codeka.common.Log;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.Configuration;

/**
 * {@link IniFileParser} parses very simple name=value pair ini files.
 */
public class IniFileParser {
  private static final Log log = new Log("IniFileParser");
  private String fileName;

  private IniFileParser(String fileName) {
    this.fileName = fileName;
  }

  public static IniFileParser builder(String fileName) {
    return new IniFileParser(fileName);
  }

  /**
   * Parses an .ini file and returns a mapping of key/value pairs.
   */
  public Map<String, String> parse() throws IOException {
    HashMap<String, String> values = new HashMap<>();

    BufferedReader reader = new BufferedReader(new FileReader(fileName));
    int lineNo = 1;
    String line = reader.readLine();
    while (line != null) {
      int comment = line.indexOf('#');
      if (comment >= 0) {
        line = line.substring(0, comment);
      }
      line = line.trim();
      if (!line.isEmpty()) {
        int equals = line.indexOf('=');
        if (equals < 0) {
          log.warning("Unexpected line in '%s':%d %s", fileName, lineNo, line);
        }

        values.put(line.substring(0, equals).trim(), line.substring(equals + 1).trim());
      }

      line = reader.readLine();
      lineNo ++;
    }

    return values;
  }

  /**
   * Parses an .ini file and populates a message with the contents of the file.
   */
  public <T extends Message.Builder> void parse(T msg) throws IOException {
    Map<String, String> values = parse();

    Descriptors.Descriptor desc = msg.getDescriptorForType();
    for (Descriptors.FieldDescriptor field : desc.getFields()) {
      String value = values.get(field.getName());
      if (value != null) {
        switch(field.getType()) {
          case BOOL:
            msg.setField(field, Boolean.parseBoolean(value));
            break;
          case ENUM:
            msg.setField(field, field.getEnumType().findValueByName(value));
            break;
          case INT32:
          case SINT32:
            msg.setField(field, Integer.parseInt(value));
            break;
          case INT64:
          case SINT64:
            msg.setField(field, Long.parseLong(value));
            break;
          case FLOAT:
            msg.setField(field, Float.parseFloat(value));
            break;
          case DOUBLE:
            msg.setField(field, Double.parseDouble(value));
            break;
          case STRING:
            msg.setField(field, value);
            break;
          default:
            log.error("Unknown field type, ignoring field '%s' in '%s': %s",
                field.getName(), fileName, field.getType());
            break;
        }
      }
    }
  }
}
