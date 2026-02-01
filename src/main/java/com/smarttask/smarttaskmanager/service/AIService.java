package com.smarttask.smarttaskmanager.service;

import com.smarttask.smarttaskmanager.model.Task;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AIService {

    // 1. DASHBOARD INSIGHTS (Rendu STATIC pour être accessible partout)
    public static String getProductivityInsights(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) return "😴 Rien à faire. Reposez-vous !";

        long overdue = tasks.stream().filter(t -> t.getDeadline() != null && t.getDeadline().isBefore(LocalDate.now()) && !"Completed".equalsIgnoreCase(t.getStatus())).count();
        long highPriority = tasks.stream().filter(t -> "High".equalsIgnoreCase(t.getPriority()) && !"Completed".equalsIgnoreCase(t.getStatus())).count();

        if (overdue > 0) return "🚨 Attention ! " + overdue + " tâches en retard !";
        if (highPriority > 0) return "🔥 Focus : Finissez les " + highPriority + " tâches prioritaires.";
        return "🏆 Excellent rythme !";
    }

    // 2. PARSING DATE (Indispensable pour AddTaskController)
    public static LocalDate parseDate(String input) {
        if (input == null || input.isEmpty()) return null;
        String lowerInput = input.toLowerCase();

        if (lowerInput.contains("aujourd'hui") || lowerInput.contains("lyoum")) return LocalDate.now();
        if (lowerInput.contains("demain") || lowerInput.contains("ghda")) return LocalDate.now().plusDays(1);
        if (lowerInput.contains("après-demain")) return LocalDate.now().plusDays(2);

        Pattern pattern = Pattern.compile("dans (\\d+) jours");
        Matcher matcher = pattern.matcher(lowerInput);
        if (matcher.find()) {
            return LocalDate.now().plusDays(Integer.parseInt(matcher.group(1)));
        }
        return null;
    }

    // 3. PRIORITÉ (Indispensable)
    public static String suggestPriority(String input) {
        if (input == null) return "Medium";
        String lower = input.toLowerCase();
        if (lower.contains("urgent") || lower.contains("important") || lower.contains("exam") || lower.contains("darouri")) return "High";
        if (lower.contains("loisir") || lower.contains("film") || lower.contains("café")) return "Low";
        return "Medium";
    }

    // 4. CATÉGORIE (Indispensable)
    public static String suggestCategory(String input) {
        if (input == null) return "Général";
        String lower = input.toLowerCase();
        if (lower.contains("code") || lower.contains("java") || lower.contains("projet")) return "Travail";
        if (lower.contains("cours") || lower.contains("réviser")) return "Études";
        if (lower.contains("sport") || lower.contains("match")) return "Santé";
        return "Personnel";
    }
}