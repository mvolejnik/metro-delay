package app.metrodelay.server.status.cz.prg.dpp;

import static app.metrodelay.ServiceStatus.*;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import jakarta.xml.bind.JAXBElement;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.CharacterData;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;

import app.metrodelay.ServiceStatus;
import app.metrodelay.StatusUpdate;
import app.metrodelay.StatusUpdateException;
import app.metrodelay.StatusUpdateImpl;
import app.metrodelay.server.status.UuidGenerator;
import app.metrodelay.libs.rss.Rss;
import app.metrodelay.libs.rss.RssException;
import app.metrodelay.libs.rss.impl.Rss20Impl;
import app.metrodelay.libs.rss.jaxb.rss20.Guid;
import app.metrodelay.libs.rss.jaxb.rss20.RssItem;
import app.metrodelay.server.status.OperatorFactory;

public class DppFactory implements OperatorFactory{

  private static final Logger l = LogManager.getLogger(DppFactory.class);

  public List<StatusUpdate> statusUpdates(InputStream rssInputStream) throws StatusUpdateException {
    l.info("statusUpdates::");
    Rss rss;
    List<RssItem> items;
    List<StatusUpdate> statusUpdates = new ArrayList<>();
    try {
      rss = new Rss20Impl(rssInputStream);
    } catch (RssException e) {
      l.error("statusUpdates::");
      throw new StatusUpdateException("Unable to parse RSS!", e);
    }

    items = rss.getRss().getChannel().getItem();
    for (RssItem item : items) {
      statusUpdates.add(parseStatusUpdate(item));
    }
    return statusUpdates;
  }

  private StatusUpdate parseStatusUpdate(RssItem rss) throws StatusUpdateException {
    l.info("parseStatusUpdate::");
    String title = null;
    String description = null;
    String type = null;
    Collection<String> lines = new ArrayList<String>();
    Calendar startAt, finishAt, expectedFinishAt;
    URL infoReference = null;
    UUID uuid = null;
    List<Object> attrs = rss.getTitleOrDescriptionOrLink();
    for (Object attr : attrs) {
      if (attr instanceof JAXBElement<?>) {
        JAXBElement e = (JAXBElement) attr;
        Class clazz = ((JAXBElement) attr).getDeclaredType();
        l.trace("parseStatusUpdate:: Attribute of type [{}].", clazz);
        String name = ((JAXBElement) attr).getName().getNamespaceURI() + ":"
            + ((JAXBElement) attr).getName().getLocalPart();
        Object value = ((JAXBElement) attr).getValue();
        if (value != null) {
          switch (name) {
          case ":guid":
            l.debug("parseStatusUpdate:: Processing GUID");
            Guid guid = (Guid) value;
            l.debug("parseStatusUpdate:: GUID [{}]", guid.getValue());
            if (!guid.getValue().isEmpty() && guid.getValue().length() > 0) {
              uuid = UuidGenerator.generate(guid.getValue());
            } else {
              l.warn("parseStatusUpdate:: Item's GUID is empty.");
            }
            break;
          case ":title":
            l.debug("parseStatusUpdate:: Processing title");
            title = value.toString().trim();
            if (StringUtils.isEmpty(title)) {
              l.info("parseStatusUpdate:: Item's title is empty.");
            }
            break;
          case ":description":
            l.debug("parseStatusUpdate:: Processing description");
            description = StringUtils.trim(value.toString());
            if (StringUtils.isEmpty(description)) {
              l.debug("parseStatusUpdate:: Item's description is empty.");
            }
            break;
          case ":link":
            String link = value.toString();
            if (StringUtils.isNotEmpty(link)) {
              try {
                infoReference = new URL(link);
              } catch (MalformedURLException e1) {
                l.warn("Unable to parse link [{}]", link);
              }
            }
            break;
          default:
            l.debug("parseStatusUpdate:: unable to parse [{}]", name);
          }
        }
      } else if (attr instanceof Element) {
        Element element = (Element) attr;
        String name = element.getLocalName();
        switch (name) {
        case "content_encoded":
          l.debug("parseStatusUpdate:: Processing content_encoded");
          CharacterData text = (CharacterData) element.getFirstChild();
          try {
            Content c = parseContent(text.getData());
            lines.addAll(c.getLines());
            type = c.getEmergencyType();
          } catch (DOMException | XMLStreamException e) {
            l.warn("Unable to parse RSS Item Content Data", e);
            l.debug("parseStatusUpdate:: [{}]", text);
          }
          break;
        default:
          l.debug("parseStatusUpdate:: unable to parse [{}]", name);
        }
      }
    }
    if (uuid == null) {
      l.warn("parseStatusUpdate:: GUID is null, generating spare (always unique) UUID.");
      uuid = UuidGenerator.generate();
      l.info("parseStatusUpdate:: Generated UUID [{}]", uuid.toString());
    }
    StatusUpdate update = new StatusUpdateImpl(uuid, title, description, type, lines, infoReference);
    l.debug("parseStatusUpdate:: StatusUpdate [{}]", update);
    return update;
  }

