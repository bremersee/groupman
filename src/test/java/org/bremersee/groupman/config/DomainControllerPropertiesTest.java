package org.bremersee.groupman.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The domain controller properties test.
 */
class DomainControllerPropertiesTest {

  private static DomainControllerProperties properties = new DomainControllerProperties();

  /**
   * Sets up.
   */
  @BeforeAll
  static void setUp() {
    properties.setGroupFindAllFilter("(objectClass=group)");
    properties.setGroupFindOneFilter("(&(objectClass=group)(cn={0}))");
    properties.setGroupNameAttribute("cn");
    properties.setGroupMemberAttribute("member");
  }

  /**
   * Gets group find by names filter.
   */
  @Test
  void getGroupFindByNamesFilter() {
    String expected = "(objectClass=group)";
    String actual = properties.getGroupFindByNamesFilter(0);
    assertEquals(expected, actual);

    expected = "(&(objectClass=group)(cn={0}))";
    actual = properties.getGroupFindByNamesFilter(1);
    assertEquals(expected, actual);

    expected = "(&(objectClass=group)(|(cn={0})(cn={1})))";
    actual = properties.getGroupFindByNamesFilter(2);
    assertEquals(expected, actual);
  }

  /**
   * Gets group find by member contains filter.
   */
  @Test
  void getGroupFindByMemberContainsFilter() {
    String expected = "(&(objectClass=group)(member={0}))";
    String actual = properties.getGroupFindByMemberContainsFilter();
    assertEquals(expected, actual);
  }
}