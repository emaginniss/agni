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

package org.emaginniss.agni.scheduler.util;

import org.emaginniss.agni.AgniBuilder;
import org.emaginniss.agni.Criteria;
import org.emaginniss.agni.Execute;
import org.emaginniss.agni.Node;
import org.emaginniss.agni.scheduler.messages.ScheduledTask;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import static org.emaginniss.agni.Execute.send;

public class ScheduledTaskJob implements Job {

    private Node node;

    public ScheduledTaskJob(Node node) {
        this.node = node;
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        ScheduledTask st = new ScheduledTask(jobExecutionContext.getJobDetail().getJobDataMap());

        Object payload = null;
        if (st.getPayloadSerialized() != null) {
            payload = node.getSerializer().deserialize(st.getPayloadSerialized(), st.getPayloadClassName());
        }
        AgniBuilder ab = new AgniBuilder(payload).type(st.getPayloadType());
        ab.criteria(new Criteria(st.getCriteria()));
        switch (Execute.valueOf(st.getExecute())) {
            case send:
                ab.send(node);
                break;
            case broadcast:
                ab.broadcast(node);
                break;
            case request:
                ab.request(node);
                break;
            case requestAll:
                ab.requestAll(node);
                break;
        }
    }
}
