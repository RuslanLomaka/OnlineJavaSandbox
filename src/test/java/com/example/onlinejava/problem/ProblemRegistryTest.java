package com.example.onlinejava.problem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.onlinejava.problem.algorithms.BubbleSortProblem;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ProblemRegistry} and the {@link Topic} taxonomy.
 */
class ProblemRegistryTest {

  private final ProblemRegistry registry = new ProblemRegistry();

  @Test
  void findsProblemsBySlug() {
    assertThat(registry.getProblem("bubble-sort")).isInstanceOf(BubbleSortProblem.class);
    assertThat(registry.findBySlug("longest-unique-substring")).isPresent();
    assertThat(registry.findBySlug("does-not-exist")).isEmpty();
  }

  @Test
  void placesExistingProblemsInTheirTopics() {
    assertThat(topicOf("bubble-sort")).isEqualTo(Topic.SORTING);
    assertThat(topicOf("binary-search")).isEqualTo(Topic.SEARCHING);
    assertThat(topicOf("two-sum")).isEqualTo(Topic.HASHING);
    assertThat(topicOf("longest-unique-substring")).isEqualTo(Topic.SLIDING_WINDOW);
  }

  @Test
  void canonicalPathUsesTopicSlug() {
    assertThat(registry.findBySlug("bubble-sort").orElseThrow().getPath())
        .isEqualTo("/problems/sorting/bubble-sort");
    assertThat(registry.findBySlug("two-sum").orElseThrow().getPath())
        .isEqualTo("/problems/hash-maps-and-sets/two-sum");
  }

  @Test
  void groupsEveryTopicInMenuOrderWithProblemsSortedByDifficultyThenTitle() {
    final Map<Topic, List<Problem>> byTopic = registry.problemsByTopic();

    assertThat(byTopic.keySet()).containsExactly(Topic.values());
    byTopic.values().forEach(problems -> assertThat(problems)
        .isSortedAccordingTo(ProblemRegistry.DISPLAY_ORDER));
    assertThat(byTopic.get(Topic.TREES)).isEmpty();
  }

  @Test
  void getAllProblemsReturnsImmutableSnapshot() {
    assertThat(registry.getAllProblems()).hasSize(5);
    final List<ProblemDefinition> snapshot = registry.getAllProblems();
    assertThatThrownBy(snapshot::clear)
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void topicSlugsAreUniqueUrlSafeAndResolvable() {
    assertThat(Arrays.stream(Topic.values()).map(Topic::slug)).doesNotHaveDuplicates()
        .allMatch(slug -> slug.matches("[a-z]+(-[a-z]+)*"));
    for (final Topic topic : Topic.values()) {
      assertThat(Topic.fromSlug(topic.slug())).contains(topic);
    }
    assertThat(Topic.fromSlug("nope")).isEmpty();
  }

  @Test
  void everyTopicBelongsToOneOfTheTwoSections() {
    assertThat(Topic.inSection(Topic.Section.DATA_STRUCTURES))
        .contains(Topic.ARRAYS, Topic.HASHING, Topic.TREES)
        .doesNotContain(Topic.SORTING);
    assertThat(Topic.inSection(Topic.Section.ALGORITHMS))
        .contains(Topic.SORTING, Topic.SEARCHING, Topic.SLIDING_WINDOW);
  }

  private Topic topicOf(final String slug) {
    return registry.findBySlug(slug).orElseThrow().getTopic();
  }
}
