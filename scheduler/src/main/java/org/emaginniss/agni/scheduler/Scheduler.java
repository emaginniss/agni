/*
 * Copyright (c) 2015, Eric A Maginniss
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL ERIC A MAGINNISS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.emaginniss.agni.scheduler;

import org.emaginniss.agni.Configuration;
import org.emaginniss.agni.Node;
import org.emaginniss.agni.annotations.Criterion;
import org.emaginniss.agni.annotations.Subscribe;
import org.emaginniss.agni.messages.StopRouting;
import org.emaginniss.agni.scheduler.messages.ScheduledTask;
import org.emaginniss.agni.scheduler.util.ScheduledTaskJob;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

public class Scheduler {

    private org.quartz.Scheduler scheduler;
    private Node node;

    public Scheduler(final Node node, Configuration configuration) {
        this.node = node;
        try {
            if (configuration.has("quartz")) {
                Properties quartzConfig = new Properties();
                configuration.getStringMap("quartz").entrySet().forEach(e -> quartzConfig.setProperty("org.quartz." + e.getKey(), e.getValue()));
                scheduler = new StdSchedulerFactory(quartzConfig).getScheduler();
            } else {
                scheduler = StdSchedulerFactory.getDefaultScheduler();
            }
            scheduler.setJobFactory((triggerFiredBundle, scheduler1) -> new ScheduledTaskJob(node));
            scheduler.start();
        } catch (SchedulerException e) {
            throw new RuntimeException("Error while starting scheduler", e);
        }

        node.register(this);
    }

    @Subscribe
    public void handleStopRouting(StopRouting sr) {
        if (sr.getNodeUuid().equals(node.getUuid())) {
            try {
                scheduler.shutdown(false);
            } catch (SchedulerException e) {
                //do nothing
            }
        }
    }

    @Subscribe(criteria = {
            @Criterion(key = "action", value = "add")
    })
    public void handleAdd(ScheduledTask scheduledTask) throws SchedulerException {
        JobDetail job = scheduler.getJobDetail(new JobKey(scheduledTask.getName(), scheduledTask.getGroup()));
        if (job == null) {
            JobBuilder jb = JobBuilder.newJob(ScheduledTaskJob.class)
                    .withIdentity(scheduledTask.getName(), scheduledTask.getGroup())
                    .usingJobData("cronString", scheduledTask.getCronString())
                    .usingJobData("description", scheduledTask.getDescription())
                    .usingJobData("group", scheduledTask.getGroup())
                    .usingJobData("name", scheduledTask.getName())
                    .usingJobData("payloadClassName", scheduledTask.getPayloadClassName())
                    .usingJobData("payloadSerialized", scheduledTask.getPayloadSerialized())
                    .usingJobData("payloadType", scheduledTask.getPayloadType())
                    .usingJobData("execute", scheduledTask.getExecute())
                    .usingJobData("intervalInMs", scheduledTask.getIntervallMs())
                    .usingJobData("startInMs", scheduledTask.getStartInMs());

            scheduledTask.getCriteria().entrySet().forEach(e -> jb.usingJobData("criteria_" + e.getKey(), e.getValue()));

            job = jb.build();

            Trigger trigger;
            if (scheduledTask.getCronString() == null) {
                TriggerBuilder tb = newTrigger()
                        .withIdentity(scheduledTask.getName(), scheduledTask.getGroup())
                        .withSchedule(simpleSchedule()
                                .withIntervalInMilliseconds(scheduledTask.getIntervallMs())
                                .repeatForever())
                        .forJob(job.getKey());

                if (scheduledTask.getStartInMs() != 0) {
                    tb.startAt(new Date(System.currentTimeMillis() + scheduledTask.getStartInMs()));
                }

                trigger = tb.build();
            } else {
                trigger = newTrigger()
                        .withIdentity(scheduledTask.getName(), scheduledTask.getGroup())
                        .withSchedule(CronScheduleBuilder.cronSchedule(scheduledTask.getCronString()))
                        .forJob(job.getKey())
                        .build();
            }
            scheduler.scheduleJob(job, trigger);
        }
    }

    @Subscribe(criteria = {
            @Criterion(key = "action", value = "delete")
    })
    public void handleDelete(ScheduledTask scheduledTask) throws SchedulerException {
        scheduler.deleteJob(new JobKey(scheduledTask.getName(), scheduledTask.getGroup()));
    }

    @Subscribe(criteria = {
            @Criterion(key = "action", value = "list")
    })
    public ScheduledTask []handleList(ScheduledTask scheduledTask) throws SchedulerException {
        List<ScheduledTask> out = new ArrayList<>();

        for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.anyGroup())) {
            JobDetail job = scheduler.getJobDetail(jobKey);
            Trigger trigger = scheduler.getTriggersOfJob(jobKey).get(0);

            ScheduledTask st = new ScheduledTask(job.getJobDataMap());
            st.setPreviousRunTime(trigger.getPreviousFireTime() == null ? 0 : trigger.getPreviousFireTime().getTime());
            st.setNextRunTime(trigger.getNextFireTime() == null ? 0 : trigger.getNextFireTime().getTime());

            out.add(st);
        }

        return out.toArray(new ScheduledTask[out.size()]);
    }

}
