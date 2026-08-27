package com.example.onlinejava.problem;

import java.util.List;

// Every problem will follow this class
public class Problem {

    private final String slug;
    private final String title;
    private final String category;
    private final ProblemType type;   // select type only from the present enum
    private final Difficulty difficulty;  // same for difficulty
    private final String description;
    private final String methodSignature;
    private final String starterCode;
    private final List<String> requirements;
    private final List<String> examples;
    private final String hint;

    public Problem(
            String slug,
            String title,
            String category,
            ProblemType type,
            Difficulty difficulty,
            String description,
            String methodSignature,
            String starterCode,
            List<String> requirements,
            List<String> examples,
            String hint

    ) {
        this.slug = slug;
        this.title = title;
        this.category = category;
        this.type = type;
        this.difficulty = difficulty;
        this.description = description;
        this.requirements = requirements;
        this.methodSignature = methodSignature;
        this.starterCode = starterCode;
        this.examples=examples;
        this.hint=hint;
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

    // Returns the list of requirements for the problem.
    public List<String> getRequirements() {
        return requirements;
    }

    // Returns the method signature required for the problem.
    public String getMethodSignature() {
        return methodSignature;
    }

    // Returns the examples shown for the problem.
    public List<String> getExamples() {
        return examples;
    }

    // Returns the hint provided for the problem.
    public String getHint() {
        return hint;
    }
}