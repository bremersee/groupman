package org.bremersee.groupman.repository.ldap;

import static org.bremersee.data.ldaptive.LdaptiveEntryMapper.getAttributeValue;

import java.util.Collections;
import javax.validation.constraints.NotNull;
import org.bremersee.data.ldaptive.LdaptiveEntryMapper;
import org.bremersee.groupman.config.DomainControllerProperties;
import org.bremersee.groupman.model.Source;
import org.bremersee.groupman.repository.GroupEntity;
import org.bremersee.groupman.repository.ldap.transcoder.GeneralizedTimeToDateValueTranscoder;
import org.bremersee.groupman.repository.ldap.transcoder.GroupMemberValueTranscoder;
import org.ldaptive.AttributeModification;
import org.ldaptive.LdapEntry;
import org.ldaptive.io.StringValueTranscoder;

public class GroupLdapMapper implements LdaptiveEntryMapper<GroupEntity> {

  private static final String WHEN_CREATED = "whenCreated";

  private static final String WHEN_CHANGED = "whenChanged";

  private static final GeneralizedTimeToDateValueTranscoder WHEN_TIME_VALUE_TRANSCODER
      = new GeneralizedTimeToDateValueTranscoder();

  private static final StringValueTranscoder STRING_VALUE_TRANSCODER = new StringValueTranscoder();

  private DomainControllerProperties properties;

  private GroupMemberValueTranscoder groupMemberValueTranscoder;

  public GroupLdapMapper(DomainControllerProperties properties) {
    this.properties = properties;
    this.groupMemberValueTranscoder = new GroupMemberValueTranscoder(properties);
  }

  @Override
  public String[] getObjectClasses() {
    return new String[0];
  }

  @Override
  public String mapDn(GroupEntity group) {
    return properties.getGroupRdn()
        + "=" + group.getName()
        + "," + properties.getGroupBaseDn();
  }

  @Override
  public GroupEntity map(LdapEntry ldapEntry) {
    if (ldapEntry == null) {
      return null;
    }
    final GroupEntity destination = new GroupEntity();
    map(ldapEntry, destination);
    return destination;
  }

  @Override
  public void map(LdapEntry source, GroupEntity destination) {
    destination.setCreatedAt(getAttributeValue(
        source, WHEN_CREATED, WHEN_TIME_VALUE_TRANSCODER, null));
    destination.setModifiedAt(getAttributeValue(
        source, WHEN_CHANGED, WHEN_TIME_VALUE_TRANSCODER, null));
    destination.setCreatedBy(properties.getAdminName());
    destination.setDescription(getAttributeValue(
        source, properties.getGroupDescriptionAttribute(), STRING_VALUE_TRANSCODER, null));
    destination.setId(getAttributeValue(
        source, properties.getGroupNameAttribute(), STRING_VALUE_TRANSCODER, null));
    destination.setName(getAttributeValue(
        source, properties.getGroupNameAttribute(), STRING_VALUE_TRANSCODER, null));
    destination.setMembers(LdaptiveEntryMapper.getAttributeValuesAsSet(
        source,
        properties.getGroupMemberAttribute(),
        groupMemberValueTranscoder));
    destination.setOwners(Collections.singleton(properties.getAdminName()));
    destination.setSource(Source.LDAP);
    destination.setVersion(1L);
  }

  @Override
  public AttributeModification[] mapAndComputeModifications(
      @NotNull GroupEntity source,
      @NotNull LdapEntry destination) {

    return new AttributeModification[0];
  }
}
