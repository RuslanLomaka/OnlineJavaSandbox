package com.example.onlinejava;

import com.example.onlinejava.problem.Problem;
import com.example.onlinejava.problem.ProblemDefinition;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.ui.Model;
import com.example.onlinejava.problem.ProblemRegistry;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class PageController {

    private final ProblemRegistry problemRegistry;

    // Creates the controller with access to the problem registry.
    public PageController(ProblemRegistry problemRegistry) {
        this.problemRegistry = problemRegistry;
    }

    @GetMapping("/sandbox")
    public String sandbox(
            @AuthenticationPrincipal OAuth2User user,
            Model model
    ) {
        String username;

        if (user == null) {
            username = "Local developer";
        } else {
            username = user.getAttribute("login");
        }

        model.addAttribute("username", username);

        return "sandbox";
    }

    @GetMapping("/problems")
    public String problems() {
        return "problems";
    }

    @GetMapping("/problems/arrays")
    public String arrays(Model model) {

        List<Problem> arrayProblems =
                problemRegistry.getAllProblems()
                        .stream()
                        .map(ProblemDefinition::getProblem)
                        .filter(problem ->
                                "Arrays".equalsIgnoreCase(
                                        problem.getCategory()
                                )
                        )
                        .toList();

        model.addAttribute("problems", arrayProblems);

        return "problems-arrays";
    }

    @GetMapping("/problems/collections")
    public String collections() {
        return "problems-collections";
    }

    @GetMapping("/problems/algorithms")
    public String algorithms() {
        return "problems-algorithms";
    }

//    @GetMapping("/problems/arrays/bubble-sort")
//    public String bubbleSort() {
//        return "arrays/bubble-sort";
//    }


    // Opens any registered problem using its category and slug.
    @GetMapping("/problems/{category}/{slug}")
    public String problem(
            @PathVariable String category,
            @PathVariable String slug,
            Model model
    ) {
        ProblemDefinition problemDefinition = problemRegistry.getProblem(slug);

        if (problemDefinition == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Problem not found");
        }

        Problem problem = problemDefinition.getProblem();

        if (!problem.getCategory().equalsIgnoreCase(category)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Problem not found");
        }

        model.addAttribute("problem", problem);

        return "problem";
    }

    @GetMapping(
            "/problems/collections/longest-unique-substring"
    )
    public String longestUniqueSubstring() {
        return "collections/longest-unique-substring";
    }

}