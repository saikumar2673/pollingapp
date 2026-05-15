package com.example.polling.results;

public record OptionResult(Long optionId, String label, long count, double percentage) {
}
