package org.acme.quickstart;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.model.batch.v1.JobSpec;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kubernetes.KubernetesConstants;
import org.apache.camel.component.kubernetes.KubernetesOperations;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;

public class TriggerCronJobRoute extends RouteBuilder {

    @Override
    public void configure() {
        from("timer:cronJobTimer?period=30000")
                .routeId("kubernetes-job-trigger")
                .process(exchange -> {
                    // Set Job metadata
                    exchange.getIn().setHeader(KubernetesConstants.KUBERNETES_JOB_NAME, "manual-trigger-" + System.currentTimeMillis());
                    exchange.getIn().setHeader(KubernetesConstants.KUBERNETES_NAMESPACE_NAME, "camel-rotta");

                    // Add labels for the Job
                    Map<String, String> jobLabels = new HashMap<>();
                    jobLabels.put("app", "jobFromCamelApp");
                    exchange.getIn().setHeader(KubernetesConstants.KUBERNETES_JOB_LABELS, jobLabels);

                    // Generate the JobSpec based on a CronJob
                    exchange.getIn().setHeader(KubernetesConstants.KUBERNETES_JOB_SPEC, generateJobSpec());
                })
                .toF("kubernetes-job://kubernetes.default.svc?operation=" + KubernetesOperations.CREATE_JOB_OPERATION)
                .log("Job created successfully.")
                .process(exchange -> {
                    System.out.println("Response: " + exchange.getIn().getBody());
                });
    }

    private JobSpec generateJobSpec() {
        JobSpec jobSpec = new JobSpec();

        PodTemplateSpec podTemplateSpec = new PodTemplateSpec();
        PodSpec podSpec = new PodSpec();
        podSpec.setRestartPolicy("Never");

        // Create container details
        podSpec.setContainers(generateContainers());

        ObjectMeta metadata = new ObjectMeta();
        Map<String, String> podLabels = new HashMap<>();
        podLabels.put("app", "podFromCamelApp");
        metadata.setLabels(podLabels);

        podTemplateSpec.setMetadata(metadata);
        podTemplateSpec.setSpec(podSpec);

        jobSpec.setTemplate(podTemplateSpec);
        return jobSpec;
    }

    private List<Container> generateContainers() {
        Container container = new Container();
        container.setName("manual-trigger-container");
        container.setImage("perl");

        List<String> command = new ArrayList<>();
        command.add("echo");
        command.add("Job triggered manually at: " + new Date());
        container.setCommand(command);

        List<Container> containers = new ArrayList<>();
        containers.add(container);
        return containers;
    }
}
