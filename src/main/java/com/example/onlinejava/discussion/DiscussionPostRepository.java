package com.example.onlinejava.discussion;

import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data access for {@link DiscussionPost}. Queries fetch authors eagerly to
 * avoid one extra query per post.
 */
public interface DiscussionPostRepository extends JpaRepository<DiscussionPost, Long> {

  /**
   * Returns a page of a problem's top-level posts, newest first.
   *
   * @param slug problem slug
   * @param pageable page request
   * @return thread roots
   */
  @Query(value = """
      select p from DiscussionPost p join fetch p.author
      where p.problemSlug = :slug and p.thread is null
      order by p.createdAt desc, p.id desc
      """,
      countQuery = """
      select count(p) from DiscussionPost p
      where p.problemSlug = :slug and p.thread is null
      """)
  Page<DiscussionPost> findThreads(@Param("slug") String slug, Pageable pageable);

  /**
   * Returns all replies of the given threads, oldest first, with the author
   * of each replied-to post.
   *
   * @param threadIds thread root ids
   * @return replies
   */
  @Query("""
      select p from DiscussionPost p
      join fetch p.author
      left join fetch p.replyTo r
      left join fetch r.author
      where p.thread.id in :threadIds
      order by p.createdAt asc, p.id asc
      """)
  List<DiscussionPost> findReplies(@Param("threadIds") Collection<Long> threadIds);

  /**
   * Counts a problem's non-deleted posts.
   *
   * @param slug problem slug
   * @return post count
   */
  long countByProblemSlugAndDeletedAtIsNull(String slug);
}
