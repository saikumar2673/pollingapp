package com.example.polling.results;

import java.util.List;

public record PollResults(long totalRespondents, List<OptionResult> options) {
}
