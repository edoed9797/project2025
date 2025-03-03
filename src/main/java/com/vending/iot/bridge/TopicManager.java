package com.vending.iot.bridge;

import java.util.HashSet;
import java.util.Set;

public class TopicManager {
    private final Set<String> topicMacchine;
    private final Set<String> topicManutenzione;
    private final Set<String> topicMonitoraggio;

    public TopicManager() {
        this.topicMacchine = new HashSet<>();
        this.topicManutenzione = new HashSet<>();
        this.topicMonitoraggio = new HashSet<>();
        inizializzaTopicPredefiniti();
    }

    private void inizializzaTopicPredefiniti() {
        // Topic macchine
        topicMacchine.add("macchine/+/stato");
        topicMacchine.add("macchine/+/allarmi");
        topicMacchine.add("macchine/+/manutenzione");

        // Topic manutenzione
        topicManutenzione.add("manutenzione/+/richieste");
        topicManutenzione.add("manutenzione/+/interventi");

        // Topic monitoraggio
        topicMonitoraggio.add("monitoraggio/+/statistiche");
        topicMonitoraggio.add("monitoraggio/+/alert");
    }

    public Set<String> getTopicMacchine() {
        return new HashSet<>(topicMacchine);
    }

    public Set<String> getTopicManutenzione() {
        return new HashSet<>(topicManutenzione);
    }

    public Set<String> getTopicMonitoraggio() {
        return new HashSet<>(topicMonitoraggio);
    }

    public void aggiungiTopicMacchina(String topic) {
        topicMacchine.add(topic);
    }

    public void aggiungiTopicManutenzione(String topic) {
        topicManutenzione.add(topic);
    }

    public void aggiungiTopicMonitoraggio(String topic) {
        topicMonitoraggio.add(topic);
    }
}
