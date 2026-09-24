package com.example.onlinejava.discussion;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Converts {@code {reaction}} path variables such as {@code thumbs_up} into
 * {@link Reaction}. Unknown codes fail conversion, which Spring MVC reports as
 * 400 Bad Request.
 */
@Component
public class ReactionConverter implements Converter<String, Reaction> {

  @Override
  public Reaction convert(final String source) {
    return Reaction.fromCode(source);
  }
}
