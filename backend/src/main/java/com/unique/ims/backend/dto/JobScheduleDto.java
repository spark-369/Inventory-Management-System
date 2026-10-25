package com.unique.ims.backend.dto;

/**
 * One entry of the application's declared message schedule: what the broker runs
 * on a timer, and which routing key can be used to trigger it on demand.
 */
public class JobScheduleDto {
    private String id;
    private String name;
    private String schedule;
    private String description;
    /** {@code JobMessageType} name accepted by the manual-trigger endpoint. */
    private String trigger;

    public JobScheduleDto() {
    }

    public JobScheduleDto(String id, String name, String schedule, String description, String trigger) {
        this.id = id;
        this.name = name;
        this.schedule = schedule;
        this.description = description;
        this.trigger = trigger;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSchedule() {
        return schedule;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTrigger() {
        return trigger;
    }

    public void setTrigger(String trigger) {
        this.trigger = trigger;
    }
}
