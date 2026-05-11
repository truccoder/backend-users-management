package com.backend.users.graph;

import static org.springframework.data.neo4j.core.schema.Relationship.Direction.INCOMING;
import static org.springframework.data.neo4j.core.schema.Relationship.Direction.OUTGOING;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import lombok.Data;
import lombok.NoArgsConstructor;

@Node("User")
@Data
@NoArgsConstructor
public class UserNode {
  @Id private String id;
  private String fullName;
  private String profilePictureUrl;

  @Relationship(type = "FRIENDS_WITH", direction = OUTGOING)
  private Set<UserNode> friends = new HashSet<>();

  @Relationship(type = "FOLLOWS", direction = OUTGOING)
  private List<FollowsRelationship> following = new ArrayList<>();

  @Relationship(type = "FOLLOWS", direction = INCOMING)
  private List<FollowedByRelationship> followers = new ArrayList<>();

  @Relationship(type = "BLOCKS", direction = OUTGOING)
  private List<BlocksRelationship> blocked = new ArrayList<>();
}