  private Content parseContent(String xmlContent) throws XMLStreamException {
    l.debug("parseContent:: Parsing content_encoded element.");
    Content content = new Content();
    StringBuilder xml = new StringBuilder();
    xml.append("<content>").append(xmlContent).append("</content>");
    XMLInputFactory f = XMLInputFactory.newInstance();
    XMLStreamReader r = f.createXMLStreamReader(IOUtils.toInputStream(xml.toString(), Charset.forName("UTF-8")));
    if (r.hasNext()) {
      r.next();
      if (r.isStartElement()) {
        String rootElementName = r.getName().getLocalPart();
        l.trace("parseContent:: Root Start Element [{}]", rootElementName);
        if ("content".equals(rootElementName)) {
          int depth = 0;
          while (r.hasNext() && depth >= 0) {
            r.next();
            if (r.isStartElement()) {
              depth++;
              String name = r.getName().getLocalPart();
              switch (name) {
              case "emergency_types":
                // Provoz omezen, Zpoždění spojů, Provoz zastaven
                l.trace("parseContent:: Parsing emergency types.");
                content.setEmergencyType(r.getElementText());
                break;
              case "time_start":
                l.trace("parseContent:: Parsing start time.");
                content.setStart(parseTimeStart());
                break;
              case "time_stop":
                l.trace("parseContent:: Parsing stop time.");
                content.setStop(parseTimeStop());
                break;
              case "time_final_stop":
                l.trace("parseContent:: Parsing final stop time.");
                content.setFinalStop(parseTimeFinalStop());
                break;
              case "integrated_rescue_system":
                l.trace("parseContent:: Parsing integrated rescue system involved.");
                // do nothing
                break;
              case "aff_line_types":
                l.trace("parseContent:: Parsing line types.");
                // do nothing
                break;
              case "aff_lines":
                l.trace("parseContent:: Parsing affected lines.");
                content.setLines(parseAffectedLines(r.getElementText()));
                break;
              default:
                l.info("parseContent(): Unsupported content type [{}]", name);
              }
            }
          }
        }
      }
    }
    return content;
  }

  private String parseSection() {

    String section = null;

    return section;
  }

  private String parseEmergencyType() {
    String type = null;

    return type;
  }

  private String parseTimeStart() {
    String start = null;

    return start;
  }

  private String parseTimeStop() {
    String stop = null;

    return stop;
  }

  private String parseTimeFinalStop() {
    String finalStop = null;

    return finalStop;
  }

  private Collection<String> parseAffectedLines(String linesStr) {
    l.debug("parseAffectedLines:: Parsing lines [{}]", linesStr);
    List<String> lines = new ArrayList<String>();
    if (linesStr != null && linesStr.trim().length() > 0) {
      String[] rawLines = linesStr.split(",");
      for (String l : rawLines) {
        lines.add(l.trim());
      }
    }
    return Collections.unmodifiableCollection(lines);
  }
  
  private ServiceStatus serviceStatus(String status){
    ServiceStatus service;
    switch (status){
    case "Provoz omezen": service = CLOSED; break;
    case "Provoz zastaven, zavedena NAD": service = CLOSED; break;
    case "Provoz zastaven": service = CLOSED; break;
    case "Ostatní": service = IGNORE; break;
    case "Zpoždění spojů": service = DELAY; break;
      default: service = NOT_AVAILABLE;
    }
    return service;
  }

  private static class Content {
    private String section;
    private String emergencyType;
    private String start;
    private String stop;
    private String finalStop;
    private String integratedRescueSystem;
    private List<String> lineTypes;
    private Collection<String> lines = Collections.<String>emptyList();

    public String getSection() {
      return section;
    }

    public void setSection(String section) {
      this.section = section;
    }

    public String getEmergencyType() {
      return emergencyType;
    }

    public void setEmergencyType(String emergencyType) {
      this.emergencyType = emergencyType;
    }

    public String getStart() {
      return start;
    }

    public void setStart(String start) {
      this.start = start;
    }

    public String getStop() {
      return stop;
    }

    public void setStop(String stop) {
      this.stop = stop;
    }

    public String getFinalStop() {
      return finalStop;
    }

    public void setFinalStop(String finalStop) {
      this.finalStop = finalStop;
    }

    public String getIntegratedRescueSystem() {
      return integratedRescueSystem;
    }

    public void setIntegratedRescueSystem(String integratedRescueSystem) {
      this.integratedRescueSystem = integratedRescueSystem;
    }

    public List<String> getLineTypes() {
      return lineTypes;
    }

    public void setLineTypes(List<String> lineTypes) {
      this.lineTypes = lineTypes;
    }

    public Collection<String> getLines() {
      return lines;
    }

    public void setLines(Collection<String> lines) {
      this.lines = new ArrayList<>();
      this.lines.addAll(lines);
    }

  }

}

