package org.acme.quickstart;

import io.fabric8.kubernetes.api.model.batch.v1.CronJob;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobSpec;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kubernetes.KubernetesConstants;



public class ConfigMapToJobRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        from("timer:cronJobTimer?period=30000") // Esegue ogni 30 secondi
                .setHeader(KubernetesConstants.KUBERNETES_NAMESPACE_NAME, constant("camel-rotta"))
                .setHeader(KubernetesConstants.KUBERNETES_CRON_JOB_NAME, constant("camel-cronjob"))
                .to("kubernetes-cronjob://kubernetes.default.svc?operation=getCronJob")
                .process(exchange -> {
                    CronJob cronJob = exchange.getMessage().getBody(CronJob.class);
                    if (cronJob != null) {
                        JobSpec jobSpec = cronJob.getSpec().getJobTemplate().getSpec();
                        Job job = new Job();
                        job.setSpec(jobSpec);
                        job.getMetadata().setName("manual-trigger-" + System.currentTimeMillis());
                        exchange.getMessage().setBody(job);
                    } else {
                        throw new RuntimeException("CronJob non trovato");
                    }
                })
                .to("kubernetes-job://kubernetes.default.svc?operation=createJob")
                .log("Esecuzione manuale del CronJob '${header." + KubernetesConstants.KUBERNETES_CRON_JOB_NAME + "}' completata.");
    }


}

