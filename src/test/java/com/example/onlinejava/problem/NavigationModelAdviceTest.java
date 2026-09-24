package com.example.onlinejava.problem;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

/**
 * Unit tests for {@link NavigationModelAdvice}.
 */
class NavigationModelAdviceTest {

  private final List<NavigationModelAdvice.NavSection> sections =
      new NavigationModelAdvice(providerOf(new ProblemRegistry())).navSections();

  @Test
  void hasDataStructuresThenAlgorithms() {
    assertThat(sections).extracting(NavigationModelAdvice.NavSection::title)
        .containsExactly("Data Structures", "Algorithms");
  }

  @Test
  void listsEveryTopicWithPathAndProblemCount() {
    final NavigationModelAdvice.NavSection algorithms = sections.get(1);

    assertThat(algorithms.topics()).extracting(NavigationModelAdvice.NavTopic::title)
        .startsWith("Sorting", "Searching");
    assertThat(algorithms.topics().get(0).path()).isEqualTo("/problems/sorting");
    assertThat(algorithms.topics().get(0).problemCount()).isEqualTo(1);
    assertThat(sections.get(0).topics())
        .filteredOn(topic -> topic.title().equals("Trees"))
        .singleElement()
        .satisfies(topic -> assertThat(topic.problemCount()).isZero());
  }

  @Test
  void worksWithoutRegistryInWebSliceTests() {
    assertThat(new NavigationModelAdvice(providerOf(null)).navSections()).isEmpty();
  }

  private static ObjectProvider<ProblemRegistry> providerOf(
      final ProblemRegistry registry
  ) {
    final StaticListableBeanFactory factory = new StaticListableBeanFactory();
    if (registry != null) {
      factory.addBean("problemRegistry", registry);
    }
    return factory.getBeanProvider(ProblemRegistry.class);
  }
}
