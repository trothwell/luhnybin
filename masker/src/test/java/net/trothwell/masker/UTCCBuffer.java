package net.trothwell.masker;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class UTCCBuffer {
  private static class DataEvent {
    private final String data;
    private final Type type;

    private DataEvent(Type type, String data) {
      this.type = type;
      this.data = data;
    }

    @Override
    public String toString() {
      return String.format("%s:%d/%s", type, data.length(), data);
    }
  }

  private static class EventsCCBuffer extends CCBuffer {
    private final List<DataEvent> events;

    private EventsCCBuffer(List<DataEvent> events) {
      this.events = events;
    }

    @Override
    void onCCData(char[] buf, int offset, int len) {
      if (len == 0) {
        throw new IllegalArgumentException();
      }
      String data = new String(buf, offset, len);
      debug("CC Data: " + data);
      events.add(new DataEvent(Type.CC, data));
    }

    @Override
    void onData(char[] buf, int offset, int len) {
      if (len == 0) {
        throw new IllegalArgumentException();
      }
      String data = new String(buf, offset, len);
      debug("TEXT Data: " + data);
      events.add(new DataEvent(Type.TEXT, data));
    }
  }

  private enum Type {
    CC, TEXT
  }

  private static void debug(String msg) {
    System.out.println(msg);
  }

  @Test
  public void test01() {
    final List<DataEvent> events = new ArrayList<DataEvent>();
    CCBuffer buf = new EventsCCBuffer(events);
    String input = "4444111122223333";
    buf.append(input);
    buf.close();
    assertEquals(1, events.size());
    assertEquals(input, events.get(0).data);
    assertEquals(Type.CC, events.get(0).type);
    debug(events.toString());
  }

  @Test
  public void test02() {
    final List<DataEvent> events = new ArrayList<DataEvent>();
    CCBuffer buf = new EventsCCBuffer(events);
    String input = "4111111111111111";
    buf.append("a" + input);
    buf.close();
    assertEquals(2, events.size());
    assertEquals("a", events.get(0).data);
    assertEquals(Type.TEXT, events.get(0).type);
    assertEquals(input, events.get(1).data);
    assertEquals(Type.CC, events.get(1).type);
    debug(events.toString());
  }

  @Test
  public void test03() {
    final List<DataEvent> events = new ArrayList<DataEvent>();
    CCBuffer buf = new EventsCCBuffer(events);
    String input = "4111111111111111";
    buf.append("a" + input + "b");
    buf.close();

    assertEquals(3, events.size());
    assertEquals("a", events.get(0).data);
    assertEquals(Type.TEXT, events.get(0).type);
    assertEquals(input, events.get(1).data);
    assertEquals(Type.CC, events.get(1).type);
    assertEquals("b", events.get(2).data);
    assertEquals(Type.TEXT, events.get(2).type);
    debug(events.toString());
  }


  @Test
  public void test04() {
    final List<DataEvent> events = new ArrayList<DataEvent>();
    CCBuffer buf = new EventsCCBuffer(events);
    String input = "911111111111111";
    buf.append(input);
    buf.close();

    assertEquals(1, events.size());
    assertEquals(input, events.get(0).data);
    assertEquals(Type.CC, events.get(0).type);
    debug(events.toString());
  }

  @Test
  public void test05() {
    final List<DataEvent> events = new ArrayList<DataEvent>();
    CCBuffer buf = new EventsCCBuffer(events);
    String input = "51111111111111";
    buf.append(input);
    buf.close();

    assertEquals(1, events.size());
    assertEquals(input, events.get(0).data);
    assertEquals(Type.CC, events.get(0).type);
    debug(events.toString());
  }

  @Test
  public void test06() {
    final List<DataEvent> events = new ArrayList<DataEvent>();
    CCBuffer buf = new EventsCCBuffer(events);
    String input = "5-1111111111111";
    buf.append(input);
    buf.close();
    debug(events.toString());

    assertEquals(1, events.size());
    assertEquals(input, events.get(0).data);
    assertEquals(Type.CC, events.get(0).type);
  }

  @Test
  public void test07() {
    final List<DataEvent> events = new ArrayList<DataEvent>();
    CCBuffer buf = new EventsCCBuffer(events);
    String input = "5-1-1-1-1-1-1-1-1-1-1-1-1-1";
    buf.append("a" + input + "b");
    buf.close();
    debug(events.toString());

    assertEquals(3, events.size());

    assertEquals("a", events.get(0).data);
    assertEquals(Type.TEXT, events.get(0).type);

    assertEquals(input, events.get(1).data);
    assertEquals(Type.CC, events.get(1).type);

    assertEquals("b", events.get(2).data);
    assertEquals(Type.TEXT, events.get(2).type);
  }

  @Test
  public void test08() {
    List<DataEvent> events = new ArrayList<DataEvent>();
    CCBuffer buf = new EventsCCBuffer(events);
    String input = "5--1111111111111";
    buf.append(input);
    buf.close();
    events = consolidate(events);
    debug(events.toString());
    assertEquals(1, events.size());
    assertEquals(input, events.get(0).data);
    assertEquals(Type.TEXT, events.get(0).type);
  }

  @Test
  public void test09() {
    List<DataEvent> events = new ArrayList<DataEvent>();
    CCBuffer buf = new EventsCCBuffer(events);;
    String data = "733101515705311323170055 33736233 5-1111111111111";
    buf.append(data);
    buf.close();
    events = consolidate(events);
    debug(events.toString());
    assertEquals(3, events.size());

    assertEquals("7331015157053113231", events.get(0).data);
    assertEquals(Type.TEXT, events.get(0).type);

    assertEquals("70055 33736233 5-11", events.get(1).data);
    assertEquals(Type.CC, events.get(1).type);

    assertEquals("11111111111", events.get(2).data);
    assertEquals(Type.TEXT, events.get(2).type);
  }

  @Test
  public void test10() {
    List<DataEvent> events = new ArrayList<DataEvent>();
    CCBuffer buf = new EventsCCBuffer(events);
    String input = "508733740140655";
    buf.append(input);
    buf.close();
    events = consolidate(events);
    debug(events.toString());
    assertEquals(1, events.size());
    assertEquals(input, events.get(0).data);
    assertEquals(Type.CC, events.get(0).type);
  }

  @Test
  public void test12() {
    final List<DataEvent> events = new ArrayList<DataEvent>();
    CCBuffer buf = new EventsCCBuffer(events);
    char[] zeros = new char[1000];
    Arrays.fill(zeros, '0');
    String input = new String(zeros);
    buf.append(input);
    buf.close();
    debug(events.toString());

    assertEquals(1, events.size());
    assertEquals(input, events.get(0).data);
    assertEquals(Type.CC, events.get(0).type);
  }

  private List<DataEvent> consolidate(List<DataEvent> events) {
    List<DataEvent> retval = new ArrayList<DataEvent>();
    DataEvent last = null;
    for (DataEvent event : events) {
      if (last == null) {
        last = event;
        continue;
      }
      if (event.type == Type.TEXT && last.type == Type.TEXT) {
        last = new DataEvent(Type.TEXT, last.data + event.data);
      } else {
        retval.add(last);
        last = event;
      }
    }
    if (last != null) {
      retval.add(last);
    }
    return retval;
  }
}
