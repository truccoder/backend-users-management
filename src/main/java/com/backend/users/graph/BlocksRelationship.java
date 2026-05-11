package com.backend.users.graph;

import java.time.OffsetDateTime;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

import lombok.Data;

@RelationshipProperties
@Data
public class BlocksRelationship {
  @Id @GeneratedValue private String id;
  @TargetNode private UserNode blockedUser;
  private OffsetDateTime createdAt;
}
