package com.example.onlinejava.discussion;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data access for {@link PostReaction}.
 */
public interface PostReactionRepository extends JpaRepository<PostReaction, PostReactionId> {

  /**
   * Aggregated reaction counts for a set of posts.
   *
   * @param postId post id
   * @param reaction reaction type
   * @param count number of users who reacted
   * @param mine 1 if the viewer is among them, else 0
   */
  record ReactionCount(Long postId, Reaction reaction, Long count, Long mine) {
  }

  /**
   * Counts reactions per post and type, and whether the viewer reacted.
   *
   * @param postIds posts to summarize
   * @param viewerId current user id
   * @return one row per (post, reaction) with at least one reaction
   */
  @Query("""
      select new com.example.onlinejava.discussion.PostReactionRepository$ReactionCount(
          r.id.postId, r.id.reaction, count(r),
          sum(case when r.id.userId = :viewerId then 1L else 0L end))
      from PostReaction r
      where r.id.postId in :postIds
      group by r.id.postId, r.id.reaction
      """)
  List<ReactionCount> summarize(
      @Param("postIds") Collection<Long> postIds,
      @Param("viewerId") long viewerId
  );
}
