package com.example.polling.poll;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;

public class PollForm {
    private String title;
    private String description;
    private PollType type = PollType.SINGLE_CHOICE;
    private PollVisibility visibility = PollVisibility.PUBLIC;
    private ResultsVisibility resultsVisibility = ResultsVisibility.ALWAYS_VISIBLE;
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime endAt;
    private List<String> options = new ArrayList<>(List.of("", ""));

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public PollType getType() { return type; }
    public void setType(PollType type) { this.type = type; }
    public PollVisibility getVisibility() { return visibility; }
    public void setVisibility(PollVisibility visibility) { this.visibility = visibility; }
    public ResultsVisibility getResultsVisibility() { return resultsVisibility; }
    public void setResultsVisibility(ResultsVisibility resultsVisibility) { this.resultsVisibility = resultsVisibility; }
    public LocalDateTime getEndAt() { return endAt; }
    public void setEndAt(LocalDateTime endAt) { this.endAt = endAt; }
    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = options; }
}
