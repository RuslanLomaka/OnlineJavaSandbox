package com.example.onlinejava.problem;

public class Problem {

    private final String slug;
    private final String title;
    private final String category;
    private final ProblemType type;   // select type only from the present enum
    private final Difficulty difficulty;  // same for difficulty
    private final String description;
    private final String starterCode;

    public Problem(
            String slug,
            String title,
            String category,
            ProblemType type,
            Difficulty difficulty,
            String description,
            String starterCode
    ) {
        this.slug = slug;
        this.title = title;
        this.category = category;
        this.type = type;
        this.difficulty = difficulty;
        this.description = description;
        this.starterCode = starterCode;
    }

    public String getSlug() {
        return slug;
    }

    public String getTitle() {
        return title;
    }

    public String getCategory() {
        return category;
    }

    public ProblemType getType() {
        return type;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public String getDescription() {
        return description;
    }

    public String getStarterCode() {
        return starterCode;
    }
}